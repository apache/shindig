/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import org.apache.shindig.util.InputStreamConsumer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a {@code RemoteObjectFetcher} using standard java.net
 * classes. Only supports HTTP fetching at present.
 */
public class BasicRemoteContentFetcher implements RemoteContentFetcher {
  private static final int CONNECT_TIMEOUT_MS = 5000;

  private final int maxObjSize;

  /**
   * Creates a new fetcher capable of retrieving objects {@code maxObjSize}
   * bytes or smaller in size.
   * @param maxObjSize Maximum size, in bytes, of object to fetch
   */
  public BasicRemoteContentFetcher(int maxObjSize) {
    this.maxObjSize = maxObjSize;
  }

  /**
   * Initializes the connection.
   *
   * @param request
   * @param options
   * @return The opened connection
   * @throws IOException
   */
  private HttpURLConnection getConnection(RemoteContentRequest request,
      ProcessingOptions options) throws IOException {
    HttpURLConnection fetcher;
    fetcher = (HttpURLConnection)request.getUri().toURL().openConnection();
    fetcher.setInstanceFollowRedirects(true);
    fetcher.setConnectTimeout(CONNECT_TIMEOUT_MS);
    Map<String, List<String>> reqHeaders = request.getAllHeaders();
    for (Map.Entry<String, List<String>> entry : reqHeaders.entrySet()) {
      List<String> value = entry.getValue();
      if (value.size() == 1) {
        fetcher.setRequestProperty(entry.getKey(), value.get(0));
      } else {
        StringBuilder headerList = new StringBuilder();
        boolean first = false;
        for (String val : value) {
          if (!first) {
            first = true;
          } else {
            headerList.append(",");
          }
          headerList.append(val);
        }
        fetcher.setRequestProperty(entry.getKey(), headerList.toString());
      }
    }
    fetcher.setDefaultUseCaches(!options.getIgnoreCache());
    return fetcher;
  }

  /**
   * @param fetcher
   * @return A RemoteContent object made by consuming the response of the
   *     given HttpURLConnection.
   */
  private RemoteContent makeResponse(HttpURLConnection fetcher)
      throws IOException {
    Map<String, List<String>> headers = fetcher.getHeaderFields();
    int responseCode = fetcher.getResponseCode();
    byte[] body = InputStreamConsumer.readToByteArray(
        fetcher.getInputStream(), maxObjSize);
    return new RemoteContent(responseCode, body, headers);
  }

  /** {@inheritDoc} */
  public RemoteContent fetch(RemoteContentRequest request,
                             ProcessingOptions options) {
    try {
      return makeResponse(getConnection(request, options));
    } catch (IOException e) {
      return RemoteContent.ERROR;
    }
  }

  public RemoteContent fetchByPost(RemoteContentRequest request,
                                   ProcessingOptions options) {
    try {
      HttpURLConnection fetcher = getConnection(request, options);
      fetcher.setRequestMethod("POST");
      fetcher.setRequestProperty("Content-Length",
                                 String.valueOf(request.getPostBodyLength()));
      fetcher.setUseCaches(false);
      fetcher.setDoInput(true);
      fetcher.setDoOutput(true);
      InputStreamConsumer.pipe(request.getPostBody(),
                               fetcher.getOutputStream());
      return makeResponse(fetcher);
    } catch (IOException e) {
      return RemoteContent.ERROR;
    }
  }
}

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a {@code RemoteObjectFetcher} using standard java.net
 * classes.
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
   * @return The opened connection
   * @throws IOException
   */
  private URLConnection getConnection(RemoteContentRequest request)
      throws IOException {
    URLConnection fetcher;
    fetcher = request.getUri().toURL().openConnection();
    fetcher.setConnectTimeout(CONNECT_TIMEOUT_MS);
    if (fetcher instanceof HttpURLConnection) {
      ((HttpURLConnection)fetcher).setInstanceFollowRedirects(true);
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
              headerList.append(',');
            }
            headerList.append(val);
          }
          fetcher.setRequestProperty(entry.getKey(), headerList.toString());
        }
      }
    }
    fetcher.setDefaultUseCaches(!request.getOptions().ignoreCache);
    return fetcher;
  }

  /**
   * @param fetcher
   * @return A RemoteContent object made by consuming the response of the
   *     given HttpURLConnection.
   */
  private RemoteContent makeResponse(URLConnection fetcher)
      throws IOException {
    Map<String, List<String>> headers = fetcher.getHeaderFields();
    int responseCode;
    if (fetcher instanceof HttpURLConnection) {
      responseCode = ((HttpURLConnection)fetcher).getResponseCode();
    } else {
      responseCode = RemoteContent.SC_OK;
    }
    byte[] body = InputStreamConsumer.readToByteArray(
        fetcher.getInputStream(), maxObjSize);
    return new RemoteContent(responseCode, body, headers);
  }

  /** {@inheritDoc} */
  public RemoteContent fetch(RemoteContentRequest request) {
    try {
      URLConnection fetcher = getConnection(request);
      if ("POST".equals(request.getMethod()) &&
          fetcher instanceof HttpURLConnection) {
        ((HttpURLConnection)fetcher).setRequestMethod("POST");
        fetcher.setRequestProperty("Content-Length",
                                   String.valueOf(request.getPostBodyLength()));
        fetcher.setUseCaches(false);
        fetcher.setDoInput(true);
        fetcher.setDoOutput(true);
        InputStreamConsumer.pipe(request.getPostBody(),
                                 fetcher.getOutputStream());
      }
      return makeResponse(fetcher);
    } catch (IOException e) {
      if (e instanceof FileNotFoundException) {
        return RemoteContent.NOT_FOUND;
      }
      return RemoteContent.ERROR;
    }
  }
}

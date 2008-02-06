/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

  /** {@inheritDoc} */
  public RemoteContent fetch(URL url, ProcessingOptions options) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    int responseCode;
    HttpURLConnection fetcher;
    Map<String, List<String>> headers = null;

    try {
      fetcher = (HttpURLConnection) url.openConnection();
      fetcher.setInstanceFollowRedirects(true);
      fetcher.setConnectTimeout(CONNECT_TIMEOUT_MS);

      responseCode = fetcher.getResponseCode();
      headers = fetcher.getHeaderFields();

      byte chunk[] = new byte[8192];
      int chunkSize;
      InputStream in = fetcher.getInputStream();
      while (out.size() < maxObjSize && (chunkSize = in.read(chunk)) != -1) {
        out.write(chunk, 0, chunkSize);
      }
    } catch (IOException e) {
      responseCode = 500;
    }

    return new RemoteContent(responseCode, out.toByteArray(), headers);
  }

  public RemoteContent fetchByPost(URL url, byte[] postData,
      ProcessingOptions options) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    int responseCode;
    HttpURLConnection fetcher;
    Map<String, List<String>> headers = null;

    try {
      fetcher = (HttpURLConnection) url.openConnection();
      fetcher.setRequestMethod("POST");
      fetcher.setInstanceFollowRedirects(true);
      fetcher.setConnectTimeout(CONNECT_TIMEOUT_MS);
      fetcher.setRequestProperty("Content-Length", String.valueOf(postData.length));
      fetcher.setUseCaches(false);
      fetcher.setDoInput(true);
      fetcher.setDoOutput(true);
      fetcher.getOutputStream().write(postData);

      responseCode = fetcher.getResponseCode();
      headers = fetcher.getHeaderFields();

      byte chunk[] = new byte[8192];
      int chunkSize;
      InputStream in = fetcher.getInputStream();
      while (out.size() < maxObjSize && (chunkSize = in.read(chunk)) != -1) {
        out.write(chunk, 0, chunkSize);
      }
    } catch (IOException e) {
      responseCode = 500;
    }

    return new RemoteContent(responseCode, out.toByteArray(), headers);
  }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import junit.framework.TestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;

public class BasicHttpFetcherTest extends TestCase {
  private HttpCache cache = new BasicHttpCache();
  private HttpFetcher fetcher
      = new BasicHttpFetcher(cache, Integer.MAX_VALUE);

  public void testFetch() throws Exception {
    String content = "Hello, world!";
    File temp = File.createTempFile(this.getName(), ".txt");
    temp.deleteOnExit();
    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
    out.write(content);
    out.close();
    HttpRequest request = new HttpRequest(temp.toURI());
    HttpResponse response = fetcher.fetch(request);
    assertEquals(HttpResponse.SC_OK, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
  }

  public void testNotExists() throws Exception {
    HttpRequest request
        = new HttpRequest(new URI("file:///does/not/exist"));
    HttpResponse response = fetcher.fetch(request);
    assertEquals(HttpResponse.SC_NOT_FOUND, response.getHttpStatusCode());
  }

  // TODO simulate fake POST requests, headers, options, etc.
}
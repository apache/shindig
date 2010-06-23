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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class BasicHttpFetcherTest extends AbstractHttpFetcherTest {
  @Before
  public void setUp() throws Exception {
    fetcher = new BasicHttpFetcher(null);
  }

  @Test
  public void testWithProxy() throws Exception {
    fetcher = new BasicHttpFetcher(BASE_URL.getAuthority());

    String content = "Hello, Gagan!";
    Uri uri = new UriBuilder(Uri.parse("http://www.google.com/search"))
        .addQueryParameter("body", content)
        .addQueryParameter("status", "201")
        .toUri();
    HttpRequest request = new HttpRequest(uri);
    HttpResponse response = fetcher.fetch(request);
    assertEquals(201, response.getHttpStatusCode());
    assertEquals(content, response.getResponseAsString());
  }
}

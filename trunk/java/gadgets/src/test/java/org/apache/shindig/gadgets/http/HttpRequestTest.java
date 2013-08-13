/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.gadgets.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Maps;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.oauth.OAuthArguments;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HttpRequestTest {
  private static final String POST_BODY = "Hello, world!";
  private static final String CONTENT_TYPE = "text/plain";
  private static final String TEST_HEADER_KEY = "X-Test-Header";
  private static final String TEST_HEADER_VALUE = "Hello!";
  private static final String TEST_HEADER_VALUE2 = "Goodbye.";
  private static final Uri DEFAULT_URI = Uri.parse("http://example.org/");

  @Test
  public void dosPreventionHeaderAdded() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    assertNotNull("DoS prevention header not present in request.",
        request.getHeader(HttpRequest.DOS_PREVENTION_HEADER));
  }

  @Test
  public void postBodyCopied() throws Exception {
    HttpRequest request  = new HttpRequest(DEFAULT_URI).setPostBody(POST_BODY.getBytes());
    assertEquals(POST_BODY.length(), request.getPostBodyLength());
    assertEquals(POST_BODY, IOUtils.toString(request.getPostBody(), "UTF-8"));
    assertEquals(POST_BODY, request.getPostBodyAsString());
  }

  @Test
  public void contentTypeExtraction() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .addHeader("Content-Type", CONTENT_TYPE);
    assertEquals(CONTENT_TYPE, request.getContentType());
  }

  @Test
  public void getHeader() throws Exception {
    Map<String, List<String>> headers = Maps.newHashMap();
    headers.put(TEST_HEADER_KEY, Arrays.asList(TEST_HEADER_VALUE));
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .addHeader(TEST_HEADER_KEY, TEST_HEADER_VALUE);
    assertEquals(TEST_HEADER_VALUE, request.getHeader(TEST_HEADER_KEY));
  }

  @Test
  public void getHeaders() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .addHeader(TEST_HEADER_KEY, TEST_HEADER_VALUE)
        .addHeader(TEST_HEADER_KEY, TEST_HEADER_VALUE2);

    Collection<String> expected = Arrays.asList(TEST_HEADER_VALUE, TEST_HEADER_VALUE2);
    assertTrue(request.getHeaders(TEST_HEADER_KEY).containsAll(expected));
  }

  @Test
  public void ignoreCacheAddsPragmaHeader() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setIgnoreCache(true);

    assertTrue("Pragma: no-cache not added when ignoreCache == true",
        request.getHeaders("Pragma").contains("no-cache"));
  }

  @Test
  public void testDefaultIsFollowRedirects() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    assertTrue(request.getFollowRedirects());
  }

  @Test
  public void copyCtorCopiesAllFields() {
    OAuthArguments oauthArguments = new OAuthArguments();
    oauthArguments.setSignOwner(false);
    oauthArguments.setSignViewer(true);
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setCacheTtl(100)
        .addHeader(TEST_HEADER_KEY, TEST_HEADER_VALUE)
        .setContainer("container")
        .setGadget(DEFAULT_URI)
        .setMethod("POST")
        .setPostBody(POST_BODY.getBytes())
        .setRewriteMimeType("text/fake")
        .setSecurityToken(new AnonymousSecurityToken())
        .setOAuthArguments(oauthArguments)
        .setAuthType(AuthType.OAUTH)
        .setFollowRedirects(false)
        .setInternalRequest(true);

    HttpRequest request2 = new HttpRequest(request).setUri(Uri.parse("http://example.org/foo"));

    assertEquals(request.getCacheTtl(), request2.getCacheTtl());
    assertEquals(request.getHeaders(), request2.getHeaders());
    assertEquals(request.getContainer(), request2.getContainer());
    assertEquals(request.getGadget(), request2.getGadget());
    assertEquals(request.getMethod(), request2.getMethod());
    assertEquals(request.getPostBodyAsString(), request2.getPostBodyAsString());
    assertEquals(request.getRewriteMimeType(), request2.getRewriteMimeType());
    assertEquals(request.getSecurityToken(), request2.getSecurityToken());
    assertEquals(request.getOAuthArguments().getSignOwner(),
        request2.getOAuthArguments().getSignOwner());
    assertEquals(request.getOAuthArguments().getSignViewer(),
        request2.getOAuthArguments().getSignViewer());
    assertEquals(AuthType.OAUTH, request.getAuthType());
    assertFalse(request.getFollowRedirects());
    assertTrue(request.isInternalRequest());
  }
}

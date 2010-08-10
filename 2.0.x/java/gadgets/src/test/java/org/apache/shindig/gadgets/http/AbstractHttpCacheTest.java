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
package org.apache.shindig.gadgets.http;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.Map;

public class AbstractHttpCacheTest {
  protected static final Uri DEFAULT_URI = Uri.parse("http://example.org/file.txt");
  private static final Uri APP_URI = Uri.parse("http://example.org/gadget.xml");
  private static final String MODULE_ID = "100";
  private static final String SERVICE_NAME = "service";
  private static final String TOKEN_NAME = "token";
  private static final String CONTAINER_NAME = "container";

  private final TestHttpCache cache = new TestHttpCache();

  @Test
  public void createKeySimple() {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);
    CacheKeyBuilder key = new CacheKeyBuilder()
        .setLegacyParam(0, DEFAULT_URI).setLegacyParam(1, AuthType.NONE);

    assertEquals(key.build(), cache.createKey(request));
  }

  @Test
  public void createKeySignedOwner() throws Exception {
    // Using a mock instead of a fake object makes the test less brittle if the interface should
    // change.
    RequestAuthenticationInfo authInfo = newMockAuthInfo(
        true /* isSignOwner */,
        false /* isSignViewer */,
        ImmutableMap.of("OAUTH_SERVICE_NAME", SERVICE_NAME, "OAUTH_TOKEN_NAME", TOKEN_NAME));
    replay(authInfo);

    String ownerId = "owner eye dee";
    SecurityToken securityToken = new BasicSecurityToken(ownerId, "", "", "",
        APP_URI.toString(), MODULE_ID, CONTAINER_NAME, null, null);

    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setAuthType(AuthType.SIGNED)
        .setOAuthArguments(new OAuthArguments(authInfo))
        .setSecurityToken(securityToken);

    CacheKeyBuilder key = new CacheKeyBuilder()
        .setLegacyParam(0, DEFAULT_URI)
        .setLegacyParam(1, AuthType.SIGNED)
        .setLegacyParam(2, ownerId)
        .setLegacyParam(3, "")
        .setLegacyParam(5, APP_URI)
        .setLegacyParam(6, MODULE_ID)
        .setLegacyParam(7, SERVICE_NAME)
        .setLegacyParam(8, TOKEN_NAME);

    assertEquals(key.build(), cache.createKey(request));
  }

  private RequestAuthenticationInfo newMockAuthInfo(boolean isSignOwner, boolean isSignViewer,
      Map<String, String> attributesMap) {
    RequestAuthenticationInfo authInfo = EasyMock.createNiceMock(RequestAuthenticationInfo.class);
    expect(authInfo.getAttributes()).andReturn(attributesMap).anyTimes();
    expect(authInfo.getAuthType()).andReturn(AuthType.SIGNED).anyTimes();
    expect(authInfo.getHref()).andReturn(DEFAULT_URI).anyTimes();
    expect(authInfo.isSignOwner()).andReturn(isSignOwner).anyTimes();
    expect(authInfo.isSignViewer()).andReturn(isSignOwner).anyTimes();
    return authInfo;
  }

  @Test
  public void createKeySignedViewer() throws Exception {
    RequestAuthenticationInfo authInfo = newMockAuthInfo(
        false /* isSignOwner */,
        true /* isSignViewer */,
        ImmutableMap.of("OAUTH_SERVICE_NAME", SERVICE_NAME, "OAUTH_TOKEN_NAME", TOKEN_NAME));
    replay(authInfo);

    String viewerId = "viewer eye dee";
    SecurityToken securityToken = new BasicSecurityToken(
        "", viewerId, "", "", APP_URI.toString(), MODULE_ID, CONTAINER_NAME, null, null);

    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setAuthType(AuthType.SIGNED)
        .setOAuthArguments(new OAuthArguments(authInfo))
        .setSecurityToken(securityToken);

    CacheKeyBuilder key = new CacheKeyBuilder()
        .setLegacyParam(0, DEFAULT_URI)
        .setLegacyParam(1, AuthType.SIGNED)
        .setLegacyParam(3, null) // The Viewer ID is in this case defaults to null
        .setLegacyParam(5, APP_URI)
        .setLegacyParam(6, MODULE_ID)
        .setLegacyParam(7, SERVICE_NAME)
        .setLegacyParam(8, TOKEN_NAME);

    assertEquals(key.build(), cache.createKey(request));
  }

  @Test
  public void createKeyWithTokenOwner() throws Exception {
    RequestAuthenticationInfo authInfo = newMockAuthInfo(
        true /* isSignOwner */,
        true /* isSignViewer */,
        ImmutableMap.of("OAUTH_SERVICE_NAME", SERVICE_NAME, "OAUTH_TOKEN_NAME", TOKEN_NAME,
            "OAUTH_USE_TOKEN", "always"));
    replay(authInfo);

    String userId = "user id";
    SecurityToken securityToken = new BasicSecurityToken(
        userId, userId, "", "", APP_URI.toString(), MODULE_ID, CONTAINER_NAME, null, null);

    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setAuthType(AuthType.SIGNED)
        .setOAuthArguments(new OAuthArguments(authInfo))
        .setSecurityToken(securityToken);

    CacheKeyBuilder key = new CacheKeyBuilder()
        .setLegacyParam(0, DEFAULT_URI)
        .setLegacyParam(1, AuthType.SIGNED)
        .setLegacyParam(2, userId)
        .setLegacyParam(3, userId)
        .setLegacyParam(4, userId)
        .setLegacyParam(5, APP_URI)
        .setLegacyParam(6, MODULE_ID)
        .setLegacyParam(7, SERVICE_NAME)
        .setLegacyParam(8, TOKEN_NAME);

    assertEquals(key.build(), cache.createKey(request));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createKeyWithoutSecurityToken() throws Exception {
    RequestAuthenticationInfo authInfo = newMockAuthInfo(
        true /* isSignOwner */,
        false /* isSignViewer */,
        ImmutableMap.<String, String>of());
    replay(authInfo);
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setAuthType(AuthType.SIGNED)
        .setOAuthArguments(new OAuthArguments(authInfo));
    cache.createKey(request);
  }


  @Test
  public void getResponse() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");
    cache.map.put(key, response);

    assertEquals(response, cache.getResponse(request));
  }

  @Test
  public void getResponseUsingPost() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setMethod("POST");
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");
    cache.map.put(key, response);

    assertNull("Did not return null when method was POST", cache.getResponse(request));
  }

  @Test
  public void getResponseUsingMethodOverride() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setMethod("POST")
        .addHeader("X-Method-Override", "GET");
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");
    cache.map.put(key, response);

    assertEquals(response, cache.getResponse(request));
  }

  @Test
  public void getResponseIgnoreCache() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");
    cache.map.put(key, response);

    request.setIgnoreCache(true);

    assertNull("Did not return null when ignoreCache was true", cache.getResponse(request));
  }

  @Test
  public void getResponseNotCacheable() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    HttpResponse response = new HttpResponseBuilder().setStrictNoCache().create();
    cache.addResponse(request, response);

    assertNull("Did not return null when response was uncacheable", cache.getResponse(request));
  }

  @Test
  public void addResponse() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    HttpResponse response = new HttpResponse("normal");
    String key = cache.createKey(request);

    assertTrue("response should have been cached", cache.addResponse(request, response));

    assertEquals(response, cache.map.get(key));
  }

  @Test
  public void addResponseIgnoreCache() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setIgnoreCache(true);
    HttpResponse response = new HttpResponse("does not matter");

    assertFalse("response should not have been cached", cache.addResponse(request, response));

    assertEquals(0, cache.map.size());
  }

  @Test
  public void addResponseNotCacheable() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    HttpResponse response = new HttpResponseBuilder().setStrictNoCache().create();
    assertFalse(cache.addResponse(request, response));

    assertEquals(0, cache.map.size());
  }
  
  @Test
  public void addResponseIfModifiedSince() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    HttpResponse response = new HttpResponseBuilder().setHttpStatusCode(HttpResponse.SC_NOT_MODIFIED).create();
    assertFalse(cache.addResponse(request, response));

    assertEquals(0, cache.map.size());
  }

  @Test
  public void addResponseUsingPost() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setMethod("POST");
    HttpResponse response = new HttpResponse("does not matter");
    assertFalse(cache.addResponse(request, response));

    assertEquals(0, cache.map.size());
  }

  @Test
  public void addResponseUsingMethodOverride() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setMethod("POST")
        .addHeader("X-Method-Override", "GET");
    HttpResponse response = new HttpResponse("normal");
    String key = cache.createKey(request);

    assertTrue(cache.addResponse(request, response));

    assertEquals(response, cache.map.get(key));
  }

  @Test
  public void addResponseWithForcedTtl() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setCacheTtl(10);

    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");

    assertTrue(cache.addResponse(request, response));

    assertEquals("public,max-age=10", cache.map.get(key).getHeader("Cache-Control"));
  }

  @Test
  public void addResponseWithForcedTtlAndStrictNoCache() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setCacheTtl(10);

    String key = cache.createKey(request);
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("result")
        .setStrictNoCache()
        .create();

    assertTrue(cache.addResponse(request, response));

    assertEquals("public,max-age=10", cache.map.get(key).getHeader("Cache-Control"));
  }

  @Test
  public void addResponseWithNoCachingHeaders() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);

    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("no headers");

    assertTrue(cache.addResponse(request, response));

    assertEquals("no headers", cache.map.get(key).getResponseAsString());
  }

  @Test
  public void removeResponse() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");
    cache.map.put(key, response);

    assertEquals(response, cache.removeResponse(request));
    assertEquals(0, cache.map.size());
  }

  @Test
  public void removeResponseIsStaled() {
    long expiration = System.currentTimeMillis() + 1000L;
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponseBuilder()
        .setExpirationTime(expiration)
        .create();
    cache.map.put(key, response);

    // The cache itself still hold and return staled value, 
    // caller responsible to decide what to do about it 
    assertEquals(response, cache.removeResponse(request));
    assertEquals(0, cache.map.size());
  }

  private static class TestHttpCache extends AbstractHttpCache {
    protected final Map<String, HttpResponse> map;

    public TestHttpCache() {
      map = Maps.newHashMap();
    }

    @Override
    public void addResponseImpl(String key, HttpResponse response) {
      map.put(key, response);
    }

    @Override
    public HttpResponse getResponseImpl(String key) {
      return map.get(key);
    }

    @Override
    public HttpResponse removeResponseImpl(String key) {
      return map.remove(key);
    }
  }
}

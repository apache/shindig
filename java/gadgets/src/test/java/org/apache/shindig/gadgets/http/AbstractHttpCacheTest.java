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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class AbstractHttpCacheTest {
  protected static final Uri DEFAULT_URI = Uri.parse("http://example.org/file.txt");
  protected static final Uri IMAGE_URI = Uri.parse("http://example.org/image.png");
  private static final Uri APP_URI = Uri.parse("http://example.org/gadget.xml");
  private static final String MODULE_ID = "100";
  private static final String SERVICE_NAME = "service";
  private static final String TOKEN_NAME = "token";
  private static final String CONTAINER_NAME = "container";

  private final TestHttpCache cache = new TestHttpCache();
  // Cache designed to return 86400ms for refetchStrictNoCacheAfterMs.
  private TestHttpCache extendedStrictNoCacheTtlCache;

  @Before
  public void setUp() {
    extendedStrictNoCacheTtlCache = new TestHttpCache();
    extendedStrictNoCacheTtlCache.setRefetchStrictNoCacheAfterMs(86400L);
  }

  @Test
  public void createKeySimple() {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);
    CacheKeyBuilder key = new CacheKeyBuilder()
        .setLegacyParam(0, DEFAULT_URI).setLegacyParam(1, AuthType.NONE);

    assertEquals(key.build(), cache.createKey(request));
  }

  private HttpRequest getMockImageRequest(String height, String width, String quality,
      boolean noExpand, String mimeType, String ua) {
    HttpRequest request = EasyMock.createMock(HttpRequest.class);
    expect(request.getUri()).andReturn(IMAGE_URI).anyTimes();
    expect(request.getAuthType()).andReturn(AuthType.NONE).anyTimes();
    expect(request.getSecurityToken()).andReturn(null).anyTimes();
    expect(request.getParam(Param.RESIZE_HEIGHT.getKey())).andReturn(height).anyTimes();
    expect(request.getParam(Param.RESIZE_WIDTH.getKey())).andReturn(width).anyTimes();
    expect(request.getParam(Param.RESIZE_QUALITY.getKey())).andReturn(quality).anyTimes();
    expect(request.getParam(Param.NO_EXPAND.getKey())).andReturn(noExpand ? "1" : null).anyTimes();
    expect(request.getRewriteMimeType()).andReturn(mimeType).anyTimes();
    expect(request.getHeader("User-Agent")).andReturn(ua).anyTimes();
    replay(request);
    return request;
  }

  @Test
  public void createKeySimpleImageRequest() throws Exception {
    // Mock the Request with Image Resize (Quality) params, without rewrite mimeType.
    HttpRequest request = getMockImageRequest("100", "80", "70", false, null, "Mozilla");
    CacheKeyBuilder key = new CacheKeyBuilder()
        .setLegacyParam(0, IMAGE_URI)
        .setLegacyParam(1, AuthType.NONE)
        .setParam("rh", "100")
        .setParam("rw", "80")
        .setParam("rq", "70")
        .setParam("ua", "Mozilla");

    assertEquals(key.build(), cache.createKey(request));
  }

  @Test
  public void createKeyImageRequestRewrite() throws Exception {
    // Mock the Request with Image Resize (Quality) params and specified rewrite mimeType.
    HttpRequest request = getMockImageRequest("100", "80", "70", true, "image/jpg", "Mozilla");
    CacheKeyBuilder key = new CacheKeyBuilder()
        .setLegacyParam(0, IMAGE_URI)
        .setLegacyParam(1, AuthType.NONE)
        .setParam("rh", "100")
        .setParam("rw", "80")
        .setParam("rq", "70")
        .setParam("ne", "1")
        .setParam("rm", "image/jpg")
        .setParam("ua", "Mozilla");

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

    extendedStrictNoCacheTtlCache.map.put(key, response);
    assertEquals(response, extendedStrictNoCacheTtlCache.getResponse(request));
  }

  @Test
  public void getResponseUsingPost() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setMethod("POST");
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");
    cache.map.put(key, response);

    assertNull("Did not return null when method was POST", cache.getResponse(request));

    extendedStrictNoCacheTtlCache.map.put(key, response);
    assertNull("Did not return null when method was POST",
               extendedStrictNoCacheTtlCache.getResponse(request));
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

    extendedStrictNoCacheTtlCache.map.put(key, response);
    assertEquals(response, extendedStrictNoCacheTtlCache.getResponse(request));
  }

  @Test
  public void getResponseIgnoreCache() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");
    cache.map.put(key, response);

    request.setIgnoreCache(true);

    assertNull("Did not return null when ignoreCache was true", cache.getResponse(request));

    extendedStrictNoCacheTtlCache.map.put(key, response);
    assertNull("Did not return null when ignoreCache was true",
               extendedStrictNoCacheTtlCache.getResponse(request));
  }

  @Test
  public void getResponseNotCacheable() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponseBuilder().setStrictNoCache().create();
    cache.map.put(key, response);

    assertNull("Did not return null when response was uncacheable", cache.getResponse(request));

    extendedStrictNoCacheTtlCache.map.put(key, response);
    assertEquals(response, extendedStrictNoCacheTtlCache.getResponse(request));
  }

  @Test
  public void addResponse() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    HttpResponse response = new HttpResponse("normal");
    String key = cache.createKey(request);

    assertNotNull("response should have been cached", cache.addResponse(request, response));
    assertEquals(response, cache.map.get(key));

    assertNotNull("response should have been cached",
               extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals(response, extendedStrictNoCacheTtlCache.map.get(key));
  }

  @Test
  public void addResponseIgnoreCache() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setIgnoreCache(true);
    HttpResponse response = new HttpResponse("does not matter");

    assertNull("response should not have been cached", cache.addResponse(request, response));
    assertEquals(0, cache.map.size());

    assertNull("response should not have been cached",
                extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals(0, extendedStrictNoCacheTtlCache.map.size());
  }

  @Test
  public void addResponseNotCacheable() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    HttpResponse response = new HttpResponseBuilder().setStrictNoCache().create();
    String key = cache.createKey(request);

    assertNull(cache.addResponse(request, response));
    assertEquals(0, cache.map.size());

    assertNotNull("response should have been cached",
               extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals(
        extendedStrictNoCacheTtlCache.buildStrictNoCacheHttpResponse(response).create(),
        extendedStrictNoCacheTtlCache.map.get(key));
  }

  @Test
  public void addResponseIfModifiedSince() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    HttpResponse response = new HttpResponseBuilder().setHttpStatusCode(HttpResponse.SC_NOT_MODIFIED).create();

    assertNull(cache.addResponse(request, response));
    assertEquals(0, cache.map.size());

    assertNull(extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals(0, extendedStrictNoCacheTtlCache.map.size());
  }

  @Test
  public void addResponseUsingPost() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setMethod("POST");
    HttpResponse response = new HttpResponse("does not matter");

    assertNull(cache.addResponse(request, response));
    assertEquals(0, cache.map.size());

    assertNull(extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals(0, extendedStrictNoCacheTtlCache.map.size());
  }

  @Test
  public void addResponseUsingMethodOverride() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setMethod("POST")
        .addHeader("X-Method-Override", "GET");
    HttpResponse response = new HttpResponse("normal");
    String key = cache.createKey(request);

    assertNotNull(cache.addResponse(request, response));
    assertEquals(response, cache.map.get(key));

    assertNotNull(extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals(response, extendedStrictNoCacheTtlCache.map.get(key));
  }

  @Test
  public void addResponseWithForcedTtl() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setCacheTtl(10);

    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");

    assertNotNull(cache.addResponse(request, response));

    assertEquals("public,max-age=10", cache.map.get(key).getHeader("Cache-Control"));

    assertNotNull(extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals("public,max-age=10",
                 extendedStrictNoCacheTtlCache.map.get(key).getHeader("Cache-Control"));
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

    assertNotNull(cache.addResponse(request, response));

    assertEquals("public,max-age=10", cache.map.get(key).getHeader("Cache-Control"));

    assertNotNull(extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals("public,max-age=10",
                 extendedStrictNoCacheTtlCache.map.get(key).getHeader("Cache-Control"));
  }

  @Test
  public void addResponseWithForcedTtlAndErrorResponse() {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setCacheTtl(10);

    String key = cache.createKey(request);
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("result")
        .setHttpStatusCode(500)
        .create();

    assertNotNull(cache.addResponse(request, response));

    assertNull(cache.map.get(key).getHeader("Cache-Control"));

    assertNotNull(extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertNull(extendedStrictNoCacheTtlCache.map.get(key).getHeader("Cache-Control"));
  }

  @Test
  public void addResponseWithNoCachingHeaders() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);

    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("no headers");

    assertNotNull(cache.addResponse(request, response));

    assertEquals("no headers", cache.map.get(key).getResponseAsString());

    assertNotNull(extendedStrictNoCacheTtlCache.addResponse(request, response));
    assertEquals("no headers", extendedStrictNoCacheTtlCache.map.get(key).getResponseAsString());
  }

  @Test
  public void buildStrictNoCacheHttpResponse() {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("result")
        .addHeader("Cache-Control", "private, max-age=1000")
        .addHeader("X-Method-Override", "GET")
        .create();
    assertTrue(response.isStrictNoCache());
    HttpResponse builtResponse = extendedStrictNoCacheTtlCache
        .buildStrictNoCacheHttpResponse(response).create();

    assertTrue(builtResponse.isStrictNoCache());
    assertEquals("", builtResponse.getResponseAsString());
    assertEquals("private, max-age=1000", builtResponse.getHeader("Cache-Control"));
    assertEquals(86400, builtResponse.getRefetchStrictNoCacheAfterMs());
    assertFalse(builtResponse.getHeaders().containsKey("Pragma"));
    assertNull(builtResponse.getHeader("X-Method-Override"));
  }

  @Test
  public void buildStrictNoCacheHttpResponseWithPragmaHeader() {
    HttpResponse response = new HttpResponseBuilder()
        .setResponseString("result")
        .addHeader("Pragma", "no-cache")
        .create();
    assertTrue(response.isStrictNoCache());
    HttpResponse builtResponse = cache
        .buildStrictNoCacheHttpResponse(response).create();

    assertTrue(builtResponse.isStrictNoCache());
    assertEquals("", builtResponse.getResponseAsString());
    assertNull(builtResponse.getHeader("Cache-Control"));
    assertEquals("no-cache", builtResponse.getHeader("Pragma"));
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
    public void removeResponseImpl(String key) {
      map.remove(key);
    }
  }
}

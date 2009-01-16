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

import static org.apache.shindig.gadgets.http.AbstractHttpCache.DEFAULT_KEY_VALUE;
import static org.apache.shindig.gadgets.http.AbstractHttpCache.KEY_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.auth.BasicSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.spec.RequestAuthenticationInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.Map;

public class AbstractHttpCacheTest {
  protected static final Uri DEFAULT_URI = Uri.parse("http://example.org/file.txt");
  private static final Uri APP_URI = Uri.parse("http://example.org/gadget.xml");
  private static final String MODULE_ID = "100";
  private static final String SERVICE_NAME = "service";
  private static final String TOKEN_NAME = "token";
  private final TestHttpCache cache = new TestHttpCache();

  @Test
  public void createKeySimple() {
    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setAuthType(AuthType.NONE);

    StringBuilder key = new StringBuilder();
    key.append(DEFAULT_URI);
    key.append(KEY_SEPARATOR);
    key.append(AuthType.NONE);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);

    String actual = cache.createKey(request);
    assertEquals(key.toString(), actual);
  }

  @Test
  public void createKeySignedOwner() throws Exception {
    OAuthArguments args = new OAuthArguments(new RequestAuthenticationInfo() {

      public Map<String, String> getAttributes() {
        return ImmutableMap.of("OAUTH_SERVICE_NAME", SERVICE_NAME,
                               "OAUTH_TOKEN_NAME", TOKEN_NAME);
      }

      public AuthType getAuthType() {
        return AuthType.SIGNED;
      }

      public Uri getHref() {
        return DEFAULT_URI;
      }

      public boolean isSignOwner() {
        return true;
      }

      public boolean isSignViewer() {
        return false;
      }
    });

    String ownerId = "owner eye dee";
    SecurityToken securityToken
        = new BasicSecurityToken(ownerId, "", "", "", APP_URI.toString(), MODULE_ID);

    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setAuthType(AuthType.SIGNED)
        .setOAuthArguments(args)
        .setSecurityToken(securityToken);

    StringBuilder key = new StringBuilder();
    key.append(DEFAULT_URI);
    key.append(KEY_SEPARATOR);
    key.append(AuthType.SIGNED);
    key.append(KEY_SEPARATOR);
    key.append(ownerId);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(APP_URI);
    key.append(KEY_SEPARATOR);
    key.append(MODULE_ID);
    key.append(KEY_SEPARATOR);
    key.append(SERVICE_NAME);
    key.append(KEY_SEPARATOR);
    key.append(TOKEN_NAME);

    String actual = cache.createKey(request);
    assertEquals(key.toString(), actual);
  }

  @Test
  public void createKeySignedViewer() throws Exception {
    OAuthArguments args = new OAuthArguments(new RequestAuthenticationInfo() {

      public Map<String, String> getAttributes() {
        return ImmutableMap.of("OAUTH_SERVICE_NAME", SERVICE_NAME,
                               "OAUTH_TOKEN_NAME", TOKEN_NAME);
      }

      public AuthType getAuthType() {
        return AuthType.SIGNED;
      }

      public Uri getHref() {
        return DEFAULT_URI;
      }

      public boolean isSignOwner() {
        return false;
      }

      public boolean isSignViewer() {
        return true;
      }
    });

    String viewerId = "viewer eye dee";
    SecurityToken securityToken
        = new BasicSecurityToken("", viewerId, "", "", APP_URI.toString(), MODULE_ID);

    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setAuthType(AuthType.SIGNED)
        .setOAuthArguments(args)
        .setSecurityToken(securityToken);

    StringBuilder key = new StringBuilder();
    key.append(DEFAULT_URI);
    key.append(KEY_SEPARATOR);
    key.append(AuthType.SIGNED);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(viewerId);
    key.append(KEY_SEPARATOR);
    key.append(DEFAULT_KEY_VALUE);
    key.append(KEY_SEPARATOR);
    key.append(APP_URI);
    key.append(KEY_SEPARATOR);
    key.append(MODULE_ID);
    key.append(KEY_SEPARATOR);
    key.append(SERVICE_NAME);
    key.append(KEY_SEPARATOR);
    key.append(TOKEN_NAME);

    String actual = cache.createKey(request);
    assertEquals(key.toString(), actual);
  }

  @Test
  public void createKeyWithTokenOwner() throws Exception {
    OAuthArguments args = new OAuthArguments(new RequestAuthenticationInfo() {

      public Map<String, String> getAttributes() {
        return ImmutableMap.of("OAUTH_SERVICE_NAME", SERVICE_NAME,
                               "OAUTH_TOKEN_NAME", TOKEN_NAME,
                               "OAUTH_USE_TOKEN", "always");
      }

      public AuthType getAuthType() {
        return AuthType.SIGNED;
      }

      public Uri getHref() {
        return DEFAULT_URI;
      }

      public boolean isSignOwner() {
        return true;
      }

      public boolean isSignViewer() {
        return true;
      }
    });

    String userId = "user id";
    SecurityToken securityToken
        = new BasicSecurityToken(userId, userId, "", "", APP_URI.toString(), MODULE_ID);

    HttpRequest request = new HttpRequest(DEFAULT_URI)
        .setAuthType(AuthType.SIGNED)
        .setOAuthArguments(args)
        .setSecurityToken(securityToken);

    StringBuilder key = new StringBuilder();
    key.append(DEFAULT_URI);
    key.append(KEY_SEPARATOR);
    key.append(AuthType.SIGNED);
    key.append(KEY_SEPARATOR);
    key.append(userId);
    key.append(KEY_SEPARATOR);
    key.append(userId);
    key.append(KEY_SEPARATOR);
    key.append(userId);
    key.append(KEY_SEPARATOR);
    key.append(APP_URI);
    key.append(KEY_SEPARATOR);
    key.append(MODULE_ID);
    key.append(KEY_SEPARATOR);
    key.append(SERVICE_NAME);
    key.append(KEY_SEPARATOR);
    key.append(TOKEN_NAME);

    String actual = cache.createKey(request);
    assertEquals(key.toString(), actual);
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
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponseBuilder().setStrictNoCache().create();
    cache.map.put(key, response);

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
  public void removeResponse() {
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponse("result");
    cache.map.put(key, response);

    assertEquals(response, cache.removeResponse(request));
    assertEquals(0, cache.map.size());
  }

  @Test
  public void removeResponseIsNoLongerUsable() {
    long expiration = System.currentTimeMillis() + 1000L;
    HttpRequest request = new HttpRequest(DEFAULT_URI);
    String key = cache.createKey(request);
    HttpResponse response = new HttpResponseBuilder()
        .setExpirationTime(expiration)
        .create();
    cache.map.put(key, response);

    TimeSource fakeClock = new FakeTimeSource(expiration - 60L);

    cache.setClock(fakeClock);

    assertNull("Returned an expired entry when removing from the cache.",
               cache.removeResponse(request));
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

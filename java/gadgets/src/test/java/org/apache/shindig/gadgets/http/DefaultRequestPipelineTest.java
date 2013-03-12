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
import static org.junit.Assert.assertSame;

import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import com.google.inject.Provider;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.DateUtil;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.oauth2.BasicOAuth2Request;
import org.apache.shindig.gadgets.oauth2.OAuth2Request;
import org.apache.shindig.gadgets.rewrite.DefaultResponseRewriterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class DefaultRequestPipelineTest {
  private static final Uri DEFAULT_URI = Uri.parse("http://example.org/gadget.xml");
  private static final String RFC1123_EPOCH = DateUtil.formatRfc1123Date(0);

  private final FakeHttpFetcher fetcher = new FakeHttpFetcher();
  private final FakeHttpCache cache = new FakeHttpCache();
  private final FakeOAuthRequestProvider oauth = new FakeOAuthRequestProvider();
  private final FakeOAuth2RequestProvider oauth2 = new FakeOAuth2RequestProvider();

  private final HttpResponseMetadataHelper helper = new HttpResponseMetadataHelper() {
    @Override
    public String getHash(HttpResponse resp) {
      return resp.getResponseAsString();
    }
  };
  private final RequestPipeline pipeline = new DefaultRequestPipeline(fetcher, cache, oauth,
          oauth2, new DefaultResponseRewriterRegistry(null, null), new NoOpInvalidationService(),
          helper);

  @Before
  public void setUp() {
    HttpResponseTest.setHttpTimeSource();
  }

  @Test
  public void authTypeNoneNotCached() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    fetcher.response = new HttpResponse("response");

    HttpResponse response = pipeline.execute(request);

    assertEquals(request, fetcher.request);
    assertEquals(fetcher.response, response);
    assertEquals(response, cache.data.get(DEFAULT_URI));
    assertEquals(1, cache.readCount);
    assertEquals(1, cache.writeCount);
    assertEquals(1, fetcher.fetchCount);
    assertEquals(1, response.getMetadata().size());
    assertEquals("response", response.getMetadata().get(HttpResponseMetadataHelper.DATA_HASH));
  }

  @Test
  public void verifyHashCode() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    int time = roundToSeconds(HttpResponseTest.timeSource.currentTimeMillis()) - 10;
    String date = DateUtil.formatRfc1123Date(1000L * time);
    HttpResponseBuilder builder = new HttpResponseBuilder().setCacheTtl(100)
            .addHeader("Date", date);
    builder.setContent("response");

    fetcher.response = builder.create();

    RequestPipeline pipeline = new DefaultRequestPipeline(fetcher, cache, oauth, oauth2,
            new DefaultResponseRewriterRegistry(null, null), new NoOpInvalidationService(),
            new HttpResponseMetadataHelper());
    HttpResponse response = pipeline.execute(request);
    assertEquals(1, response.getMetadata().size());
    assertEquals("q7u8tbpmidtu1gtqhjv0kb0rvo",
            response.getMetadata().get(HttpResponseMetadataHelper.DATA_HASH));
    assertEquals(date, response.getHeader("Date"));
    assertEquals(roundToSeconds(90000 - 1), roundToSeconds(response.getCacheTtl() - 1));
  }

  @Test
  public void verifyFixedDate() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    int time = roundToSeconds(HttpResponseTest.timeSource.currentTimeMillis());
    String date = DateUtil.formatRfc1123Date(1000L * time - 1000
            - DefaultRequestPipeline.DEFAULT_DRIFT_LIMIT_MS);
    HttpResponseBuilder builder = new HttpResponseBuilder().setCacheTtl(100)
            .addHeader("Date", date);
    builder.setContent("response");

    fetcher.response = builder.create();

    RequestPipeline pipeline = new DefaultRequestPipeline(fetcher, cache, oauth, oauth2,
            new DefaultResponseRewriterRegistry(null, null), new NoOpInvalidationService(),
            new HttpResponseMetadataHelper());
    HttpResponse response = pipeline.execute(request);
    // Verify time is current time instead of expired
    assertEquals(DateUtil.formatRfc1123Date(1000L * time), response.getHeader("Date"));
    assertEquals(roundToSeconds(100000 - 1), roundToSeconds(response.getCacheTtl() - 1));
  }

  @Test
  public void authTypeNoneWasCached() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    HttpResponse cached = new HttpResponse("cached");
    cache.data.put(DEFAULT_URI, cached);

    HttpResponse response = pipeline.execute(request);

    assertEquals(cached, response);
    assertEquals(1, cache.readCount);
    assertEquals(0, cache.writeCount);
    assertEquals(0, fetcher.fetchCount);
  }

  @Test
  public void authTypeNoneWasCachedButStale() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    HttpResponse cached = new HttpResponseBuilder().setStrictNoCache().create();
    cache.data.put(DEFAULT_URI, cached);

    HttpResponse fetched = new HttpResponse("fetched");
    fetcher.response = fetched;

    HttpResponse response = pipeline.execute(request);

    assertEquals(fetched, response);
    assertEquals(request, fetcher.request);
    assertEquals(fetched, cache.data.get(DEFAULT_URI));
    assertEquals(1, cache.readCount);
    assertEquals(1, cache.writeCount);
    assertEquals(1, fetcher.fetchCount);
    assertEquals(1, response.getMetadata().size());
    assertEquals("fetched", response.getMetadata().get(HttpResponseMetadataHelper.DATA_HASH));

  }

  @Test
  public void authTypeNoneStaleCachedServed() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    HttpResponse cached = new HttpResponseBuilder().setCacheTtl(-1).create();
    cache.data.put(DEFAULT_URI, cached);

    fetcher.response = HttpResponse.error();

    HttpResponse response = pipeline.execute(request);

    assertEquals(cached, response); // cached item is served instead of 500
    assertEquals(request, fetcher.request);
    assertEquals(1, cache.readCount);
    assertEquals(0, cache.writeCount);
    assertEquals(1, fetcher.fetchCount);
  }

  @Test
  public void authTypeNoneWasCachedErrorStale() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    HttpResponse cached = new HttpResponseBuilder().setCacheTtl(-1).setHttpStatusCode(401).create();
    cache.data.put(DEFAULT_URI, cached);

    HttpResponse fetched = HttpResponse.error();
    fetcher.response = fetched;

    HttpResponse response = pipeline.execute(request);

    assertEquals(fetched, response); // 500 served because cached is an error (401)
    assertEquals(request, fetcher.request);
    assertEquals(fetched, cache.data.get(DEFAULT_URI));
    assertEquals(1, cache.readCount);
    assertEquals(1, cache.writeCount);
    assertEquals(1, fetcher.fetchCount);
  }

  @Test
  public void authTypeNoneIgnoreCache() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE).setIgnoreCache(
            true);

    HttpResponse fetched = new HttpResponse("fetched");
    fetcher.response = fetched;

    HttpResponse response = pipeline.execute(request);

    assertEquals(fetched, response);
    assertEquals(request, fetcher.request);
    assertEquals(0, cache.readCount);
    assertEquals(0, cache.writeCount);
    assertEquals(1, fetcher.fetchCount);
  }

  @Test
  public void authTypeNoneStaleConditionalGet() throws Exception {
    // Cached response that is stale.  Test that a conditional GET is used.
    // Verify that the cached response is updated and returned.
    // Verify that the 304 doesn't get cached.
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    HttpResponse cached = new HttpResponseBuilder()
                                .setHeader(HttpHeaders.LAST_MODIFIED, RFC1123_EPOCH)
                                .setHeader(HttpHeaders.ETAG, "ETAG")
                                .setCacheTtl(-1)
                                .create();
    cache.data.put(DEFAULT_URI, cached);

    String expiresDate = DateUtil.formatRfc1123Date(System.currentTimeMillis() + 3600 * 1000);
    String maxAge = "max-age=3600";
    HttpResponse notModified = new HttpResponseBuilder()
                                    .setHttpStatusCode(HttpResponse.SC_NOT_MODIFIED)
                                    .setHeader(HttpHeaders.EXPIRES, expiresDate)
                                    .setHeader(HttpHeaders.CACHE_CONTROL, maxAge)
                                    .create();
    fetcher.response = notModified;
    HttpResponse response = pipeline.execute(request);

    HttpResponse expectedResponse = new HttpResponseBuilder(cached)
                                      .setHeader(HttpHeaders.EXPIRES, expiresDate)
                                      .setHeader(HttpHeaders.CACHE_CONTROL, maxAge)
                                      .create();

    assertEquals(RFC1123_EPOCH, fetcher.request.getHeader(HttpHeaders.IF_MODIFIED_SINCE));
    assertEquals("ETAG", fetcher.request.getHeader(HttpHeaders.IF_NONE_MATCH));
    assertEquals(expectedResponse, response);
    assertEquals(expectedResponse, cache.data.get(DEFAULT_URI));
    assertEquals(1, cache.readCount);
    assertEquals(1, cache.writeCount);
    assertEquals(1, fetcher.fetchCount);
  }

  @Test
  public void authTypeNoneStaleConditionalGetNoExpiresNoMaxAge() throws Exception {
    // Cached response that is stale and a conditional GET is used. Response has no Expires
    // header and no Cache-Control header with max-age. Remove the cached entry from the cache and
    // return it.
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    HttpResponse cached = new HttpResponseBuilder()
                                .setHeader(HttpHeaders.LAST_MODIFIED, RFC1123_EPOCH)
                                .setCacheTtl(-1)
                                .create();
    cache.data.put(DEFAULT_URI, cached);

    HttpResponse notModified = new HttpResponseBuilder()
                                    .setHttpStatusCode(HttpResponse.SC_NOT_MODIFIED)
                                    .create();
    fetcher.response = notModified;
    HttpResponse response = pipeline.execute(request);

    assertEquals(RFC1123_EPOCH, fetcher.request.getHeader(HttpHeaders.IF_MODIFIED_SINCE));
    assertEquals(cached, response);
    assertEquals(null, cache.data.get(DEFAULT_URI));
    assertEquals(1, cache.readCount);
    assertEquals(0, cache.writeCount);
    assertEquals(1, fetcher.fetchCount);
  }

  @Test
  public void authTypeNoneStaleConditionalGetNoLastModified() throws Exception {
    // Cached response is stale and has no Last-Modified header on it. Test that an
    // If-Modified-Since header is not issued.
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    HttpResponse cached = new HttpResponseBuilder()
                                .setCacheTtl(-1)
                                .create();
    cache.data.put(DEFAULT_URI, cached);

    fetcher.response = HttpResponse.error(); // Really don't care what this is.
    pipeline.execute(request);

    assertEquals(null, fetcher.request.getHeader(HttpHeaders.IF_MODIFIED_SINCE));
  }

  @Test
  public void authTypeNoneStaleConditionalGetNoEtag() throws Exception {
    // Cached response is stale and has no Etag header on it. Test that an
    // If-None-Match header is not issued.
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.NONE);

    HttpResponse cached = new HttpResponseBuilder()
                                .setCacheTtl(-1)
                                .create();
    cache.data.put(DEFAULT_URI, cached);

    fetcher.response = HttpResponse.error(); // Really don't care what this is.
    pipeline.execute(request);

    assertEquals(null, fetcher.request.getHeader(HttpHeaders.IF_NONE_MATCH));
  }

  @Test
  public void authTypeOAuthNotCached() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.OAUTH);

    oauth.httpResponse = new HttpResponse("oauth result");

    HttpResponse response = pipeline.execute(request);

    assertEquals(oauth.httpResponse, response);
    assertEquals(request, oauth.httpRequest);
    assertEquals(response, cache.data.get(DEFAULT_URI));
    assertEquals(1, oauth.fetchCount);
    assertEquals(0, fetcher.fetchCount);
    assertEquals(1, cache.readCount);
    assertEquals(1, cache.writeCount);
  }

  @Test
  public void authTypeOAuthWasCached() throws Exception {
    HttpRequest request = new HttpRequest(DEFAULT_URI).setAuthType(AuthType.OAUTH);

    HttpResponse cached = new HttpResponse("cached");
    cache.data.put(DEFAULT_URI, cached);

    HttpResponse response = pipeline.execute(request);

    assertEquals(cached, response);
    assertEquals(0, oauth.fetchCount);
    assertEquals(0, fetcher.fetchCount);
    assertEquals(1, cache.readCount);
    assertEquals(0, cache.writeCount);
  }

  private static int roundToSeconds(long ts) {
    return (int) (ts / 1000);
  }

  @Test
  public void testFixedDateOk() throws Exception {
    int time = roundToSeconds(HttpResponseTest.timeSource.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
            .addHeader(
                    "Date",
                    DateUtil.formatRfc1123Date(1000L * time + 1000
                            - DefaultRequestPipeline.DEFAULT_DRIFT_LIMIT_MS)).setCacheTtl(100)
            .create();

    HttpResponse newResponse = DefaultRequestPipeline.maybeFixDriftTime(response);
    assertSame(response, newResponse);
  }

  @Test
  public void testFixedDateOld() throws Exception {
    int time = roundToSeconds(HttpResponseTest.timeSource.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
            .addHeader(
                    "Date",
                    DateUtil.formatRfc1123Date(1000L * time - 1000
                            - DefaultRequestPipeline.DEFAULT_DRIFT_LIMIT_MS)).setCacheTtl(100)
            .create();

    response = DefaultRequestPipeline.maybeFixDriftTime(response);
    // Verify that the old time is ignored:
    assertEquals(time + 100, roundToSeconds(response.getCacheExpiration()));
    assertEquals(DateUtil.formatRfc1123Date(HttpResponseTest.timeSource.currentTimeMillis()),
            response.getHeader("Date"));
  }

  @Test
  public void testFixedDateNew() throws Exception {
    int time = roundToSeconds(HttpResponseTest.timeSource.currentTimeMillis());
    HttpResponse response = new HttpResponseBuilder()
            .addHeader(
                    "Date",
                    DateUtil.formatRfc1123Date(1000L * time + 1000
                            + DefaultRequestPipeline.DEFAULT_DRIFT_LIMIT_MS)).setCacheTtl(100)
            .create();

    response = DefaultRequestPipeline.maybeFixDriftTime(response);
    // Verify that the old time is ignored:
    assertEquals(time + 100, roundToSeconds(response.getCacheExpiration()));
    assertEquals(DateUtil.formatRfc1123Date(HttpResponseTest.timeSource.currentTimeMillis()),
            response.getHeader("Date"));
  }

  public static class FakeHttpFetcher implements HttpFetcher {
    protected HttpRequest request;
    protected HttpResponse response;
    protected int fetchCount = 0;

    protected FakeHttpFetcher() {}

    public HttpResponse fetch(HttpRequest request) throws GadgetException {
      fetchCount++;
      this.request = request;
      if (response == null) {
        throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT);
      }
      return response;
    }
  }

  public static class FakeHttpCache implements HttpCache {
    protected final Map<Uri, HttpResponse> data = Maps.newHashMap();
    protected int writeCount = 0;
    protected int readCount = 0;

    protected FakeHttpCache() {}

    public HttpResponse addResponse(HttpRequest request, HttpResponse response) {
      writeCount++;
      data.put(request.getUri(), response);
      return response;
    }

    public HttpResponse getResponse(HttpRequest request) {
      readCount++;
      return data.get(request.getUri());
    }

    public HttpResponse removeResponse(HttpRequest key) {
      return data.remove(key.getUri());
    }

    public String createKey(HttpRequest request) {
      return request.getUri().getQuery();
    }
  }

  public static class FakeOAuthRequestProvider implements Provider<OAuthRequest> {
    protected int fetchCount = 0;
    protected HttpRequest httpRequest;
    protected HttpResponse httpResponse;

    protected FakeOAuthRequestProvider() {}

    private final OAuthRequest oauthRequest = new OAuthRequest(null, null) {
      @Override
      public HttpResponse fetch(HttpRequest request) {
        fetchCount++;
        httpRequest = request;
        return httpResponse;
      }
    };

    public OAuthRequest get() {
      return oauthRequest;
    }

  }

  public static class FakeOAuth2RequestProvider implements Provider<OAuth2Request> {
    protected int fetchCount = 0;
    protected HttpRequest httpRequest;
    protected HttpResponse httpResponse;

    protected FakeOAuth2RequestProvider() {}

    private final OAuth2Request oauth2Request = new BasicOAuth2Request(null, null, null, null,
            null, null, null, false, null) {
      @Override
      public HttpResponse fetch(HttpRequest request) {
        fetchCount++;
        httpRequest = request;
        return httpResponse;
      }
    };

    public OAuth2Request get() {
      return oauth2Request;
    }

  }
}

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

import com.google.common.collect.Multimap;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.shindig.common.Nullable;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.util.DateUtil;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.oauth2.OAuth2Request;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterList.RewriteFlow;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewritingException;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A standard implementation of a request pipeline. Performs request caching and
 * signing on top of standard HTTP requests.
 */
@Singleton
public class DefaultRequestPipeline implements RequestPipeline {
  private final HttpFetcher httpFetcher;
  private final HttpCache httpCache;
  private final Provider<OAuthRequest> oauthRequestProvider;
  private final Provider<OAuth2Request> oauth2RequestProvider;
  private final ResponseRewriterRegistry responseRewriterRegistry;
  private final InvalidationService invalidationService;
  private final HttpResponseMetadataHelper metadataHelper;

  // At what point you don't trust remote server date stamp on response (in milliseconds)
  // (Should be less then DEFAULT_TTL)
  static final long DEFAULT_DRIFT_LIMIT_MS = 3L * 60L * 1000L;

  @Inject(optional = true) @Named("shindig.http.date-drift-limit-ms")
  private static long responseDateDriftLimit = DEFAULT_DRIFT_LIMIT_MS;

  //class name for logging purpose
  private static final String classname = DefaultRequestPipeline.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);

  @Inject
  public DefaultRequestPipeline(HttpFetcher httpFetcher,
                                HttpCache httpCache,
                                Provider<OAuthRequest> oauthRequestProvider,
                                Provider<OAuth2Request> oauth2RequestProvider,
                                @RewriterRegistry(rewriteFlow = RewriteFlow.REQUEST_PIPELINE)
                                ResponseRewriterRegistry responseRewriterRegistry,
                                InvalidationService invalidationService,
                                @Nullable HttpResponseMetadataHelper metadataHelper) {
    this.httpFetcher = httpFetcher;
    this.httpCache = httpCache;
    this.oauthRequestProvider = oauthRequestProvider;
    this.oauth2RequestProvider = oauth2RequestProvider;
    this.responseRewriterRegistry = responseRewriterRegistry;
    this.invalidationService = invalidationService;
    this.metadataHelper = metadataHelper;
  }

  public HttpResponse execute(HttpRequest request) throws GadgetException {
    final String method = "execute";
    normalizeProtocol(request);

    HttpResponse cachedResponse = checkCachedResponse(request);

    HttpResponse invalidatedResponse = null;
    HttpResponse staleResponse = null;

    // Note that we don't remove invalidated entries from the cache as we want them to be
    // available in the event of a backend fetch failure.
    // Note that strict no-cache entries are dummy responses and should not be used.
    if (cachedResponse != null && !cachedResponse.isStrictNoCache()) {
      if (!cachedResponse.isStale()) {
        if (invalidationService.isValid(request, cachedResponse)) {
          if(LOG.isLoggable(Level.FINEST)) {
            LOG.logp(Level.FINEST, classname, method, MessageKeys.CACHED_RESPONSE,
                    new Object[]{request.getUri().toString()});
          }
          return cachedResponse;
        } else {
          invalidatedResponse = cachedResponse;
        }
      } else {
        if (!cachedResponse.isError()) {
          // Remember good but stale cached response, to be served if server unavailable
          staleResponse = cachedResponse;
        }
      }
    }

    // If we have a stale response, perform a conditional GET.
    // Note: Fixing up the request with these headers will not affect http response caching. See
    // org.apache.shindig.gadgets.http.AbstractHttpCache.createKey(HttpRequest)
    if (staleResponse != null) {
      final String lastModified = staleResponse.getHeader(HttpHeaders.LAST_MODIFIED);
      if (lastModified != null) {
        request.setHeader(HttpHeaders.IF_MODIFIED_SINCE, lastModified);
      }

      final String etag = staleResponse.getHeader(HttpHeaders.ETAG);
      if (etag != null) {
        request.setHeader(HttpHeaders.IF_NONE_MATCH, etag);
      }
    }

    HttpResponse fetchedResponse = fetchResponse(request);
    fetchedResponse = fixFetchedResponse(request, fetchedResponse, invalidatedResponse,
        staleResponse);
    return fetchedResponse;
  }

  /**
   * Normalizing the HttpRequest object to verify the validity of the request.
   *
   * @param request
   * @throws GadgetException
   */
  protected void normalizeProtocol(HttpRequest request) throws GadgetException {
    // Normalize the protocol part of the URI
    if (request.getUri().getScheme()== null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "Url " + request.getUri().toString() + " does not include scheme",
          HttpResponse.SC_BAD_REQUEST);
    } else if (!"http".equals(request.getUri().getScheme()) &&
        !"https".equals(request.getUri().getScheme())) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "Invalid request url scheme in url: " + Utf8UrlCoder.encode(request.getUri().toString()) +
            "; only \"http\" and \"https\" supported.",
            HttpResponse.SC_BAD_REQUEST);
    }
  }

  /**
   * Check the HttpRequest object has already allow caching and if do try to get it from cache.
   * Override this if you want to add additional logic to get response for the request from cache.
   *
   * @param request
   * @return HttpResponse object which either the cached response or null
   */
  protected HttpResponse checkCachedResponse(HttpRequest request) {
    HttpResponse cachedResponse;
    if (!request.getIgnoreCache()) {
      cachedResponse = httpCache.getResponse(request);
    } else {
      cachedResponse = null;
    }
    return cachedResponse;
  }

  /**
   * Fetch the response from the network using the right fetcher
   * Override this if you need to extend the current behavior of supported auth type.
   *
   * @param request
   * @return  HttpResponse object fetched from network
   * @throws GadgetException
   */
  protected HttpResponse fetchResponse(HttpRequest request) throws GadgetException {
    HttpResponse fetchedResponse;
    switch (request.getAuthType()) {
      case NONE:
        fetchedResponse = httpFetcher.fetch(request);
        break;
      case SIGNED:
      case OAUTH:
        fetchedResponse = oauthRequestProvider.get().fetch(request);
        break;
      case OAUTH2:
        fetchedResponse = oauth2RequestProvider.get().fetch(request);
        break;
      default:
        return HttpResponse.error();
    }
    return fetchedResponse;
  }

  /**
   * Attempt to "fix" the response by checking its validity and adding additional metadata
   * Override this if you need to add more processing to the HttpResponse before being cached and
   * returned.
   *
   * @param request
   * @param fetchedResponse
   * @param invalidatedResponse
   * @param staleResponse
   * @return HttpResponse object that has been updated with some metadata tags.
   * @throws GadgetException
   */
  protected HttpResponse fixFetchedResponse(HttpRequest request, HttpResponse fetchedResponse,
      @Nullable HttpResponse invalidatedResponse, @Nullable HttpResponse staleResponse)
      throws GadgetException {
    final String method = "fixFetchedResponse";
    if (fetchedResponse.isError() && invalidatedResponse != null) {
      // Use the invalidated cached response if it is not stale. We don't update its
      // mark so it remains invalidated
      return invalidatedResponse;
    }

    if (fetchedResponse.getHttpStatusCode() >= 500 && staleResponse != null) {
      // If we have trouble accessing the remote server,
      // Lets try the latest good but staled result
      if(LOG.isLoggable(Level.FINEST)) {
        LOG.logp(Level.FINEST, classname, method, MessageKeys.STALE_RESPONSE,
            new Object[]{request.getUri().toString()});
      }
      return staleResponse;
    }

    fetchedResponse = maybeFixDriftTime(fetchedResponse);

    // 304 Not Modified
    // Return the stale response and update what is in the cache with any new headers per
    // http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
    //
    // "If a cache uses a received 304 response to update a cache entry,
    //  the cache MUST update the entry to reflect any new field values
    //  given in the response."
    if (fetchedResponse.getHttpStatusCode() == HttpResponse.SC_NOT_MODIFIED) {

      // Update the stale response's headers with the new headers from the fetched response.
      // If the response from the remote server doesn't have an "Expires" or "Cache-Control" header,
      // we should service the current request and remove the stale response from the cache. We rely
      // on those headers to be present so that the response that is in the cache will return the
      // correct value when isStale() is called. Without removing the stale response, we would get
      // stuck in a loop of doing conditional GETs for the same stale resource every time it is
      // requested.
      final Multimap<String, String> fetchedResponseHeaders = fetchedResponse.getHeaders();
      if (fetchedResponse.getCacheControlMaxAge() == -1
              && fetchedResponse.getExpiresTime() == -1) {
        // CONSIDER: We could try to recurse in this case and do a non-conditional-get for the resource.
        return httpCache.removeResponse(request);
      }

      HttpResponseBuilder httpResponseBuilder = new HttpResponseBuilder(staleResponse);
      for (String headerName : fetchedResponseHeaders.keySet()) {
        httpResponseBuilder.removeHeader(headerName);
      }

      httpResponseBuilder.addAllHeaders(fetchedResponse.getHeaders());
      return cacheFetchedResponse(request, httpResponseBuilder.create());
    }

    if (!fetchedResponse.isError() && !request.getIgnoreCache() && request.getCacheTtl() != 0) {
      try {
        fetchedResponse =
            responseRewriterRegistry.rewriteHttpResponse(request, fetchedResponse, null);
      } catch (RewritingException e) {
        throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e,
            e.getHttpStatusCode());
      }
    }

    // Set response hash value in metadata (used for url versioning)
    fetchedResponse = HttpResponseMetadataHelper.updateHash(fetchedResponse, metadataHelper);

    // cache the fetched response if possible
    fetchedResponse = cacheFetchedResponse(request, fetchedResponse);

    return fetchedResponse;
  }

  /**
   * Cache the HttpResponse object before being returned to caller.
   * The default implementation also invalidate the response object by marking it as valid so the
   * next request can detect if it has been invalidated.
   * Override this if you need to add additional processing to cache the response.
   *
   * @param request
   * @param fetchedResponse
   * @return HttpResponse object that has been updated with some metadata tags.
   */
  protected HttpResponse cacheFetchedResponse(HttpRequest request, HttpResponse fetchedResponse) {
    if (!request.getIgnoreCache()) {
      // Mark the response with invalidation information prior to caching
      if (fetchedResponse.getCacheTtl() > 0) {
        fetchedResponse = invalidationService.markResponse(request, fetchedResponse);
      }
      HttpResponse cached = httpCache.addResponse(request, fetchedResponse);
      if (cached != null) {
        fetchedResponse = cached; // possibly modified response.
      }
    }

    return fetchedResponse;
  }

  /**
   * Verify response time, and if response time is off from current time by more then
   * speficied time change response time to be current time.
   * The function resolve cases that remote server time is wrong, which can cause
   * resources to expire prematurly or served after they should be expired.
   * The allowd drift time is configured by responseDateDriftLimit.
   * @param response the response to fix
   * @return new response with fix date or original reesponse
   */
  public static HttpResponse maybeFixDriftTime(HttpResponse response) {
    Collection<String> dates = response.getHeaders("Date");

    if (!dates.isEmpty()) {
      Date d = DateUtil.parseRfc1123Date(dates.iterator().next());
      if (d != null) {
        long timestamp = d.getTime();
        long currentTime = HttpUtil.getTimeSource().currentTimeMillis();
        if (Math.abs(currentTime - timestamp) > responseDateDriftLimit) {
          // Do not trust the date from response if it is too old (server time out of sync)
          HttpResponseBuilder builder = new HttpResponseBuilder(response);
          builder.setHeader("Date", DateUtil.formatRfc1123Date(currentTime));
          response = builder.create();
        }
      }
    }
    return response;
  }
}

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
package org.apache.shindig.gadgets.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.Nullable;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.admin.GadgetAdminStore;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterList.RewriteFlow;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.UriUtils;
import org.apache.shindig.gadgets.uri.UriUtils.DisallowedHeaders;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Handles open proxy requests.
 */
@Singleton
public class ProxyHandler {
  private final RequestPipeline requestPipeline;
  private final ResponseRewriterRegistry contentRewriterRegistry;
  protected final boolean remapInternalServerError;
  private final GadgetAdminStore gadgetAdminStore;
  private final Integer longLivedRefreshSec;
  private static final String POST = "POST";

  @Inject
  public ProxyHandler(RequestPipeline requestPipeline,
      @RewriterRegistry(rewriteFlow = RewriteFlow.DEFAULT) ResponseRewriterRegistry contentRewriterRegistry,
      @Named("shindig.proxy.remapInternalServerError") Boolean remapInternalServerError,
      GadgetAdminStore gadgetAdminStore,
      @Named("org.apache.shindig.gadgets.servlet.longLivedRefreshSec") int longLivedRefreshSec) {
    this.requestPipeline = requestPipeline;
    this.contentRewriterRegistry = contentRewriterRegistry;
    this.remapInternalServerError = remapInternalServerError;
    this.gadgetAdminStore = gadgetAdminStore;
    this.longLivedRefreshSec = longLivedRefreshSec;
  }

  /**
   * Generate a remote content request based on the parameters sent from the client.
   * @param uriCtx
   * @param tgt
   * @param postBody
   */
  private HttpRequest buildHttpRequest(ProxyUriManager.ProxyUri uriCtx, Uri tgt, @Nullable String postBody)
      throws GadgetException, IOException {
    ServletUtil.validateUrl(tgt);
    HttpRequest req = uriCtx.makeHttpRequest(tgt);
    req.setRewriteMimeType(uriCtx.getRewriteMimeType());
    if (postBody != null) {
      req.setMethod(POST);
      // convert String into InputStream
      req.setPostBody(new ByteArrayInputStream(postBody.getBytes()));
    }
    if (req.getHeader("User-Agent") == null) {
      final String userAgent = uriCtx.getUserAgent();
      if (userAgent != null) {
        req.setHeader("User-Agent", userAgent);
      }
    }
    return req;
  }

  public HttpResponse fetch(ProxyUriManager.ProxyUri proxyUri) throws IOException, GadgetException {
    return fetch(proxyUri, null);
  }

  public HttpResponse fetch(ProxyUriManager.ProxyUri proxyUri, @Nullable String postBody)
      throws IOException, GadgetException {
    HttpRequest rcr = buildHttpRequest(proxyUri, proxyUri.getResource(), postBody);
    if (rcr == null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
        "No url parameter in request", HttpResponse.SC_BAD_REQUEST);
    }

    if (rcr.getGadget() != null &&
            !gadgetAdminStore.isWhitelisted(rcr.getContainer(), rcr.getGadget().toString())) {
      throw new GadgetException(GadgetException.Code.NON_WHITELISTED_GADGET,
        "The requested content is unavailable", HttpResponse.SC_FORBIDDEN);
    }

    HttpResponse results = requestPipeline.execute(rcr);

    if (results.isError()) {
      // Error: try the fallback. Particularly useful for proxied images.
      Uri fallbackUri = proxyUri.getFallbackUri();
      if (fallbackUri != null) {
        HttpRequest fallbackRcr = buildHttpRequest(proxyUri, fallbackUri, null);
        results = requestPipeline.execute(fallbackRcr);
      }
    }

    if (contentRewriterRegistry != null) {
      try {
        results = contentRewriterRegistry.rewriteHttpResponse(rcr, results, null);
      } catch (RewritingException e) {
        // Throw exception if the RETURN_ORIGINAL_CONTENT_ON_ERROR param is not
        // set to "true" or the error is irrecoverable from.
        if (!proxyUri.shouldReturnOrigOnErr() || !isRecoverable(results)) {
          throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e,
                  e.getHttpStatusCode());
        }
      }
    }

    HttpResponseBuilder response = new HttpResponseBuilder(results);
    response.clearAllHeaders();

    try {
      ServletUtil.setCachingHeaders(response, proxyUri.translateStatusRefresh(longLivedRefreshSec,
        (int) (results.getCacheTtl() / 1000)), false);
    } catch (GadgetException gex) {
      return ServletUtil.errorResponse(gex);
    }

    UriUtils.copyResponseHeadersAndStatusCode(results, response, remapInternalServerError, true,
      DisallowedHeaders.CACHING_DIRECTIVES, // Proxy sets its own caching headers.
      DisallowedHeaders.CLIENT_STATE_DIRECTIVES, // Overridden or irrelevant to proxy.
      DisallowedHeaders.OUTPUT_TRANSFER_DIRECTIVES);

    // Set Content-Type and Content-Disposition. Do this after copy results headers,
    // in order to prevent those from overwriting the correct values.
    setResponseContentHeaders(response, results);

    UriUtils.maybeRewriteContentType(rcr, response);

    // TODO: replace this with streaming APIs when ready
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy(results.getResponse(), baos);
    response.setResponse(baos.toByteArray());
    return response.create();
  }

  protected void setResponseContentHeaders(HttpResponseBuilder response, HttpResponse results) {
    // We're skipping the content disposition header for flash due to an issue with Flash player 10
    // This does make some sites a higher value phishing target, but this can be mitigated by
    // additional referer checks.
    if (!isFlash(response.getHeader("Content-Type"), results.getHeader("Content-Type"))) {
      String contentDispositionValue = results.getHeader("Content-Disposition");
      if (StringUtils.isBlank(contentDispositionValue)
              || contentDispositionValue.indexOf("attachment;") == -1
              || contentDispositionValue.indexOf("filename") == -1) {
        response.setHeader("Content-Disposition", "attachment;filename=p.txt");
      } else {
        response.setHeader("Content-Disposition", contentDispositionValue);
      }
    }
    if (results.getHeader("Content-Type") == null) {
      response.setHeader("Content-Type", "application/octet-stream");
    }
  }

  private static final String FLASH_CONTENT_TYPE = "application/x-shockwave-flash";

  /**
   * Test for presence of flash
   *
   * @param responseContentType
   *          the Content-Type header from the HttpResponseBuilder
   * @param resultsContentType
   *          the Content-Type header from the HttpResponse
   * @return true if either content type matches that of Flash
   */
  private boolean isFlash(String responseContentType, String resultsContentType) {
    return StringUtils.startsWithIgnoreCase(responseContentType, FLASH_CONTENT_TYPE)
            || StringUtils.startsWithIgnoreCase(resultsContentType, FLASH_CONTENT_TYPE);
  }

  /**
   * Returns true in case the error encountered while rewriting the content is recoverable. The
   * rationale behind it is that errors should be thrown only in case of serious grave errors
   * (defined to be un recoverable). It should always be preferred to handle errors and return the
   * original content at least.
   *
   * @param results
   *          The result of rewriting.
   * @return True if the error is recoverable, false otherwise.
   */
  public boolean isRecoverable(HttpResponse results) {
    return !(Strings.isNullOrEmpty(results.getResponseAsString()) && results.getHeaders() == null);
  }
}

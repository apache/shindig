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
package org.apache.shindig.gadgets.servlet;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.DomWalker;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.AccelUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.apache.shindig.gadgets.uri.UriCommon;
import org.apache.shindig.gadgets.uri.UriUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Handles requests for accel servlet.
 * The objective is to accelerate web pages.
 */
@Singleton
public class AccelHandler extends ProxyBase {
  private static final Logger logger = Logger.getLogger(ProxyHandler.class.getName());

  static final String ERROR_FETCHING_DATA = "Error fetching data";

  // TODO: parameterize these.
  static final Integer LONG_LIVED_REFRESH = (365 * 24 * 60 * 60);  // 1 year
  static final Integer DEFAULT_REFRESH = (60 * 60);                // 1 hour

  protected final RequestPipeline requestPipeline;
  protected final ResponseRewriterRegistry contentRewriterRegistry;
  protected final AccelUriManager uriManager;

  @Inject
  public AccelHandler(RequestPipeline requestPipeline,
                      @Named("shindig.accelerate.response.rewriter.registry")
                      ResponseRewriterRegistry contentRewriterRegistry,
                      AccelUriManager accelUriManager) {
    this.requestPipeline = requestPipeline;
    this.contentRewriterRegistry = contentRewriterRegistry;
    this.uriManager = accelUriManager;
  }

  @Override
  protected void doFetch(HttpServletRequest request, HttpServletResponse response)
      throws IOException, GadgetException {
    // TODO: Handle if modified since headers.

    // Parse and normalize to get a proxied request uri.
    Uri requestUri = new UriBuilder(request).toUri();
    ProxyUriManager.ProxyUri proxyUri = getProxyUri(requestUri);

    // Fetch the content of the requested uri.
    HttpRequest req = buildHttpRequest(request, proxyUri);
    HttpResponse results = requestPipeline.execute(req);

    if (!handleErrors(results, response)) {
      // In case of errors where we want to short circuit the rewriting and
      // throw appropriate gadget exception.
      return;
    }

    // Rewrite the content.
    try {
      results = contentRewriterRegistry.rewriteHttpResponse(req, results);
    } catch (RewritingException e) {
      if (!isRecoverable(req, results, e)) {
        throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e,
                                  e.getHttpStatusCode());
      }
    }

    // Copy the response headers and status code to the final http servlet
    // response.
    UriUtils.copyResponseHeadersAndStatusCode(
        results, response, true,
        UriUtils.DisallowedHeaders.OUTPUT_TRANSFER_DIRECTIVES,
        UriUtils.DisallowedHeaders.CLIENT_STATE_DIRECTIVES);

    // Override the content type of the final http response if the input request
    // had the rewrite mime type header.
    rewriteContentType(req, response);

    IOUtils.copy(results.getResponse(), response.getOutputStream());
  }

  /**
   * Returns the proxy uri encapsulating the request uri.
   * @param requestUri The request uri.
   * @return The proxy uri encapsulating the request uri.
   * @throws GadgetException In case of errors.
   */
  public ProxyUriManager.ProxyUri getProxyUri(Uri requestUri) throws GadgetException {
    Uri proxiedUri = uriManager.parseAndNormalize(requestUri);
    String uriString = proxiedUri.getQueryParameter(UriCommon.Param.URL.getKey());

    // Throw BAD_GATEWAY in case parsing of url fails.
    Uri normalizedUri;
    try {
      normalizedUri = Uri.parse(uriString);
    } catch (Uri.UriException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
                                "Failed to parse uri: " + uriString,
                                HttpServletResponse.SC_BAD_GATEWAY);
    }

    Gadget gadget = DomWalker.makeGadget(requestUri);
    return new ProxyUriManager.ProxyUri(gadget, normalizedUri);
  }

  /**
   * Returns true in case the error encountered while rewriting the content
   * is recoverable. The rationale behind it is that errors should be thrown
   * only in case of serious grave errors (defined to be un recoverable).
   * It should always be preferred to handle errors and return the original
   * content at least.
   *
   * TODO: Think through all cases which are recoverable to enforce minimal
   * possible set of constraints.
   * TODO: Log the exception and context around it.
   *
   * @param req The http request for fetching the resource.
   * @param results The result of rewriting.
   * @param exception Exception caught.
   * @return True if the error is recoverable, false otherwise.
   */
  protected boolean isRecoverable(HttpRequest req, HttpResponse results,
                                  RewritingException exception) {
    return !(StringUtils.isEmpty(results.getResponseAsString()) &&
             results.getHeaders() == null);
  }

  /**
   * Build an HttpRequest object encapsulating the request details as requested
   * by the user.
   * @param request The http request.
   * @param uriToProxyOrRewrite The parsed uri to proxy or rewrite through
   *   accel servlet.
   * @return Remote content request based on the parameters sent from the client.
   * @throws GadgetException In case the data could not be fetched.
   */
  protected HttpRequest buildHttpRequest(HttpServletRequest request,
                                         ProxyUriManager.ProxyUri uriToProxyOrRewrite)
      throws GadgetException {
    Uri tgt = uriToProxyOrRewrite.getResource();
    validateUrl(tgt);
    HttpRequest req = uriToProxyOrRewrite.makeHttpRequest(tgt);
    if (req == null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "No url parameter in request", HttpResponse.SC_BAD_REQUEST);
    }

    // Copy the post body if it exists.
    UriUtils.copyRequestData(request, req);

    // Set and copy headers.
    this.setRequestHeaders(request, req);
    UriUtils.copyRequestHeaders(
        request, req,
        UriUtils.DisallowedHeaders.POST_INCOMPATIBLE_DIRECTIVES);

    req.setFollowRedirects(false);
    return req;
  }

  /**
   * Rewrite the content type of the final http response if the request has the
   * rewrite-mime-type param.
   * @param req The http request.
   * @param response The final http response to be returned to user.
   */
  protected void rewriteContentType(HttpRequest req, HttpServletResponse response) {
    String contentType = response.getContentType();
    String requiredType = req.getRewriteMimeType();
    if (!StringUtils.isEmpty(requiredType)) {
      if (requiredType.endsWith("/*") && !StringUtils.isEmpty(contentType)) {
        requiredType = requiredType.substring(0, requiredType.length() - 2);
      }
      response.setContentType(requiredType);
    }
  }

  /**
   * Process errors when fetching uri using request pipeline by throwing
   * GadgetException in error cases.
   * @param results The http response returned by request pipeline.
   * @param response The http servlet response to be returned to the user.
   * @return True if there is no error, false otherwise.
   * @throws IOException In case of errors.
   */
  protected boolean handleErrors(HttpResponse results, HttpServletResponse response)
      throws IOException {
    if (results == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERROR_FETCHING_DATA);
      return false;
    }
    if (results.getHttpStatusCode() == HttpServletResponse.SC_NOT_FOUND) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, ERROR_FETCHING_DATA);
      return false;
    } else if (results.isError()) {
      response.sendError(HttpServletResponse.SC_BAD_GATEWAY, ERROR_FETCHING_DATA);
      return false;
    }

    return true;
  }
}

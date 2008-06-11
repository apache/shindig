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

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;

import com.google.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyHandler extends ProxyBase {
  private static final Logger logger =
      Logger.getLogger(ProxyHandler.class.getPackage().getName());

  private final static Set<String> DISALLOWED_RESPONSE_HEADERS
      = new HashSet<String>();
  static {
    DISALLOWED_RESPONSE_HEADERS.add("set-cookie");
    DISALLOWED_RESPONSE_HEADERS.add("content-length");
    DISALLOWED_RESPONSE_HEADERS.add("content-encoding");
    DISALLOWED_RESPONSE_HEADERS.add("etag");
    DISALLOWED_RESPONSE_HEADERS.add("last-modified");
    DISALLOWED_RESPONSE_HEADERS.add("accept-ranges");
    DISALLOWED_RESPONSE_HEADERS.add("vary");
    DISALLOWED_RESPONSE_HEADERS.add("expires");
    DISALLOWED_RESPONSE_HEADERS.add("date");
    DISALLOWED_RESPONSE_HEADERS.add("pragma");
    DISALLOWED_RESPONSE_HEADERS.add("cache-control");
  }

  // This isn't a final field because we want to support optional injection.
  // This is a limitation of Guice, but this workaround...works.
  private final HttpFetcher fetcher;
  private final LockedDomainService domainLocker;
  private final ContentRewriter rewriter;

  @Inject
  public ProxyHandler(HttpFetcher fetcher,
                      LockedDomainService lockedDomainService,
                      ContentRewriter rewriter) {
    this.fetcher = fetcher;
    this.domainLocker = lockedDomainService;
    this.rewriter = rewriter;
  }

  /**
   * Generate a remote content request based on the parameters sent from the client.
   */
  private HttpRequest buildHttpRequest(HttpServletRequest request) throws GadgetException {
    URI url = validateUrl(request.getParameter(URL_PARAM));
    HttpRequest.Options options = new HttpRequest.Options();
    if (request.getParameter(GADGET_PARAM) != null) {
      options.gadgetUri = URI.create(request.getParameter(GADGET_PARAM));
    }
    options.rewriter = rewriter;

    // Allow the rewriter to use an externally forced mime type. This is needed
    // allows proper rewriting of <script src="x"/> where x is returned with
    // a content type like text/html which unfortunately happens all too often
    options.rewriteMimeType = request.getParameter(REWRITE_MIME_TYPE_PARAM);

    return new HttpRequest(url, options);
  }

  @Override
  public void fetch(HttpServletRequest request, HttpServletResponse response)
      throws IOException, GadgetException {
    if (request.getHeader("If-Modified-Since") != null) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    String host = request.getHeader("Host");
    if (!domainLocker.embedCanRender(host)) {
      // Force embedded images and the like to their own domain to avoid XSS
      // in gadget domains.
      String msg = "Embed request for url " + getParameter(request, URL_PARAM, "") +
          " made to wrong domain " + host;
      logger.info(msg);
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER, msg);
    }

    HttpRequest rcr = buildHttpRequest(request);
    HttpResponse results = fetcher.fetch(rcr);

    setResponseHeaders(request, response, results);

    for (Map.Entry<String, List<String>> entry : results.getAllHeaders().entrySet()) {
      String name = entry.getKey();
      if (!DISALLOWED_RESPONSE_HEADERS.contains(name.toLowerCase())) {
        for (String value : entry.getValue()) {
          response.addHeader(name, value);
        }
      }
    }

    if (rcr.getOptions().rewriteMimeType != null) {
      response.setContentType(rcr.getOptions().rewriteMimeType);
    }

    if (results.getHttpStatusCode() != HttpResponse.SC_OK) {
      response.sendError(results.getHttpStatusCode());
    }

    response.getOutputStream().write(results.getResponseAsBytes());
  }
}

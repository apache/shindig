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

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handles open proxy requests.
 */
@Singleton
public class ProxyHandler extends ProxyBase {
  private static final Logger logger =
      Logger.getLogger(ProxyHandler.class.getPackage().getName());

  private static final Collection<String> DISALLOWED_RESPONSE_HEADERS = Sets.newHashSet(
      "set-cookie", "content-length", "content-encoding", "etag", "last-modified" ,"accept-ranges",
      "vary", "expires", "date", "pragma", "cache-control"
  );

  // This isn't a final field because we want to support optional injection.
  // This is a limitation of Guice, but this workaround...works.
  private final HttpFetcher fetcher;
  private final LockedDomainService lockedDomainService;
  private final ContentRewriter rewriter;

  @Inject
  public ProxyHandler(HttpFetcher fetcher,
                      LockedDomainService lockedDomainService,
                      ContentRewriter rewriter) {
    this.fetcher = fetcher;
    this.lockedDomainService = lockedDomainService;
    this.rewriter = rewriter;
  }

  /**
   * Generate a remote content request based on the parameters sent from the client.
   */
  private HttpRequest buildHttpRequest(HttpServletRequest request) throws GadgetException {
    Uri url = validateUrl(request.getParameter(URL_PARAM));

    HttpRequest req = new HttpRequest(url);

    if (request.getParameter(GADGET_PARAM) != null) {
      req.setGadget(Uri.parse(request.getParameter(GADGET_PARAM)));
    }

    req.setContentRewriter(rewriter);

    // Allow the rewriter to use an externally forced mime type. This is needed
    // allows proper rewriting of <script src="x"/> where x is returned with
    // a content type like text/html which unfortunately happens all too often
    req.setRewriteMimeType(request.getParameter(REWRITE_MIME_TYPE_PARAM));

    // If the proxy request specifies a refresh param then we want to force the min TTL for
    // the retrieved entry in the cache regardless of the headers on the content when it
    // is fetched from the original source.
    if (request.getParameter(REFRESH_PARAM) != null) {
      try {
        req.setCacheTtl(Integer.parseInt(request.getParameter(REFRESH_PARAM)));
      } catch (NumberFormatException nfe) {
        // Ignore
      }
    }

    return req;
  }

  @Override
  public void fetch(HttpServletRequest request, HttpServletResponse response)
      throws IOException, GadgetException {
    if (request.getHeader("If-Modified-Since") != null) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    String host = request.getHeader("Host");
    if (!lockedDomainService.embedCanRender(host)) {
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


    for (Map.Entry<String, List<String>> entry : results.getHeaders().entrySet()) {
      String name = entry.getKey();
      if (!DISALLOWED_RESPONSE_HEADERS.contains(name.toLowerCase())) {
        for (String value : entry.getValue()) {
          response.addHeader(name, value);
        }
      }
    }

    if (rcr.getRewriteMimeType() != null) {
      response.setContentType(rcr.getRewriteMimeType());
    }

    if (results.getHttpStatusCode() != HttpResponse.SC_OK) {
      response.sendError(results.getHttpStatusCode());
    }

    IOUtils.copy(results.getResponse(), response.getOutputStream());
  }
}

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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.SecurityTokenException;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthRequestParams;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.spec.Auth;
import org.apache.shindig.gadgets.spec.Preload;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles gadgets.io.makeRequest requests.
 *
 * Unlike ProxyHandler, this may perform operations such as OAuth or signed fetch.
 */
public class MakeRequestHandler extends ProxyBase{
  protected static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";
  protected static final String POST_DATA_PARAM = "postData";
  protected static final String METHOD_PARAM = "httpMethod";
  protected static final String SECURITY_TOKEN_PARAM = "st";
  protected static final String HEADERS_PARAM = "headers";
  protected static final String NOCACHE_PARAM = "nocache";
  protected static final String SIGN_VIEWER = "signViewer";
  protected static final String SIGN_OWNER = "signOwner";
  protected static final String CONTENT_TYPE_PARAM = "contentType";
  protected static final String NUM_ENTRIES_PARAM = "numEntries";
  protected static final String DEFAULT_NUM_ENTRIES = "3";
  protected static final String GET_SUMMARIES_PARAM = "getSummaries";

  private final SecurityTokenDecoder securityTokenDecoder;
  private final ContentFetcherFactory contentFetcherFactory;
  private final ContentRewriter rewriter;

  @Inject
  public MakeRequestHandler(ContentFetcherFactory contentFetcherFactory,
                            SecurityTokenDecoder securityTokenDecoder,
                            ContentRewriter rewriter) {
    this.contentFetcherFactory = contentFetcherFactory;
    this.securityTokenDecoder = securityTokenDecoder;
    this.rewriter = rewriter;
  }

  /**
   * Executes a request, returning the response as JSON to be handled by makeRequest.
   */
  @Override
  public void fetch(HttpServletRequest request, HttpServletResponse response)
      throws GadgetException, IOException {
    HttpRequest rcr = buildHttpRequest(request);

    // Figure out whether authentication is required
    Auth auth = Auth.parse(getParameter(request, Preload.AUTHZ_ATTR, ""));
    SecurityToken authToken = null;
    if (auth != Auth.NONE) {
      authToken = extractAndValidateToken(request);
    }

    // Build the chain of fetchers that will handle the request
    HttpFetcher fetcher = getHttpFetcher(auth, authToken, request);

    // Serialize the response
    HttpResponse results = fetcher.fetch(rcr);

    // Serialize the response
    String output = convertResponseToJson(authToken, request, results);

    // Find and set the refresh interval
    setResponseHeaders(request, response, results);

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json; charset=utf-8");
    response.getWriter().write(UNPARSEABLE_CRUFT + output);
  }

  /**
   * Generate a remote content request based on the parameters
   * sent from the client.
   * @throws GadgetException
   */
  private HttpRequest buildHttpRequest(HttpServletRequest request) throws GadgetException {
    try {
      String encoding = request.getCharacterEncoding();
      if (encoding == null) {
        encoding = "UTF-8";
      }

      URI url = validateUrl(request.getParameter(URL_PARAM));
      String method = request.getMethod();
      Map<String, List<String>> headers = null;
      byte[] postBody = null;

      method = getParameter(request, METHOD_PARAM, "GET");
      postBody = getParameter(request, POST_DATA_PARAM, "").getBytes();

      String headerData = request.getParameter(HEADERS_PARAM);
      if (headerData == null || headerData.length() == 0) {
        headers = Collections.emptyMap();
      } else {
        // We actually only accept single key value mappings now.
        headers = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
        String[] headerList = headerData.split("&");
        for (String header : headerList) {
          String[] parts = header.split("=");
          if (parts.length != 2) {
            throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
                "Malformed header specified,");
          }
          headers.put(URLDecoder.decode(parts[0], encoding),
              Arrays.asList(URLDecoder.decode(parts[1], encoding)));
        }
      }

      removeUnsafeHeaders(headers);

      HttpRequest.Options options = new HttpRequest.Options();
      options.ignoreCache = "1".equals(request.getParameter(NOCACHE_PARAM));
      if (request.getParameter(SIGN_VIEWER) != null) {
        options.viewerSigned = Boolean.parseBoolean(request.getParameter(SIGN_VIEWER));
      }
      if (request.getParameter(SIGN_OWNER) != null) {
        options.ownerSigned = Boolean.parseBoolean(request.getParameter(SIGN_OWNER));
      }
      if (request.getParameter(GADGET_PARAM) != null) {
        options.gadgetUri = URI.create(request.getParameter(GADGET_PARAM));
      }
      options.rewriter = rewriter;

      // Allow the rewriter to use an externally forced mime type. This is needed
      // allows proper rewriting of <script src="x"/> where x is returned with
      // a content type like text/html which unfortunately happens all too often
      options.rewriteMimeType = request.getParameter(REWRITE_MIME_TYPE_PARAM);

      return new HttpRequest(method, url, headers, postBody, options);
    } catch (UnsupportedEncodingException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Removes unsafe headers from the header set.
   */
  private void removeUnsafeHeaders(Map<String, List<String>> headers) {
    // Host must be removed.
    final String[] badHeaders = new String[] {
        // No legitimate reason to over ride these.
        // TODO: We probably need to test variations as well.
        "Host", "Accept", "Accept-Encoding"
    };
    for (String bad : badHeaders) {
      headers.remove(bad);
    }
  }

  /**
   * At the moment our fetcher chain is short.  In the future we might add
   * additional layers for things like caching or throttling.
   *
   * Whatever we do needs to return a reference to the OAuthFetcher, if it is
   * present, so we can pull data out as needed.
   */
  private HttpFetcher getHttpFetcher(Auth auth, SecurityToken token, HttpServletRequest request)
      throws GadgetException {
    switch (auth) {
      case NONE:
        return contentFetcherFactory.get();
      case SIGNED:
        return contentFetcherFactory.getSigningFetcher(token);
      case AUTHENTICATED:
        return contentFetcherFactory.getOAuthFetcher(token, new OAuthRequestParams(request));
      default:
        return contentFetcherFactory.get();
    }
  }

  /**
   * Format a response as JSON, including additional JSON inserted by
   * chained content fetchers.
   */
  private String convertResponseToJson(SecurityToken authToken, HttpServletRequest request,
      HttpResponse results) throws GadgetException {
    try {
      JSONObject resp = new JSONObject();
      String originalUrl = request.getParameter(ProxyBase.URL_PARAM);
      String body = results.getResponseAsString();
      if ("FEED".equals(request.getParameter(CONTENT_TYPE_PARAM))) {
        resp.put("body", processFeed(originalUrl, request, body));
      } else {
        resp.put("body", body);
      }
      resp.put("rc", results.getHttpStatusCode());

      if (authToken != null) {
        String updatedAuthToken = authToken.getUpdatedToken();
        if (updatedAuthToken != null) {
          resp.put("st", updatedAuthToken);
        }
      }

      // Merge in additional response data
      for (Map.Entry<String, String> entry : results.getMetadata().entrySet()) {
        resp.put(entry.getKey(), entry.getValue());
      }
      // Use raw param as key as URL may have to be decoded
      return new JSONObject().put(originalUrl, resp).toString();
    } catch (JSONException e) {
      return "";
    }
  }

  /**
   * @param request
   * @return A valid token for the given input.
   */
  private SecurityToken extractAndValidateToken(HttpServletRequest request)
      throws GadgetException {
    String token = getParameter(request, SECURITY_TOKEN_PARAM, "");
    try {
      return securityTokenDecoder.createToken(token);
    } catch (SecurityTokenException e) {
      throw new GadgetException(GadgetException.Code.INVALID_SECURITY_TOKEN, e);
    }
  }

  /**
   * Processes a feed (RSS or Atom) using FeedProcessor.
   */
  private String processFeed(String url, HttpServletRequest req, String xml)
      throws GadgetException {
    boolean getSummaries = Boolean.parseBoolean(getParameter(req, GET_SUMMARIES_PARAM, "false"));
    int numEntries = Integer.parseInt(getParameter(req, NUM_ENTRIES_PARAM, DEFAULT_NUM_ENTRIES));
    return new FeedProcessor().process(url, xml, getSummaries, numEntries).toString();
  }
}

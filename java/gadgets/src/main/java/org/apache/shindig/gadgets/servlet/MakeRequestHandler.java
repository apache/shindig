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

import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.FetchResponseUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles gadgets.io.makeRequest requests.
 *
 * Unlike ProxyHandler, this may perform operations such as OAuth or signed fetch.
 */
@Singleton
public class MakeRequestHandler extends ProxyBase {
  // Relaxed visibility for ease of integration. Try to avoid relying on these.
  public static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";
  public static final String POST_DATA_PARAM = "postData";
  public static final String METHOD_PARAM = "httpMethod";
  public static final String HEADERS_PARAM = "headers";
  public static final String NOCACHE_PARAM = "nocache";
  public static final String CONTENT_TYPE_PARAM = "contentType";
  public static final String NUM_ENTRIES_PARAM = "numEntries";
  public static final String DEFAULT_NUM_ENTRIES = "3";
  public static final String GET_SUMMARIES_PARAM = "getSummaries";
  public static final String AUTHZ_PARAM = "authz";

  private final RequestPipeline requestPipeline;
  private final ContentRewriterRegistry contentRewriterRegistry;

  @Inject
  public MakeRequestHandler(RequestPipeline requestPipeline,
      ContentRewriterRegistry contentRewriterRegistry) {
    this.requestPipeline = requestPipeline;
    this.contentRewriterRegistry = contentRewriterRegistry;
  }

  /**
   * Executes a request, returning the response as JSON to be handled by makeRequest.
   */
  @Override
  public void fetch(HttpServletRequest request, HttpServletResponse response)
      throws GadgetException, IOException {
    HttpRequest rcr = buildHttpRequest(request);

    // Serialize the response
    HttpResponse results = requestPipeline.execute(rcr);

    // Rewrite the response
    if (contentRewriterRegistry != null) {
      results = contentRewriterRegistry.rewriteHttpResponse(rcr, results);
    }

    // Serialize the response
    String output = convertResponseToJson(rcr.getSecurityToken(), request, results);

    // Find and set the refresh interval
    setResponseHeaders(request, response, results);

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(UNPARSEABLE_CRUFT + output);
  }

  /**
   * Generate a remote content request based on the parameters
   * sent from the client.
   * @throws GadgetException
   */
  private HttpRequest buildHttpRequest(HttpServletRequest request) throws GadgetException {
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }

    Uri url = validateUrl(request.getParameter(URL_PARAM));

    HttpRequest req = new HttpRequest(url)
        .setMethod(getParameter(request, METHOD_PARAM, "GET"))
        .setPostBody(getParameter(request, POST_DATA_PARAM, "").getBytes())
        .setContainer(getContainer(request));

    String headerData = getParameter(request, HEADERS_PARAM, "");
    if (headerData.length() > 0) {
      String[] headerList = headerData.split("&");
      for (String header : headerList) {
        String[] parts = header.split("=");
        if (parts.length != 2) {
          throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
              "Malformed header specified,");
        }
        String headerName = Utf8UrlCoder.decode(parts[0]);
        if (!HttpRequestHandler.BAD_HEADERS.contains(headerName.toUpperCase())) {
          req.addHeader(headerName, Utf8UrlCoder.decode(parts[1]));
        }
      }
    }

    req.setIgnoreCache("1".equals(request.getParameter(NOCACHE_PARAM)));

    if (request.getParameter(GADGET_PARAM) != null) {
      req.setGadget(Uri.parse(request.getParameter(GADGET_PARAM)));
    }

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
    // Allow the rewriter to use an externally forced mime type. This is needed
    // allows proper rewriting of <script src="x"/> where x is returned with
    // a content type like text/html which unfortunately happens all too often
    req.setRewriteMimeType(request.getParameter(REWRITE_MIME_TYPE_PARAM));

    // Figure out whether authentication is required
    AuthType auth = AuthType.parse(getParameter(request, AUTHZ_PARAM, null));
    req.setAuthType(auth);
    if (auth != AuthType.NONE) {
      req.setSecurityToken(extractAndValidateToken(request));
      req.setOAuthArguments(new OAuthArguments(auth, request));
    }
    return req;
  }

  /**
   * Format a response as JSON, including additional JSON inserted by
   * chained content fetchers.
   */
  private String convertResponseToJson(SecurityToken authToken, HttpServletRequest request,
      HttpResponse results) throws GadgetException {
    try {
      String originalUrl = request.getParameter(ProxyBase.URL_PARAM);
      String body = "";
      if (results.getHttpStatusCode() == 200) {
        body = results.getResponseAsString();

        if ("FEED".equals(request.getParameter(CONTENT_TYPE_PARAM))) {
          body = processFeed(originalUrl, request, body);
        }
      }
      JSONObject resp = FetchResponseUtils.getResponseAsJson(results, null, body);

      if (authToken != null) {
        String updatedAuthToken = authToken.getUpdatedToken();
        if (updatedAuthToken != null) {
          resp.put("st", updatedAuthToken);
        }
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
  private SecurityToken extractAndValidateToken(HttpServletRequest request) throws GadgetException {
    SecurityToken token = new AuthInfo(request).getSecurityToken();
    if (token == null) {
      throw new GadgetException(GadgetException.Code.INVALID_SECURITY_TOKEN);
    }
    return token;
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

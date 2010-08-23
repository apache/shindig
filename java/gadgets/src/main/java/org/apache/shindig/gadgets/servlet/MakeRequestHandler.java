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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.FetchResponseUtils;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.UriCommon;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles gadgets.io.makeRequest requests.
 *
 * Unlike ProxyHandler, this may perform operations such as OAuth or signed fetch.
 */
@Singleton
public class MakeRequestHandler {
  // Relaxed visibility for ease of integration. Try to avoid relying on these.
  public static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";
  public static final String POST_DATA_PARAM = "postData";
  public static final String METHOD_PARAM = "httpMethod";
  public static final String HEADERS_PARAM = "headers";
  public static final String CONTENT_TYPE_PARAM = "contentType";
  public static final String NUM_ENTRIES_PARAM = "numEntries";
  public static final String DEFAULT_NUM_ENTRIES = "3";
  public static final String GET_SUMMARIES_PARAM = "getSummaries";
  public static final String GET_FULL_HEADERS_PARAM = "getFullHeaders";
  public static final String AUTHZ_PARAM = "authz";

  private final RequestPipeline requestPipeline;
  private final ResponseRewriterRegistry contentRewriterRegistry;

  @Inject
  public MakeRequestHandler(RequestPipeline requestPipeline,
      ResponseRewriterRegistry contentRewriterRegistry) {
    this.requestPipeline = requestPipeline;
    this.contentRewriterRegistry = contentRewriterRegistry;
  }

  /**
   * Executes a request, returning the response as JSON to be handled by makeRequest.
   */
  public void fetch(HttpServletRequest request, HttpServletResponse response)
      throws GadgetException, IOException {
    HttpRequest rcr = buildHttpRequest(request);

    // Serialize the response
    HttpResponse results = requestPipeline.execute(rcr);

    // Rewrite the response
    if (contentRewriterRegistry != null) {
      try {
        results = contentRewriterRegistry.rewriteHttpResponse(rcr, results);
      } catch (RewritingException e) {
        throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e,
            e.getHttpStatusCode());
      }
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
  protected HttpRequest buildHttpRequest(HttpServletRequest request) throws GadgetException {
    String urlStr = request.getParameter(Param.URL.getKey());
    if (urlStr == null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          Param.URL.getKey() + " parameter is missing.", HttpResponse.SC_BAD_REQUEST);
    }

    Uri url = null;
    try {
      url = ServletUtil.validateUrl(Uri.parse(urlStr));
    } catch (IllegalArgumentException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "Invalid " + Param.URL.getKey() + " parameter", HttpResponse.SC_BAD_REQUEST);
    }

    HttpRequest req = new HttpRequest(url)
        .setMethod(getParameter(request, METHOD_PARAM, "GET"))
        .setContainer(getContainer(request));

    setPostData(request,req);

    String headerData = getParameter(request, HEADERS_PARAM, "");
    if (headerData.length() > 0) {
      String[] headerList = StringUtils.split(headerData, '&');
      for (String header : headerList) {
        String[] parts = StringUtils.splitPreserveAllTokens(header, '=');
        if (parts.length != 2) {
          throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
              "Malformed header param specified:" + header, HttpResponse.SC_BAD_REQUEST);
        }
        String headerName = Utf8UrlCoder.decode(parts[0]);
        if (!HttpRequestHandler.BAD_HEADERS.contains(headerName.toUpperCase())) {
          req.addHeader(headerName, Utf8UrlCoder.decode(parts[1]));
        }
      }
    }

    // Set the default content type  for post requests when a content type is not specified
    if ("POST".equals(req.getMethod()) && req.getHeader("Content-Type")==null) {
      req.addHeader("Content-Type", "application/x-www-form-urlencoded");
    }

    req.setIgnoreCache("1".equals(request.getParameter(Param.NO_CACHE.getKey())));

    if (request.getParameter(Param.GADGET.getKey()) != null) {
      req.setGadget(Uri.parse(request.getParameter(Param.GADGET.getKey())));
    }

    // If the proxy request specifies a refresh param then we want to force the min TTL for
    // the retrieved entry in the cache regardless of the headers on the content when it
    // is fetched from the original source.
    if (request.getParameter(Param.REFRESH.getKey()) != null) {
      try {
        req.setCacheTtl(Integer.parseInt(request.getParameter(Param.REFRESH.getKey())));
      } catch (NumberFormatException nfe) {
        // Ignore
      }
    }
    // Allow the rewriter to use an externally forced mime type. This is needed
    // allows proper rewriting of <script src="x"/> where x is returned with
    // a content type like text/html which unfortunately happens all too often
    req.setRewriteMimeType(request.getParameter(Param.REWRITE_MIME_TYPE.getKey()));

    // Figure out whether authentication is required
    AuthType auth = AuthType.parse(getParameter(request, AUTHZ_PARAM, null));
    req.setAuthType(auth);
    if (auth != AuthType.NONE) {
      req.setSecurityToken(extractAndValidateToken(request));
      req.setOAuthArguments(new OAuthArguments(auth, request));
    }

    ServletUtil.setXForwardedForHeader(request, req);
    return req;
  }

  /**
   * Set http request post data according to servlet request.
   * It uses header encoding if available, and defaulted to utf8
   * Override the function if different behavior is needed.
   */
  protected void setPostData(HttpServletRequest request, HttpRequest req)
      throws GadgetException {
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }
    try {
      req.setPostBody(getParameter(request, POST_DATA_PARAM, "")
          .getBytes(encoding.toUpperCase()));
    } catch (UnsupportedEncodingException e) {
      // We might consider enumerating at least a small list of encodings
      // that we must always honor. For now, we return SC_BAD_REQUEST since
      // the encoding parameter could theoretically be anything.
      throw new GadgetException(Code.HTML_PARSE_ERROR, e, HttpResponse.SC_BAD_REQUEST);
    }
  }

  /**
   * Format a response as JSON, including additional JSON inserted by
   * chained content fetchers.
   */
  protected String convertResponseToJson(SecurityToken authToken, HttpServletRequest request,
      HttpResponse results) throws GadgetException {
    boolean getFullHeaders =
        Boolean.parseBoolean(getParameter(request, GET_FULL_HEADERS_PARAM, "false"));
    String originalUrl = request.getParameter(Param.URL.getKey());
    String body = results.getResponseAsString();
    if (body.length() > 0) {
      if ("FEED".equals(request.getParameter(CONTENT_TYPE_PARAM))) {
        body = processFeed(originalUrl, request, body);
      }
    }
    Map<String, Object> resp =
        FetchResponseUtils.getResponseAsJson(results, null, body, getFullHeaders);

    if (authToken != null) {
      String updatedAuthToken = authToken.getUpdatedToken();
      if (updatedAuthToken != null) {
        resp.put("st", updatedAuthToken);
      }
    }

    // Use raw param as key as URL may have to be decoded
    return JsonSerializer.serialize(Collections.singletonMap(originalUrl, resp));
  }

  protected RequestPipeline getRequestPipeline() {
    return requestPipeline;
  }

  /**
   * @param request
   * @return A valid token for the given input.
   */
  private SecurityToken extractAndValidateToken(HttpServletRequest request) throws GadgetException {
    SecurityToken token = new AuthInfo(request).getSecurityToken();
    if (token == null) {
      // TODO: Determine appropriate external error code for this.
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
    int numEntries;
    try {
      numEntries = Integer.valueOf(getParameter(req, NUM_ENTRIES_PARAM, DEFAULT_NUM_ENTRIES));
    } catch (NumberFormatException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "numEntries paramater is not a number", HttpResponse.SC_BAD_REQUEST);
    }
    return new FeedProcessor().process(url, xml, getSummaries, numEntries).toString();
  }

  /**
   * Extracts the container name from the request.
   */
  @SuppressWarnings("deprecation")
  protected static String getContainer(HttpServletRequest request) {
    String container = request.getParameter(Param.CONTAINER.getKey());
    if (container == null) {
      container = request.getParameter(Param.SYND.getKey());
    }
    return container != null ? container : ContainerConfig.DEFAULT_CONTAINER;
  }

  /**
   * getParameter helper method, returning default value if param not present.
   */
  protected static String getParameter(HttpServletRequest request, String key, String defaultValue) {
    String ret = request.getParameter(key);
    return ret != null ? ret : defaultValue;
  }

  /**
   * Sets cache control headers for the response.
   */
  @SuppressWarnings("boxing")
  protected static void setResponseHeaders(HttpServletRequest request,
      HttpServletResponse response, HttpResponse results) throws GadgetException {
    int refreshInterval = 0;
    if (results.isStrictNoCache() || "1".equals(request.getParameter(UriCommon.Param.NO_CACHE.getKey()))) {
      refreshInterval = 0;
    } else if (request.getParameter(UriCommon.Param.REFRESH.getKey()) != null) {
      try {
        refreshInterval =  Integer.valueOf(request.getParameter(UriCommon.Param.REFRESH.getKey()));
      } catch (NumberFormatException nfe) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
            "refresh parameter is not a number", HttpResponse.SC_BAD_REQUEST);
      }
    } else {
      refreshInterval = Math.max(60 * 60, (int)(results.getCacheTtl() / 1000L));
    }
    HttpUtil.setCachingHeaders(response, refreshInterval, false);

    // Always set Content-Disposition header as XSS prevention mechanism.
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");

    if (response.getContentType() == null) {
      response.setContentType("application/octet-stream");
    }
  }
}

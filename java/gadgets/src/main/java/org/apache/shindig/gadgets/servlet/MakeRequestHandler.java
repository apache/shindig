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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.auth.AuthInfoUtil;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.JsonSerializer;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.FetchResponseUtils;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.admin.GadgetAdminStore;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth2.OAuth2Arguments;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.gadgets.process.Processor;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterList.RewriteFlow;
import org.apache.shindig.gadgets.rewrite.ResponseRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewriterRegistry;
import org.apache.shindig.gadgets.rewrite.RewritingException;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Handles gadgets.io.makeRequest requests.
 *
 * Unlike ProxyHandler, this may perform operations such as OAuth or signed fetch.
 */
@Singleton
public class MakeRequestHandler implements ContainerConfig.ConfigObserver {
  // Relaxed visibility for ease of integration. Try to avoid relying on these.
  public static final String ALIAS_PARAM = "alias";
  public static final String POST_DATA_PARAM = "postData";
  public static final String METHOD_PARAM = "httpMethod";
  public static final String HEADERS_PARAM = "headers";
  public static final String CONTENT_TYPE_PARAM = "contentType";
  public static final String NUM_ENTRIES_PARAM = "numEntries";
  public static final String DEFAULT_NUM_ENTRIES = "3";
  public static final String GET_SUMMARIES_PARAM = "getSummaries";
  public static final String GET_FULL_HEADERS_PARAM = "getFullHeaders";
  public static final String AUTHZ_PARAM = "authz";
  public static final String MAX_POST_SIZE_KEY = "gadgets.jsonProxyUrl.maxPostSize";
  public static final String MULTI_PART_FORM_POST = "MPFP";
  public static final String MULTI_PART_FORM_POST_IFRAME = "iframe";
  public static final String GADGETS_FEATURES = "gadgets.features";
  public static final String CORE_IO = "core.io";
  public static final String UNPARSEABLE_CRUFT = "unparseableCruft";
  public static final int MAX_POST_SIZE_DEFAULT = 5 * 1024 * 1024; // 5 MiB
  public static final String IFRAME_RESPONSE_PREFIX = "<html><head></head><body><textarea></textarea><script type='text/javascript'>document.getElementsByTagName('TEXTAREA')[0].value='";
  public static final String IFRAME_RESPONSE_SUFFIX = "';</script></body></html>";

  private final Map<String, String> unparseableCruftMsgs;
  private final RequestPipeline requestPipeline;
  private final ResponseRewriterRegistry contentRewriterRegistry;
  private final Provider<FeedProcessor> feedProcessorProvider;
  private final GadgetAdminStore gadgetAdminStore;
  private final Processor processor;
  private final LockedDomainService lockedDomainService;
  private final Map<String, Integer> maxPostSizes;

  @Inject
  public MakeRequestHandler(
          ContainerConfig config,
          RequestPipeline requestPipeline,
          @RewriterRegistry(rewriteFlow = RewriteFlow.DEFAULT) ResponseRewriterRegistry contentRewriterRegistry,
          Provider<FeedProcessor> feedProcessorProvider, GadgetAdminStore gadgetAdminStore,
          Processor processor, LockedDomainService lockedDomainService) {

    this.requestPipeline = requestPipeline;
    this.contentRewriterRegistry = contentRewriterRegistry;
    this.feedProcessorProvider = feedProcessorProvider;
    this.gadgetAdminStore = gadgetAdminStore;
    this.processor = processor;
    this.lockedDomainService = lockedDomainService;
    this.maxPostSizes = Maps.newConcurrentMap();
    this.unparseableCruftMsgs = Maps.newConcurrentMap();
    config.addConfigObserver(this, true);
  }

  /**
   * Executes a request, returning the response as JSON to be handled by makeRequest.
   */
  public void fetch(HttpServletRequest request, HttpServletResponse response)
          throws GadgetException, IOException {

    HttpRequest rcr = buildHttpRequest(request);
    String container = rcr.getContainer();
    final Uri gadgetUri = rcr.getGadget();
    if (gadgetUri == null) {
      throw new GadgetException(GadgetException.Code.MISSING_PARAMETER,
              "Unable to find gadget in request", HttpResponse.SC_FORBIDDEN);
    }

    Gadget gadget;
    GadgetContext context = new HttpGadgetContext(request) {
      @Override
      public Uri getUrl() {
        return gadgetUri;
      }
      @Override
      public boolean getIgnoreCache() {
        return getParameter("bypassSpecCache").equals("1");
      }
    };
    try {
      gadget = processor.process(context);
    } catch (ProcessingException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
              "Error processing gadget", e, HttpResponse.SC_BAD_REQUEST);
    }

    // Validate gadget is correct for the host.
    // Ensures that the gadget has not hand crafted this request to represent itself as
    // another gadget in a locked domain environment.
    if (!lockedDomainService.isGadgetValidForHost(context.getHost(), gadget, container)) {
      throw new GadgetException(GadgetException.Code.GADGET_HOST_MISMATCH,
              "The gadget is incorrect for this request", HttpResponse.SC_FORBIDDEN);
    }

    if (!gadgetAdminStore.isWhitelisted(container, gadgetUri.toString())) {
      throw new GadgetException(GadgetException.Code.NON_WHITELISTED_GADGET,
              "The requested content is unavailable", HttpResponse.SC_FORBIDDEN);
    }

    // Serialize the response
    HttpResponse results = requestPipeline.execute(rcr);

    // Rewrite the response
    if (contentRewriterRegistry != null) {
      try {
        results = contentRewriterRegistry.rewriteHttpResponse(rcr, results, gadget);
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
    response.setCharacterEncoding("UTF-8");

    PrintWriter out = response.getWriter();
    if ("1".equals(getParameter(request, MULTI_PART_FORM_POST_IFRAME, null))) {
      response.setContentType("text/html");
      out.write(IFRAME_RESPONSE_PREFIX);
      out.write(StringEscapeUtils.escapeEcmaScript(this.unparseableCruftMsgs.get(container)));
      out.write(StringEscapeUtils.escapeEcmaScript(output));
      out.write(IFRAME_RESPONSE_SUFFIX);
    } else {
      response.setContentType("application/json");
      out.write(this.unparseableCruftMsgs.get(container) + output);
    }
  }

  /**
   * Generate a remote content request based on the parameters sent from the client.
   *
   * @throws GadgetException
   */
  protected HttpRequest buildHttpRequest(HttpServletRequest request) throws GadgetException {
    String urlStr = getParameter(request, Param.URL.getKey(), null);
    if (urlStr == null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER, Param.URL.getKey()
              + " parameter is missing.", HttpResponse.SC_BAD_REQUEST);
    }

    Uri url;
    try {
      url = ServletUtil.validateUrl(Uri.parse(urlStr));
    } catch (IllegalArgumentException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER, "Invalid "
              + Param.URL.getKey() + " parameter", HttpResponse.SC_BAD_REQUEST);
    }

    final SecurityToken token = AuthInfoUtil.getSecurityTokenFromRequest(request);
    String container = null;
    Uri gadgetUri = null;
    if ("1".equals(getParameter(request, MULTI_PART_FORM_POST, null))) {
      // This endpoint is being used by the proxied-form-post feature.
      // Require a token.
      if (token == null) {
        throw new GadgetException(GadgetException.Code.INVALID_SECURITY_TOKEN);
      }
    }

    // If we have a token, we should use it.
    if (token != null && !token.isAnonymous()) {
      container = token.getContainer();
      String appurl = token.getAppUrl();
      if (appurl != null) {
        gadgetUri = Uri.parse(appurl);
      }
    } else {
      container = getContainer(request);
      String gadgetUrl = getParameter(request, Param.GADGET.getKey(), null);
      if (gadgetUrl != null) {
        gadgetUri = Uri.parse(gadgetUrl);
      }
    }

    HttpRequest req = new HttpRequest(url).setMethod(getParameter(request, METHOD_PARAM, "GET"))
            .setContainer(container).setGadget(gadgetUri);

    if ("POST".equals(req.getMethod()) || "PUT".equals(req.getMethod())) {
      setPostData(container, request, req);
    }

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

    // Set the default content type for post requests when a content type is not specified
    if ("POST".equals(req.getMethod()) && req.getHeader("Content-Type") == null) {
      req.addHeader("Content-Type", "application/x-www-form-urlencoded");
    } else if ("1".equals(getParameter(request, MULTI_PART_FORM_POST, null))) {
      // We need the entire header from the original request because it comes with a boundary value
      // we need to reuse.
      req.setHeader("Content-Type", request.getHeader("Content-Type"));
    }

    req.setIgnoreCache("1".equals(getParameter(request, Param.NO_CACHE.getKey(), null)));



    // If the proxy request specifies a refresh param then we want to force the min TTL for
    // the retrieved entry in the cache regardless of the headers on the content when it
    // is fetched from the original source.
    String refresh = getParameter(request, Param.REFRESH.getKey(), null);
    if (refresh != null) {
      try {
        req.setCacheTtl(Integer.parseInt(refresh));
      } catch (NumberFormatException ignore) {}
    }
    // Allow the rewriter to use an externally forced mime type. This is needed
    // allows proper rewriting of <script src="x"/> where x is returned with
    // a content type like text/html which unfortunately happens all too often
    req.setRewriteMimeType(getParameter(request, Param.REWRITE_MIME_TYPE.getKey(), null));

    // Figure out whether authentication is required
    AuthType auth = AuthType.parse(getParameter(request, AUTHZ_PARAM, null));
    req.setAuthType(auth);
    if (auth != AuthType.NONE) {
      req.setSecurityToken(extractAndValidateToken(request));
      if (auth == AuthType.OAUTH2) {
        req.setOAuth2Arguments(new OAuth2Arguments(request));
      } else {
        req.setOAuthArguments(new OAuthArguments(auth, request));
      }
    } else {
      // if not authenticated, set the token that we received
      req.setSecurityToken(token);
    }

    if (req.getHeader("User-Agent") == null) {
      final String userAgent = request.getHeader("User-Agent");
      if (userAgent != null) {
        req.setHeader("User-Agent", userAgent);
      }
    }

    ServletUtil.setXForwardedForHeader(request, req);
    return req;
  }

  /**
   * Set http request post data according to servlet request. It uses header encoding if available,
   * and defaulted to utf8 Override the function if different behavior is needed.
   */
  protected void setPostData(String container, HttpServletRequest request, HttpRequest req)
          throws GadgetException {
    if (maxPostSizes.get(container) < request.getContentLength()) {
      throw new GadgetException(GadgetException.Code.POST_TOO_LARGE, "Posted data too large.",
          HttpResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    }

    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }
    try {
      String contentType = request.getHeader("Content-Type");
      if (contentType != null && contentType.startsWith("multipart/form-data")) {
        // TODO: This will read the entire posted response in server memory.
        // Is there a way to stream this even with OAUTH flows?
        req.setPostBody(request.getInputStream());
      } else {
        req.setPostBody(getParameter(request, POST_DATA_PARAM, "").getBytes(encoding.toUpperCase()));
      }
    } catch (UnsupportedEncodingException e) {
      // We might consider enumerating at least a small list of encodings
      // that we must always honor. For now, we return SC_BAD_REQUEST since
      // the encoding parameter could theoretically be anything.
      throw new GadgetException(Code.HTML_PARSE_ERROR, e, HttpResponse.SC_BAD_REQUEST);
    } catch (IOException e) {
      // Something went wrong reading the request data.
      // TODO: perhaps also support a max post size and enforce it by throwing and catching
      // exceptions here.
      throw new GadgetException(Code.INTERNAL_SERVER_ERROR, e, HttpResponse.SC_BAD_REQUEST);
    }
  }

  /**
   * Format a response as JSON, including additional JSON inserted by chained content fetchers.
   */
  protected String convertResponseToJson(SecurityToken authToken, HttpServletRequest request,
          HttpResponse results) throws GadgetException {
    boolean getFullHeaders = Boolean.parseBoolean(getParameter(request, GET_FULL_HEADERS_PARAM,
            "false"));
    String originalUrl = getParameter(request, Param.URL.getKey(), null);
    String body = results.getResponseAsString();
    if (body.length() > 0) {
      if ("FEED".equals(getParameter(request, CONTENT_TYPE_PARAM, null))) {
        body = processFeed(originalUrl, request, body);
      }
    }
    Map<String, Object> resp = FetchResponseUtils.getResponseAsJson(results, null, body,
            getFullHeaders);

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
    SecurityToken token = AuthInfoUtil.getSecurityTokenFromRequest(request);
    if (token == null) {
      // TODO: Determine appropriate external error code for this.
      throw new GadgetException(GadgetException.Code.INVALID_SECURITY_TOKEN);
    }
    return token;
  }

  /**
   * Processes a feed (RSS or Atom) using FeedProcessor.
   */
  private String processFeed(String url, HttpServletRequest req, String xml) throws GadgetException {
    boolean getSummaries = Boolean.parseBoolean(getParameter(req, GET_SUMMARIES_PARAM, "false"));
    int numEntries;
    try {
      numEntries = Integer.valueOf(getParameter(req, NUM_ENTRIES_PARAM, DEFAULT_NUM_ENTRIES));
    } catch (NumberFormatException e) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
              "numEntries paramater is not a number", HttpResponse.SC_BAD_REQUEST);
    }
    return feedProcessorProvider.get().process(url, xml, getSummaries, numEntries).toString();
  }

  /**
   * Extracts the container name from the request.
   */
  @SuppressWarnings("deprecation")
  protected static String getContainer(HttpServletRequest request) {
    String container = getParameter(request, Param.CONTAINER.getKey(), null);
    if (container == null) {
      container = getParameter(request, Param.SYND.getKey(), null);
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
  protected void setResponseHeaders(HttpServletRequest request,
          HttpServletResponse response, HttpResponse results) throws GadgetException {
    int refreshInterval = 0;
    if (results.isStrictNoCache()
            || "1".equals(getParameter(request, Param.NO_CACHE.getKey(), null))) {
      refreshInterval = 0;
    } else if (getParameter(request, Param.REFRESH.getKey(), null) != null) {
      try {
        refreshInterval = Integer.valueOf(getParameter(request, Param.REFRESH.getKey(), null));
      } catch (NumberFormatException nfe) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
                "refresh parameter is not a number", HttpResponse.SC_BAD_REQUEST);
      }
    } else {
      refreshInterval = Math.max(60 * 60, (int) (results.getCacheTtl() / 1000L));
    }
    HttpUtil.setCachingHeaders(response, refreshInterval, false);

    /*
     * The proxied-form-post feature uses this endpoint to post a form
     * element (in order to support file upload).
     *
     * For cross-browser support (IE) it requires that we use a hidden iframe
     * to post the request. Setting Content-Disposition breaks that solution.
     * In this particular case, we will always have a security token, so we
     * shouldn't need to be as cautious here.
     */
    if (!"1".equals(getParameter(request, MULTI_PART_FORM_POST, null))) {
      // Always set Content-Disposition header as XSS prevention mechanism.
      response.setHeader("Content-Disposition", "attachment;filename=p.txt");
    }

    if (response.getContentType() == null) {
      response.setContentType("application/octet-stream");
    }
  }

  public void containersChanged(ContainerConfig config, Collection<String> changed,
      Collection<String> removed) {
    for (String container : changed) {
      Integer maxPostSize = config.getInt(container, MAX_POST_SIZE_KEY);
      if (maxPostSize == 0) {
        maxPostSize = MAX_POST_SIZE_DEFAULT;
      }
      maxPostSizes.put(container, maxPostSize);
      Map<String, Map<String, String>> features = config.getMap(container, GADGETS_FEATURES);
      if (features != null) {
        Map<String, String> coreIO = (Map<String, String>) features.get(CORE_IO);
        if (coreIO != null) {
          unparseableCruftMsgs.put(container, coreIO.get(UNPARSEABLE_CRUFT));
        }
      }
    }
    for (String container : removed) {
      maxPostSizes.remove(container);
      unparseableCruftMsgs.remove(container);
    }
  }
}

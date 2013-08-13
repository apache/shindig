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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.JsonProperty;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
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
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * An alternate implementation of the Http proxy service using the standard API dispatcher for REST
 * / JSON-RPC calls. The basic form of the request is as follows
 * ...
 * method : http.<HTTP method name>
 * params : {
 *    href : <endpoint to fetch content from>,
 *    headers : { <header-name> : [<header-value>, ...]},
 *    format : <"text", "json", "feed">
 *    body : <request body>
 *    gadget : <url of gadget spec for calling application>
 *    authz: : <none | oauth | signed>,
 *    sign_owner: <boolean, default true>
 *    sign_viewer: <boolean, default true>
 *    ...<additional auth arguments. See OAuthArguments>
 *    refreshInterval : <Integer time in seconds to force as cache TTL. Default is to use response headers>
 *    noCache : <Bypass container content cache. Default false>
 *    sanitize : <Force sanitize fetched content. Default false>
 *    summarize : <If contentType == "FEED" summarize the results. Default false>
 *    entryCount : <If contentType == "FEED" limit results to specified no of items. Default 3>
 * }
 *
 * A successful response response will have the form
 *
 * data : {
 *    status : <HTTP status code.>
 *    headers : { <header name> : [<header val1>, <header val2>, ...], ...}
 *    content : <response body>: string if 'text', JSON is 'feed' or 'json' format
 *    token : <If security token provides a renewed value.>
 *    metadata : { <metadata entry> : <metadata value>, ...}
 * }
 *
 * It's important to note that requests which generate HTTP error responses such as 500 are returned
 * in the above format. The RPC itself succeeded in these cases. If an RPC error occurred the client
 * should introspect the error message for information as to the cause.
 *
 * TODO: send errors using "result", not plain content
 *
 * @see MakeRequestHandler
 */
@Service(name = "http")
public class HttpRequestHandler {

  static final Set<String> BAD_HEADERS = ImmutableSet.of("HOST", "ACCEPT-ENCODING");

  private static final String CLASSNAME = HttpRequestHandler.class.getName();
  private static final Logger LOG = Logger.getLogger(CLASSNAME, MessageKeys.MESSAGES);

  private final RequestPipeline requestPipeline;
  private final ResponseRewriterRegistry contentRewriterRegistry;
  private final Provider<FeedProcessor> feedProcessorProvider;
  private final Processor processor;

  @Inject
  public HttpRequestHandler(RequestPipeline requestPipeline,
      @RewriterRegistry(rewriteFlow = RewriteFlow.DEFAULT)
      ResponseRewriterRegistry contentRewriterRegistry,
      Provider<FeedProcessor> feedProcessorProvider,
      Processor processor) {
    this.requestPipeline = requestPipeline;
    this.contentRewriterRegistry = contentRewriterRegistry;
    this.feedProcessorProvider = feedProcessorProvider;
    this.processor = processor;
  }


  /** Execute an HTTP GET request */
  @Operation(httpMethods = {"POST","GET"}, path = "/get")
  public HttpApiResponse get(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    assertNoBody(httpReq, "GET");
    return execute("GET", httpReq, request);
  }

  /** Execute an HTTP POST request */
  @Operation(httpMethods = "POST", path = "/post")
  public HttpApiResponse post(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    return execute("POST", httpReq, request);
  }

  /** Execute an HTTP PUT request */
  @Operation(httpMethods = "POST", path = "/put")
  public HttpApiResponse put(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    return execute("PUT", httpReq, request);
  }

  /** Execute an HTTP DELETE request */
  @Operation(httpMethods = "POST", path = "/delete")
  public HttpApiResponse delete(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    assertNoBody(httpReq, "DELETE");
    return execute("DELETE", httpReq, request);
  }

  /** Execute an HTTP HEAD request */
  @Operation(httpMethods = {"POST","GET"}, path = "/head")
  public HttpApiResponse head(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    assertNoBody(httpReq, "HEAD");
    return execute("HEAD", httpReq, request);
  }

  private void assertNoBody(HttpApiRequest httpRequest, String method) {
    if (httpRequest.body != null) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
         "Request body not supported for " + method);
    }
  }

  /**
   * Dispatch the request
   */
  private HttpApiResponse execute(String method, HttpApiRequest httpApiRequest,
      final BaseRequestItem requestItem) {
    if (httpApiRequest.href == null) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "href parameter is missing");
    }

    // Canonicalize the path
    Uri href = normalizeUrl(httpApiRequest.href);
    try {
      HttpRequest req = new HttpRequest(href);
      req.setMethod(method);
      if (httpApiRequest.body != null) {
        req.setPostBody(httpApiRequest.body.getBytes());
      }

      // Copy over allowed headers
      for (Map.Entry<String, List<String>> header : httpApiRequest.headers.entrySet()) {
        if (!BAD_HEADERS.contains(header.getKey().trim().toUpperCase())) {
          for (String value : header.getValue()) {
            req.addHeader(header.getKey(), value);
          }
        }
      }

      // Extract the gadget URI from the request or the security token
      final Uri gadgetUri = getGadgetUri(requestItem.getToken(), httpApiRequest);
      if (gadgetUri == null) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
            "Gadget URI not specified in request");
      }
      req.setGadget(gadgetUri);

      // Detect the authz parsing
      if (httpApiRequest.authz != null) {
        req.setAuthType(AuthType.parse(httpApiRequest.authz));
      }

      req.setSecurityToken(requestItem.getToken());

      final AuthType authType = req.getAuthType();
      if (authType != AuthType.NONE) {
        if (authType == AuthType.OAUTH2) {
          Map<String, String> authSettings = getAuthSettings(requestItem);
          OAuth2Arguments oauth2Args = new OAuth2Arguments(req.getAuthType(), authSettings);

          req.setOAuth2Arguments(oauth2Args);
        } else {
          Map<String, String> authSettings = getAuthSettings(requestItem);
          OAuthArguments oauthArgs = new OAuthArguments(req.getAuthType(), authSettings);
          oauthArgs.setSignOwner(httpApiRequest.signOwner);
          oauthArgs.setSignViewer(httpApiRequest.signViewer);

          req.setOAuthArguments(oauthArgs);
        }
      }

      // TODO: Allow the rewriter to use an externally forced mime type. This is needed
      // allows proper rewriting of <script src="x"/> where x is returned with
      // a content type like text/html which unfortunately happens all too often

      req.setIgnoreCache(httpApiRequest.noCache);
      req.setSanitizationRequested(httpApiRequest.sanitize);

      // If the proxy request specifies a refresh param then we want to force the min TTL for
      // the retrieved entry in the cache regardless of the headers on the content when it
      // is fetched from the original source.
      if (httpApiRequest.refreshInterval != null) {
        req.setCacheTtl(httpApiRequest.refreshInterval);
      }

      final HttpRequest request = req;
      HttpResponse results = requestPipeline.execute(req);
      GadgetContext context = new GadgetContext() {
        @Override
        public Uri getUrl() {
          return gadgetUri;
        }

        @Override
        public String getParameter(String key) {
          return request.getParam(key);
        }

        @Override
        public boolean getIgnoreCache() {
          return request.getIgnoreCache();
        }

        @Override
        public String getContainer() {
          return requestItem.getToken().getContainer();
        }

        @Override
        public boolean getDebug() {
          return "1".equalsIgnoreCase(getParameter(Param.DEBUG.getKey()));
        }
      };
      // TODO: os:HttpRequest and Preload do not use the content rewriter.
      // Should we really do so here?
      try {
        Gadget gadget = processor.process(context);
        results = contentRewriterRegistry.rewriteHttpResponse(req, results, gadget);
      } catch (ProcessingException e) {
        //If there is an error creating the gadget object just rewrite the content without
        //the gadget object.  This will result in any content rewrite params not being
        //honored, but its better than the request failing all together.
        if(LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, CLASSNAME, "execute", MessageKeys.GADGET_CREATION_ERROR, e);
        }
        results = contentRewriterRegistry.rewriteHttpResponse(req, results, null);
      }

      HttpApiResponse httpApiResponse = new HttpApiResponse(results,
          transformBody(httpApiRequest, results),
          httpApiRequest);

      // Renew the security token if we can
      if (requestItem.getToken() != null) {
        String updatedAuthToken = requestItem.getToken().getUpdatedToken();
        if (updatedAuthToken != null) {
          httpApiResponse.token = updatedAuthToken;
        }
      }
      return httpApiResponse;
    } catch (GadgetException ge) {
      throw new ProtocolException(ge.getHttpStatusCode(), ge.getMessage(), ge);
    } catch (RewritingException re) {
      throw new ProtocolException(re.getHttpStatusCode(),
          re.getMessage(), re);
    }
  }

  /**
   * Extract all unknown keys into a map for extra auth params.
   */
  private Map<String, String> getAuthSettings(BaseRequestItem requestItem) {
    // Keys in a request item are always Strings
    @SuppressWarnings("unchecked")
    Set<String> allParameters = requestItem.getTypedRequest(Map.class).keySet();

    Map<String, String> authSettings = Maps.newHashMap();
    for (String paramName : allParameters) {
      if (!HttpApiRequest.KNOWN_PARAMETERS.contains(paramName)) {
        authSettings.put(paramName, requestItem.getParameter(paramName));
      }
    }

    return authSettings;
  }

  /** Helper method to normalize Uri that does not have scheme or empty path  */
  protected Uri normalizeUrl(Uri url) {
    if (url.getScheme() == null) {
      // Assume http
      url = new UriBuilder(url).setScheme("http").toUri();
    }

    if (url.getPath() == null || url.getPath().length() == 0) {
      url = new UriBuilder(url).setPath("/").toUri();
    }

    return url;
  }


  /** Format a response as JSON, including additional JSON inserted by chained content fetchers. */
  protected Object transformBody(HttpApiRequest request, HttpResponse results)
      throws GadgetException {
    String body = results.getResponseAsString();
    if ("feed".equalsIgnoreCase(request.format)) {
      return processFeed(request, body);
    } else if ("json".equalsIgnoreCase(request.format)) {
      try {
        body = body.trim();
        if(body.length() > 0 && body.charAt(0) == '[') {
          return new JSONArray(body);
        } else {
          return new JSONObject(body);
        }
      } catch (JSONException e) {
        // TODO: include data block with invalid JSON
        throw new ProtocolException(HttpServletResponse.SC_NOT_ACCEPTABLE, "Response not valid JSON", e);
      }
    }

    return body;
  }

  /** Processes a feed (RSS or Atom) using FeedProcessor. */
  protected Object processFeed(HttpApiRequest req, String responseBody)
      throws GadgetException {
    return feedProcessorProvider.get().process(req.href.toString(), responseBody, req.summarize,
        req.entryCount);
  }

  /** Extract the gadget URL from the request or the security token */
  protected Uri getGadgetUri(SecurityToken token, HttpApiRequest httpApiRequest) {
    if (token != null && token.getAppUrl() != null) {
      return Uri.parse(token.getAppUrl());
    }
    return null;
  }

  /**
   * Simple type that represents an Http request to execute on the callers behalf
   */
  public static class HttpApiRequest {
    static final Set<String> KNOWN_PARAMETERS = ImmutableSet.of(
        "alias", "href", "headers", "body", "gadget", "authz", "sign_owner",
        "sign_viewer", "format", "refreshInterval", "noCache", "sanitize",
        "summarize", "entryCount");

    // Content to fetch / execute
    Uri href;

    Map<String, List<String>> headers = Maps.newHashMap();

    /** POST body */
    String body;

    /** Authorization type ("none", "signed", "oauth") */
    String authz = "none";

    /** Should the request be signed by owner? */
    boolean signOwner = true;

    /** Should the request be signed by viewer? */
    boolean signViewer = true;

    // The format type to coerce the response into. Supported values are
    // "text", "json", and "feed".
    String format;

    // Use Integer here to allow for null
    Integer refreshInterval;

    // Bypass http caches
    boolean noCache;

    // Use HTML/CSS sanitizer
    boolean sanitize;

    // Control feed handling
    boolean summarize;
    int entryCount = 3;

    public Uri getHref() {
      return href;
    }

    public void setHref(Uri url) {
      this.href = url;
    }

    public Map<String, List<String>> getHeaders() {
      return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
      this.headers = headers;
    }

    public String getBody() {
      return body;
    }

    public void setBody(String body) {
      this.body = body;
    }

    public Integer getRefreshInterval() {
      return refreshInterval;
    }

    public void setRefreshInterval(Integer refreshInterval) {
      this.refreshInterval = refreshInterval;
    }

    public boolean isNoCache() {
      return noCache;
    }

    public void setNoCache(boolean noCache) {
      this.noCache = noCache;
    }

    public boolean isSanitize() {
      return sanitize;
    }

    public void setSanitize(boolean sanitize) {
      this.sanitize = sanitize;
    }

    public String getFormat() {
      return format;
    }

    public void setFormat(String format) {
      this.format = format;
    }

    public String getAuthz() {
      return authz;
    }

    public void setAuthz(String authz) {
      this.authz = authz;
    }

    public boolean isSignViewer() {
      return signViewer;
    }

    @JsonProperty("sign_viewer")
    public void setSignViewer(boolean signViewer) {
      this.signViewer = signViewer;
    }

    public boolean isSignOwner() {
      return signOwner;
    }

    @JsonProperty("sign_owner")
    public void setSignOwner(boolean signOwner) {
      this.signOwner = signOwner;
    }

    public boolean isSummarize() {
      return summarize;
    }

    public void setSummarize(boolean summarize) {
      this.summarize = summarize;
    }

    public int getEntryCount() {
      return entryCount;
    }

    public void setEntryCount(int entryCount) {
      this.entryCount = entryCount;
    }
  }

  /**
   * Response to request for Http content
   */
  public static class HttpApiResponse {
    // Http status code
    int status;

    // Returned headers
    Map<String, Collection<String>> headers;

    // Body content, either a String or a JSON-type structure
    Object content;

    // Renewed security token if available
    String token;

    // Metadata associated with the response.
    Map<String, String> metadata;

    public HttpApiResponse(int status) {
      this.status = status;
    }

    /**
     * Construct response based on HttpResponse from fetcher
     */
    public HttpApiResponse(HttpResponse response, Object content, HttpApiRequest httpApiRequest) {
      this.status = response.getHttpStatusCode();
      this.headers = new TreeMap<String, Collection<String>>(String.CASE_INSENSITIVE_ORDER);

      if (response.getHeaders().containsKey("set-cookie")) {
        this.headers.put("set-cookie", response.getHeaders("set-cookie"));
      }
      if (response.getHeaders().containsKey("location")) {
        this.headers.put("location", response.getHeaders("location"));
      }

      this.content = content;

      this.metadata = response.getMetadata();
    }

    public int getStatus() {
      return status;
    }

    public void setStatus(int status) {
      this.status = status;
    }

    public Map<String, Collection<String>> getHeaders() {
      return headers;
    }

    public void setHeaders(Map<String, Collection<String>> headers) {
      this.headers = headers;
    }

    public Object getContent() {
      return content;
    }

    public void setContent(Object content) {
      this.content = content;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public Map<String, String> getMetadata() {
      // TODO - Review this once migration of JS occurs. Currently MakeRequestHandler suppresses
      // this on output but that choice may not be the best one for compatibility.
      // Suppress metadata on output if it's empty
      if (metadata != null && metadata.isEmpty()) {
        return null;
      }
      return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
    }
  }
}

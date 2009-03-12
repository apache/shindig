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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.FeedProcessor;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.ResponseError;
import org.apache.shindig.protocol.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

/**
 * An alternate implementation of the Http proxy service using the standard API dispatcher for REST
 * / JSON-RPC calls. The basic form of the request is as follows
 * ...
 * method : http.<HTTP method name>
 * params : {
 *    url : <endpoint to fetch content from>,
 *    headers : { <header-name> : <header-value>, ...},
 *    contentType : <coerce content to specified mime type>
 *    body : <request body>
 *    gadget : <url of gadget spec for calling application>
 *    auth : {
 *          type : <None | OAuth | Signed>,
 *          signOwner : <boolean, default true>
 *          signViewer : <boolean, default true>
 *          ...<additional auth arguments. See OAuthArguments>
 *    },
 *    refresh : <Integer time in seconds to force as cache TTL. Default is to use response headers>
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
 *    body : <response body>
 *    token : <If security token provides a renewed value.>
 *    metadata : { <metadata entry> : <metadata value>, ...}
 * }
 *
 * Its important to note that requests which generate HTTP error responses such as 500 are returned
 * in the above format. The RPC itself succeeded in these cases. If an RPC error occurred the client
 * should introspect the error message for information as to the cause.
 *
 * @see MakeRequestHandler
 */
@Service(name = "http")
public class HttpRequestHandler {

  static final Set<String> BAD_HEADERS = ImmutableSet.of("HOST", "ACCEPT", "ACCEPT-ENCODING");

  private final RequestPipeline requestPipeline;
  private final ContentRewriterRegistry contentRewriterRegistry;

  @Inject
  public HttpRequestHandler(RequestPipeline requestPipeline,
      ContentRewriterRegistry contentRewriterRegistry) {
    this.requestPipeline = requestPipeline;
    this.contentRewriterRegistry = contentRewriterRegistry;
  }


  /** Execute an HTTP GET request */
  @Operation(httpMethods = {"POST","GET"}, path = "/get")
  public HttpApiResponse get(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    assertNoBody(httpReq, "GET");
    return execute("GET", httpReq, request.getToken());
  }

  /** Execute an HTTP POST request */
  @Operation(httpMethods = "POST", path = "/post")
  public HttpApiResponse post(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    return execute("POST", httpReq, request.getToken());
  }

  /** Execute an HTTP PUT request */
  @Operation(httpMethods = "POST", path = "/put")
  public HttpApiResponse put(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    return execute("PUT", httpReq, request.getToken());
  }

  /** Execute an HTTP DELETE request */
  @Operation(httpMethods = "POST", path = "/delete")
  public HttpApiResponse delete(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    assertNoBody(httpReq, "DELETE");
    return execute("DELETE", httpReq, request.getToken());
  }

  /** Execute an HTTP HEAD request */
  @Operation(httpMethods = {"POST","GET"}, path = "/head")
  public HttpApiResponse head(BaseRequestItem request) {
    HttpApiRequest httpReq = request.getTypedRequest(HttpApiRequest.class);
    assertNoBody(httpReq, "HEAD");
    return execute("HEAD", httpReq, request.getToken());
  }

  private void assertNoBody(HttpApiRequest httpRequest, String method) {
    if (httpRequest.body != null) {
      throw new ProtocolException(ResponseError.BAD_REQUEST,
          "Request body not supported for " + method);
    }
  }

  /**
   * Dispatch the request
   *
   * @param method HTTP method to execute
   */
  private HttpApiResponse execute(String method, HttpApiRequest httpApiRequest,
      SecurityToken token) {
    if (httpApiRequest.url == null) {
      throw new ProtocolException(ResponseError.BAD_REQUEST, "Url parameter is missing");
    }

    // Canonicalize the path
    if (httpApiRequest.url.getPath() == null || httpApiRequest.url.getPath().length() == 0) {
      httpApiRequest.url = new UriBuilder(httpApiRequest.url).setPath("/").toUri();
    }

    try {
      HttpRequest req = new HttpRequest(httpApiRequest.url);
      req.setMethod(method);
      if (httpApiRequest.body != null) {
        req.setPostBody(httpApiRequest.body.getBytes());
      }

      // Copy over allowed headers
      for (Map.Entry<String, String> header : httpApiRequest.headers.entrySet()) {
        if (!BAD_HEADERS.contains(header.getKey().trim().toUpperCase())) {
          req.addHeader(header.getKey(), header.getValue());
        }
      }

      // Extract the gadget URI from the request or the security token
      Uri gadgetUri = getGadgetUri(token, httpApiRequest);
      if (gadgetUri == null) {
        throw new ProtocolException(ResponseError.BAD_REQUEST,
            "Gadget URI not specified in request");
      }
      req.setGadget(gadgetUri);

      // Detect the auth parsing
      if (httpApiRequest.auth != null && httpApiRequest.auth.get("type") != null) {
        req.setAuthType(AuthType.parse(httpApiRequest.auth.get("type")));
      }

      if (req.getAuthType() != AuthType.NONE) {
        req.setSecurityToken(token);
        req.setOAuthArguments(new OAuthArguments(req.getAuthType(), httpApiRequest.auth));
      }

      // Allow the rewriter to use an externally forced mime type. This is needed
      // allows proper rewriting of <script src="x"/> where x is returned with
      // a content type like text/html which unfortunately happens all too often
      req.setRewriteMimeType(httpApiRequest.contentType);
      req.setIgnoreCache(httpApiRequest.noCache);
      req.setSanitizationRequested(httpApiRequest.sanitize);

      // If the proxy request specifies a refresh param then we want to force the min TTL for
      // the retrieved entry in the cache regardless of the headers on the content when it
      // is fetched from the original source.
      if (httpApiRequest.refresh != null) {
        req.setCacheTtl(httpApiRequest.refresh);
      }

      HttpResponse results = requestPipeline.execute(req);
      if (contentRewriterRegistry != null) {
        results = contentRewriterRegistry.rewriteHttpResponse(req, results);
      }

      HttpApiResponse httpApiResponse = new HttpApiResponse(results,
          tranformBody(httpApiRequest, results),
          httpApiRequest);

      // Renew the security token if we can
      if (token != null) {
        String updatedAuthToken = token.getUpdatedToken();
        if (updatedAuthToken != null) {
          httpApiResponse.token = updatedAuthToken;
        }
      }
      return httpApiResponse;
    } catch (GadgetException ge) {
      throw new ProtocolException(ResponseError.INTERNAL_ERROR, ge.getMessage());
    }
  }

  protected Uri normalizeUrl(Uri url) {
    if (url.getScheme() == null) {
      // Assume http
      url = new UriBuilder(url).setScheme("http").toUri();
    } else if (!"http".equals(url.getScheme()) && !"https"
        .equals(url.getScheme())) {
      throw new ProtocolException(ResponseError.BAD_REQUEST, "Only HTTP and HTTPS are supported");
    }
    return url;
  }


  /** Format a response as JSON, including additional JSON inserted by chained content fetchers. */
  protected String tranformBody(HttpApiRequest request, HttpResponse results)
      throws GadgetException {
    String body = results.getResponseAsString();
    if ("FEED".equals(request.contentType)) {
      body = processFeed(request, body);
    }
    return body;
  }

  /** Processes a feed (RSS or Atom) using FeedProcessor. */
  protected String processFeed(HttpApiRequest req, String responseBody)
      throws GadgetException {
    return new FeedProcessor().process(req.url.toString(), responseBody, req.summarize,
        req.entryCount).toString();
  }

  /** Extract the gadget URL from the request or the security token */
  protected Uri getGadgetUri(SecurityToken token, HttpApiRequest httpApiRequest) {
    if (token != null && token.getAppUrl() != null) {
      return Uri.parse(token.getAppUrl());
    } else if (httpApiRequest.gadget != null) {
      return httpApiRequest.gadget;
    }
    return null;
  }

  /**
   * Simple type that represents an Http request to execute on the callers behalf
   */
  public static class HttpApiRequest {

    // Content to fetch / execute
    Uri url;

    // TODO : Consider support Map<String, List<String>> to match response
    Map<String, String> headers = Maps.newHashMap();

    String body;

    // Allowed to be null if it can be derived from the security token
    Uri gadget;

    Map<String, String> auth = Maps.newHashMap();

    // The content type to coerce the response into. "FEED" has special meaning
    String contentType;

    // Use Integer here to allow for null
    Integer refresh;

    // Bypass http caches
    boolean noCache;

    // Use HTML/CSS sanitizer
    boolean sanitize;

    // Control feed handling
    boolean summarize;
    int entryCount = 3;

    public Uri getUrl() {
      return url;
    }

    public void setUrl(Uri url) {
      this.url = url;
    }

    public Uri getGadget() {
      return gadget;
    }

    public void setGadget(Uri gadget) {
      this.gadget = gadget;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    public void setHeaders(Map<String, String> headers) {
      this.headers = headers;
    }

    public String getBody() {
      return body;
    }

    public void setBody(String body) {
      this.body = body;
    }

    public Integer getRefresh() {
      return refresh;
    }

    public void setRefresh(Integer refresh) {
      this.refresh = refresh;
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

    public String getContentType() {
      return contentType;
    }

    public void setContentType(String contentType) {
      this.contentType = contentType;
    }

    public Map<String, String> getAuth() {
      return auth;
    }

    public void setAuth(Map<String, String> auth) {
      this.auth = auth;
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

    // Body content
    String body;

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
    public HttpApiResponse(HttpResponse response, String body, HttpApiRequest httpApiRequest) {
      this.status = response.getHttpStatusCode();
      this.headers = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);

      if (response.getHeaders().containsKey("set-cookie")) {
        this.headers.put("set-cookie", response.getHeaders("set-cookie"));
      }
      if (response.getHeaders().containsKey("location")) {
        this.headers.put("location", response.getHeaders("location"));
      }
      // Override the content-type if specified
      if (httpApiRequest.contentType != null) {
        this.headers.put("Content-Type", ImmutableList.of(httpApiRequest.contentType));
      }
      this.body = body;

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

    public String getBody() {
      return body;
    }

    public void setBody(String body) {
      this.body = body;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public Map<String, String> getMetadata() {
      // TODO - Review this once migration of JS occurs. Currently MakeRequestHandler suppresses
      // this on output but that choice may not be the best one for compatability.
      // Suppress metadata on output if its empty
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

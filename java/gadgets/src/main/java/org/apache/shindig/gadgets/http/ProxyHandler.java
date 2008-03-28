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
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSigner;
import org.apache.shindig.gadgets.GadgetToken;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContentRequest;
import org.apache.shindig.util.InputStreamConsumer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyHandler {
  private static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";
  private static final String POST_DATA_PARAM = "postData";
  private static final String METHOD_PARAM = "httpMethod";
  private static final String SECURITY_TOKEN_PARAM = "st";
  private static final String HEADERS_PARAM = "headers";
  private static final String NOCACHE_PARAM = "nocache";
  private static final String URL_PARAM = "url";
  private static final String AUTHZ_PARAM = "authz";
  private static final String UNSIGNED_FETCH = "none";
  private static final String SIGNED_FETCH = "signed";
  
  private final RemoteContentFetcher remoteFetcher;

  private final static Set<String> DISALLOWED_RESPONSE_HEADERS
    = new HashSet<String>();
  static {
    DISALLOWED_RESPONSE_HEADERS.add("set-cookie");
    DISALLOWED_RESPONSE_HEADERS.add("content-length");
  }

  public ProxyHandler(RemoteContentFetcher fetcher) {
    this.remoteFetcher = fetcher;
  }

  /**
   * Information about our chain of RemoteContentFetchers.
   *
   * This class is kind of silly at the moment, but it'll be useful once
   * we need to peek at the state of intermediate fetchers in the chain.
   * (Which we will need to do for OAuth.)
   */
  private static class FetcherChain {
    public RemoteContentFetcher fetcher;
    
    public FetcherChain(RemoteContentFetcher fetcher) {
      this.fetcher = fetcher;
    }
  }
  
  public void fetchJson(HttpServletRequest request,
                        HttpServletResponse response,
                        CrossServletState state)
      throws ServletException, IOException, GadgetException {
    
    // Build up the request to make
    RemoteContentRequest rcr = buildRemoteContentRequest(request);
    
    // Figure out whether we need to sign the request
    FetcherChain chain = buildFetcherChain(request, response, state);

    // Dispatch the request and serialize the response.
    RemoteContent results = chain.fetcher.fetch(rcr);

    String output;
    try {
      JSONObject resp = new JSONObject();
      response.setStatus(HttpServletResponse.SC_OK);
      if (results != null) {
        resp.put("body", results.getResponseAsString());
        resp.put("rc", results.getHttpStatusCode());
      }
      // Use raw param as key as URL may have to be decoded
      String originalUrl = request.getParameter(URL_PARAM);
      JSONObject json = new JSONObject().put(originalUrl, resp);
      output = UNPARSEABLE_CRUFT + json.toString();
    } catch (JSONException e) {
      output = "";
    }
    response.setContentType("application/json; charset=utf-8");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");
    response.getWriter().write(output);
  }

  @SuppressWarnings("unchecked")
  private RemoteContentRequest buildRemoteContentRequest(HttpServletRequest request)
  throws ServletException {
    try {

      String encoding = request.getCharacterEncoding();
      if (encoding == null) {
        encoding = "UTF-8";
      }

      String method = request.getMethod();
      Map<String, List<String>> headers = null;
      byte[] postBody = null;

      if ("POST".equals(method)) {
        method = getParameter(request, METHOD_PARAM, "GET");
        postBody = getParameter(request, POST_DATA_PARAM, "").getBytes();

        String headerData = request.getParameter(HEADERS_PARAM);
        if (headerData == null || headerData.length() == 0) {
          headers = Collections.emptyMap();
        } else {
          // We actually only accept single key value mappings now.
          headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
          String[] headerList = headerData.split("&");
          for (String header : headerList) {
            String[] parts = header.split("=");
            if (parts.length != 2) {
              // Malformed headers
              throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
              "malformed header specified");
            }
            headers.put(URLDecoder.decode(parts[0], encoding),
                Arrays.asList(URLDecoder.decode(parts[1], encoding)));
          }
        }
      } else {
        postBody = null;
        headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
          String header = headerNames.nextElement();
          headers.put(header, Collections.list(request.getHeaders(header)));
        }
      }

      removeUnsafeHeaders(headers);

      RemoteContentRequest.Options options
        = new RemoteContentRequest.Options();
      options.ignoreCache = "1".equals(request.getParameter(NOCACHE_PARAM));
      URI target = validateUrl(request.getParameter(URL_PARAM));
      RemoteContentRequest req = new RemoteContentRequest(
          method, target, headers, postBody, options);

      return req;
    } catch (UnsupportedEncodingException e) {
      throw new ServletException(e);
    } catch (GadgetException e) {
      throw new ServletException(e);
    }
  }

  /**
   * At the moment our fetcher chain is short.  In the future we might add
   * additional layers for things like caching or throttling.
   */
  private FetcherChain buildFetcherChain(HttpServletRequest request,
      HttpServletResponse response, CrossServletState state) throws GadgetException {
    String authzType = getParameter(request, AUTHZ_PARAM, "");
    if (authzType.equals("") || authzType.equals(UNSIGNED_FETCH)) {
      return new FetcherChain(remoteFetcher);
    }
    GadgetSigner signer = state.getGadgetSigner();
    if (signer == null) {
      throw new GadgetException(GadgetException.Code.UNSUPPORTED_FEATURE,
          "authenticated makeRequest not supported");
    } 
    String tokenString = getParameter(request, SECURITY_TOKEN_PARAM, "");
    GadgetToken securityToken = signer.createToken(tokenString);
    if (securityToken == null) {
      throw new GadgetException(GadgetException.Code.INVALID_GADGET_TOKEN);
    }
    RemoteContentFetcher realFetcher = null;
    if (authzType.equals(SIGNED_FETCH)) {
      realFetcher = state.makeSigningFetcher(remoteFetcher, securityToken);
    }
    if (realFetcher == null) {
      throw new GadgetException(GadgetException.Code.UNSUPPORTED_FEATURE,
          String.format("Unsupported auth type %s", authzType));
    }
    return new FetcherChain(realFetcher);
  }

  
  /**
   * This is called for embedding images inline in gadgets, e.g. via img src links
   * created via IG_Embed.
   */
  public void fetch(HttpServletRequest request,
                    HttpServletResponse response,
                    CrossServletState state)
      throws ServletException, IOException, GadgetException {
    RemoteContentRequest rcr = buildRemoteContentRequest(request);
    RemoteContent results = remoteFetcher.fetch(rcr);
    
    // TODO: why are we checking for caching headers *after* we sent the request?
    if (request.getHeader("If-Modified-Since") != null) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    int status = results.getHttpStatusCode();
    response.setStatus(status);
    if (status == HttpServletResponse.SC_OK) {
      Map<String, List<String>> headers = results.getAllHeaders();
      if (headers.get("Cache-Control") == null) {
        // Cache for 1 hour by default for images.
        HttpUtil.setCachingHeaders(response, 60 * 60);
      }
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String name = entry.getKey();
        List<String> values = entry.getValue();
        if (name != null && values != null
            && !DISALLOWED_RESPONSE_HEADERS.contains(name.toLowerCase())) {
          for (String value : values) {
            response.addHeader(name, value);
          }
        }
      }
      response.getOutputStream().write(
          InputStreamConsumer.readToByteArray(results.getResponse()));
    }
  }

  /**
   * Removes unsafe headers from the header set.
   * @param headers
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
   * Validates that the given url is valid for this request.
   * 
   * non-private for testing only.
   *
   * @param urlToValidate
   * @return A URL object of the URL
   * @throws ServletException if the URL fails security checks or is malformed.
   */
   URI validateUrl(String urlToValidate) throws ServletException {
    if (urlToValidate == null) {
      throw new ServletException("url parameter is missing.");
    }
    try {

      URI url = new URI(urlToValidate);

      if (url.getScheme() == null) {
        throw new ServletException("Invalid URL " + url.toString());
      }
      if (!url.getScheme().equals("http")) {
        throw new ServletException("Unsupported scheme: " + url.getScheme());
      }
      if (url.getPath() == null || url.getPath().length() == 0) {
        // Forcibly set the path to "/" if it is empty
        url = new URI(url.getScheme(),
                      url.getUserInfo(),
                      url.getHost(),
                      url.getPort(),
                      "/", url.getQuery(),
                      url.getFragment());
      }
      return url;
    } catch (URISyntaxException use) {
      throw new ServletException("Malformed URL " + use.getMessage());
    }
  }

  /**
   * Extracts the first parameter from the parameter map with the given name.
   * @param request
   * @param name
   * @return The parameter, if found, or defaultValue
   */
  private static String getParameter(HttpServletRequest request,
                                     String name, String defaultValue) {
    String ret = request.getParameter(name);
    return ret == null ? defaultValue : ret;
  }
}

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
import org.apache.shindig.gadgets.RequestSigner;
import org.apache.shindig.util.InputStreamConsumer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyHandler {
  private static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";

  private final RemoteContentFetcher fetcher;

  private final static Set<String> DISALLOWED_RESPONSE_HEADERS
    = new HashSet<String>();
  static {
    DISALLOWED_RESPONSE_HEADERS.add("set-cookie");
    DISALLOWED_RESPONSE_HEADERS.add("content-length");
  }

  public ProxyHandler(RemoteContentFetcher fetcher) {
    this.fetcher = fetcher;
  }

  public void fetchJson(HttpServletRequest request,
                        HttpServletResponse response,
                        CrossServletState state)
      throws ServletException, IOException, GadgetException {

    // Fetch the content and convert it into JSON.
    RemoteContent results = fetchContent(request, state);
    response.setStatus(results.getHttpStatusCode());
    if (results.getHttpStatusCode() == HttpServletResponse.SC_OK) {
      String output;
      try {
        // Use raw param as key as URL may have to be decoded
        JSONObject resp = new JSONObject()
            .put("body", results.getResponseAsString())
            .put("rc", results.getHttpStatusCode());
        String url = request.getParameter("url");
        JSONObject json = new JSONObject().put(url, resp);
        output = UNPARSEABLE_CRUFT + json.toString();
      } catch (JSONException e) {
        output = "";
      }
      response.setContentType("application/json; charset=utf-8");
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Content-Disposition", "attachment;filename=p.txt");
      response.getWriter().write(output);
    }
  }

  public void fetch(HttpServletRequest request,
                    HttpServletResponse response,
                    CrossServletState state)
      throws ServletException, IOException, GadgetException {
    RemoteContent results = fetchContent(request, state);

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
   * Fetch the content for a request
   *
   * @param request
   */
  @SuppressWarnings("unchecked")
  private RemoteContent fetchContent(HttpServletRequest request,
      CrossServletState state) throws ServletException, GadgetException {
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }

    try {
      URL originalUrl = validateUrl(request.getParameter("url"));
      GadgetSigner signer = state.getGadgetSigner();
      URL signedUrl;
      if ("signed".equals(request.getParameter("authz"))) {
        GadgetToken token = extractAndValidateToken(request, signer);
        if (token == null) {
          return new RemoteContent(HttpServletResponse.SC_UNAUTHORIZED,
              "Invalid security token.".getBytes(), null);
        }
        signedUrl = signUrl(state, originalUrl, token, request);
      } else {
        signedUrl = originalUrl;
      }
      String method = request.getMethod();
      Map<String, List<String>> headers = null;
      byte[] postBody = null;

      if ("POST".equals(method)) {
        method = getParameter(request, "httpMethod", "GET");
        postBody = URLDecoder.decode(
            getParameter(request, "postData", ""), encoding).getBytes();

        String headerData = request.getParameter("headers");
        if (headerData == null || headerData.length() == 0) {
          headers = Collections.emptyMap();
        } else {
          // We actually only accept single key value mappings now.
          headers = new HashMap<String, List<String>>();
          String[] headerList = headerData.split("&");
          for (String header : headerList) {
            String[] parts = header.split("=");
            if (parts.length != 2) {
              // Malformed headers
              return RemoteContent.ERROR;
            }
            headers.put(URLDecoder.decode(parts[0], encoding),
                Arrays.asList(URLDecoder.decode(parts[1], encoding)));
          }
        }
      } else {
        postBody = null;
        headers = new HashMap<String, List<String>>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
          String header = headerNames.nextElement();
          headers.put(header, Collections.list(request.getHeaders(header)));
        }
      }

      removeUnsafeHeaders(headers);

      RemoteContentRequest.Options options
          = new RemoteContentRequest.Options();
      options.ignoreCache = "1".equals(request.getParameter("nocache"));
      RemoteContentRequest req = new RemoteContentRequest(
          method, signedUrl.toURI(), headers, postBody, options);
      return fetcher.fetch(req);
    } catch (UnsupportedEncodingException e) {
      throw new ServletException(e);
    } catch (URISyntaxException e) {
      throw new ServletException(e);
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
   * @param urlToValidate
   * @return A URL object of the URL
   * @throws ServletException if the URL fails security checks or is malformed.
   */
  private URL validateUrl(String urlToValidate) throws ServletException {
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
      return url.toURL();
    } catch (URISyntaxException use) {
      throw new ServletException("Malformed URL " + use.getMessage());
    } catch (MalformedURLException mfe) {
      throw new ServletException("Malformed URL " + mfe.getMessage());
    }
  }

  /**
   * @param request
   * @return A valid token for the given input.
   * @throws GadgetException
   */
  private GadgetToken extractAndValidateToken(HttpServletRequest request,
      GadgetSigner signer) throws GadgetException {
    if (signer == null) {
      return null;
    }
    String token = getParameter(request, "st", "");
    return signer.createToken(token);
  }

  /**
   * Sign a URL with a GadgetToken if needed
   * @return The signed url.
   */
  @SuppressWarnings("unchecked")
  private URL signUrl(CrossServletState state, URL originalUrl, GadgetToken token,
      HttpServletRequest request) throws GadgetException {
    String method = getParameter(request, "httpMethod", "GET");
    String body = getParameter(request, "postBody", null);
    RequestSigner signer = state.makeSignedFetchRequestSigner(token);
    return signer.signRequest(method, originalUrl, body);
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

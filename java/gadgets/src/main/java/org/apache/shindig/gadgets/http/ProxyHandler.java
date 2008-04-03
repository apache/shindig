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
import org.apache.shindig.gadgets.GadgetToken;
import org.apache.shindig.gadgets.GadgetTokenDecoder;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContentRequest;
import org.apache.shindig.gadgets.SigningFetcherFactory;
import org.apache.shindig.util.InputStreamConsumer;

import com.google.inject.Inject;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyHandler {
  public static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";
  public static final String POST_DATA_PARAM = "postData";
  public static final String METHOD_PARAM = "httpMethod";
  public static final String SECURITY_TOKEN_PARAM = "st";
  public static final String HEADERS_PARAM = "headers";
  public static final String NOCACHE_PARAM = "nocache";
  public static final String URL_PARAM = "url";
  public static final String AUTHZ_PARAM = "authz";
  public static final String AUTHZ_NONE = "none";
  public static final String AUTHZ_SIGNED = "signed";

  // Not used yet.
  // private static final String AUTHZ_AUTHORIZED = "authorized";

  private final RemoteContentFetcher fetcher;
  private final GadgetTokenDecoder gadgetTokenDecoder;
  private final SigningFetcherFactory signingFetcherFactory;

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
  }

  @Inject
  public ProxyHandler(@ProxiedContentFetcher RemoteContentFetcher fetcher,
                      GadgetTokenDecoder gadgetTokenDecoder,
                      SigningFetcherFactory signingFetcherFactory) {
    this.fetcher = fetcher;
    this.gadgetTokenDecoder = gadgetTokenDecoder;
    this.signingFetcherFactory = signingFetcherFactory;
  }

  /**
   * Retrieves a file and returns it as a json response.
   *
   * @param request
   * @param response
   * @throws IOException
   * @throws GadgetException
   */
  public void fetchJson(HttpServletRequest request,
                        HttpServletResponse response)
      throws IOException, GadgetException {
    // Fetch the content and convert it into JSON.
    RemoteContent results = fetchContent(request);
    response.setStatus(HttpServletResponse.SC_OK);
    String output;
    try {
      // Use raw param as key as URL may have to be decoded
      JSONObject resp = new JSONObject()
          .put("body", results.getResponseAsString())
          .put("rc", results.getHttpStatusCode());
      String url = request.getParameter(URL_PARAM);
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

  /**
   * Normal proxy fetch
   * @param request
   * @param response
   * @throws IOException
   * @throws GadgetException
   */
  public void fetch(HttpServletRequest request,
                    HttpServletResponse response)
      throws IOException, GadgetException {
    if (request.getHeader("If-Modified-Since") != null) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    RemoteContent results = fetchContent(request);

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
  private RemoteContent fetchContent(HttpServletRequest request)
      throws GadgetException {
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }

    try {
      URI url = validateUrl(request.getParameter(URL_PARAM));
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
          headers = new TreeMap<String, List<String>>(
              String.CASE_INSENSITIVE_ORDER);
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
        headers
            = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
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
      RemoteContentRequest req = new RemoteContentRequest(
          method, url, headers, postBody, options);

      RemoteContentFetcher realFetcher;
      String authzType = getParameter(request, AUTHZ_PARAM, AUTHZ_NONE);
      if (AUTHZ_SIGNED.equals(authzType)) {
        GadgetToken token = extractAndValidateToken(request);
        realFetcher = signingFetcherFactory.getSigningFetcher(fetcher, token);
      } else if (AUTHZ_NONE.equals(authzType)) {
        realFetcher = fetcher;
      } else {
        throw new GadgetException(GadgetException.Code.UNSUPPORTED_FEATURE,
            "Unsupported authorization type requested.");
      }
      return realFetcher.fetch(req);
    } catch (UnsupportedEncodingException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
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
   * @throws GadgetException if the URL fails security checks or is malformed.
   */
  public URI validateUrl(String urlToValidate) throws GadgetException {
    if (urlToValidate == null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "url parameter is missing.");
    }
    try {
      URI url = new URI(urlToValidate);
      if (!"http".equals(url.getScheme())) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
            "Invalid request url scheme; only \"http\" supported.");
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
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "url parameter is not a valid url.");
    }
  }

  /**
   * @param request
   * @return A valid token for the given input.
   * @throws GadgetException
   */
  private GadgetToken extractAndValidateToken(HttpServletRequest request)
      throws GadgetException {
    String token = getParameter(request, SECURITY_TOKEN_PARAM, "");
    return gadgetTokenDecoder.createToken(token);
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

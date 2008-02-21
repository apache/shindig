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
import org.apache.shindig.gadgets.ProcessingOptions;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContentRequest;
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
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyHandler {
  private static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";

  private final RemoteContentFetcher fetcher;

  public ProxyHandler(RemoteContentFetcher fetcher) {
    this.fetcher = fetcher;
  }

  public void fetchJson(HttpServletRequest request,
                        HttpServletResponse response,
                        GadgetSigner signer)
      throws ServletException, IOException, GadgetException {
    GadgetToken token = extractAndValidateToken(request, signer);
    String url = request.getParameter("url");
    URL originalUrl = validateUrl(url);
    URL signedUrl = signUrl(originalUrl, token, request);

    // Fetch the content and convert it into JSON.
    // TODO: Fetcher needs to handle variety of HTTP methods.
    RemoteContent results = fetchContent(signedUrl,
                                         request,
                                         new HttpProcessingOptions(request));

    response.setStatus(results.getHttpStatusCode());
    if (results.getHttpStatusCode() == HttpServletResponse.SC_OK) {
      String output;
      try {
        // Use raw param as key as URL may have to be decoded
        JSONObject resp = new JSONObject()
            .put("body", results.getResponseAsString())
            .put("rc", results.getHttpStatusCode());
        String json = new JSONObject().put(url, resp).toString();
        output = UNPARSEABLE_CRUFT + json;
      } catch (JSONException e) {
        output = "";
      }

      setCachingHeaders(response);
      response.setContentType("application/json; charset=utf-8");
      response.setHeader("Content-Disposition", "attachment;filename=p.txt");
      response.getWriter().write(output);
    }
  }

  public void fetch(HttpServletRequest request,
                    HttpServletResponse response,
                    GadgetSigner signer)
      throws ServletException, IOException, GadgetException {
    GadgetToken token = extractAndValidateToken(request, signer);
    URL originalUrl = validateUrl(request.getParameter("url"));
    URL signedUrl = signUrl(originalUrl, token, request);

    // TODO: Fetcher needs to handle variety of HTTP methods.
    RemoteContent results = fetchContent(signedUrl,
                                         request,
                                         new HttpProcessingOptions(request));

    int status = results.getHttpStatusCode();
    response.setStatus(status);
    if (status == HttpServletResponse.SC_OK) {
      // Fill out the response.
      setCachingHeaders(response);
      Map<String, List<String>> headers = results.getAllHeaders();
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String name = entry.getKey();
        List<String> values = entry.getValue();
        if (name != null
            && values != null
            && name.compareToIgnoreCase("Cache-Control") != 0
            && name.compareToIgnoreCase("Expires") != 0
            && name.compareToIgnoreCase("Content-Length") != 0) {
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
   */
  @SuppressWarnings("unchecked")
  private RemoteContent fetchContent(URL signedUrl,
                                     HttpServletRequest request,
                                     ProcessingOptions procOptions)
      throws ServletException {
    String encoding = request.getCharacterEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }
    try {
      if ("POST".equals(request.getMethod())) {
        String method = getParameter(request, "httpMethod", "GET");
        String postData = URLDecoder.decode(
            getParameter(request, "postData", ""), encoding);

        Map<String, List<String>> headers;
        String headerData = request.getParameter("headers");
        if (headerData == null) {
          headers = Collections.emptyMap();
        } else {
          if (headerData.length() == 0) {
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
        }

        removeUnsafeHeaders(headers);

        RemoteContentRequest req = new RemoteContentRequest(
            signedUrl.toURI(), headers, postData.getBytes());
        if ("POST".equals(method)) {
          return fetcher.fetchByPost(req, procOptions);
        } else {
          return fetcher.fetch(req, procOptions);
        }
      } else {
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
          String header = headerNames.nextElement();
          headers.put(header, Collections.list(request.getHeaders(header)));
        }
        removeUnsafeHeaders(headers);
        RemoteContentRequest req
            = new RemoteContentRequest(signedUrl.toURI(), headers);
        return fetcher.fetch(req, procOptions);
      }
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
        "Host", "Accept-Encoding", "Accept"
    };
    for (String bad : badHeaders) {
      headers.remove(bad);
    }
  }

  /**
   * Validates that the given url is valid for this reques.t
   *
   * @param url
   * @return A URL object of the URL
   * @throws ServletException if the URL fails security checks or is malformed.
   */
  private URL validateUrl(String url) throws ServletException {
    if (url == null) {
      throw new ServletException("url parameter is missing.");
    }
    try {
      URI origin = new URI(url);
      if (origin.getScheme() == null) {
        throw new ServletException("Invalid URL " + origin.toString());
      }
      if (!origin.getScheme().equals("http")) {
        throw new ServletException("Unsupported scheme: " + origin.getScheme());
      }
      if (origin.getPath() == null || origin.getPath().length() == 0) {
        // Forcibly set the path to "/" if it is empty
        origin = new URI(origin.getScheme(),
            origin.getUserInfo(), origin.getHost(),
            origin.getPort(),
            "/", origin.getQuery(),
            origin.getFragment());
      }
      return origin.toURL();
    } catch (URISyntaxException use) {
      throw new ServletException("Malformed URL " + use.getMessage());
    } catch (MalformedURLException mfe) {
      throw new ServletException("Malformed URL " + mfe.getMessage());
    }
  }

  /**
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
   * Sets HTTP caching headers
   *
   * @param response The HTTP response
   */
  private void setCachingHeaders(HttpServletResponse response) {
    // TODO: Re-implement caching behavior if appropriate.
    response.setHeader("Cache-Control", "private; max-age=0");
    response.setDateHeader("Expires", System.currentTimeMillis() - 30);
  }

  /**
   * Sign a URL with a GadgetToken if needed
   * @return The signed url.
   */
  @SuppressWarnings("unchecked")
  private URL signUrl(URL originalUrl, GadgetToken token,
      HttpServletRequest request) throws GadgetException {
    if (token == null || !"signed".equals(request.getParameter("authz"))) {
      return originalUrl;
    }
    String method = getParameter(request, "httpMethod", "GET");
    return token.signUrl(originalUrl, method, request.getParameterMap());
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

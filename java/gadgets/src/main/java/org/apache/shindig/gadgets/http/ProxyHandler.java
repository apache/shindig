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

import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSigner;
import org.apache.shindig.gadgets.GadgetToken;
import org.apache.shindig.gadgets.ProcessingOptions;
import org.apache.shindig.gadgets.RemoteContent;
import org.apache.shindig.gadgets.RemoteContentFetcher;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyHandler {
  private static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";
  private static final int TWO_HOURS_IN_MS = 7200000;
  private static final int ONE_HOUR_IN_SECS = 3600;
  private static final int MAX_PROXY_SIZE = 1024 * 1024;
  
  private final RemoteContentFetcher fetcher;

  public ProxyHandler() {
    this(new BasicRemoteContentFetcher(MAX_PROXY_SIZE));
  }
  
  public ProxyHandler(RemoteContentFetcher fetcher) {
    this.fetcher = fetcher;
  }

  public void fetchJson(HttpServletRequest request,
                        HttpServletResponse response,
                        GadgetSigner signer)
      throws ServletException, IOException {

    GadgetToken token = extractAndValidateToken(request, signer);
    URL originalUrl = extractAndValidateUrl(request);
    URL signedUrl = signUrl(originalUrl, token, request);

    // Fetch the content and convert it into JSON.
    // TODO: Fetcher needs to handle variety of HTTP methods.
    RemoteContent results = fetchContent(signedUrl, request,
        new ProcessingOptions());

    String output;
    try {
      String json = new JSONObject().put(originalUrl.toString(), new JSONObject()
          .put("body", new String(results.getByteArray()))
          .put("rc", results.getHttpStatusCode())
          ).toString();
      output = UNPARSEABLE_CRUFT + json;
    } catch (JSONException e) {
      output = "";
    }
    response.setStatus(HttpServletResponse.SC_OK);
    setCachingHeaders(response);
    response.setContentType("application/json; charset=utf-8");
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");
    PrintWriter pw = response.getWriter();
    pw.write(output);
  }

  public void fetch(HttpServletRequest request,
                    HttpServletResponse response,
                    GadgetSigner signer)
      throws ServletException, IOException {

    GadgetToken token = extractAndValidateToken(request, signer);
    URL originalUrl = extractAndValidateUrl(request);
    URL signedUrl = signUrl(originalUrl, token, request);

    // TODO: Fetcher needs to handle variety of HTTP methods.
    RemoteContent results = fetchContent(signedUrl, request,
        new ProcessingOptions());

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
      response.getOutputStream().write(results.getByteArray());
    }
  }

  /**
   * Fetch the content for a request
   */
  private RemoteContent fetchContent(URL signedUrl, HttpServletRequest request,
      ProcessingOptions procOptions) throws ServletException {
    try {
      if ("POST".equals(request.getMethod())) {
        String data = request.getParameter("postData");
        return fetcher.fetchByPost(signedUrl,
            URLDecoder.decode(data, request.getCharacterEncoding()).getBytes(),
            procOptions);
      } else {
        return fetcher.fetch(signedUrl, new ProcessingOptions());
      }
    } catch (UnsupportedEncodingException uee) {
      throw new ServletException(uee);
    }
  }

  /**
   * Gets the url= parameter from the request and applies some basic sanity
   * checking.
   *
   * @param request The HTTP request from the browser.
   * @return A URL object of the URL
   * @throws ServletException if the URL fails security checks or is malformed.
   */
  private URL extractAndValidateUrl(HttpServletRequest request)
      throws ServletException {
    String url = request.getParameter("url");
    if (url == null) {
      throw new ServletException("Missing url parameter");
    }

    // TODO: are there other tests that should be here?
    // url.matches("[a-zA-Z0-9_:%&#+-]+"), perhaps?
    if (!url.startsWith("http://")) {
      throw new ServletException("url parameter does not start with http://");
    }

    URL origin;
    try {
      origin = new URL(url);
    } catch (MalformedURLException e) {
      throw new ServletException("Malformed url parameter");
    }
    return origin;
  }

  /**
   * @return A valid token for the given input.
   * @throws ServletException
   */
  private GadgetToken extractAndValidateToken(HttpServletRequest request,
      GadgetSigner signer) throws ServletException {
    try {
      if (signer == null) return null;
      String token = request.getParameter("st");
      if (token == null) {
        token = "";
      }
      return signer.createToken(token);
    } catch (GadgetException ge) {
      throw new ServletException(ge);
    }
  }

  /**
   * Sets HTTP headers that instruct the browser to cache for 2 hours.
   *
   * @param response The HTTP response
   */
  private void setCachingHeaders(HttpServletResponse response) {
    // TODO: figure out why we're not using the same amount of time for these
    // headers.
    response.setHeader("Cache-Control", "public,max-age=" + ONE_HOUR_IN_SECS);
    response.setDateHeader("Expires", System.currentTimeMillis()
                                     + TWO_HOURS_IN_MS);
  }

  /**
   * Sign a URL with a GadgetToken if needed
   * @return 
   */
  private URL signUrl(URL originalUrl, GadgetToken token,
      HttpServletRequest request) throws ServletException {
    try {
      if (token == null ||
          !"signed".equals(request.getParameter("authz"))) {
        return originalUrl;
      }
      return token.signUrl(originalUrl, "GET", // TODO: request.getMethod() 
          request.getParameterMap());
    } catch (GadgetException ge) {
      throw new ServletException(ge);
    }
  }

}

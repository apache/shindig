/*
 * $Id$
 *
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.BasicRemoteContentFetcher;
import org.apache.shindig.gadgets.RemoteContent;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
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
  private static final BasicRemoteContentFetcher fetcher =
      new BasicRemoteContentFetcher(MAX_PROXY_SIZE);

  public void fetchJson(HttpServletRequest request,
                        HttpServletResponse response)
      throws ServletException, IOException {
    // TODO: If this request is coming in on a Host: that does not match
    // the configured gadget rendering host, we should check for edit tokens
    // somehow.

    // Validate url= parameter
    URL origin = extractAndValidateUrl(request);

    // Fetch the content and convert it into JSON.

    RemoteContent results = fetcher.fetch(origin);
    String output;
    try {
      String json = new JSONObject().put(origin.toString(), new JSONObject()
          .put("body", new String(results.getByteArray()))
          .put("rc", results.getHttpStatusCode())
          ).toString();
      output = UNPARSEABLE_CRUFT + json;
    } catch (JSONException e) {
      output = "";
    }

    setCachingHeaders(response);
    response.setContentType("application/json; charset=utf-8");
    response.setHeader("Content-Disposition", "attachment;filename=p.txt");
    PrintWriter pw = response.getWriter();
    pw.write(output);
  }

  public void fetch(HttpServletRequest request,
                    HttpServletResponse response)
      throws ServletException, IOException {
    // Validate url= parameter
    URL origin = extractAndValidateUrl(request);

    RemoteContent results = fetcher.fetch(origin);
    int status = results.getHttpStatusCode();
    response.setStatus(status);
    if (status == 200) {
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
}

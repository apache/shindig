/*
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class JsonpProxyServlet extends HttpServlet {
  private static final String UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";
  private static final int TWO_HOURS_IN_MS = 7200000;
  private static final int ONE_HOUR_IN_SECS = 3600;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // TODO: If this request is coming in on a Host: that does not match
    // the configured gadget rendering host, we should check for edit tokens
    // somehow.

    // Validate url= parameter
    URL origin_url = extractAndValidateUrl(request);

    // Fetch the content and convert it into JSON.
    BasicRemoteContentFetcher fetcher = new BasicRemoteContentFetcher(1024*1024);
    RemoteContent results = fetcher.fetch(origin_url);
    String response_template = buildResponse(origin_url.toString(), results);

    // Fill out the response.
    setCachingHeaders(response);
    PrintWriter pw = response.getWriter();
    pw.write(response_template);
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
      throw new ServletException("Missing url= parameter");
    }

    if (!url.startsWith("http://")) {
      throw new ServletException("url= parameter does not start with http://");
    }

    URL origin_url;
    try {
      origin_url = new URL(url);
    } catch (MalformedURLException e) {
      throw new ServletException("malformed url= parameter");
    }
    return origin_url;
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
    response.setDateHeader("Expires", new Date().getTime() + TWO_HOURS_IN_MS);
  }

  /**
   * Converts the results of the fetch into a response suitable for parsing by
   * JavaScript.
   *
   * @param url             Canonical representation of the URL
   * @param origin_response The results of the fetch
   * @return A string suitable for writing to the browser.
   */
  private String buildResponse(String url,
                               RemoteContent origin_response) {
    String escaped_output =
        javaScriptStringEscape(new String(origin_response.getByteArray()));

    String response_template = UNPARSEABLE_CRUFT +
        "{ '{{URL}}' : " +
        "{ 'body': '{{CONTENT}}', " +
        "  'rc': {{HTTP_STATUS_CODE}} } }";

    response_template =
        response_template.replace("{{URL}}", javaScriptStringEscape(url));
    response_template =
        response_template.replace("{{CONTENT}}", escaped_output);
    response_template = response_template
        .replace("{{HTTP_STATUS_CODE}}",
            Integer.toString(origin_response.getHttpStatusCode()));
    return response_template;
  }

  /**
   * Escapes a string so that it can be used as a JavaScript literal.
   * <p/>
   * TODO: fix this to support proper JS escaping.  This doesn't handle Unicode
   * characters correctly.
   *
   * @param unescaped The unescaped string.
   * @return a string with all JavaScript metacharacters escaped
   */
  private String javaScriptStringEscape(String unescaped) {
    StringBuilder escaped = new StringBuilder();
    for (int i = 0; i < unescaped.length(); i++) {
      char c = unescaped.charAt(i);
      if (Character.isDigit(c) || Character.isLetter(c) ||
          Character.isWhitespace(c)) {
        escaped.append(c);
      } else {
        escaped.append("\\x");
        escaped.append(Integer.toHexString(c));
      }
    }
    return escaped.toString();
  }
}

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
package org.apache.shindig.gadgets.uri;

import com.google.common.collect.ImmutableSet;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility functions related to URI and Http servlet response management.
 */
public class UriUtils {
  /**
   * Enum of disallowed response headers that should not be passed on as is to
   * the user. The webserver serving out the response should be responsible
   * for filling these.
   */
  public enum DisallowedHeaders {
    // Directives controlled by the serving infrastructure.
    OUTPUT_TRANSFER_DIRECTIVES(ImmutableSet.of(
        "content-length", "transfer-encoding", "content-encoding", "server",
        "accept-ranges")),

    CACHING_DIRECTIVES(ImmutableSet.of("vary", "expires", "date", "pragma",
                                       "cache-control")),

    CLIENT_STATE_DIRECTIVES(ImmutableSet.of("set-cookie", "www-authenticate")),

    // Headers that the fetcher itself would like to fill. For example,
    // httpclient library crashes if Content-Length header is set in the
    // request being fetched.
    POST_INCOMPATIBLE_DIRECTIVES(ImmutableSet.of("content-length"));

    // Miscellaneous headers we should take care of, but are left for now.
    // "set-cookie", "content-length", "content-encoding", "etag",
    // "last-modified" ,"accept-ranges", "vary", "expires", "date",
    // "pragma", "cache-control", "transfer-encoding", "www-authenticate"

    private Set<String> disallowedHeaders;
    DisallowedHeaders(Set<String> disallowedHeaders) {
      this.disallowedHeaders = disallowedHeaders;
    }

    public Set<String> getDisallowedHeaders() {
      return disallowedHeaders;
    }
  }

  /**
   * Returns true if the header name is valid.
   * NOTE: RFC 822 section 3.1.2 describes the structure of header fields. 
   * @param name The header name.
   * @return True if the header name is valid, false otherwise.
   */
  public static boolean isValidHeaderName(String name) {
    char[] dst = new char[name.length()];
    name.getChars(0, name.length(), dst, 0);

    for (char c : dst) {
      if (c < 33 || c > 126) {
        return false;
      }
      if (c == ':') {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the header value is valid.
   * NOTE: RFC 822 section 3.1.2 describes the structure of header fields.
   * @param val The header value.
   * @return True if the header value is valid, false otherwise.
   */
  public static boolean isValidHeaderValue(String val) {
    // TODO: complete this.
    return true;
  }

  /**
   * Copies the http response headers and status code to the final servlet
   *   response.
   * @param data The http response when fetching the requested accel uri.
   * @param resp The servlet response to return back to client.
   * @param setHeaders If true, then setHeader method of HttpServletResponse is
   *   called, otherwise addHeader is called for every header.
   * @param disallowedResponseHeaders Disallowed response headers to omit from the response
   *   returned to the user. 
   * @throws IOException In case the http response was not successful.
   */
  public static void copyResponseHeadersAndStatusCode(
      HttpResponse data, HttpServletResponse resp,
      boolean setHeaders,
      DisallowedHeaders... disallowedResponseHeaders)
      throws IOException {
    // Pass original return code:
    resp.setStatus(data.getHttpStatusCode());

    Set<String> allDisallowedHeaders = new HashSet<String>();
    for (DisallowedHeaders h : disallowedResponseHeaders) {
      allDisallowedHeaders.addAll(h.getDisallowedHeaders());
    }

    for (Map.Entry<String, String> entry : data.getHeaders().entries()) {
      if (isValidHeaderName(entry.getKey()) && isValidHeaderValue(entry.getValue()) &&
          !allDisallowedHeaders.contains(entry.getKey().toLowerCase())) {
        if (setHeaders) {
          resp.setHeader(entry.getKey(), entry.getValue());
        } else {
          resp.addHeader(entry.getKey(), entry.getValue());
        }
      }
    }

    // External "internal error" should be mapped to gateway error.
    if (data.getHttpStatusCode() == HttpResponse.SC_INTERNAL_SERVER_ERROR) {
      resp.sendError(HttpResponse.SC_BAD_GATEWAY);
    }
  }

  /**
   * Copies headers from HttpServletRequest object to HttpRequest object.
   * @param data Servlet request to copy headers from.
   * @param req The HttpRequest object to copy headers to.
   * @param disallowedRequestHeaders Disallowed request headers to omit from
   *   the servlet request
   */
  public static void copyRequestHeaders(HttpServletRequest data,
                                        HttpRequest req,
                                        DisallowedHeaders... disallowedRequestHeaders) {
    Set<String> allDisallowedHeaders = new HashSet<String>();
    for (DisallowedHeaders h : disallowedRequestHeaders) {
      allDisallowedHeaders.addAll(h.getDisallowedHeaders());
    }

    Enumeration headerNames = data.getHeaderNames();
    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        Object headerObj = headerNames.nextElement();
        if (!(headerObj instanceof String)) {
          continue;
        }

        String header = (String) headerObj;
        Enumeration headerValues = data.getHeaders(header);
        if (headerValues != null && headerValues.hasMoreElements() &&
            isValidHeaderName(header) &&
            !allDisallowedHeaders.contains(header.toLowerCase())) {
          // Remove existing values of this header.
          req.removeHeader(header);

          while (headerValues.hasMoreElements()) {
            Object valueObj = headerValues.nextElement();
            if (valueObj != null && valueObj instanceof String &&
                isValidHeaderValue((String) valueObj)) {
              // Add this header to data.
              req.addHeader(header, (String) valueObj);
            }
          }
        }
      }
    }
  }

  /**
   * Copies the post data from HttpServletRequest object to HttpRequest object.
   * @param request Servlet request to copy post data from.
   * @param req The HttpRequest object to copy post data to.
   * @throws GadgetException In case of errors.
   */
  public static void copyRequestData(HttpServletRequest request,
                                     HttpRequest req) throws GadgetException {
    req.setMethod(request.getMethod());
    try {
      if (request.getMethod().toLowerCase().equals("post")) {
        req.setPostBody(request.getInputStream());
      }
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }
}

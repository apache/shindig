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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility functions related to URI and Http servlet response management.
 *
 * @since 2.0.0
 */
public final class UriUtils {
  public static final String CHARSET = "charset";
  //class name for logging purpose
  private static final String classname = UriUtils.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);

  private UriUtils() {}

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
                                       "cache-control", "etag", "last-modified")),

    CLIENT_STATE_DIRECTIVES(ImmutableSet.of("set-cookie", "set-cookie2", "www-authenticate")),

    AUTHENTICATION_DIRECTIVES(ImmutableSet.of("www-authenticate")),

    // Headers that the fetcher itself would like to fill. For example,
    // httpclient library crashes if Content-Length header is set in the
    // request being fetched.
    POST_INCOMPATIBLE_DIRECTIVES(ImmutableSet.of("content-length")),

    HOST_HEADER(ImmutableSet.of("host"));

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
   * According to the RFC, a header name (or field-name) must be composed of printable ASCII
   * characters (i.e., characters that have values between 33. and 126. decimal, except colon).
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
   * According to the RFC, a header value (or field-body) may be composed of any ASCII characters,
   * except CR or LF.
   * @param val The header value.
   * @return True if the header value is valid, false otherwise.
   */
  public static boolean isValidHeaderValue(String val) {
    char[] dst = new char[val.length()];
    val.getChars(0, val.length(), dst, 0);

    for (char c : dst) {
      if (c == 13 || c == 10) {
        // CR and LF.
        return false;
      }
      if (c > 127) {
        return false;
      }
    }
    return true;
  }

  /**
   * Copies the http response headers and status code to the final servlet
   *   response.
   * @param data The http response when fetching the requested accel uri.
   * @param resp The servlet response to return back to client.
   * @param remapInternalServerError If true, then SC_INTERNAL_SERVER_ERROR is
   *   remapped to SC_BAD_GATEWAY.
   * @param setHeaders If true, then setHeader method of HttpServletResponse is
   *   called, otherwise addHeader is called for every header.
   * @param disallowedResponseHeaders Disallowed response headers to omit from the response
   *   returned to the user.
   * @throws IOException In case the http response was not successful.
   */
  public static void copyResponseHeadersAndStatusCode(
      HttpResponse data, HttpResponseBuilder resp,
      boolean remapInternalServerError,
      boolean setHeaders,
      DisallowedHeaders... disallowedResponseHeaders)
      throws IOException {
    // Pass original return code:
    resp.setHttpStatusCode(data.getHttpStatusCode());

    Set<String> allDisallowedHeaders = new HashSet<String>();
    for (DisallowedHeaders h : disallowedResponseHeaders) {
      allDisallowedHeaders.addAll(h.getDisallowedHeaders());
    }

    for (Map.Entry<String, String> entry : data.getHeaders().entries()) {
      if (isValidHeaderName(entry.getKey()) && isValidHeaderValue(entry.getValue()) &&
          !allDisallowedHeaders.contains(entry.getKey().toLowerCase())) {
        try {
          if (setHeaders) {
            resp.setHeader(entry.getKey(), entry.getValue());
          } else {
            resp.addHeader(entry.getKey(), entry.getValue());
          }
        } catch (IllegalArgumentException e) {
          // Skip illegal header
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "copyResponseHeadersAndStatusCode", MessageKeys.SKIP_ILLEGAL_HEADER, new Object[] {entry.getKey(),entry.getValue()});
          }
        }
      }
    }

    if (remapInternalServerError) {
      // External "internal error" should be mapped to gateway error.
      if (data.getHttpStatusCode() == HttpResponse.SC_INTERNAL_SERVER_ERROR) {
        resp.setHttpStatusCode(HttpResponse.SC_BAD_GATEWAY);
      }
    }
  }

  /**
   * Copies headers from HttpServletRequest object to HttpRequest object.
   * @param origRequest Servlet request to copy headers from.
   * @param req The HttpRequest object to copy headers to.
   * @param disallowedRequestHeaders Disallowed request headers to omit from
   *   the servlet request
   */
  public static void copyRequestHeaders(HttpRequest origRequest,
                                        HttpRequest req,
                                        DisallowedHeaders... disallowedRequestHeaders) {
    Set<String> allDisallowedHeaders = new HashSet<String>();
    for (DisallowedHeaders h : disallowedRequestHeaders) {
      allDisallowedHeaders.addAll(h.getDisallowedHeaders());
    }

    for (Map.Entry<String, List<String>> inHeader : origRequest.getHeaders().entrySet()) {
      String header = inHeader.getKey();
      List<String> headerValues = inHeader.getValue();

      if (headerValues != null && !headerValues.isEmpty() &&
          isValidHeaderName(header) &&
          !allDisallowedHeaders.contains(header.toLowerCase())) {
        // Remove existing values of this header.
        req.removeHeader(header);
        for (String headerVal : headerValues) {
          if (isValidHeaderValue(headerVal)) {
            req.addHeader(header, headerVal);
          }
        }
      }
    }
  }

  /**
   * Copies the post data from HttpServletRequest object to HttpRequest object.
   * @param origRequest Request to copy post data from.
   * @param req The HttpRequest object to copy post data to.
   * @throws GadgetException In case of errors.
   */
  public static void copyRequestData(HttpRequest origRequest,
                                     HttpRequest req) throws GadgetException {
    req.setMethod(origRequest.getMethod());
    try {
      if (origRequest.getMethod().equalsIgnoreCase("post")) {
        req.setPostBody(origRequest.getPostBody());
      }
    } catch (IOException e) {
      throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR, e);
    }
  }

  /**
   * Rewrite the content type of the final http response if the request has the
   * rewrite-mime-type param.
   * @param req The http request.
   * @param response The final http response to be returned to user.
   */
  public static void maybeRewriteContentType(HttpRequest req, HttpResponseBuilder response) {
    String responseType = response.getHeader("Content-Type");
    String requiredType = req.getRewriteMimeType();
    if (!Strings.isNullOrEmpty(requiredType)) {
      // Use a 'Vary' style check on the response
      if (requiredType.endsWith("/*") && !Strings.isNullOrEmpty(responseType)) {
        String requiredTypePrefix = requiredType.substring(0, requiredType.length() - 1);
        if (!responseType.toLowerCase().startsWith(requiredTypePrefix.toLowerCase())) {
          // TODO: We are currently setting the content type to something like x/* (e.g. text/*)
          // which is not a valid content type. Need to fix this.
          response.setHeader("Content-Type", requiredType);
        }
      } else {
        response.setHeader("Content-Type", requiredType);
      }
    }
  }

  /**
   * Parses the value of content-type header and returns the content type header
   * without the 'charset' attribute.
   * @param content The content type header value.
   * @return Content type header value without charset.
   */
  public static String getContentTypeWithoutCharset(String content) {
    String contentTypeWithoutCharset = content;
    String[] parts = StringUtils.split(content, ';');
    if (parts.length >= 2) {
      StringBuilder contentTypeWithoutCharsetBuilder = new StringBuilder(parts.length);
      contentTypeWithoutCharsetBuilder.append(parts[0]);

      for (int i = 1; i < parts.length; i++) {
        String parameterAndValue = parts[i].trim().toLowerCase();
        String[] splits = StringUtils.split(parameterAndValue, '=');
        if (splits.length > 0 && !splits[0].trim().equals(CHARSET)) {
          contentTypeWithoutCharsetBuilder.append(';').append(parts[i]);
        }
      }
      contentTypeWithoutCharset = contentTypeWithoutCharsetBuilder.toString();
    }

    return contentTypeWithoutCharset;
  }
}

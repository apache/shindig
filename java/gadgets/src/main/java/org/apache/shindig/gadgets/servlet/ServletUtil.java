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

import com.google.common.base.Strings;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.Pair;
import org.apache.shindig.common.servlet.HttpUtil;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.common.util.Utf8UrlCoder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;

/**
 * Utility routines for dealing with servlets.
 *
 * @since 2.0.0
 */
public final class ServletUtil {
  public static final String REMOTE_ADDR_KEY = "RemoteAddress";
  public static final String DATA_URI_KEY = "dataUri";

  private ServletUtil() {}

  /**
   * Returns an HttpRequest object encapsulating the servlet request.
   * NOTE: Request parameters are not explicitly taken care of, instead we copy
   * the InputStream and query parameters separately.
   *
   * @param servletReq The http servlet request.
   * @return An HttpRequest object with all the information provided by the
   *   servlet request.
   * @throws IOException In case of errors.
   */
  public static HttpRequest fromHttpServletRequest(HttpServletRequest servletReq) throws IOException {
    HttpRequest req = new HttpRequest(new UriBuilder(servletReq).toUri());

    Enumeration<?> headerNames = servletReq.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      Object obj = headerNames.nextElement();
      if (obj instanceof String) {
        String headerName = (String) obj;

        Enumeration<?> headerValues = servletReq.getHeaders(headerName);
        while (headerValues.hasMoreElements()) {
          obj = headerValues.nextElement();
          if (obj instanceof String) {
            req.addHeader(headerName, (String) obj);
          }
        }
      }
    }

    req.setMethod(servletReq.getMethod());
    if ("POST".equalsIgnoreCase(req.getMethod())) {
      req.setPostBody(servletReq.getInputStream());
    }
    req.setParam(REMOTE_ADDR_KEY, servletReq.getRemoteAddr());
    return req;
  }

  public static void setCachingHeaders(HttpResponseBuilder response, int ttl, boolean noProxy) {
    // Initial cache control headers are in this response, we should now sanitize them or set them if they are missing.
    String cacheControl = response.getHeader("Cache-Control");
    String pragma = response.getHeader("Pragma");
    for (Pair<String, String> header : HttpUtil.getCachingHeadersToSet(ttl, cacheControl, pragma, noProxy)) {
      response.setHeader(header.one, header.two);
    }
  }

  public static void copyToServletResponseAndOverrideCacheHeaders(
      HttpResponse response, HttpServletResponse servletResponse)
      throws IOException {
    copyHeadersAndStatusToServletResponse(response, servletResponse);
    HttpUtil.setCachingHeaders(servletResponse, (int)(response.getCacheTtl() / 1000L));
    copyContentToServletResponse(response, servletResponse);
  }

  public static void copyToServletResponse(
      HttpResponse response, HttpServletResponse servletResponse) throws IOException {
    copyHeadersAndStatusToServletResponse(response, servletResponse);
    copyContentToServletResponse(response, servletResponse);
  }

  public static void copyContentToServletResponse(
      HttpResponse response, HttpServletResponse servletResponse) throws IOException {
    servletResponse.setContentLength(response.getContentLength());
    IOUtils.copy(response.getResponse(), servletResponse.getOutputStream());

  }
  public static void copyHeadersAndStatusToServletResponse(
      HttpResponse response, HttpServletResponse servletResponse) {
    servletResponse.setStatus(response.getHttpStatusCode());
    for (Map.Entry<String, String> header : response.getHeaders().entries()) {
      servletResponse.addHeader(header.getKey(), header.getValue());
    }
  }

  /**
   * Validates and normalizes the given url, ensuring that it is non-null, has
   * scheme http or https, and has a path value of some kind.
   *
   * @return A URI representing a validated form of the url.
   * @throws GadgetException If the url is not valid.
   */
  public static Uri validateUrl(Uri urlToValidate) throws GadgetException {
    if (urlToValidate == null) {
      throw new GadgetException(GadgetException.Code.MISSING_PARAMETER, "Missing url param",
          HttpResponse.SC_BAD_REQUEST);
    }
    UriBuilder url = new UriBuilder(urlToValidate);
    if (!"http".equals(url.getScheme()) && !"https".equals(url.getScheme())) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "Invalid request url scheme in url: " + Utf8UrlCoder.encode(urlToValidate.toString()) +
          "; only \"http\" and \"https\" supported.", HttpResponse.SC_BAD_REQUEST);
    }
    if (url.getPath() == null || url.getPath().length() == 0) {
      url.setPath("/");
    }
    return url.toUri();
  }

  /**
   * Sets standard forwarding headers on the proxied request.
   * @param inboundRequest
   * @param req
   * @throws GadgetException
   */
  public static void setXForwardedForHeader(HttpRequest inboundRequest, HttpRequest req)
      throws GadgetException {
    String forwardedFor = getXForwardedForHeader(inboundRequest.getHeader("X-Forwarded-For"),
        inboundRequest.getParam(ServletUtil.REMOTE_ADDR_KEY));
    if (forwardedFor != null) {
      req.setHeader("X-Forwarded-For", forwardedFor);
    }
  }

  public static void setXForwardedForHeader(HttpServletRequest inboundRequest, HttpRequest req) {
    String forwardedFor = getXForwardedForHeader(inboundRequest.getHeader("X-Forwarded-For"),
        inboundRequest.getRemoteAddr());
    if (forwardedFor != null) {
      req.setHeader("X-Forwarded-For", forwardedFor);
    }
  }

  private static String getXForwardedForHeader(String origValue, String remoteAddr) {
    if (!Strings.isNullOrEmpty(remoteAddr)) {
      if (Strings.isNullOrEmpty(origValue)) {
        origValue = remoteAddr;
      } else {
        origValue = remoteAddr + ", " + origValue;
      }
    }
    return origValue;
  }

  /**
   * @return An HttpResponse object wrapping the given GadgetException.
   */
  public static HttpResponse errorResponse(GadgetException e) {
    return new HttpResponseBuilder().setHttpStatusCode(e.getHttpStatusCode())
        .setHeader("Content-Type", "text/plain")
        .setResponseString(e.getMessage() != null ? e.getMessage() : "").create();
  }

  /**
   * Converts the given {@code HttpResponse} into JSON form, with at least
   * one field, dataUri, containing a Data URI that can be inlined into an HTML page.
   * Any metadata on the given {@code HttpResponse} is also added as fields.
   *
   * @param response Input HttpResponse to convert to JSON.
   * @return JSON-containing HttpResponse.
   * @throws IOException If there are problems reading from {@code response}.
   */
  public static HttpResponse convertToJsonResponse(HttpResponse response) throws IOException {
    // Pull out charset, if present. If not, this operation simply returns contentType.
    String contentType = response.getHeader("Content-Type");
    if (contentType == null) {
      contentType = "";
    } else if (contentType.contains(";")) {
      contentType = StringUtils.split(contentType, ';')[0].trim();
    }
    // First and most importantly, emit dataUri.
    // Do so in streaming fashion, to avoid needless buffering.
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(os);
    pw.write("{\n  ");
    pw.write(DATA_URI_KEY);
    pw.write(":'data:");
    pw.write(contentType);
    pw.write(";base64;charset=");
    pw.write(response.getEncoding());
    pw.write(",");
    pw.flush();

    // Stream out the base64-encoded data.
    // Ctor args indicate to encode w/o line breaks.
    Base64InputStream b64input = new Base64InputStream(response.getResponse(), true, 0, null);
    byte[] buf = new byte[1024];

    try {
      int read;
      while (( read = b64input.read(buf, 0, 1024)) > 0) {
        os.write(buf, 0, read);
      }
    } finally {
      IOUtils.closeQuietly(b64input);
    }

    // Complete the JSON object.
    pw.write("',\n  ");
    boolean first = true;
    for (Map.Entry<String, String> metaEntry : response.getMetadata().entrySet()) {
      if (DATA_URI_KEY.equals(metaEntry.getKey())) continue;
      if (!first) {
        pw.write(",\n  ");
      }
      first = false;
      pw.write("'");
      pw.write(StringEscapeUtils.escapeEcmaScript(metaEntry.getKey()).replace("'", "\'"));
      pw.write("':'");
      pw.write(StringEscapeUtils.escapeEcmaScript(metaEntry.getValue()).replace("'", "\'"));
      pw.write("'");
    }
    pw.write("\n}");
    pw.flush();

    return new HttpResponseBuilder()
        .setHeader("Content-Type", "application/json")
        .setResponseNoCopy(os.toByteArray())
        .create();
  }
}

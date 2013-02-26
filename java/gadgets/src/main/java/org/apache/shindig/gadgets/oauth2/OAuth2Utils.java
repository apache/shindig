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
package org.apache.shindig.gadgets.oauth2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;

import com.google.common.collect.Maps;

/**
 * Some common OAuth2 related utility methods
 *
 */
public class OAuth2Utils {
  private final static String LOG_CLASS = OAuth2Utils.class.getName();
  private static final FilteredLogger LOG = FilteredLogger.getFilteredLogger(OAuth2Utils.LOG_CLASS);

  /**
   * Normalizes a URL and parameters. If the URL already contains parameters,
   * new parameters will be added properly.
   *
   * @param url2
   *          is the base URL to normalize
   * @param queryParams
   *          query parameters to add to the URL
   * @param fragmentParams
   *          fragment params to add to the URL
   * @return normalized url with parameter
   */
  public static String buildUrl(final String url2, final Map<String, String> queryParams,
      final Map<String, String> fragmentParams) {
    // Get any existing params
    String url = url2;
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    final Uri uri = Uri.parse(url);
    final Map<String, List<String>> existingQueryParams = uri.getQueryParameters();
    final Map<String, List<String>> existingFragmentParams = uri.getFragmentParameters();
    final int index = url.indexOf('?');
    String urlNoParams = url;
    if (index >= 0) {
      urlNoParams = urlNoParams.substring(0, index);
    }

    final Map<String, String> queryParams2 = Maps.newHashMapWithExpectedSize(5);
    if ((existingQueryParams != null) && !existingQueryParams.isEmpty()) {
      for (final Entry<String, List<String>> entry : existingQueryParams.entrySet()) {
        queryParams2.put(entry.getKey(), entry.getValue().get(0));
      }
    }

    final Map<String, String> fragmentParams2 = Maps.newHashMapWithExpectedSize(5);
    if ((existingFragmentParams != null) && !existingFragmentParams.isEmpty()) {
      for (final Entry<String, List<String>> entry : existingFragmentParams.entrySet()) {
        fragmentParams2.put(entry.getKey(), entry.getValue().get(0));
      }
    }

    if (queryParams != null) {
      queryParams2.putAll(queryParams);
    }
    if (fragmentParams != null) {
      fragmentParams2.putAll(fragmentParams);
    }

    final StringBuilder buff = new StringBuilder(urlNoParams);
    if ((queryParams != null) && !queryParams.isEmpty()) {
      if (urlNoParams.contains("?")) {
        buff.append('&');
      } else {
        buff.append('?');
      }
      buff.append(OAuth2Utils.convertQueryString(queryParams2));
    }
    if ((fragmentParams != null) && !fragmentParams.isEmpty()) {
      if (urlNoParams.contains("#")) {
        buff.append('&');
      } else {
        buff.append('#');
      }
      buff.append(OAuth2Utils.convertQueryString(fragmentParams2));
    }
    return buff.toString();
  }

  /**
   * Converts a Map<String, String> to a URL query string.
   *
   * @param params
   *          represents the Map of query parameters
   *
   * @return String is the URL encoded parameter String
   */
  public static String convertQueryString(final Map<String, String> params) {
    if (params == null) {
      return "";
    }
    final List<NameValuePair> nvp = new ArrayList<NameValuePair>();
    for (final String key : new TreeSet<String>(params.keySet())) {
      if (params.get(key) != null) {
        nvp.add(new BasicNameValuePair(key, params.get(key)));
      }
    }
    return URLEncodedUtils.format(nvp, "UTF-8");
  }

  /**
   * Fetch bearer token from http request
   *
   * @param req httpServletRequest
   *
   * @return String bearer token from the request
   */
  public static String fetchBearerTokenFromHttpRequest(final HttpServletRequest req) {
    String bearerToken = req.getParameter("access_token");
    if ((bearerToken == null) || bearerToken.equals("")) {
      final String header = req.getHeader("Authorization");
      if ((header != null) && header.contains("Bearer")) {
        final String[] parts = header.split("\\s+");
        bearerToken = parts[parts.length - 1];
      }
    }
    return bearerToken;
  }

  /**
   * Fetch client secret from http request for a given client.
   *
   * @param req
   *          httpServletRequest
   * @param clientId
   *          id of the client
   *
   * @return String client secret from the request
   */
  public static String fetchClientSecretFromHttpRequest(final String clientId,
      final HttpServletRequest req) {
    String secret = req.getParameter("client_secret");
    if ((secret == null) || secret.equals("")) {
      final String header = req.getHeader("Authorization");
      if ((header != null) && header.contains("Basic")) {
        final byte[] decodedSecret = Base64.decodeBase64(secret);
        try {
          String temp = new String(decodedSecret, "UTF-8");
          String[] parts = StringUtils.split(temp, ':');
          if ((parts != null) && (parts.length == 2) && (parts[0].equals(clientId))) {
            secret = parts[1];
          }
        } catch (final UnsupportedEncodingException e) {
          if (OAuth2Utils.LOG.isLoggable()) {
            OAuth2Utils.LOG.log("UnsupportedEncodingException", e);
          }
          return null;
        }
      }
    }
    return secret;
  }

  /**
   * Check if the given Uri is in the allowedDomains array.
   *
   * @param uri
   *          The uri
   * @param allowedDomains
   *          allowed domains
   *
   * @return boolean true if uri is allowed
   */
  public static boolean isUriAllowed(final Uri uri, final String[] allowedDomains) {
    if (allowedDomains == null || allowedDomains.length == 0) {
      // if white list is not specified, allow client to access any domain
      return true;
    }
    String host = uri.getAuthority();
    final int pos = host.indexOf(':');
    if (pos != -1) {
      host = host.substring(0, pos);
    }
    host = host.toLowerCase();
    for (String domain : allowedDomains) {
      if (domain != null) {
        domain = domain.trim();
        domain = domain.toLowerCase();
        if (domain.startsWith(".") && host.endsWith(domain) || domain.equalsIgnoreCase(host)) {
          return true;
        }
      }
    }
    return false;
  }
}

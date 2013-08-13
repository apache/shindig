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
package org.apache.shindig.social.core.oauth2;

import java.util.Map;

import org.apache.shindig.common.uri.UriBuilder;

/**
 * Collection of utility classes to support OAuth 2.0 operations.
 */
public class OAuth2Utils {

  /**
   * Converts a Map<String, String> to a URL query string.
   *
   * @param params represents the Map of query parameters
   *
   * @return String is the URL encoded parameter String
   */
  public static String convertQueryString(Map<String, String> params) {
    if (params == null) return "";
    UriBuilder builder = new UriBuilder();
    builder.addQueryParameters(params);
    return builder.getQuery();
  }

  /**
   * Normalizes a URL and parameters. If the URL already contains parameters,
   * new parameters will be added properly.
   *
   * @param URL is the base URL to normalize
   * @param queryParams query parameters to add to the URL
   * @param fragmentParams fragment params to add to the URL
   */
  public static String buildUrl(String url, Map<String, String> queryParams,
      Map<String, String> fragmentParams) {
    UriBuilder builder = new UriBuilder();
    builder.setPath(url);
    if (queryParams != null) builder.addQueryParameters(queryParams);
    if (fragmentParams != null) builder.addFragmentParameters(fragmentParams);
    return builder.toString();
  }
}

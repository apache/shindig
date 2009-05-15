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
package org.apache.shindig.common.uri;

import org.apache.shindig.common.util.Utf8UrlCoder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Constructs Uris from inputs.
 *
 * Note that the builder will only automatically encode query parameters that are added. Other
 * parameters must be encoded explicitly.
 */
public class UriBuilder {
  private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)=([^&=]*)");

  private String scheme;
  private String authority;
  private String path;
  private String query;
  private String fragment;
  private final Map<String, List<String>> queryParameters;


  /**
   * Construct a new builder from an existing uri.
   */
  public UriBuilder(Uri uri) {
    scheme = uri.getScheme();
    authority = uri.getAuthority();
    path = uri.getPath();
    query = uri.getQuery();
    fragment = uri.getFragment();

    queryParameters = Maps.newLinkedHashMap(uri.getQueryParameters());
  }

  /**
   * Create an empty builder.
   */
  public UriBuilder() {
    queryParameters =  Maps.newLinkedHashMap();
  }

  /**
   * Construct a builder by parsing a string.
   */
  public static UriBuilder parse(String text) {
    return new UriBuilder(Uri.parse(text));
  }

  /**
   * Convert the builder to a Uri.
   */
  public Uri toUri() {
    return new Uri(this);
  }

  /**
   * @return The scheme part of the uri, or null if none was specified.
   */
  public String getScheme() {
    return scheme;
  }

  public UriBuilder setScheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  /**
   * @return The authority part of the uri, or null if none was specified.
   */
  public String getAuthority() {
    return authority;
  }

  public UriBuilder setAuthority(String authority) {
    this.authority = authority;
    return this;
  }

  /**
   * @return The path part of the uri, or null if none was specified.
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path component of the Uri.
   */
  public UriBuilder setPath(String path) {
    this.path = path;
    return this;
  }

  /**
   * @return The query part of the uri, or null if none was specified.
   */
  public String getQuery() {
    if (query == null) {
      query = joinParameters(queryParameters);
    }
    return query;
  }

  /**
   * Assigns the specified query string as the query portion of the uri, automatically decoding
   * parameters to populate the parameter map for calls to getParameter.
   */
  public UriBuilder setQuery(String query) {
    queryParameters.clear();
    queryParameters.putAll(splitParameters(query));
    this.query = query;
    return this;
  }

  public UriBuilder addQueryParameter(String name, String value) {
    query = null;
    List<String> params = queryParameters.get(name);
    if (params == null) {
      params = Lists.newArrayList();
      queryParameters.put(name, params);
    }
    params.add(value);
    return this;
  }

  public UriBuilder addQueryParameters(Map<String, String> parameters) {
    query = null;
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      addQueryParameter(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Force overwrites a given query parameter with the given value.
   */
  public UriBuilder putQueryParameter(String name, String... values) {
    query = null;
    queryParameters.put(name, Lists.newArrayList(values));
    return this;
  }

  /**
   * Force overwrites a given query parameter with the given value.
   */
  public UriBuilder putQueryParameter(String name, Iterable<String> values) {
    query = null;
    queryParameters.put(name, Lists.newArrayList(values));
    return this;
  }
  
  /**
   * Removes a query parameter.
   */
  public UriBuilder removeQueryParameter(String name) {
    query = null;
    queryParameters.remove(name);
    return this;
  }
  
  /**
   * @return The queryParameters part of the uri, separated into component parts.
   */
  public Map<String, List<String>> getQueryParameters() {
    return queryParameters;
  }

  /**
   * @return All queryParameters parameters with the given name.
   */
  public List<String> getQueryParameters(String name) {
    return queryParameters.get(name);
  }

  /**
   * @return The first queryParameters parameter value with the given name.
   */
  public String getQueryParameter(String name) {
    Collection<String> values = queryParameters.get(name);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.iterator().next();
  }

  /**
   * @return The queryParameters fragment.
   */
  public String getFragment() {
    return fragment;
  }

  public UriBuilder setFragment(String fragment) {
    this.fragment = fragment;
    return this;
  }

  /**
   * Utility method for joining key / value pair parameters into a url-encoded string.
   */
  static String joinParameters(Map<String, List<String>> query) {
    if (query.isEmpty()) {
      return null;
    }
    StringBuilder buf = new StringBuilder();
    boolean firstDone = false;
    for (Map.Entry<String, List<String>> entry : query.entrySet()) {
      String name = Utf8UrlCoder.encode(entry.getKey());
      for (String value : entry.getValue()) {
        if (firstDone) {
          buf.append('&');
        }
        firstDone = true;

        buf.append(name)
           .append('=')
           .append(Utf8UrlCoder.encode(value));
      }
    }
    return buf.toString();
  }

  /**
   * Utility method for splitting a parameter string into key / value pairs.
   */
  public static Map<String, List<String>> splitParameters(String query) {
    if (query == null) {
      return Collections.emptyMap();
    }
    Map<String, List<String>> params = Maps.newHashMap();
    Matcher paramMatcher = QUERY_PATTERN.matcher(query);
    while (paramMatcher.find()) {
      String name = Utf8UrlCoder.decode(paramMatcher.group(1));
      String value = Utf8UrlCoder.decode(paramMatcher.group(2));
      List<String> values = params.get(name);
      if (values == null) {
        values = Lists.newArrayList();
        params.put(name, values);
      }
      values.add(value);
    }
    return Collections.unmodifiableMap(params);
  }

  @Override
  public String toString() {
    return toUri().toString();
  }

  @Override
  public int hashCode() {
    return toUri().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {return true;}
    if (!(obj instanceof UriBuilder)) {return false;}

    return toString().equals(obj.toString());
  }
}

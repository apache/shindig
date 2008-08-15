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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Constructs Uris from inputs.
 */
public class UriBuilder {
  private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)=([^&=]*)");

  private String scheme;
  private String authority;
  private String path;
  private Multimap<String, String> query;
  private String fragment;

  /**
   * Construct a new builder from an existing uri.
   */
  public UriBuilder(Uri uri) {
    scheme = uri.getScheme();
    authority = uri.getAuthority();
    path = uri.getPath();
    query = Multimaps.newLinkedListMultimap(uri.getQueryParameters());
    fragment = uri.getFragment();
  }

  /**
   * Create an empty builder.
   */
  public UriBuilder() {
    query = Multimaps.newLinkedListMultimap();
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
    return new Uri(scheme, authority, path, query, fragment);
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

  public UriBuilder setPath(String path) {
    this.path = path;
    return this;
  }

  /**
   * @return The query part of the uri, or null if none was specified.
   */
  public String getQuery() {
    return joinParameters(query);
  }

  public UriBuilder setQuery(String query) {
    this.query.clear();
    this.query.putAll(splitParameters(query));
    return this;
  }

  public UriBuilder addQueryParameter(String name, String value) {
    query.put(name, value);
    return this;
  }

  public UriBuilder addQueryParameters(Map<String, String> parameters) {
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      query.put(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * @return The query part of the uri, separated into component parts.
   */
  public Multimap<String, String> getQueryParameters() {
    return query;
  }

  /**
   * @return All query parameters with the given name.
   */
  public Collection<String> getQueryParameters(String name) {
    return query.get(name);
  }

  /**
   * @return The first query parameter value with the given name.
   */
  public String getQueryParameter(String name) {
    Collection<String> values = query.get(name);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.iterator().next();
  }

  /**
   * @return The query fragment.
   */
  public String getFragment() {
    return fragment;
  }

  public UriBuilder setFragment(String fragment) {
    this.fragment = fragment;
    return this;
  }

  /**
   * Utility method for joining key / value pair parameters into a string.
   */
  static String joinParameters(Multimap<String, String> query) {
    if (query.size() == 0) {
      return null;
    }
    StringBuilder buf = new StringBuilder();
    boolean firstDone = false;
    for (Map.Entry<String, String> entry : query.entries()) {
      if (firstDone) {
        buf.append("&");
      }
      firstDone = true;
      buf.append(Utf8UrlCoder.encode(entry.getKey()))
         .append("=")
         .append(Utf8UrlCoder.encode(entry.getValue()));
    }
    return buf.toString();
  }

  static Multimap<String, String> splitParameters(String query) {
    if (query == null) {
      return Multimaps.immutableMultimap();
    }
    Multimap<String, String> params = Multimaps.newLinkedListMultimap();
    Matcher paramMatcher = QUERY_PATTERN.matcher(query);
    while (paramMatcher.find()) {
      params.put(Utf8UrlCoder.decode(paramMatcher.group(1)),
                 Utf8UrlCoder.decode(paramMatcher.group(2)));
    }
    return Multimaps.unmodifiableMultimap(params);
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
  public boolean equals(Object rhs) {
    if (rhs == this) {return true;}
    if (!(rhs instanceof UriBuilder)) {return false;}

    return toString().equals(rhs.toString());
  }
}

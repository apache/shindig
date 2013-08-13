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
import javax.servlet.http.HttpServletRequest;

/**
 * Constructs Uris from inputs.
 *
 * Note that the builder will only automatically encode query parameters that are added. Other
 * parameters must be encoded explicitly.
 */
public final class UriBuilder {
  private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)=([^&=]*)");

  private String scheme;
  private String authority;
  private String path;
  private final ParamString query;
  private final ParamString fragment;

  /**
   * Construct a new builder from an existing uri.
   */
  public UriBuilder(Uri uri) {
    scheme = uri.getScheme();
    authority = uri.getAuthority();
    path = uri.getPath();
    query = new ParamString(uri.getQuery());
    fragment = new ParamString(uri.getFragment());
  }

  /**
   * Construct a new builder from a servlet request.
   */
  public UriBuilder(HttpServletRequest req) {
    scheme = req.getScheme().toLowerCase();
    int serverPort = req.getServerPort();
    authority = req.getServerName() +
        ((serverPort == 80 && "http".equals(scheme)) ||
         (serverPort == 443 && "https".equals(scheme)) ||
         (serverPort <= 0) ? "" :
           ":" + serverPort);
    path = req.getRequestURI();
    query = new ParamString(req.getQueryString());
    fragment = new ParamString();
  }

  /**
   * Create an empty builder.
   */
  public UriBuilder() {
    query =  new ParamString();
    fragment = new ParamString();
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
   * @return The queryParameters fragment.
   */
  public String getQuery() {
    return query.getString();
  }

  /**
   * Assigns the specified query string as the query portion of the uri, automatically decoding
   * parameters to populate the parameter map for calls to getParameter.
   */
  public UriBuilder setQuery(String str) {
    query.setString(str);
    return this;
  }

  public UriBuilder addQueryParameter(String name, String value) {
    query.add(name, value);
    return this;
  }

  public UriBuilder addQueryParameters(Map<String, String> parameters) {
    query.addAll(parameters);
    return this;
  }

  /**
   * Force overwrites a given query parameter with the given value.
   */
  public UriBuilder putQueryParameter(String name, String... values) {
    query.put(name, values);
    return this;
  }

  /**
   * Force overwrites a given query parameter with the given value.
   */
  public UriBuilder putQueryParameter(String name, Iterable<String> values) {
    query.put(name, values);
    return this;
  }

  /**
   * Removes a query parameter.
   */
  public UriBuilder removeQueryParameter(String name) {
    query.remove(name);
    return this;
  }

  /**
   * @return The queryParameters part of the uri, separated into component parts.
   */
  public Map<String, List<String>> getQueryParameters() {
    return query.getParams();
  }

  /**
   * @return All queryParameters parameters with the given name.
   */
  public List<String> getQueryParameters(String name) {
    return query.getParams(name);
  }

  /**
   * @return The first queryParameters parameter value with the given name.
   */
  public String getQueryParameter(String name) {
    return query.get(name);
  }

  /**
   * @return The queryParameters fragment.
   */
  public String getFragment() {
    return fragment.getString();
  }

  public UriBuilder setFragment(String str) {
    fragment.setString(str);
    return this;
  }

  public UriBuilder addFragmentParameter(String name, String value) {
    fragment.add(name, value);
    return this;
  }

  public UriBuilder addFragmentParameters(Map<String, String> parameters) {
    fragment.addAll(parameters);
    return this;
  }

  /**
   * Force overwrites a given fragment parameter with the given value.
   */
  public UriBuilder putFragmentParameter(String name, String... values) {
    fragment.put(name, values);
    return this;
  }

  /**
   * Force overwrites a given fragment parameter with the given value.
   */
  public UriBuilder putFragmentParameter(String name, Iterable<String> values) {
    fragment.put(name, values);
    return this;
  }

  /**
   * Removes a fragment parameter.
   */
  public UriBuilder removeFragmentParameter(String name) {
    fragment.remove(name);
    return this;
  }

  /**
   * @return The fragmentParameters part of the uri, separated into component parts.
   */
  public Map<String, List<String>> getFragmentParameters() {
    return fragment.getParams();
  }

  /**
   * @return All fragmentParameters parameters with the given name.
   */
  public List<String> getFragmentParameters(String name) {
    return fragment.getParams(name);
  }

  /**
   * @return The first fragmentParameters parameter value with the given name.
   */
  public String getFragmentParameter(String name) {
    return fragment.get(name);
  }

  /**
   * Utility method for joining key / value pair parameters into a url-encoded string.
   */
  public static String joinParameters(Map<String, List<String>> query) {
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
    Map<String, List<String>> params = Maps.newLinkedHashMap();
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

  private static final class ParamString {
    private final Map<String, List<String>> params;
    private String str;

    private ParamString() {
      this.params = Maps.newLinkedHashMap();
    }

    private ParamString(String str) {
      this();
      setString(str);
    }

    /**
     * @return The queryParameters fragment.
     */
    public String getString() {
      if (str == null) {
        str = joinParameters(params);
      }
      return str;
    }

    /**
     * Assigns the specified query string as the query portion of the uri, automatically decoding
     * parameters to populate the parameter map for calls to getParameter.
     */
    public void setString(String str) {
      params.clear();
      params.putAll(splitParameters(str));
      this.str = str;
    }

    public void add(String name, String value) {
      str = null;
      List<String> values = params.get(name);
      if (values == null) {
        values = Lists.newArrayList();
        params.put(name, values);
      }
      values.add(value);
    }

    public void addAll(Map<String, String> parameters) {
      str = null;
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        add(entry.getKey(), entry.getValue());
      }
    }

    /**
     * Force overwrites a given query parameter with the given value.
     */
    public void put(String name, String... values) {
      str = null;
      params.put(name, Lists.newArrayList(values));
    }

    /**
     * Force overwrites a given query parameter with the given value.
     */
    public void put(String name, Iterable<String> values) {
      str = null;
      params.put(name, Lists.newArrayList(values));
    }

    /**
     * Removes a query parameter.
     */
    public void remove(String name) {
      str = null;
      params.remove(name);
    }

    /**
     * @return The queryParameters part of the uri, separated into component parts.
     */
    public Map<String, List<String>> getParams() {
      return params;
    }

    /**
     * @return All queryParameters parameters with the given name.
     */
    public List<String> getParams(String name) {
      return params.get(name);
    }

    /**
     * @return The first queryParameters parameter value with the given name.
     */
    public String get(String name) {
      Collection<String> values = params.get(name);
      if (values == null || values.isEmpty()) {
        return null;
      }
      return values.iterator().next();
    }
  }
}

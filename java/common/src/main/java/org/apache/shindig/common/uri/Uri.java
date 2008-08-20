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

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
* Represents a Uniform Resource Identifier (URI) reference as defined by <a
* href="http://tools.ietf.org/html/rfc3986">RFC 3986</a>.
*
* Assumes that all url components are UTF-8 encoded.
*/
public final class Uri {
  private final String text;
  private final String scheme;
  private final String authority;
  private final String path;
  private final String query;
  private final String fragment;

  private final Multimap<String, String> queryParameters;


  Uri(UriBuilder builder) {
    scheme = builder.getScheme();
    authority = builder.getAuthority();
    path = builder.getPath();
    query = builder.getQuery();
    fragment = builder.getFragment();
    Multimap<String, String> copy = Multimaps.newArrayListMultimap(builder.getQueryParameters());
    queryParameters = Multimaps.unmodifiableMultimap(copy);

    StringBuilder out = new StringBuilder();

    if (scheme != null) {
      out.append(scheme).append(':');
    }
    if (authority != null) {
      out.append("//").append(authority);
    }
    if (path != null) {
      out.append(path);
    }
    if (query != null) {
      out.append('?').append(query);
    }
    if (fragment != null) {
      out.append('#').append(fragment);
    }
    text = out.toString();
  }

  /**
   * Produces a new Uri from a text representation.
   *
   * @param text The text uri.
   * @return A new Uri, parsed into components.
   */
  public static Uri parse(String text) {
    try {
      return fromJavaUri(new URI(text));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Convert a java.net.URI to a Uri.
   */
  public static Uri fromJavaUri(URI uri) {
    return new UriBuilder()
        .setScheme(uri.getScheme())
        .setAuthority(uri.getRawAuthority())
        .setPath(uri.getRawPath())
        .setQuery(uri.getRawQuery())
        .setFragment(uri.getRawFragment())
        .toUri();
  }

  /**
   * @return a java.net.URI equal to this Uri.
   */
  public URI toJavaUri() {
    return URI.create(toString());
  }

  /**
   * @return The scheme part of the uri, or null if none was specified.
   */
  public String getScheme() {
    return scheme;
  }

  /**
   * @return The authority part of the uri, or null if none was specified.
   */
  public String getAuthority() {
    return authority;
  }

  /**
   * @return The path part of the uri, or null if none was specified.
   */
  public String getPath() {
    return path;
  }

  /**
   * @return The query part of the uri, or null if none was specified.
   */
  public String getQuery() {
    return query;
  }

  /**
   * @return The query part of the uri, separated into component parts.
   */
  public Multimap<String, String> getQueryParameters() {
    return queryParameters;
  }

  /**
   * @return All query parameters with the given name.
   */
  public Collection<String> getQueryParameters(String name) {
    return queryParameters.get(name);    
  }

  /**
   * @return The first query parameter value with the given name.
   */
  public String getQueryParameter(String name) {
    Collection<String> values = queryParameters.get(name);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.iterator().next();
  }

  /**
   * @return The uri fragment.
   */
  public String getFragment() {
    return fragment;
  }

  @Override
  public String toString() {
    return text;
  }

  @Override
  public int hashCode() {
    return text.hashCode();
  }

  @Override
  public boolean equals(Object rhs) {
    if (rhs == this) {return true;}
    if (!(rhs instanceof Uri)) {return false;}

    return text.equals(((Uri)rhs).text);
  }
}

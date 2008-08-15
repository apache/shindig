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
  private final String queryString;
  private final Multimap<String, String> query;
  private final String fragment;

  /**
   * Produces a new Uri from a text representation.
   *
   * @param text The text uri.
   * @return A new Uri, parsed into components.
   */
  public static Uri parse(String text) {
    try {
      URI uri = new URI(text);
      return new Uri(uri.getScheme(),
                     uri.getAuthority(),
                     uri.getPath(),
                     UriBuilder.splitParameters(uri.getRawQuery()),
                     uri.getFragment());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  Uri(String scheme, String authority, String path, Multimap<String, String> query,
      String fragment) {
    this.scheme = scheme;
    this.authority = authority;
    this.path = path;
    this.query = Multimaps.unmodifiableMultimap(query);
    this.queryString = UriBuilder.joinParameters(query);
    this.fragment = fragment;

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
    if (queryString != null) {
      out.append('?').append(queryString);
    }
    if (fragment != null) {
      out.append('#').append(fragment);
    }
    this.text = out.toString();
  }

  /**
   * Converts the Uri to a java.net.URI.
   */
  public URI toJavaUri() throws URISyntaxException {
    return new URI(scheme, authority, path, queryString, fragment);
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
    return queryString;
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

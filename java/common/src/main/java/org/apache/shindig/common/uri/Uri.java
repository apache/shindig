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

import com.google.common.base.Join;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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

  private final Map<String, List<String>> queryParameters;

  private static UriParser parser = new DefaultUriParser();

  @Inject(optional = true)
  public static void setUriParser(UriParser uriParser) {
    parser = uriParser;
  }

  Uri(UriBuilder builder) {
    scheme = builder.getScheme();
    authority = builder.getAuthority();
    path = builder.getPath();
    query = builder.getQuery();
    fragment = builder.getFragment();
    queryParameters
        = Collections.unmodifiableMap(Maps.newLinkedHashMap(builder.getQueryParameters()));

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
    return parser.parse(text);
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
    try {
      return new URI(toString());
    } catch (URISyntaxException e) {
      // Shouldn't ever happen.
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Resolves a given url relative to this url. Resolution rules are the same as for
   * {@code java.net.URI.resolve(URI)}
   *
   * @param other The url to resolve against.
   * @return The new url.
   */
  public Uri resolve(Uri other) {
    // return  this.resolveNew(other);
    if (other == null) {
      return null;
    }
    
    return fromJavaUri(toJavaUri().resolve(other.toJavaUri()));
  }

  public Uri resolveNew(Uri other) {
    if (other == null) {
      return null;
    }


    String scheme = other.getScheme();
    String authority = other.getAuthority();
    String path = other.getPath();
    String query = other.getQuery();
    String fragment = other.getFragment();

    if (scheme != null && scheme.length() > 0) {
      // Do nothing - this will accept other's fields verbatim.
    } else if (authority != null) {
      // Schema-relative ie. "//newhost.com/foo?q=s". Take base scheme.
      scheme = getScheme();
    } else if (path != null && path.length() > 0) {
      // Resolve other path against current. Keep prerequisites.
      scheme = getScheme();
      authority = getAuthority();
      path = resolvePath(path);
    } else if (query != null && query.length() > 0) {
      // Accept query + fragment verbatim. Use base scheme/authority/path.
      scheme = getScheme();
      authority = getAuthority();
      // Treat query-relative as ""-path with query.
      path = resolvePath("");
    } else if (fragment != null && fragment.length() > 0) {
      // Accept fragment verbatim. Use base scheme/authority/path/query.
      scheme = getScheme();
      authority = getAuthority();
      path = getPath();
      query = getQuery();
    }

    return new UriBuilder()
        .setScheme(scheme)
        .setAuthority(authority)
        .setPath(path)
        .setQuery(query)
        .setFragment(fragment)
        .toUri();
  }

  /**
   * Resolves {@code otherPath} against the current path, returning the result.
   * Implements RFC 2396 resolution rules.
   */
  private String resolvePath(String otherPath) {
    if (otherPath.startsWith("/")) {
      // Optimization: just accept other.
      return otherPath;
    }
    
    // Relative path. Treat current path as a stack, otherPath as a List
    // in order to merge.
    LinkedList<String> pathStack = new LinkedList<String>();
    String curPath = getPath() != null ? getPath() : "/";  // Just in case.
    StringTokenizer tok = new StringTokenizer(curPath, "/");

    while (tok.hasMoreTokens()) {
      pathStack.add(tok.nextToken());
    }
    if (!curPath.endsWith("/")) {
      // The first entry in mergePath overwrites the last in the pathStack.
      // eg. curPath = "/foo/bar", otherPath = "baz" --> "/foo/baz".
      pathStack.removeLast();
    }

    LinkedList<String> mergePath = new LinkedList<String>();
    StringTokenizer tok2 = new StringTokenizer(otherPath, "/");
    while (tok2.hasMoreTokens()) {
      mergePath.add(tok2.nextToken());
    }
    if (otherPath.endsWith("/") || otherPath.equals("")) {
      // Retains the ending slash in the final join.
      mergePath.add("");
    }

    // Merge mergePath into pathStack.
    for (String mergeComponent : mergePath) {
      if (mergeComponent.equals(".")) {
        // Retain current position in the path. Continue.
        continue;
      } else if (mergeComponent.equals("..")) {
        // Pop one off the path stack if available. If not do nothing.
        if (!pathStack.isEmpty()) {
          pathStack.removeLast();
        }
      } else {
        // Append latest to the path.
        pathStack.add(mergeComponent);
      }
    }

    if (getAuthority() != null) {
      pathStack.addFirst(""); // get an initial / on the front..
    }
    return Join.join("/", pathStack);
  }

  /**
   * @return True if the Uri is absolute.
   */
  public boolean isAbsolute() {
    return scheme != null && authority != null;
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
  public Map<String, List<String>> getQueryParameters() {
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
  public boolean equals(Object obj) {
    if (obj == this) {return true;}
    if (!(obj instanceof Uri)) {return false;}
    return Objects.equal(text, ((Uri)obj).text);
  }
}

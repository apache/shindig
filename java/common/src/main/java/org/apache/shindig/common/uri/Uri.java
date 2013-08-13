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

import com.google.common.base.Strings;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
  private final Map<String, List<String>> fragmentParameters;

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
    queryParameters = ImmutableMap.copyOf(builder.getQueryParameters());
    fragmentParameters = ImmutableMap.copyOf(builder.getFragmentParameters());

    StringBuilder out = new StringBuilder();

    if (scheme != null) {
      out.append(scheme).append(':');
    }
    if (authority != null) {
      out.append("//").append(authority);
      // insure that there's a separator between authority/path
      if (path != null && path.length() > 1 && !path.startsWith("/")) {
        out.append('/');
      }
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
      return parser.parse(text);
    } catch (IllegalArgumentException e) {
      // This occurs all the time. Wrap the exception in a Uri-specific
      // exception, yet one that remains a RuntimeException, so that
      // callers may catch a specific exception rather than a blanket
      // Exception, as a compromise between throwing a checked exception
      // here (forcing wide-scale refactoring across the code base) and
      // forcing users to simply catch abstract Exceptions here and there.
      throw new UriException(e);
    }
  }

  /**
   * Convert a java.net.URI to a Uri.
   * @param uri the uri to convert
   * @return a shindig Uri
   */
  public static Uri fromJavaUri(URI uri) {
    if (uri.isOpaque()) {
      throw new UriException("No support for opaque Uris " + uri.toString());
    }
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
      throw new UriException(e);
    }
  }

  /**
   * Derived from Harmony
   * Resolves a given url relative to this url. Resolution rules are the same as for
   * {@code java.net.URI.resolve(URI)}
   */
  public Uri resolve(Uri relative) {
    if (relative == null) {
      return null;
    }
    if (relative.isAbsolute()) {
      return relative;
    }

    UriBuilder result;
    if (Strings.isNullOrEmpty(relative.path) && relative.scheme == null
        && relative.authority == null && relative.query == null
        && relative.fragment != null) {
      // if the relative URI only consists of fragment,
      // the resolved URI is very similar to this URI,
      // except that it has the fragement from the relative URI.
      result = new UriBuilder(this);
      result.setFragment(relative.fragment);
    } else if (relative.scheme != null) {
      result = new UriBuilder(relative);
    } else if (relative.authority != null) {
      // if the relative URI has authority,
      // the resolved URI is almost the same as the relative URI,
      // except that it has the scheme of this URI.
      result = new UriBuilder(relative);
      result.setScheme(scheme);
    } else {
      // since relative URI has no authority,
      // the resolved URI is very similar to this URI,
      // except that it has the query and fragment of the relative URI,
      // and the path is different.
      result = new UriBuilder(this);
      result.setFragment(relative.fragment);
      result.setQuery(relative.query);
      String relativePath = Objects.firstNonNull(relative.path, "");
      if (relativePath.startsWith("/")) { //$NON-NLS-1$
        result.setPath(relativePath);
      } else {
        // resolve a relative reference
        String basePath = path != null ? path : "/";
        int endindex = basePath.lastIndexOf('/') + 1;
        result.setPath(normalizePath(basePath.substring(0, endindex) + relativePath));
      }
    }
    Uri resolved = result.toUri();
    validate(resolved);
    return resolved;
  }

  private static void validate(Uri uri) {
    if (Strings.isNullOrEmpty(uri.authority) &&
        Strings.isNullOrEmpty(uri.path) &&
        Strings.isNullOrEmpty(uri.query)) {
      throw new UriException("Invalid scheme-specific part");
    }
  }

  /**
   * Dervived from harmony
   * normalize path, and return the resulting string
   */
  private static String normalizePath(String path) {
    // count the number of '/'s, to determine number of segments
    int index = -1;
    int pathlen = path.length();
    int size = 0;
    if (pathlen > 0 && path.charAt(0) != '/') {
      size++;
    }
    while ((index = path.indexOf('/', index + 1)) != -1) {
      if (index + 1 < pathlen && path.charAt(index + 1) != '/') {
        size++;
      }
    }

    String[] seglist = new String[size];
    boolean[] include = new boolean[size];

    // break the path into segments and store in the list
    int current = 0;
    int index2 = 0;
    index = (pathlen > 0 && path.charAt(0) == '/') ? 1 : 0;
    while ((index2 = path.indexOf('/', index + 1)) != -1) {
      seglist[current++] = path.substring(index, index2);
      index = index2 + 1;
    }

    // if current==size, then the last character was a slash
    // and there are no more segments
    if (current < size) {
      seglist[current] = path.substring(index);
    }

    // determine which segments get included in the normalized path
    for (int i = 0; i < size; i++) {
      include[i] = true;
      if (seglist[i].equals("..")) { //$NON-NLS-1$
        int remove = i - 1;
        // search back to find a segment to remove, if possible
        while (remove > -1 && !include[remove]) {
          remove--;
        }
        // if we find a segment to remove, remove it and the ".."
        // segment
        if (remove > -1 && !seglist[remove].equals("..")) { //$NON-NLS-1$
          include[remove] = false;
          include[i] = false;
        }
      } else if (seglist[i].equals(".")) { //$NON-NLS-1$
        include[i] = false;
      }
    }

    // put the path back together
    StringBuilder newpath = new StringBuilder();
    if (path.startsWith("/")) { //$NON-NLS-1$
      newpath.append('/');
    }

    for (int i = 0; i < seglist.length; i++) {
      if (include[i]) {
        newpath.append(seglist[i]);
        newpath.append('/');
      }
    }

    // if we used at least one segment and the path previously ended with
    // a slash and the last segment is still used, then delete the extra
    // trailing '/'
    if (!path.endsWith("/") && seglist.length > 0 //$NON-NLS-1$
        && include[seglist.length - 1]) {
      newpath.deleteCharAt(newpath.length() - 1);
    }

    String result = newpath.toString();

    // check for a ':' in the first segment if one exists,
    // prepend "./" to normalize
    index = result.indexOf(':');
    index2 = result.indexOf('/');
    if (index != -1 && (index < index2 || index2 == -1)) {
      newpath.insert(0, "./"); //$NON-NLS-1$
      result = newpath.toString();
    }
    return result;
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

  /**
   * @return The fragment part of the uri, separated into component parts.
   */
  public Map<String, List<String>> getFragmentParameters() {
    return fragmentParameters;
  }

  /**
   * @return All query parameters with the given name.
   */
  public Collection<String> getFragmentParameters(String name) {
    return fragmentParameters.get(name);
  }

  /**
   * @return The first query parameter value with the given name.
   */
  public String getFragmentParameter(String name) {
    Collection<String> values = fragmentParameters.get(name);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.iterator().next();
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

  /**
   * Interim typed, but not checked, exception facilitating migration
   * of Uri methods to throwing a checked UriException later.
   */
  public static final class UriException extends IllegalArgumentException {
    private UriException(Exception e) {
      super(e);
    }

    private UriException(String msg) {
      super(msg);
    }
  }
}

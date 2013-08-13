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
package org.apache.shindig.gadgets.features;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.servlet.UserAgent;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A FeatureResource that supports being supplied only to certain browsers.
 *
 * This is optional functionality, activated by the browser="..." attribute on
 * a &lt;script&gt; element. That attribute's value is interpreted as a
 * comma-separated list of BROWSER-versionKey matchers.
 *
 * BROWSER must match (case-insensitive) the list of UserAgent.Browser enum values
 * eg. "MSIE" or "FIREFOX".
 *
 * versionKey is OPERATORversionNumber, where OPERATOR may be one of:
 * ^ - regex
 * = - exact match
 * >, >=, <, <= - greater than/less than matches
 * [no operator] - exact match
 *
 * If no browser="..." attribute is specified, the resource always matches. Otherwise,
 * if ANY of the browser-versionKey matchers match, the resource matches. In such case,
 * the delegate FeatureResource's content methods are consulted. Otherwise, "" is returned
 * for content.
 *
 * Example:
 * browser="FireFox->=3, MSIE-6.0 would match FireFox 3.x.y (any) and IE 6.0 (only).
 *
 * To activate this capability, you may use the provided Loader class and bind it
 * as your FeatureResourceLoader implementation; or build your own that wraps its resources
 * in BrowserSpecificFeatureResource.
 */
public class BrowserSpecificFeatureResource implements FeatureResource {
  private final Provider<UserAgent> uaProvider;
  private final FeatureResource delegate;
  private final Map<UserAgent.Browser, List<VersionMatcher>> browserMatch;

  public BrowserSpecificFeatureResource(
      Provider<UserAgent> uaProvider, FeatureResource delegate, String browserKey) {
    this.uaProvider = uaProvider;
    this.delegate = delegate;
    this.browserMatch = populateBrowserMatchers(browserKey);
  }

  public String getContent() {
    if (browserMatches()) {
      return delegate.getContent();
    }
    return "";
  }

  public String getDebugContent() {
    if (browserMatches()) {
      return delegate.getDebugContent();
    }
    return "";
  }

  public boolean isExternal() {
    return delegate.isExternal();
  }

  public boolean isProxyCacheable() {
    // If browser-specific (ie. browserMatch has some qualifiers in it), not proxy cacheable
    // (since the vast majority of browsers don't support Vary: User-Agent, we just say "false")
    // Otherwise, delegate this call.
    return browserMatch.isEmpty() ? delegate.isProxyCacheable() : false;
  }

  public String getName() {
    return delegate.getName();
  }

  public Map<String, String> getAttribs() {
    return delegate.getAttribs();
  }

  private boolean browserMatches() {
    if (browserMatch.isEmpty()) {
      // Not browser-sensitive.
      return true;
    }
    UserAgent ua = uaProvider.get();
    List<VersionMatcher> versionMatchers = browserMatch.get(ua.getBrowser());
    if (versionMatchers != null) {
      for (VersionMatcher matcher : versionMatchers) {
        if (matcher.matches(ua.getVersion())) return true;
      }
    }
    return false;
  }

  private static Map<UserAgent.Browser, List<VersionMatcher>> populateBrowserMatchers(
      String browserKey) {
    Map<UserAgent.Browser, List<VersionMatcher>> map = Maps.newHashMap();
    if (browserKey == null || browserKey.length() == 0) {
      return map;
    }

    // Comma-delimited list of <browser>-<versionKey> pairs.
    String[] entries = StringUtils.split(browserKey, ',');
    for (String entry : entries) {
      entry = entry.trim();
      String[] browserAndVersion = StringUtils.split(entry, '-');
      String browser = browserAndVersion[0];
      String versionKey = browserAndVersion.length == 2 ? browserAndVersion[1] : null;

      // This may throw an IllegalArgumentException, (properly) indicating a faulty feature.xml
      UserAgent.Browser browserEnum = UserAgent.Browser.valueOf(browser.toUpperCase());
      if (!map.containsKey(browserEnum)) {
        map.put(browserEnum, Lists.<VersionMatcher>newLinkedList());
      }
      map.get(browserEnum).add(new VersionMatcher(versionKey));
    }

    return map;
  }

  /**
   * Simple FeatureResourceLoader implementation that wraps all resource loads in
   * a browser-filtering delegator.
   */
  public static class Loader extends FeatureResourceLoader {
    private final Provider<UserAgent> uaProvider;

    @Inject
    public Loader(HttpFetcher fetcher, TimeSource timeSource, FeatureFileSystem fileSystem,
        Provider<UserAgent> uaProvider) {
      super(fetcher, timeSource, fileSystem);
      this.uaProvider = uaProvider;
    }

    @Override
    public FeatureResource load(Uri uri, Map<String, String> attribs) throws GadgetException {
      return new BrowserSpecificFeatureResource(
          uaProvider, super.load(uri, attribs), attribs.get("browser"));
    }
  }

  private static final class VersionMatcher {
    private static final Op[] OPS = {
      new Op("^") {
        @Override
        public boolean match(String in, String key) {
          return in.matches(key);
        }
      },
      new Op("=") {
        @Override
        public boolean match(String in, String key) {
          return in.equals(key) || num(in).eq(num(key));
        }
      },
      new Op(">") {
        @Override
        public boolean match(String in, String key) {
          return num(in).gt(num(key));
        }
      },
      new Op(">=") {
        @Override
        public boolean match(String in, String key) {
          return in.equals(key) || num(in).eq(num(key)) || num(in).gt(num(key));
        }
      },
      new Op("<") {
        @Override
        public boolean match(String in, String key) {
          return num(in).lt(num(key));
        }
      },
      new Op("<=") {
        @Override
        public boolean match(String in, String key) {
          return in.equals(key) || num(in).eq(num(key)) || num(in).lt(num(key));
        }
      },
    };

    private final String versionKey;

    private VersionMatcher(String versionKey) {
      if (versionKey != null && versionKey.length() != 0) {
        this.versionKey = versionKey;
      } else {
        // No qualifier = match all (shortcut)
        this.versionKey = null;
      }
    }

    public boolean matches(String version) {
      if (versionKey == null || versionKey.equals(version)) {
        // Match-all or exact-string-match.
        return true;
      }
      for (Op op : OPS) {
        if (op.apply(version, versionKey)) {
          return true;
        }
      }
      return false;
    }

    private static VersionNumber num(String str) {
      return new VersionNumber(str);
    }

    private static abstract class Op {
      private final String pfx;

      private Op(String pfx) {
        this.pfx = pfx;
      }

      private boolean apply(String version, String key) {
        if (version.startsWith(pfx)) {
          version = version.substring(pfx.length());
          return match(version, key);
        }
        return false;
      }

      public abstract boolean match(String in, String key);
    }

    private static final class VersionNumber {
      private final int[] parts;

      private VersionNumber(String str) {
        String[] strParts = StringUtils.split(str, '.');
        int[] intParts = new int[strParts.length];
        try {
          for (int i = 0; i < strParts.length; ++i) {
            intParts[i] = Integer.parseInt(strParts[i]);
          }
        } catch (NumberFormatException e) {
          intParts = null;
        }
        this.parts = intParts;
      }

      public boolean eq(VersionNumber other) {
        return Arrays.equals(this.parts, other.parts);
      }

      public boolean lt(VersionNumber other) {
        for (int i = 0; i < this.parts.length; ++i) {
          int otherVal = (i < other.parts.length) ? other.parts[i] : 0;  // 0's fill in the rest
          if (this.parts[i] > otherVal) {
            return false;
          }
        }
        return true;
      }

      public boolean gt(VersionNumber other) {
        for (int i = 0; i < this.parts.length; ++i) {
          int otherVal = (i < other.parts.length) ? other.parts[i] : 0;  // 0's fill in the rest
          if (this.parts[i] < otherVal) {
            return false;
          }
        }
        return true;
      }
    }
  }
}

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
package org.apache.shindig.gadgets.rewrite;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parser for the "content-rewrite" feature. The supported params are
 * include-url and exclude-url which honor multiple occurances of the parameter,
 * these are simple case insensitive substrings, with "*" being the match-all
 * wildcard. Additionally expires is the seconds for caching of the rewritten
 * result. For legacy applications include-urls and exclude-urls, which are
 * regular expressions as well as a common seperated list in include-tags.
 * Default values are container specific.
 */
public class ContentRewriterFeature {
  protected static final String INCLUDE_URLS = "include-urls";
  protected static final String EXCLUDE_URLS = "exclude-urls";
  protected static final String INCLUDE_URL = "include-url";
  protected static final String EXCLUDE_URL = "exclude-url";
  protected static final String INCLUDE_TAGS = "include-tags";
  protected static final String EXPIRES = "expires";

  public static final Integer EXPIRES_HTTP = -1;  // -1 = Use HTTP.

  protected enum PatternOptions {
    ALL, NONE, REGEX, STRINGS
  }

  /**
   * Factory for content rewriter features.
   */
  @Singleton
  public static class Factory {
    private final GadgetSpecFactory specFactory;
    private final Provider<DefaultConfig> defaultConfig;

    private final LoadingCache<GadgetSpec, Config> rewriterConfigCache = CacheBuilder
        .newBuilder()
        .weakKeys()
        .build(
            new CacheLoader<GadgetSpec, Config>() {
              @Override
              public Config load(GadgetSpec spec) throws Exception {
                return new Config(spec, defaultConfig.get());
              }
            }
        );

    @Inject
    public Factory(GadgetSpecFactory specFactory, Provider<DefaultConfig> defaultConfig) {
      this.specFactory = specFactory;
      this.defaultConfig = defaultConfig;
    }

    public Config getDefault() {
      return defaultConfig.get();
    }

    public Config get(HttpRequest request) {
      GadgetSpec spec;
      final Uri gadgetUrl = request.getGadget();
      final boolean isIgnoreCache = request.getIgnoreCache();
      if (gadgetUrl != null) {
        try {
          GadgetContext context = new GadgetContext() {
            @Override
            public Uri getUrl() {
              return gadgetUrl;
            }

            @Override
            public boolean getIgnoreCache() {
              return isIgnoreCache;
            }
          };

          spec = specFactory.getGadgetSpec(context);
          if (spec != null) {
            return get(spec);
          }
        } catch (GadgetException ge) {
          // Falls through to default.
        }
      }
      return defaultConfig.get();
    }

    public Config get(GadgetSpec spec) {
      return rewriterConfigCache.getUnchecked(spec);
    }

    /**
     * Create a rewriter feature that allows all URIs to be rewritten.
     */
    public Config createRewriteAllFeature(int ttl) {
      return new Config(
          ".*", "", (ttl == -1) ? "HTTP" : Integer.toString(ttl),
          "", false, true, false);
    }
  }

  @Singleton
  public static class DefaultConfig extends Config {
    @Inject
    public DefaultConfig(
        @Named("shindig.content-rewrite.include-urls")String includeUrls,
        @Named("shindig.content-rewrite.exclude-urls")String excludeUrls,
        @Named("shindig.content-rewrite.expires")String expires,
        @Named("shindig.content-rewrite.include-tags")String includeTags,
        @Named("shindig.content-rewrite.only-allow-excludes")boolean onlyAllowExcludes,
        @Named("shindig.content-rewrite.enable-split-js-concat")boolean enableSplitJsConcat,
        @Named("shindig.content-rewrite.enable-single-resource-concat")boolean
            enableSingleResourceConcatenation) {
      super(includeUrls, excludeUrls, expires, includeTags, onlyAllowExcludes,
            enableSplitJsConcat, enableSingleResourceConcatenation);
    }
  }

  public static class Config {
    private final MatchBundle includes;
    private final MatchBundle excludes;

    // Use tree set to maintain order for fingerprint
    private final Set<String> includeTags;

    // If null then dont enforce a min TTL for proxied content.
    // Use contents headers
    private final Integer expires;
    private final boolean onlyAllowExcludes;
    private final boolean enableSplitJs;
    private final boolean enableSingleResourceConcatenation;

    // Lazily computed
    private Integer fingerprint;

    /**
     * Constructor which takes a gadget spec and container settings
     * as "raw" input strings.
     *
     * @param defaultInclude As a regex
     * @param defaultExclude As a regex
     * @param defaultExpires Either "HTTP" or a ttl in seconds
     * @param defaultTags Set of default tags that can be rewritten
     * @param onlyAllowExcludes If includes are always implicitly "all"
     * @param enableSplitJs If split-JS technique is enabled
     * @param enableSingleResourceConcatenation If single resource can be concatenated with itself
     */
    Config(String defaultInclude,
        String defaultExclude, String defaultExpires, String defaultTags,
        boolean onlyAllowExcludes, boolean enableSplitJs, boolean enableSingleResourceConcatenation) {
      // Set up includes from defaultInclude param
      this.includes = getMatchBundle(paramTrim(defaultInclude),
          Collections.<String>emptyList());

      // Set up excludes from defaultExclude param
      this.excludes = getMatchBundle(paramTrim(defaultExclude),
          Collections.<String>emptyList());

      // Parse includeTags
      ImmutableSet.Builder<String> includeTagsBuilder = ImmutableSet.builder();
      for (String s : Splitter.on(',').trimResults().omitEmptyStrings().split(defaultTags.toLowerCase())) {
        includeTagsBuilder.add(s);
      }
      this.includeTags = includeTagsBuilder.build();

      // Parse expires field
      int expiresVal = EXPIRES_HTTP;
      try {
        expiresVal = Integer.parseInt(paramTrim(defaultExpires));
      } catch (NumberFormatException e) {
        // Fall through to default.
      }
      this.expires = expiresVal;

      // Save config for onlyAllowExcludes
      this.onlyAllowExcludes = onlyAllowExcludes;
      this.enableSplitJs = enableSplitJs;
      this.enableSingleResourceConcatenation = enableSingleResourceConcatenation;
    }

    Config(GadgetSpec spec, Config defaultConfig) {
      this.onlyAllowExcludes = defaultConfig.onlyAllowExcludes;

      Feature f = spec.getModulePrefs().getFeatures().get("content-rewrite");

      // Include overrides.
      // Note: Shindig originally supported the plural versions with regular
      // expressions. But the OpenSocial specification v0.9 allows for singular
      // spellings, with multiple values. Plus they are case insensitive substrings.
      // For backward compatibility, if the singular versions are present they
      // will override the plural versions. 10/6/09
      String includeRegex = defaultConfig.includes.param;
      Collection<String> includeUrls = Lists.newArrayList();
      if (f != null && !onlyAllowExcludes) {
        if (f.getParams().containsKey(INCLUDE_URLS)) {
          includeRegex = f.getParam(INCLUDE_URLS);
        }
        Collection<String> paramUrls = f.getParamCollection(INCLUDE_URL);
        for (String url : paramUrls) {
          includeUrls.add(url.trim().toLowerCase());
        }
      }
      this.includes = getMatchBundle(includeRegex, includeUrls);

      // Exclude overrides. Only use the exclude regex specified by the
      // gadget spec if !onlyAllowExcludes.
      String excludeRegex = defaultConfig.excludes.param;
      Collection<String> excludeUrls = Lists.newArrayList();
      if (f != null) {
        if (f.getParams().containsKey(EXCLUDE_URLS)) {
          excludeRegex = f.getParam(EXCLUDE_URLS);
        }
        Collection<String> eParamUrls = f.getParamCollection(EXCLUDE_URL);
        for (String url : eParamUrls) {
          excludeUrls.add(url.trim().toLowerCase());
        }
      }
      this.excludes = getMatchBundle(excludeRegex, excludeUrls);

      // Spec-specified include tags.
      Set<String> tagsVal;
      if (f != null && f.getParams().containsKey(INCLUDE_TAGS)) {
        tagsVal = Sets.newTreeSet();
        for (String tag : Splitter.on(',').trimResults().omitEmptyStrings().split(f.getParam(INCLUDE_TAGS))) {
          tagsVal.add(tag.toLowerCase());
        }
        if (onlyAllowExcludes) {
          // Only excludes are allowed. Keep only subset of
          // specified tags that are in the defaults.
          tagsVal.retainAll(defaultConfig.includeTags);
        }
      } else {
        tagsVal = ImmutableSortedSet.copyOf(defaultConfig.includeTags);
      }
      this.includeTags = tagsVal;

      // Let spec/feature override if present and smaller than default.
      int expiresVal = defaultConfig.expires;
      if (f != null && f.getParams().containsKey(EXPIRES)) {
        try {
          int overrideVal = Integer.parseInt(f.getParam(EXPIRES));
          expiresVal = (expiresVal == EXPIRES_HTTP || overrideVal < expiresVal) ?
              overrideVal : expiresVal;
        } catch (NumberFormatException e) {
          // Falls through to default.
          if ("HTTP".equalsIgnoreCase(f.getParam(EXPIRES).trim())) {
            expiresVal = EXPIRES_HTTP;
          }
        }
      }
      this.expires = expiresVal;
      this.enableSplitJs = defaultConfig.enableSplitJs;
      this.enableSingleResourceConcatenation = defaultConfig.enableSingleResourceConcatenation;
    }

    private String paramTrim(String param) {
      if (param == null) {
        return param;
      }

      return param.trim();
    }

    private MatchBundle getMatchBundle(String regex, Collection<String> matches) {
      MatchBundle bundle = new MatchBundle();
      bundle.param = regex;
      bundle.matches = matches;

      if (bundle.matches.isEmpty() && Strings.isNullOrEmpty(bundle.param)) {
        bundle.options = PatternOptions.NONE;
      } else if (bundle.matches.size() == 1) {
        String firstVal = bundle.matches.iterator().next();
        if ("*".equals(firstVal)) {
          bundle.options = PatternOptions.ALL;
        } else if ("".equals(firstVal)){
          bundle.options = PatternOptions.NONE;
        } else {
          bundle.options = PatternOptions.STRINGS;
        }
      } else if (bundle.matches.size() > 1) {
        bundle.options = PatternOptions.STRINGS;
      } else {
        if (".*".equals(bundle.param)) {
          bundle.options = PatternOptions.ALL;
        } else {
          bundle.options = PatternOptions.REGEX;
        }
        bundle.pattern = Pattern.compile(bundle.param);
      }
      return bundle;
    }

    private static class MatchBundle {
      private String param;
      private PatternOptions options;
      private Pattern pattern;
      private Collection<String> matches;
    }

    protected boolean shouldInclude(String url) {
      return matcherMatches(url, includes);
    }

    protected boolean shouldExclude(String url) {
      return matcherMatches(url, excludes);
    }

    private static boolean matcherMatches(String url, MatchBundle bundle) {
      switch (bundle.options) {
      case NONE:
        return false;
      case ALL:
        return true;
      case REGEX:
        return bundle.pattern.matcher(url).find();
      case STRINGS:
        // "*" is handled by ALL
        String urllc = url.toLowerCase();
        for (String substr : bundle.matches) {
          if (urllc.contains(substr))
            return true;
        }
        return false;
      }
      return false;
    }

    public boolean isRewriteEnabled() {
      return includes.options != PatternOptions.NONE &&
             excludes.options != PatternOptions.ALL;
    }

    public boolean shouldRewriteURL(String url) {
      return shouldInclude(url) && !shouldExclude(url);
    }

    public boolean shouldRewriteTag(String tag) {
      if (tag != null) {
        return this.includeTags.contains(tag.toLowerCase());
      }
      return false;
    }

    public Set<String> getIncludedTags() {
      return includeTags;
    }

    /**
     * @return the min TTL to enforce or null if proxy should respect headers
     */
    public Integer getExpires() {
      return expires;
    }

    public boolean isSplitJsEnabled() {
      return enableSplitJs;
    }

    public boolean isSingleResourceConcatEnabled() {
      return enableSingleResourceConcatenation;
    }

    /**
     * @return fingerprint of rewriting rule for cache-busting
     */
    public int getFingerprint() {
      if (fingerprint == null) {
        int result =
            (includes.pattern != null ?
              includes.pattern.pattern().hashCode() : 0);
        for (String s : includes.matches) {
          result = 31 * result + s.hashCode();
        }
        result = 31 * result +
            (excludes.pattern != null ?
              excludes.pattern.pattern().hashCode() : 0);
        for (String s : excludes.matches) {
          result = 31 * result + s.hashCode();
        }
        for (String s : includeTags) {
          result = 31 * result + s.hashCode();
        }
        fingerprint = result;
      }
      return fingerprint;
    }
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
 * 
 * TODO: This really needs to be fixed, because it makes GadgetSpec mutable. It
 * is *ONLY* needed by code in the rewrite package.
 */
public class ContentRewriterFeature {

  protected static final String INCLUDE_URLS = "include-urls";
  protected static final String EXCLUDE_URLS = "exclude-urls";
  protected static final String INCLUDE_URL = "include-url";
  protected static final String EXCLUDE_URL = "exclude-url";
  protected static final String INCLUDE_TAGS = "include-tags";
  protected static final String EXPIRES = "expires";

  public static final String EXPIRES_DEFAULT = "HTTP";

  // Use tree set to maintain order for fingerprint
  protected Set<String> includeTags;

  protected enum PATTERNS {
    ALL, NONE, REGEX, STRINGS
  }

  protected PATTERNS includePatterns;
  protected PATTERNS excludePatterns;

  protected Pattern includePattern;
  protected Pattern excludePattern;
  protected Pattern excludeOverridePattern;
  protected Collection<String> includes;
  protected Collection<String> excludes;

  // If null then dont enforce a min TTL for proxied content. Use contents
  // headers
  protected Integer expires;

  protected Integer fingerprint;

  /**
   * Constructor which takes a gadget spec and the default container settings
   * 
   * @param spec
   * @param defaultInclude
   *          As a regex
   * @param defaultExclude
   *          As a regex
   * @param defaultExpires
   *          Either "HTTP" or a ttl in seconds
   * @param defaultTags
   *          Set of default tags that can be rewritten
   */
  public ContentRewriterFeature(GadgetSpec spec, String defaultInclude,
      String defaultExclude, String defaultExpires, Set<String> defaultTags,
      boolean onlyAllowExcludes) {
    Feature f = null;
    if (spec != null) {
      f = spec.getModulePrefs().getFeatures().get("content-rewrite");
    }
    setUpIncludes(f, defaultInclude, onlyAllowExcludes);
    setUpExcludes(f, defaultExclude, onlyAllowExcludes);
    setUpIncludeTags(f, defaultTags, onlyAllowExcludes);
    setUpExpires(f, defaultExpires, onlyAllowExcludes);
  }

  protected void setUpExpires(Feature f, String defaultExpires,
      boolean onlyAllowExcludes) {
    Integer defaultExpiresVal = null;
    try {
      defaultExpiresVal = new Integer(defaultExpires);
    } catch (NumberFormatException e) {
      // ignore
    }
    List<String> expiresOptions = Lists.newArrayListWithCapacity(3);
    if (f != null) {
      if (f.getParams().containsKey(EXPIRES)) {
        String p = normalizeParam(f.getParam(EXPIRES), null);
        Integer expiresParamVal = null;
        try {
          expiresParamVal = new Integer(p);
        } catch (NumberFormatException e) {
          // ignore
        }
        if (!onlyAllowExcludes || defaultExpiresVal == null
            || (expiresParamVal != null && expiresParamVal < defaultExpiresVal))
          expiresOptions.add(p);
      }
    }

    expiresOptions.add(defaultExpires);
    expiresOptions.add(EXPIRES_DEFAULT);

    for (String expiryOption : expiresOptions) {
      try {
        expires = new Integer(expiryOption);
        break;
      } catch (NumberFormatException nfe) {
        // Not an integer
        if (EXPIRES_DEFAULT.equalsIgnoreCase(expiryOption)) {
          break;
        }
      }
    }
  }

  protected void setUpIncludeTags(Feature f, Set<String> defaultTags,
      boolean onlyAllowExcludes) {
    this.includeTags = ImmutableSortedSet.copyOf(defaultTags);
    if (f != null) {
      String includeTagList = f.getParam(INCLUDE_TAGS);
      if (includeTagList != null) {
        Set<String> tags = Sets.newTreeSet();
        for (String tag : StringUtils.split(includeTagList, ',')) {
          if (tag != null) {
            tags.add(tag.trim().toLowerCase());
          }
        }
        if (onlyAllowExcludes) {
          tags.retainAll(defaultTags);
        }
        includeTags = tags;
      }
    }
  }

  // Note: Shindig originally supported the plural versions with regular
  // expressions. But the OpenSocial specification v0.9 allows for singular
  // spellings, with multiple values. Plus they are case insensitive substrings.
  // For backward compatibility, if the singular versions are present they
  // will override the plural versions. 10/6/09

  protected void setUpIncludes(Feature f, String defaultInclude,
      boolean onlyAllowExcludes) {
    String includeRegex = normalizeParam(defaultInclude, null);

    if (f != null && !onlyAllowExcludes) {
      if (f.getParams().containsKey(INCLUDE_URLS)) {
        includeRegex = normalizeParam(f.getParam(INCLUDE_URLS), includeRegex);
      }

      Collection<String> includeUrls = f.getParamCollection(INCLUDE_URL);
      if (includeUrls.isEmpty()) {
        includes = Collections.emptyList();
      } else if (includeUrls.contains("*")) {
        includes = Collections.singleton("*");
      } else {
        includes = new ArrayList<String>(includeUrls.size());
        for (String s : includeUrls) {
          if (s.length() > 0)
            includes.add(s.toLowerCase());
        }
      }
    } else {
      includes = Collections.emptyList();
    }

    if (includes.isEmpty()
        && (includeRegex == null || "".equals(includeRegex))) {
      includePatterns = PATTERNS.NONE;
    } else if (includes.size() > 0) {
      if (includes.size() == 1 && "*".equals(includes.iterator().next())) {
        includePatterns = PATTERNS.ALL;
      } else {
        includePatterns = PATTERNS.STRINGS;
      }
    } else {
      if (".*".equals(includeRegex)) {
        includePatterns = PATTERNS.ALL;
      } else {
        includePatterns = PATTERNS.REGEX;
      }
      includePattern = Pattern.compile(includeRegex);
    }
  }

  protected void setUpExcludes(Feature f, String defaultExclude,
      boolean onlyAllowExcludes) {
    String excludeRegex = normalizeParam(defaultExclude, null);
    String excludeOverrideRegex = onlyAllowExcludes ? excludeRegex : null;

    if (f != null) {
      // Note use of default for exclude as null here to allow clearing value in
      // the presence of a container default.
      if (f.getParams().containsKey(EXCLUDE_URLS)) {
        excludeRegex = normalizeParam(f.getParam(EXCLUDE_URLS), null);
      }

      Collection<String> excludeUrls = f.getParamCollection(EXCLUDE_URL);
      if (excludeUrls.isEmpty()) {
        excludes = Collections.emptyList();
      } else if (excludeUrls.contains("*")) {
        excludes = Collections.singleton("*");
      } else {
        excludes = new ArrayList<String>(excludeUrls.size());
        // Override Shindig defaults
        excludeRegex = null;
        for (String s : excludeUrls) {
          if (s.length() > 0)
            excludes.add(s.toLowerCase());
        }
      }
    } else {
      excludes = Collections.emptyList();
    }

    if (excludes.isEmpty()
        && (excludeRegex == null || "".equals(excludeRegex))) {
      excludePatterns = PATTERNS.NONE;
    } else if (excludes.size() > 0) {
      if (excludes.size() == 1 && "*".equals(excludes.iterator().next())) {
        excludePatterns = PATTERNS.ALL;
      } else {
        excludePatterns = PATTERNS.STRINGS;
      }
    } else {
      if (".*".equals(excludeRegex)) {
        excludePatterns = PATTERNS.ALL;
      } else {
        excludePatterns = PATTERNS.REGEX;
      }
      excludePattern = Pattern.compile(excludeRegex);
    }

    if (excludeOverrideRegex != null
        && !excludeOverrideRegex.equals(excludeRegex)) {
      excludeOverridePattern = Pattern.compile(excludeOverrideRegex);
      if (excludePatterns == PATTERNS.NONE)
        excludePatterns = PATTERNS.REGEX;
    }
  }

  protected String normalizeParam(String paramValue, String defaultVal) {
    if (paramValue == null) {
      return defaultVal;
    }
    paramValue = paramValue.trim();
    if (paramValue.length() == 0) {
      return defaultVal;
    }
    return paramValue;
  }

  protected boolean shouldInclude(String url) {
    switch (includePatterns) {
    case NONE:
      return false;
    case ALL:
      return true;
    case REGEX:
      return includePattern.matcher(url).find();
    case STRINGS:
      // "*" is handled by ALL
      String urllc = url.toLowerCase();
      for (String substr : includes) {
        if (urllc.indexOf(substr) >= 0)
          return true;
      }
      return false;
    }
    return false;
  }

  protected boolean shouldExclude(String url) {
    switch (excludePatterns) {
    case NONE:
      return false;
    case ALL:
      return true;
    case REGEX:
      return (excludeOverridePattern != null && excludeOverridePattern.matcher(
          url).find())
          || (excludePattern != null && excludePattern.matcher(url).find());
    case STRINGS:
      if (excludeOverridePattern != null
          && excludeOverridePattern.matcher(url).find())
        return true;
      // "*" is handled by ALL
      String urllc = url.toLowerCase();
      for (String substr : excludes) {
        if (urllc.indexOf(substr) >= 0)
          return true;
      }
      return false;
    }
    return false;
  }

  public boolean isRewriteEnabled() {
    return includePatterns != PATTERNS.NONE && excludePatterns != PATTERNS.ALL;
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

  /**
   * @return fingerprint of rewriting rule for cache-busting
   */
  public int getFingerprint() {
    if (fingerprint == null) {
      int result;
      result = (includePattern != null ? includePattern.pattern().hashCode()
          : 0);
      for (String s : includes) {
        result = 31 * result + s.hashCode();
      }
      result = 31 * result
          + (excludePattern != null ? excludePattern.pattern().hashCode() : 0);
      for (String s : excludes) {
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

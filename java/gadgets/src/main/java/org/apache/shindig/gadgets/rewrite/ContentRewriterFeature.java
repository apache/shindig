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

import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Parser for the "content-rewrite" feature. The supported params are
 * include-urls,exclude-urls,include-tags. Default values are container specific
 */
public class ContentRewriterFeature {

  private static final String INCLUDE_URLS = "include-urls";
  private static final String EXCLUDE_URLS = "exclude-urls";
  private static final String INCLUDE_TAGS = "include-tags";
  private static final String EXPIRES = "expires";

  public static final String EXPIRES_DEFAULT = "HTTP";

  // Use tree set to maintain order for fingerprint
  private TreeSet<String> includeTags;

  private boolean includeAll;
  private boolean includeNone;

  private Pattern include;
  private Pattern exclude;

  // If null then dont enforce a min TTL for proxied content. Use contents headers
  private Integer expires;

  private Integer fingerprint;

  /**
   * Constructor which takes a gadget spec and the default container settings
   *
   * @param spec
   * @param defaultInclude As a regex
   * @param defaultExclude As a regex
   * @param defaultExpires Either "HTTP" or a ttl in seconds
   * @param defaultTags    Set of default tags that can be rewritten
   */
  public ContentRewriterFeature(GadgetSpec spec, String defaultInclude,
                                String defaultExclude,
                                String defaultExpires,
      Set<String> defaultTags) {
    Feature f = spec.getModulePrefs().getFeatures().get("content-rewrite");
    String includeRegex = normalizeParam(defaultInclude, null);
    String excludeRegex = normalizeParam(defaultExclude, null);

    this.includeTags = new TreeSet<String>(defaultTags);

    List<String> expiresOptions = Lists.newArrayListWithCapacity(3);
    if (f != null) {
      if (f.getParams().containsKey(INCLUDE_URLS)) {
        includeRegex = normalizeParam(f.getParams().get(INCLUDE_URLS), includeRegex);
      }

      // Note use of default for exclude as null here to allow clearing value in the
      // presence of a container default.
      if (f.getParams().containsKey(EXCLUDE_URLS)) {
        excludeRegex = normalizeParam(f.getParams().get(EXCLUDE_URLS), null);
      }
      String includeTagList = f.getParams().get(INCLUDE_TAGS);
      if (includeTagList != null) {
        TreeSet<String> tags = new TreeSet<String>();
        for (String tag : includeTagList.split(",")) {
          if (tag != null) {
            tags.add(tag.trim().toLowerCase());
          }
        }
        includeTags = tags;
      }

      if (f.getParams().containsKey(EXPIRES)) {
        expiresOptions.add(normalizeParam(f.getParams().get(EXPIRES), null));
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

    if (".*".equals(includeRegex) && excludeRegex == null) {
      includeAll = true;
    }

    if (".*".equals(excludeRegex) || includeRegex == null) {
      includeNone = true;
    }

    if (includeRegex != null) {
      include = Pattern.compile(includeRegex);
    }
    if (excludeRegex != null) {
      exclude = Pattern.compile(excludeRegex);
    }
  }

  private String normalizeParam(String paramValue, String defaultVal) {
    if (paramValue == null) {
      return defaultVal;
    }
    paramValue = paramValue.trim();
    if (paramValue.length() == 0) {
      return defaultVal;
    }
    return paramValue;
  }

  public boolean isRewriteEnabled() {
    return !includeNone;
  }

  public boolean shouldRewriteURL(String url) {
    if (includeNone) {
      return false;
    } else if (includeAll) {
      return true;
    } else if (include.matcher(url).find()) {
      return !(exclude != null && exclude.matcher(url).find());
    }
    return false;
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
      result = (include != null ? include.pattern().hashCode() : 0);
      result = 31 * result + (exclude != null ? exclude.pattern().hashCode() : 0);
      for (String s : includeTags) {
        result = 31 * result + s.hashCode();
      }
      fingerprint =  result;
    }
    return fingerprint;
  }
  
  public static class Factory {
    private final String defaultIncludeUrls;
    private final String defaultExcludeUrls;
    private final String defaultExpires;
    private final Set<String> defaultIncludeTags;
    
    public Factory(String includeUrls, String excludeUrls, String expires,
        Set<String> includeTags) {
      defaultIncludeUrls = includeUrls;
      defaultExcludeUrls = excludeUrls;
      defaultExpires = expires;
      defaultIncludeTags = includeTags;
    }
    
    public ContentRewriterFeature get(GadgetSpec spec) {
      ContentRewriterFeature rewriterFeature =
        (ContentRewriterFeature)spec.getAttribute("content-rewrite");
      if (rewriterFeature == null) {
        rewriterFeature = new ContentRewriterFeature(spec, defaultIncludeUrls,
            defaultExcludeUrls, defaultExpires, defaultIncludeTags);
        spec.setAttribute("content-rewrite", rewriterFeature);
      }
      return rewriterFeature;
    }
  }
}

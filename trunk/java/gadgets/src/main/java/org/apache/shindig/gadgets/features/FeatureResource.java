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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Interface yielding content/code for JS features.
 */
public interface FeatureResource {
  /**
   * @return "Normal"-mode content for the feature, eg. optimized/obfuscated JS.
   */
  String getContent();

  /**
   * @return Debug-mode content for the feature.
   */
  String getDebugContent();

  /**
   * @return True if the content is actually a URL to be included via &lt;script src&gt;
   */
  boolean isExternal();

  /**
   * @return True if the JS can be cached by intermediary proxies or not.
   */
  boolean isProxyCacheable();

  /**
   * @return A descriptive name used to reference the JS in various debug and stats contexts.
   */
  String getName();

  /**
   * @return XML-defined attributes associated with this for resource selection/inclusion.
   */
  Map<String, String> getAttribs();

  /**
   * Helper base class to avoid having to implement rarely-overridden isExternal/isProxyCacheable
   * functionality in FeatureResource.
   */
  public abstract class Default implements FeatureResource {
    public boolean isExternal() {
      return false;
    }

    public boolean isProxyCacheable() {
      return true;
    }

    public Map<String, String> getAttribs() {
      return ImmutableMap.of();
    }
  }

  public abstract class Attribute extends Default {
    private final Map<String, String> attribs;

    public Attribute(Map<String, String> attribs) {
      this.attribs = attribs;
    }

    @Override
    public Map<String, String> getAttribs() {
      return attribs;
    }
  }

  public class Simple extends Default {
    private final String content;
    private final String debugContent;
    private final String name;

    public Simple(String content, String debugContent, String name) {
      this.content = content;
      this.debugContent = debugContent;
      this.name = name;
    }

    public String getContent() {
      return content;
    }

    public String getDebugContent() {
      return debugContent;
    }

    public String getName() {
      return name;
    }
  }
}

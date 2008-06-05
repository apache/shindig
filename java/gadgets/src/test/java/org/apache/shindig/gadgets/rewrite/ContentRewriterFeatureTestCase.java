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

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Test basic parsing of content-rewriter feature
 */
public class ContentRewriterFeatureTestCase extends BaseRewriterTestCase {

  public void testContainerDefaultIncludeAll() throws Exception {
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithoutRewrite(), ".*", "", tags);
    assertTrue(contentRewriterFeature.isRewriteEnabled());
    assertTrue(contentRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testContainerDefaultIncludeNone() throws Exception {
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithoutRewrite(), "", ".*", tags);
    assertFalse(contentRewriterFeature.isRewriteEnabled());
    assertFalse(contentRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testContainerDefaultExcludeOverridesInclude() throws Exception {
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithoutRewrite(), ".*", ".*", tags);
    assertFalse(contentRewriterFeature.isRewriteEnabled());
    assertFalse(contentRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testSpecExcludeOverridesContainerDefaultInclude() throws Exception {
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithRewrite("", ".*", tags), ".*", "", tags);
    assertFalse(contentRewriterFeature.isRewriteEnabled());
    assertFalse(contentRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testSpecIncludeOverridesContainerDefaultExclude() throws Exception {
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithRewrite(".*", "", tags), "", ".*", tags);
    assertTrue(contentRewriterFeature.isRewriteEnabled());
    assertTrue(contentRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testExcludeOverridesInclude() throws Exception {
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithRewrite("test\\.com", "test", tags), "", "", tags);
    assertTrue(contentRewriterFeature.isRewriteEnabled());
    assertFalse(contentRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testIncludeOnlyMatch() throws Exception {
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithRewrite("test\\.com", "testx", tags), "", "", tags);
    assertTrue(contentRewriterFeature.isRewriteEnabled());
    assertTrue(contentRewriterFeature.shouldRewriteURL("http://www.test.com"));
    assertFalse(contentRewriterFeature.shouldRewriteURL("http://testx.test.com"));
  }

  public void testTagRewrite() throws Exception {
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithRewrite("test\\.com", "testx", tags), "", "", tags);
    assertFalse(contentRewriterFeature.shouldRewriteTag("IFRAME"));
    assertTrue(contentRewriterFeature.shouldRewriteTag("img"));
    assertTrue(contentRewriterFeature.shouldRewriteTag("ScripT"));
  }

  public void testOverrideTagRewrite() throws Exception {
    Set<String> newTags = Sets.newHashSet("iframe");
    contentRewriterFeature = new ContentRewriterFeature(getSpecWithRewrite("test\\.com", "testx", newTags), "", "", tags);
    assertTrue(contentRewriterFeature.shouldRewriteTag("IFRAME"));
    assertFalse(contentRewriterFeature.shouldRewriteTag("img"));
    assertFalse(contentRewriterFeature.shouldRewriteTag("ScripT"));
    assertFalse(contentRewriterFeature.shouldRewriteTag("link"));
  }
}

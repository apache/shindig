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
    defaultRewriterFeature = new ContentRewriterFeature(createSpecWithoutRewrite(), ".*", "", "0", tags);
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testContainerDefaultIncludeNone() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(createSpecWithoutRewrite(), "", ".*", "0", tags);
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testContainerDefaultExcludeOverridesInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(createSpecWithoutRewrite(), ".*", ".*", "0",
        tags);
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testSpecExcludeOverridesContainerDefaultInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(createSpecWithRewrite("", ".*", "0", tags), ".*",
        "", "0", tags);
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testSpecIncludeOverridesContainerDefaultExclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(createSpecWithRewrite(".*", "", "0", tags), "",
        ".*", "0", tags);
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testExcludeOverridesInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(
        createSpecWithRewrite("test\\.com", "test", "0", tags), "", "", "0", tags);
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  public void testIncludeOnlyMatch() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(
        createSpecWithRewrite("test\\.com", "testx", "0", tags), "", "", "0", tags);
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://testx.test.com"));
  }

  public void testTagRewrite() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(
        createSpecWithRewrite("test\\.com", "testx", "0", tags), "", "", "0", tags);
    assertFalse(defaultRewriterFeature.shouldRewriteTag("IFRAME"));
    assertTrue(defaultRewriterFeature.shouldRewriteTag("img"));
    assertTrue(defaultRewriterFeature.shouldRewriteTag("ScripT"));
  }

  public void testOverrideTagRewrite() throws Exception {
    Set<String> newTags = Sets.newHashSet("iframe");
    defaultRewriterFeature = new ContentRewriterFeature(
        createSpecWithRewrite("test\\.com", "testx", "0", newTags), "", "", "0", tags);
    assertTrue(defaultRewriterFeature.shouldRewriteTag("IFRAME"));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("img"));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("ScripT"));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("link"));
  }

  public void testExpiresTimeParse() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(
        createSpecWithRewrite("test\\.com", "testx", "12345", tags), "", "", "0", tags);
    assertNotNull(defaultRewriterFeature.getExpires());
    assertNotNull(defaultRewriterFeature.getExpires() == 12345);
  }

  public void testExpiresHTTPParse() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(
        createSpecWithRewrite("test\\.com", "testx", "htTp ", tags), "", "", "12345", tags);
    assertNull(defaultRewriterFeature.getExpires());
  }

  public void testExpiresInvalidParse() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature(
        createSpecWithRewrite("test\\.com", "testx", "junk", tags), "", "", "12345", tags);
    assertNotNull(defaultRewriterFeature.getExpires());
    assertNotNull(defaultRewriterFeature.getExpires() == 12345);
  }

}

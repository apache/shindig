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

import org.junit.Test;

import com.google.common.collect.Sets;

import java.util.Set;
import static org.junit.Assert.*;

/**
 * Test basic parsing of content-rewriter feature
 */
public class ContentRewriterFeatureTestCase extends BaseRewriterTestCase {
  @Test
  public void testContainerDefaultIncludeAll() throws Exception {
    defaultRewriterFeature =
        new ContentRewriterFeature.Config(createSpecWithoutRewrite(),
          new ContentRewriterFeature.DefaultConfig(".*", "", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testContainerDefaultIncludeNone() throws Exception {
    defaultRewriterFeature =
        new ContentRewriterFeature.Config(createSpecWithoutRewrite(),
          new ContentRewriterFeature.DefaultConfig("", ".*", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testContainerDefaultExcludeOverridesInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(createSpecWithoutRewrite(),
        new ContentRewriterFeature.DefaultConfig(".*", ".*", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecExcludeOverridesContainerDefaultInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("", ".*", "0", tags),
        new ContentRewriterFeature.DefaultConfig(".*", "", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecExcludeOnlyOverridesContainerDefaultInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite(null, ".*", null, null),
        new ContentRewriterFeature.DefaultConfig(".*", "", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecExcludeOverridesContainerDefaultExclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite(".*", "", "0", tags),
        new ContentRewriterFeature.DefaultConfig("", ".*", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testExcludeOverridesInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "test", "0", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testIncludeOnlyMatch() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "0", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://testx.test.com"));
  }

  @Test
  public void testTagRewrite() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "0", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("IFRAME"));
    assertTrue(defaultRewriterFeature.shouldRewriteTag("img"));
    assertTrue(defaultRewriterFeature.shouldRewriteTag("ScripT"));
  }

  @Test
  public void testOverrideTagRewrite() throws Exception {
    Set<String> newTags = Sets.newHashSet("iframe");
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "0", newTags),
        new ContentRewriterFeature.DefaultConfig("", "", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.shouldRewriteTag("IFRAME"));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("img"));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("ScripT"));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("link"));
  }

  @Test
  public void testExpiresTimeParse() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "12345", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "0", TAGS, false, false, false));
    assertNotNull(defaultRewriterFeature.getExpires());
    assertNotNull(defaultRewriterFeature.getExpires() == 12345);
  }

  @Test
  public void testExpiresHTTPParse() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "htTp ", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "12345", TAGS, false, false, false));
    assertEquals(ContentRewriterFeature.EXPIRES_HTTP, defaultRewriterFeature.getExpires());
  }

  @Test
  public void testExpiresOverwriteTooBig() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "20000", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "12345", TAGS, false, false, false));
    assertEquals(12345, defaultRewriterFeature.getExpires().intValue());
  }

  @Test
  public void testExpiresBadValue() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "X", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "12345", TAGS, false, false, false));
    assertEquals(12345, defaultRewriterFeature.getExpires().intValue());
  }

  @Test
  public void testExpiresOverwrite() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "10", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "12345", TAGS, false, false, false));
    assertEquals(10, defaultRewriterFeature.getExpires().intValue());
  }

  @Test
  public void testExpiresOverwriteDefault() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "10", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "-1", TAGS, false, false, false));
    assertEquals(10, defaultRewriterFeature.getExpires().intValue());
  }

  @Test
  public void testExpiresInvalidParse() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "junk", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "12345", TAGS, false, false, false));
    assertNotNull(defaultRewriterFeature.getExpires());
    assertNotNull(defaultRewriterFeature.getExpires() == 12345);
  }

  @Test
  public void testSpecEmptyContainerWithExclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite(null, null, null, null),
        new ContentRewriterFeature.DefaultConfig(".*", "test", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.foobar.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecExcludeOnlyOverridesContainerWithExclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite(null, "", null, null),
        new ContentRewriterFeature.DefaultConfig(".*", "test", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.foobar.com"));
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecEmptyDoesNotOverridesContainerDefaultNoInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite(null, null, null, null),
        new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.foobar.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecIncludeOnlyOverridesContainerDefaultNoInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite(".*", null, null, null),
        new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.foobar.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSplitJsSupported() throws Exception {
    defaultRewriterFeature =
        new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, true, false);
    assertTrue(defaultRewriterFeature.isSplitJsEnabled());
  }

  @Test
  public void testSplitJsNotSupported() throws Exception {
    defaultRewriterFeature =
      new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, false, false);
    assertFalse(defaultRewriterFeature.isSplitJsEnabled());
  }

  @Test
  public void testSingleResourceConcatEnabled() throws Exception {
    defaultRewriterFeature =
        new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, false, true);
    assertTrue(defaultRewriterFeature.isSingleResourceConcatEnabled());
  }

  @Test
  public void testSingleResourceConcatNotEnabled() throws Exception {
    defaultRewriterFeature =
      new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, false, false);
    assertFalse(defaultRewriterFeature.isSingleResourceConcatEnabled());
  }
}

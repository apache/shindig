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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;

import org.junit.Test;

/**
 * Test basic parsing of content-rewriter feature using Open Social v0.9 keywords
 */
public class ContentRewriterFeatureTestCaseOS9 extends BaseRewriterTestCase {

  @Test
  public void testSpecExcludeOverridesContainerDefaultInclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(new String[] { "" }, new String[] { "*" }, "0", tags),
        new ContentRewriterFeature.DefaultConfig(".*", "", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecExcludeOverridesMultipleContainerDefaultInclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(null, new String[] { "foo", "bar" }, "0", tags),
        new ContentRewriterFeature.DefaultConfig(".*", "", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.foo.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.bar.com"));
  }

  @Test
  public void testSpecExcludeOnlyOverridesContainerDefaultInclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(null, new String[] { "*" }, null, null),
        new ContentRewriterFeature.DefaultConfig(".*", "", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecExcludeOverridesContainerDefaultExclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(new String[] { "*" }, new String[] { "" }, "0", tags),
        new ContentRewriterFeature.DefaultConfig("", ".*", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testExcludeOverridesInclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(new String[] { "test.com" }, new String[] { "test" }, "0", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testIncludeOnlyMatch() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(new String[] { "test.com" }, new String[] { "testx" }, "0", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://testx.test.com"));
  }

  @Test
  public void testSpecEmptyContainerWithExclude() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(null, null, null, null),
        new ContentRewriterFeature.DefaultConfig(".*", "test", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.foobar.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecExcludeOnlyOverridesContainerWithExclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(null, new String[] { "" }, null, null),
        new ContentRewriterFeature.DefaultConfig(".*", "test", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.foobar.com"));
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecEmptyDoesNotOverridesContainerDefaultNoInclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(null, null, null, null),
        new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, false, false));
    assertFalse(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://www.foobar.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecIncludeOnlyOverridesContainerDefaultNoInclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(new String[] { "*" }, null, null, null),
        new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.foobar.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecIncludeMultipleOnlyOverridesContainerDefaultNoInclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(new String[] { "foo", "bar" }, null, null, null),
        new ContentRewriterFeature.DefaultConfig("", "test", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.foo.com"));
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.bar.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecIncludeMultipleOnlyOverridesContainerDefaultInclude()
      throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(new String[] { "foo", "bar" }, null, null, null),
        new ContentRewriterFeature.DefaultConfig("*", "test", "0", TAGS, false, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.foo.com"));
    assertTrue(defaultRewriterFeature.shouldRewriteURL("http://www.bar.com"));
    assertFalse(defaultRewriterFeature.shouldRewriteURL("http://www.test.com"));
  }

  @Test
  public void testSpecExcludeDisallowOverrideIncludeUrls() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("norewrite", null, null, null),
        new ContentRewriterFeature.DefaultConfig("^http://www.include.com", "def", "3600", TAGS, true, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature
        .shouldRewriteURL("http://www.include.com/abc"));
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://www.include.com/def"));
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://www.norewrite.com/abc"));
  }

  @Test
  public void testSpecExcludeOverrideExcludeUrls() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite(null, "abc", null, null),
        new ContentRewriterFeature.DefaultConfig("^http://www.include.com", "def", "3600", TAGS, true, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://www.include.com/abc"));
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://www.norewrite.com/abc"));
    assertTrue(defaultRewriterFeature
        .shouldRewriteURL("http://www.include.com/def"));
  }

  @Test
  public void testSpecExcludeDisallowOverrideIncludeUrlOS9() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(new String[] { "abc" }, null, null, null),
        new ContentRewriterFeature.DefaultConfig("^http://www.include.com", "", "3600", TAGS, true, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature
        .shouldRewriteURL("http://www.include.com/abc"));
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://www.norewrite.com/abc"));
  }

  @Test
  public void testSpecExcludeDisallowOverrideExcludeUrlOS9() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewriteOS9(null, new String[] { "def" }, null, null),
        new ContentRewriterFeature.DefaultConfig("^http://www.include.com", "", "3600", TAGS, true, false, false));
    assertTrue(defaultRewriterFeature.isRewriteEnabled());
    assertTrue(defaultRewriterFeature
        .shouldRewriteURL("http://www.include.com/abc"));
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://www.include.com/def"));
    assertFalse(defaultRewriterFeature
        .shouldRewriteURL("http://www.norewrite.com/abc"));
  }

  @Test
  public void testSpecExcludeDisallowOverrideDefaultExpires() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "3000", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "", TAGS, true, false, false));
    assertNotNull(defaultRewriterFeature.getExpires());
    assertNotNull(defaultRewriterFeature.getExpires() == 3000);
  }

  @Test
  public void testSpecExcludeDisallowOverrideExpiresGreater() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "8000", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "3000", TAGS, true, false, false));
    assertNotNull(defaultRewriterFeature.getExpires());
    assertNotNull(defaultRewriterFeature.getExpires() == 3000);
  }

  @Test
  public void testSpecExcludeDisallowOverrideExpiresLesser() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "2000", tags),
        new ContentRewriterFeature.DefaultConfig("", "", "3000", TAGS, true, false, false));
    assertNotNull(defaultRewriterFeature.getExpires());
    assertNotNull(defaultRewriterFeature.getExpires() == 2000);
  }

  @Test
  public void testSpecExcludeDisallowOverrideTagsSubset() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite("test\\.com", "testx", "0", Sets.newHashSet("img")),
        new ContentRewriterFeature.DefaultConfig("", "", "0", "img,script", true, false, false));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("IFRAME"));
    assertTrue(defaultRewriterFeature.shouldRewriteTag("img"));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("ScripT"));
  }

  @Test
  public void testSpecExcludeDisallowOverrideTagsSuperset() throws Exception {
    defaultRewriterFeature = new ContentRewriterFeature.Config(
        createSpecWithRewrite( "test\\.com", "testx", "0", Sets.newHashSet("img", "script", "link")),
        new ContentRewriterFeature.DefaultConfig("", "", "0", "img,script", true, false, false));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("IFRAME"));
    assertTrue(defaultRewriterFeature.shouldRewriteTag("img"));
    assertTrue(defaultRewriterFeature.shouldRewriteTag("ScripT"));
    assertFalse(defaultRewriterFeature.shouldRewriteTag("link"));
  }

}

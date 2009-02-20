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
package org.apache.shindig.gadgets.rewrite.image;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import static junit.framework.Assert.*;

import static junit.framework.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

/**
 * Tests for ImageRewriter
 */
public class ImageRewriterTest {

  private ImageRewriter rewriter;

//  @Override
  @Before
  public void setUp() throws Exception {
    rewriter = new BasicImageRewriter(new OptimizerConfig());
  }

  @Test
  public void testRewriteValidImageWithValidMimeAndExtn() throws Exception {
    byte[] bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
        "org/apache/shindig/gadgets/rewrite/image/inefficient.png"));
    HttpResponse original = new HttpResponseBuilder()
        .setHeader("Content-Type", "image/png")
        .setHttpStatusCode(HttpResponse.SC_OK)
        .setResponse(bytes)
        .create();
    HttpResponse rewritten = rewriter.rewrite(Uri.parse("some.png"), original);
    assertEquals(rewritten.getHttpStatusCode(), HttpResponse.SC_OK);
    assertTrue(rewritten.getContentLength() < original.getContentLength());
  }

  @Test
  public void testRewriteValidImageWithInvalidMimeAndFileExtn() throws Exception {
    byte[] bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
        "org/apache/shindig/gadgets/rewrite/image/inefficient.png"));
    HttpResponse original = new HttpResponseBuilder()
        .setHeader("Content-Type", "notimage/anything")
        .setHttpStatusCode(HttpResponse.SC_OK)
        .setResponse(bytes)
        .create();
    HttpResponse rewritten = rewriter.rewrite(Uri.parse("some.junk"), original);
    assertEquals(rewritten.getHttpStatusCode(), HttpResponse.SC_OK);
    assertTrue(rewritten.getContentLength() < original.getContentLength());
  }

  @Test
  public void testRewriteInvalidImageContentWithValidMime() throws Exception {
    HttpResponse original = new HttpResponseBuilder()
        .setHeader("Content-Type", "image/png")
        .setHttpStatusCode(HttpResponse.SC_OK)
        .setResponseString("This is not a PNG")
        .create();
    HttpResponse rewritten = rewriter.rewrite(Uri.parse("some.junk"), original);
    assertEquals(rewritten.getHttpStatusCode(), HttpResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    assertEquals(rewritten.getResponseAsString(),
        "Content is not an image but mime type asserts it is");
  }

  @Test
  public void testRewriteInvalidImageContentWithValidFileExtn() throws Exception {
    HttpResponse original = new HttpResponseBuilder()
        .setHeader("Content-Type", "notimage/anything")
        .setHttpStatusCode(HttpResponse.SC_OK)
        .setResponseString("This is not an image")
        .create();
    HttpResponse rewritten = rewriter.rewrite(Uri.parse("some.png"), original);
    assertEquals(rewritten.getHttpStatusCode(), HttpResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    assertEquals(rewritten.getResponseAsString(),
        "Content is not an image but file extension asserts it is");
  }

  @Test
  @Ignore("This test is known failing with the current sanselan codebase as of Feb 20, 2009")
  public void testNoRewriteAnimatedGIF() throws Exception {
    byte[] bytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
        "org/apache/shindig/gadgets/rewrite/image/animated.gif"));
    HttpResponse original = new HttpResponseBuilder()
        .setHeader("Content-Type", "image/gif")
        .setHttpStatusCode(HttpResponse.SC_OK)
        .setResponse(bytes)
        .create();
    assertSame(rewriter.rewrite(Uri.parse("animated.gif"), original), original);
  }

}
 

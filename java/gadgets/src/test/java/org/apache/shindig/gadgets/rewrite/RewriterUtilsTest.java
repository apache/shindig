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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.uri.UriCommon;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for RewriterUtils.
 */
public class RewriterUtilsTest {
  @Test
  public void testIsHtmlWithoutHtmlTagContext() throws Exception {
    HttpRequest req = new HttpRequest(Uri.parse("http://www.example.org/"));
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/html");
    assertTrue(RewriterUtils.isHtml(req, builder));
  }

  @Test
  public void testIsHtmlReturnsFalseIfNonHtmlTagContext() throws Exception {
    HttpRequest req = new HttpRequest(Uri.parse("http://www.example.org/"));
    req.setParam(UriCommon.Param.HTML_TAG_CONTEXT.getKey(), "script");
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/html");
    assertFalse(RewriterUtils.isHtml(req, builder));
  }

  @Test
  public void testIsHtmlReturnsTrueIfHtmlAcceptingTagContext() throws Exception {
    HttpRequest req = new HttpRequest(Uri.parse("http://www.example.org/"));
    req.setParam(UriCommon.Param.HTML_TAG_CONTEXT.getKey(), "link");
    HttpResponseBuilder builder = new HttpResponseBuilder()
        .addHeader("Content-Type", "text/html");
    assertTrue(RewriterUtils.isHtml(req, builder));

    req.setParam(UriCommon.Param.HTML_TAG_CONTEXT.getKey(), "iframe");
    assertTrue(RewriterUtils.isHtml(req, builder));
  }
}

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
package org.apache.shindig.gadgets.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterTestBase;
import org.junit.Test;

public class CajaResponseRewriterTest extends RewriterTestBase {
  private static final Uri CONTENT_URI = Uri.parse("http://www.example.org/content");

  private String rewrite(HttpRequest request, HttpResponse response) throws Exception {
    return rewrite(request, response, null);
  }

  private String rewrite(HttpRequest request, HttpResponse response, Gadget gadget) throws Exception {
    request.setSanitizationRequested(true);
    ResponseRewriter rewriter = createRewriter();

    HttpResponseBuilder hrb = new HttpResponseBuilder(parser, response);
    rewriter.rewrite(request, hrb, gadget);
    return hrb.getContent();
  }

  private ResponseRewriter createRewriter() {
    return new CajaResponseRewriter(new RequestPipeline() {
      public HttpResponse execute(HttpRequest request) {
        return null;
      }
    });
  }

  @Test
  public void testJs() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/javascript");
    req.setCajaRequested(true);
    HttpResponse response = new HttpResponseBuilder().setResponseString("var a;").create();
    String sanitized = "___.di(IMPORTS___,'a');";

    assertTrue(rewrite(req, response).contains(sanitized));
    assertTrue(rewrite(req, response, gadget).contains(sanitized));
  }

  @Test
  public void testJsWithoutCaja() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/javascript");
    req.setCajaRequested(false);
    HttpResponse response = new HttpResponseBuilder().setResponseString("var a;").create();
    String sanitized = "var a;";

    assertTrue(rewrite(req, response).contains(sanitized));
    assertTrue(rewrite(req, response, gadget).contains(sanitized));
  }

  @Test
  public void testNonJs() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/html");
    req.setCajaRequested(true);
    HttpResponse response = new HttpResponseBuilder().setResponseString("<html></html>").create();

    assertEquals("", rewrite(req, response));
    assertEquals("", rewrite(req, response, gadget));
  }
}

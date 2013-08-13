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
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.parse.caja.CajaCssParser;
import org.apache.shindig.gadgets.parse.caja.CajaCssSanitizer;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ResponseRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterTestBase;
import org.apache.shindig.gadgets.uri.PassthruManager;
import org.junit.Test;

import com.google.inject.util.Providers;

public class SanitizingResponseRewriterTest extends RewriterTestBase {
  private static final Uri CONTENT_URI = Uri.parse("http://www.example.org/content");
  private static final String PROXY_HOST = "proxy.com";
  private static final String PROXY_PATH = "/gadgets/proxy";
  private static final String PROXY_BASE = PROXY_HOST + PROXY_PATH;

  private String rewrite(HttpRequest request, HttpResponse response) throws Exception {
    return rewrite(request, response, null);
  }

  private String rewrite(HttpRequest request, HttpResponse response, Gadget gadget) throws Exception {
    request.setSanitizationRequested(true);
    ResponseRewriter rewriter = createRewriter(Collections.<String>emptySet(),
        Collections.<String>emptySet());

    HttpResponseBuilder hrb = new HttpResponseBuilder(parser, response);
    rewriter.rewrite(request, hrb, gadget);
    if (hrb.getNumChanges() == 0) {
      return null;
    }
    return hrb.getContent();
  }

  private ResponseRewriter createRewriter(Set<String> tags, Set<String> attributes) {
    ContentRewriterFeature.Factory rewriterFeatureFactory =
        new ContentRewriterFeature.Factory(null,
          Providers.of(new ContentRewriterFeature.DefaultConfig(
            ".*", "", "HTTP", "embed,img,script,link,style", false, false, false)));
    return new SanitizingResponseRewriter(rewriterFeatureFactory,
        new CajaCssSanitizer(new CajaCssParser()), new PassthruManager(PROXY_HOST, PROXY_PATH));
  }

  @Test
  public void enforceInvalidProxedCssRejected() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/css");
    HttpResponse response = new HttpResponseBuilder().setResponseString("doEvil()").create();
    String sanitized = "";
    assertEquals(sanitized, rewrite(req, response));
    assertEquals(sanitized, rewrite(req, response, gadget));
  }

  @Test
  public void enforceValidProxedCssAccepted() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/css");
    HttpResponse response = new HttpResponseBuilder().setResponseString(
        "@import url('http://www.evil.com/more.css'); A { font : BOLD }").create();
    // The caja css sanitizer does *not* remove the initial colon in urls
    // since this does not work in IE
    String sanitized =
      // Resultant URL is just the "sanitized" version of same, since we're using
      // PassthruUriManager for testing purposes.
      "@import url('http://" + PROXY_BASE + "?url="
        + "http%3A%2F%2Fwww.evil.com%2Fmore.css&sanitize=1&rewriteMime=text%2Fcss');\n"
        + "A {\n"
        + "  font: BOLD\n"
        + '}';
    String rewritten = rewrite(req, response);
    String rewrittenGadget = rewrite(req, response, gadget);
    assertEquals(sanitized, rewritten);
    assertEquals(sanitized, rewrittenGadget);
  }

  @Test
  public void enforceValidProxedCssAcceptedNoCache() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/css");
    req.setIgnoreCache(true);
    HttpResponse response = new HttpResponseBuilder().setResponseString(
        "@import url('http://www.evil.com/more.css'); A { font : BOLD }").create();
    // The caja css sanitizer does *not* remove the initial colon in urls
    // since this does not work in IE
    String sanitized =
      "@import url('http://" + PROXY_BASE + "?url="
        + "http%3A%2F%2Fwww.evil.com%2Fmore.css&sanitize=1&rewriteMime=text%2Fcss');\n"
        + "A {\n"
        + "  font: BOLD\n"
        + '}';
    String rewritten = rewrite(req, response);
    String rewrittenGadget = rewrite(req, response, gadget);
    assertEquals(sanitized, rewritten);
    assertEquals(sanitized, rewrittenGadget);
  }

  @Test
  public void enforceInvalidProxedImageRejected() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("image/*");
    HttpResponse response = new HttpResponseBuilder().setResponse("NOTIMAGE".getBytes()).create();
    String sanitized = "";
    assertEquals(sanitized, rewrite(req, response));
    assertEquals(sanitized, rewrite(req, response, gadget));
  }

  @Test
  public void validProxiedImageAccepted() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("image/*");
    HttpResponse response = new HttpResponseBuilder().setResponse(
        IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream(
            "org/apache/shindig/gadgets/rewrite/image/inefficient.png"))).create();
    assertNull(rewrite(req, response));
    assertNull(rewrite(req, response, gadget));
  }

  @Test
  public void enforceUnknownMimeTypeRejected() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    req.setRewriteMimeType("text/foo");
    HttpResponse response = new HttpResponseBuilder().setResponseString("doEvil()").create();
    String sanitized = "";
    assertEquals(sanitized, rewrite(req, response));
    assertEquals(sanitized, rewrite(req, response, gadget));
  }

  @Test
  public void enforceMissingMimeTypeRejected() throws Exception {
    Gadget gadget = mockGadget();
    control.replay();
    HttpRequest req = new HttpRequest(CONTENT_URI);
    // A request without a mime type, but requesting sanitization, should be rejected
    req.setRewriteMimeType(null);
    HttpResponse response = new HttpResponseBuilder().setResponseString("doEvil()").create();
    String sanitized = "";
    assertEquals(sanitized, rewrite(req, response));
    assertEquals(sanitized, rewrite(req, response, gadget));
  }
}

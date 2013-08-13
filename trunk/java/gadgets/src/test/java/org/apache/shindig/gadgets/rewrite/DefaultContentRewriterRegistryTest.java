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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import com.google.common.collect.Lists;

public class DefaultContentRewriterRegistryTest extends BaseRewriterTestCase {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private List<CaptureRewriter> rewriters;
  private List<ResponseRewriter> contentRewriters;
  private ResponseRewriterRegistry registry;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    rewriters = Lists.newArrayList(new CaptureRewriter(), new CaptureRewriter());
    contentRewriters = Lists.<ResponseRewriter>newArrayList(rewriters);
    registry = new DefaultResponseRewriterRegistry(contentRewriters, parser);
  }

  @Test
  public void testRewriteHttpResponse() throws Exception {
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(SPEC_URL);
    HttpResponse response = new HttpResponse(body);

    HttpResponse rewritten = registry.rewriteHttpResponse(request, response, null);

    assertTrue("First rewriter not invoked.", rewriters.get(0).responseWasRewritten());
    assertTrue("Second rewriter not invoked.", rewriters.get(1).responseWasRewritten());

    assertEquals(response, rewritten);
  }

  /**
   * This test ensures that we dont call HttpResponse.getResponseAsString if no content
   * rewriter does so either. This is important
   * from a performance and content consistency standpoint. Because HttpResponse is final
   * we test that no new response object was created.
   */
  @Test
  public void testNoDecodeHttpResponseForUnRewriteableMimeTypes() throws Exception {
    List<ResponseRewriter> rewriters = Lists.newArrayList();
    rewriters.add(new ResponseRewriter() {
      public void rewrite(HttpRequest request, HttpResponseBuilder response, Gadget gadget)
              throws RewritingException {
        // Do nothing.
      }
    });
    registry = new DefaultResponseRewriterRegistry(rewriters, parser);

    HttpRequest req = control.createMock(HttpRequest.class);
    EasyMock.expect(req.getRewriteMimeType()).andStubReturn("unknown");

    control.replay();
    HttpResponse rewritten = registry.rewriteHttpResponse(req, fakeResponse.create(), null);
    // Assert that response is untouched
    assertSame(rewritten, fakeResponse.create());
    control.verify();
  }
}

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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import org.apache.commons.io.IOUtils;
import org.easymock.classextension.EasyMock;

/**
 *
 */
public class CSSContentRewriterTest extends BaseRewriterTestCase {
  private CSSContentRewriter rewriter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ContentRewriterFeature overrideFeature =
        rewriterFeatureFactory.get(createSpecWithRewrite(".*", ".*exclude.*", "HTTP",
            HTMLContentRewriter.TAGS));
    ContentRewriterFeatureFactory factory = mockContentRewriterFeatureFactory(overrideFeature);
    rewriter = new CSSContentRewriter(factory, DEFAULT_PROXY_BASE);
  }

  public void testCssBasic() throws Exception {
    String content = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic.css"));
    String expected = IOUtils.toString(this.getClass().getClassLoader().
        getResourceAsStream("org/apache/shindig/gadgets/rewrite/rewritebasic-expected.css"));
    HttpRequest request = new HttpRequest(Uri.parse("http://www.example.org/path/rewritebasic.css"));
    request.setMethod("GET");
    request.setGadget(SPEC_URL);

    HttpResponse response = new HttpResponseBuilder().setHeader("Content-Type", "text/css")
      .setResponseString(content).create();

    MutableContent mc = new MutableContent(null, content);
    rewriter.rewrite(request, response, mc);

    assertEquals(expected, mc.getContent());
  }

    public void testNoRewriteUnknownMimeType() {
    // Strict mock as we expect no calls
    MutableContent mc = mock(MutableContent.class, true);
    HttpRequest req = mock(HttpRequest.class);
    EasyMock.expect(req.getRewriteMimeType()).andReturn("unknown");
    replay();
    assertNull(rewriter.rewrite(req, fakeResponse, mc));
    verify();
  }
}

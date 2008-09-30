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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Lists;

import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class DefaultContentRewriterRegistryTest {
  private final List<CaptureRewriter> rewriters
      = Arrays.asList(new CaptureRewriter(), new CaptureRewriter());
  private final List<ContentRewriter> contentRewriters
      = Lists.<ContentRewriter>newArrayList(rewriters);
  private final ContentRewriterRegistry registry
      = new DefaultContentRewriterRegistry(contentRewriters, null);

  @Test
  public void rewriteGadget() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content>" + body + "</Content></Module>";
    GadgetSpec spec = new GadgetSpec(URI.create("#"), xml);
    GadgetContext context = new GadgetContext();
    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);

    String rewritten = registry.rewriteGadget(gadget, body);

    assertTrue("First rewriter not invoked.", rewriters.get(0).viewWasRewritten());
    assertTrue("Second rewriter not invoked.", rewriters.get(1).viewWasRewritten());

    assertEquals(body, rewritten);
  }

  @Test
  public void rewriteHttpResponse() throws Exception {
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(Uri.parse("#"));
    HttpResponse response = new HttpResponse(body);

    HttpResponse rewritten = registry.rewriteHttpResponse(request, response);

    assertTrue("First rewriter not invoked.", rewriters.get(0).responseWasRewritten());
    assertTrue("Second rewriter not invoked.", rewriters.get(1).responseWasRewritten());

    assertEquals(response, rewritten);
  }
}

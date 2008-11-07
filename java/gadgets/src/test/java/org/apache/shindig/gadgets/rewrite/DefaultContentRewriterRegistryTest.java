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

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.TestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.util.Arrays;
import java.util.List;

public class DefaultContentRewriterRegistryTest extends TestCase {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private List<CaptureRewriter> rewriters;
  private List<ContentRewriter> contentRewriters;
  private ContentRewriterRegistry registry;
  private GadgetHtmlParser parser;

  protected void setUp() throws Exception {
    Injector injector = Guice.createInjector(new ParseModule());
    parser = injector.getInstance(GadgetHtmlParser.class);
    rewriters = Arrays.asList(new CaptureRewriter(), new CaptureRewriter());
    contentRewriters = Lists.<ContentRewriter>newArrayList(rewriters);
    registry = new DefaultContentRewriterRegistry(contentRewriters, parser);
  }

  public void testRewriteGadget() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content>" + body + "</Content></Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);
    GadgetContext context = new GadgetContext();
    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);

    String rewritten = registry.rewriteGadget(gadget, body);

    assertTrue("First rewriter not invoked.", rewriters.get(0).viewWasRewritten());
    assertTrue("Second rewriter not invoked.", rewriters.get(1).viewWasRewritten());

    assertEquals(body, rewritten);
  }

  public void testRewriteHttpResponse() throws Exception {
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(SPEC_URL);
    HttpResponse response = new HttpResponse(body);

    HttpResponse rewritten = registry.rewriteHttpResponse(request, response);

    assertTrue("First rewriter not invoked.", rewriters.get(0).responseWasRewritten());
    assertTrue("Second rewriter not invoked.", rewriters.get(1).responseWasRewritten());

    assertEquals(response, rewritten);
  }

  public void testRewriteView() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content>" + body + "</Content></Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);
    GadgetContext context = new GadgetContext();
    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);

    String rewritten = registry.rewriteGadget(gadget, spec.getView("default"));

    assertTrue("First rewriter invoked.", rewriters.get(0).viewWasRewritten());
    assertTrue("Second rewriter invoked.", rewriters.get(1).viewWasRewritten());

    assertEquals(body, rewritten); 
  }
}

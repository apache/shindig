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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.junit.Before;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;

public class DomWalkerTestBase {
  protected static final Uri GADGET_URI = Uri.parse("http://example.com/gadget.xml");
  protected static final String CONTAINER = "container";

  protected Document doc;

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(Modules.override(new ParseModule())
        .with(new AbstractModule() {
          @Override
          protected void configure() {
            bind(Integer.class).annotatedWith(
                Names.named("shindig.cache.lru.default.capacity"))
                  .toInstance(0);
          }
        }));
    DOMImplementation domImpl = injector.getInstance(DOMImplementation.class);
    doc = domImpl.createDocument(null, null, null);
  }

  protected Element elem(String tag, String... attrStrs) {
    Element elem = doc.createElement(tag);
    for (int i = 0; attrStrs != null && i < attrStrs.length; i += 2) {
      Attr attr = doc.createAttribute(attrStrs[i]);
      attr.setValue(attrStrs[i+1]);
      elem.setAttributeNode(attr);
    }
    return elem;
  }

  protected Element htmlDoc(Node[] headNodes, Node... bodyNodes) {
    // Clear document of all nodes.
    while (doc.hasChildNodes()) {
      doc.removeChild(doc.getFirstChild());
    }

    // Recreate document with valid HTML structure.
    Element html = elem("html");
    Element head = elem("head");
    appendAll(head, headNodes);
    Element body = elem("body");
    appendAll(body, bodyNodes);
    html.appendChild(head);
    html.appendChild(body);
    doc.appendChild(html);

    return html;
  }

  private void appendAll(Node parent, Node[] children) {
    if (children == null || children.length == 0) return;

    for (Node child : children) {
      parent.appendChild(child);
    }
  }

  protected Gadget gadget() {
    return gadget(false, false);
  }

  protected Gadget gadget(boolean debug, boolean ignoreCache) {
    return gadget(debug, ignoreCache, null);
  }

  protected Gadget gadget(boolean debug, boolean ignoreCache, Uri curviewHref) {
    GadgetSpec spec = createMock(GadgetSpec.class);
    expect(spec.getUrl()).andReturn(GADGET_URI).anyTimes();
    Gadget gadget = createMock(Gadget.class);
    expect(gadget.getSpec()).andReturn(spec).anyTimes();
    GadgetContext ctx = createMock(GadgetContext.class);
    expect(ctx.getParameter(Param.REFRESH.getKey())).andReturn(null).anyTimes();
    expect(ctx.getDebug()).andReturn(debug).anyTimes();
    expect(ctx.getIgnoreCache()).andReturn(ignoreCache).anyTimes();
    expect(ctx.getContainer()).andReturn(CONTAINER).anyTimes();
    expect(gadget.getContext()).andReturn(ctx).anyTimes();
    View currentView = createMock(View.class);
    expect(currentView.getHref()).andReturn(curviewHref).anyTimes();
    expect(gadget.getCurrentView()).andReturn(currentView).anyTimes();
    replay(ctx, spec, currentView, gadget);
    return gadget;
  }
}

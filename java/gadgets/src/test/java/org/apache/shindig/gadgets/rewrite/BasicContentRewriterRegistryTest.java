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

import org.easymock.classextension.EasyMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import junit.framework.TestCase;

public class BasicContentRewriterRegistryTest extends TestCase {
  public void testNoArgsCreatedBasicRegistry() {
    BasicContentRewriterRegistry r = new BasicContentRewriterRegistry(null);
    assertNotNull(r.getRewriters());
    assertEquals(0, r.getRewriters().size());
  }
  
  public void testSingleValuedBasicRegistry() {
    BasicContentRewriterRegistry r = new BasicContentRewriterRegistry(
        new NoOpContentRewriter());
    assertNotNull(r.getRewriters());
    assertEquals(1, r.getRewriters().size());
    assertTrue(r.getRewriters().get(0) instanceof NoOpContentRewriter);
  }
  
  public void testBasicContentRegistryWithAdds() {
    ContentRewriter cr0 = new NoOpContentRewriter();
    BasicContentRewriterRegistry r = new BasicContentRewriterRegistry(cr0);
    ContentRewriter cr1 = new NoOpContentRewriter();
    ContentRewriter cr2 = new NoOpContentRewriter();
    r.appendRewriter(cr1);
    r.appendRewriter(cr2);
    assertNotNull(r.getRewriters());
    assertEquals(3, r.getRewriters().size());
    assertSame(cr0, r.getRewriters().get(0));
    assertSame(cr1, r.getRewriters().get(1));
    assertSame(cr2, r.getRewriters().get(2));
  }
  
  public void testRunGadgetRewrites() throws Exception {
    BasicContentRewriterRegistry r = new BasicContentRewriterRegistry(null);
    StringBuilder appendFull = new StringBuilder();
    for (int i = 0; i < 3; ++i) {
      String appendNew = "-" + i;
      appendFull.append(appendNew);
      r.appendRewriter(new AppendingRewriter(appendNew));
    }
    String inputContent = "foo";
    String rewrittenContent = inputContent + appendFull.toString();
    
    GadgetSpec spec = EasyMock.createNiceMock(GadgetSpec.class);
    View view = EasyMock.createNiceMock(View.class);
    expect(view.getName()).andReturn(GadgetSpec.DEFAULT_VIEW).anyTimes();
    expect(view.getType()).andReturn(View.ContentType.HTML).anyTimes();
    expect(view.getContent()).andReturn(inputContent).anyTimes();
    expect(spec.getView(GadgetSpec.DEFAULT_VIEW)).andReturn(view).anyTimes();
    GadgetContext context = EasyMock.createNiceMock(GadgetContext.class);
    expect(context.getView()).andReturn(GadgetSpec.DEFAULT_VIEW).anyTimes();
    replay(context, view, spec);
    
    Gadget gadget = new Gadget(context, spec, null, null);
    assertEquals(inputContent, gadget.getContent());
    assertTrue(r.rewriteGadget(context, gadget));
    assertEquals(rewrittenContent, gadget.getContent());
  }
}

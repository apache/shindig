/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;

import org.apache.shindig.common.cache.DefaultCacheProvider;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;
import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

import java.net.URI;

public class CachingContentRewriterRegistryTest extends TestCase {
  public void testCachedRewriteIsReturned() throws Exception {
    // Sort of a weird test, but gets the basic idea of caching across.
    // Perform a rewrite, then update the rewriter in such a way that
    // would yield a different rewritten result, but ensure that
    // the result of the rewriter is the old version, which hasn't
    // yet expired. To ensure no expiry, set expiration date
    // to the largest possible date.
    CachingContentRewriterRegistry r = new CachingContentRewriterRegistry(null,
        new DefaultCacheProvider(), 100, 0, Integer.MAX_VALUE);
    StringBuilder appendFull = new StringBuilder();
    for (int i = 0; i < 3; ++i) {
      String appendNew = "-" + i;
      appendFull.append(appendNew);
      r.appendRewriter(new AppendRewriter(appendNew));
    }
    String inputContent = "foo";
    String rewrittenContent = inputContent + appendFull.toString();
    
    GadgetSpec spec = EasyMock.createNiceMock(GadgetSpec.class);
    View view = EasyMock.createNiceMock(View.class);
    expect(view.getName()).andReturn(GadgetSpec.DEFAULT_VIEW).anyTimes();
    expect(view.getType()).andReturn(View.ContentType.HTML).anyTimes();
    expect(view.getContent()).andReturn(inputContent).anyTimes();
    expect(spec.getView(GadgetSpec.DEFAULT_VIEW)).andReturn(view).anyTimes();
    expect(spec.getAttribute(GadgetSpec.EXPIRATION_ATTRIB)).andReturn(Long.MAX_VALUE).anyTimes();
    expect(spec.getUrl()).andReturn(new URI("http://gadget.org/gadget.xml")).anyTimes();
    GadgetContext context = EasyMock.createNiceMock(GadgetContext.class);
    expect(context.getView()).andReturn(GadgetSpec.DEFAULT_VIEW).anyTimes();
    replay(context, view, spec);
    
    Gadget gadget = new Gadget(context, spec, null, null);
    assertEquals(inputContent, gadget.getContent());
    
    // Should be rewritten the first time.
    assertTrue(r.rewriteGadget(context, gadget));
    assertEquals(rewrittenContent, gadget.getContent());
    
    r.appendRewriter(new AppendRewriter("-end"));
    
    // Should also be rewritten the second time, but with the previous
    // expected rewritten content value.
    Gadget nextGadget = new Gadget(context, spec, null, null);
    assertTrue(r.rewriteGadget(context, nextGadget));
    assertEquals(rewrittenContent, nextGadget.getContent());
  }
}

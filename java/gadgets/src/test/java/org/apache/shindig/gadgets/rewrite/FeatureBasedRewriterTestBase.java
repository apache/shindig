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
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParsedHtmlNode;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.View;

import junit.framework.TestCase;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeatureBasedRewriterTestBase extends TestCase {
  protected URI baseUri;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    baseUri = new URI("http://gadget.org/dir/gadget.xml");
  }
  
  protected ContentRewriterFeature.Factory mockContentRewriterFeatureFactory(
      ContentRewriterFeature feature) {
    return new MockRewriterFeatureFactory(feature);
  }
  
  protected ContentRewriterFeature makeFeature(String... includedTags) {
    ContentRewriterFeature rewriterFeature =
        EasyMock.createNiceMock(ContentRewriterFeature.class);
    Set<String> tags = new HashSet<String>();
    for (String tag : includedTags) {
      tags.add(tag);
    }
    expect(rewriterFeature.isRewriteEnabled()).andReturn(true).anyTimes();
    expect(rewriterFeature.getIncludedTags()).andReturn(tags).anyTimes();
    expect(rewriterFeature.getFingerprint()).andReturn(-840722081).anyTimes();
    replay(rewriterFeature);
    return rewriterFeature;
  }
  
  protected String rewriteHelper(ContentRewriter rewriter, String s,
      ParsedHtmlNode[] p) throws Exception {
    GadgetHtmlParser parser = EasyMock.createNiceMock(GadgetHtmlParser.class);
    List<ParsedHtmlNode> expected = p != null ? Arrays.asList(p) : null;
    expect(parser.parse(s)).andReturn(expected).anyTimes();
    View view = EasyMock.createNiceMock(View.class);
    expect(view.getContent()).andReturn(s).anyTimes();
    expect(view.getName()).andReturn(GadgetSpec.DEFAULT_VIEW).anyTimes();
    GadgetSpec spec = EasyMock.createNiceMock(GadgetSpec.class);
    expect(spec.getUrl()).andReturn(baseUri).anyTimes();
    expect(spec.getView(GadgetSpec.DEFAULT_VIEW)).andReturn(view).anyTimes();
    replay(parser, view, spec);
    Gadget gadget = new Gadget(new GadgetContext(), spec, null, null, parser);
    rewriter.rewrite(gadget);
    return gadget.getContent();
  }
  
  private static class MockRewriterFeatureFactory extends ContentRewriterFeature.Factory {
    private final ContentRewriterFeature feature;
    
    public MockRewriterFeatureFactory(ContentRewriterFeature feature) {
      super(".*", "", "HTTP", null);
      this.feature = feature;
    }
    
    @Override
    public ContentRewriterFeature get(GadgetSpec spec) {
      return feature;
    }
  }
}

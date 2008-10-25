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

import junit.framework.TestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import static org.easymock.EasyMock.expect;
import org.easymock.classextension.EasyMock;
import static org.easymock.classextension.EasyMock.replay;
import org.w3c.dom.Document;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public abstract class FeatureBasedRewriterTestBase extends TestCase {
  URI baseUri;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    baseUri = new URI("http://gadget.org/dir/gadget.xml");
  }

  ContentRewriterFeature.Factory mockContentRewriterFeatureFactory(
      ContentRewriterFeature feature) {
    return new MockRewriterFeatureFactory(feature);
  }

  ContentRewriterFeature makeFeature(String... includedTags) {
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

  String rewriteHelper(ContentRewriter rewriter, String s, Document doc)
      throws Exception {
    GadgetHtmlParser parser = EasyMock.createNiceMock(GadgetHtmlParser.class);
    expect(parser.parseDom(s)).andReturn(doc).anyTimes();

    replay(parser);

    MutableContent mc = new MutableContent(parser);
    mc.setContent(s);

    GadgetSpec spec = new GadgetSpec(Uri.fromJavaUri(baseUri),
        "<Module><ModulePrefs title=''/><Content><![CDATA[" + s + "]]></Content></Module>");

    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return baseUri;
      }
    };

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);
    rewriter.rewrite(gadget, mc);

    String rewrittenContent = mc.getContent();

    // Strip around the HTML tags for convenience
    int htmlTagIndex = rewrittenContent.indexOf("<HTML>");
    if (htmlTagIndex != -1) {
      return rewrittenContent.substring(htmlTagIndex + 6,
          rewrittenContent.lastIndexOf("</HTML>"));
    }
    return rewrittenContent;
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

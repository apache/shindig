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

import com.google.common.base.Joiner;
import com.google.inject.util.Providers;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;

import java.util.Set;

/**
 * Base class for testing content rewriting functionality
 */
public abstract class BaseRewriterTestCase {
  public static final Uri SPEC_URL = Uri.parse("http://www.example.org/dir/g.xml");
  public static final String DEFAULT_PROXY_BASE = "http://www.test.com/dir/proxy?url=";
  public static final String DEFAULT_CONCAT_BASE = "http://www.test.com/dir/concat?";

  public static final String MOCK_CONTAINER = "mock";
  public static final String MOCK_PROXY_BASE =
    replaceDefaultWithMockServer(DEFAULT_PROXY_BASE);
  public static final String MOCK_CONCAT_BASE =
    replaceDefaultWithMockServer(DEFAULT_CONCAT_BASE);
  protected final String TAGS = "embed,img,script,link,style";

  protected Set<String> tags;
  protected ContentRewriterFeature.Config defaultRewriterFeature;
  protected ContentRewriterFeature.Factory rewriterFeatureFactory;
  protected GadgetHtmlParser parser;
  protected Injector injector;
  protected HttpResponseBuilder fakeResponse;
  protected IMocksControl control;

  @Before
  public void setUp() throws Exception {
    rewriterFeatureFactory = new ContentRewriterFeature.Factory(null,
        Providers.of(new ContentRewriterFeature.DefaultConfig(".*", "", "86400", "embed,img,script,link,style", false, false, false)));
    defaultRewriterFeature = rewriterFeatureFactory.getDefault();
    tags = defaultRewriterFeature.getIncludedTags();
    injector = Guice.createInjector(getParseModule(), new PropertiesModule(), new TestModule());
    parser = injector.getInstance(GadgetHtmlParser.class);
    fakeResponse = new HttpResponseBuilder().setHeader("Content-Type", "unknown")
        .setResponse(new byte[]{ (byte)0xFE, (byte)0xFF});
    control = EasyMock.createControl();
  }

  private Module getParseModule() {
    return Modules.override(new ParseModule()).with(new AbstractModule() {
      @Override
      protected void configure() {
        bind(GadgetHtmlParser.class).to(getParserClass());
      }
    });
  }

  protected Class<? extends GadgetHtmlParser> getParserClass() {
    return NekoSimplifiedHtmlParser.class;
  }

  public static GadgetSpec createSpecWithRewrite(String include, String exclude, String expires,
      Set<String> tags) throws GadgetException {
    StringBuilder xml = new StringBuilder();
    xml.append("<Module>");
    xml.append("<ModulePrefs title=\"title\">");
    xml.append("<Optional feature=\"content-rewrite\">\n");
    if(expires != null)
      xml.append("      <Param name=\"expires\">" + expires + "</Param>\n");
    if(include != null)
      xml.append("      <Param name=\"include-urls\">" + include + "</Param>\n");
    if(exclude != null)
      xml.append("      <Param name=\"exclude-urls\">" + exclude + "</Param>\n");
    if(tags != null)
      xml.append("      <Param name=\"include-tags\">" + Joiner.on(',').join(tags) + "</Param>\n");
    xml.append("</Optional>");
    xml.append("</ModulePrefs>");
    xml.append("<Content type=\"html\">Hello!</Content>");
    xml.append("</Module>");
    return new GadgetSpec(SPEC_URL, xml.toString());
  }

  public static GadgetSpec createSpecWithRewriteOS9(String[] includes, String[] excludes, String expires,
      Set<String> tags) throws GadgetException {
    StringBuilder xml = new StringBuilder();
    xml.append("<Module>");
    xml.append("<ModulePrefs title=\"title\">");
    xml.append("<Optional feature=\"content-rewrite\">\n");
    if(expires != null)
      xml.append("      <Param name=\"expires\">" + expires + "</Param>\n");
    if(includes != null)
      for (String include : includes) {
        xml.append("      <Param name=\"include-url\">" + include + "</Param>\n");
      }
    if(excludes != null)
      for (String exclude : excludes) {
        xml.append("      <Param name=\"exclude-url\">" + exclude + "</Param>\n");
      }
    if(tags != null)
      xml.append("      <Param name=\"include-tags\">" + Joiner.on(',').join(tags) + "</Param>\n");
    xml.append("</Optional>");
    xml.append("</ModulePrefs>");
    xml.append("<Content type=\"html\">Hello!</Content>");
    xml.append("</Module>");
    return new GadgetSpec(SPEC_URL, xml.toString());
  }

  public static GadgetSpec createSpecWithoutRewrite() throws GadgetException {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"title\">" +
                 "</ModulePrefs>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    return new GadgetSpec(SPEC_URL, xml);
  }

  public static String replaceDefaultWithMockServer(String originalText) {
    return originalText.replace("test.com", "mock.com");
  }

  protected ContentRewriterFeature.Factory mockContentRewriterFeatureFactory(
      ContentRewriterFeature.Config feature) {
    return new FakeRewriterFeatureFactory(feature);
  }

  String rewriteHelper(GadgetRewriter rewriter, String s)
      throws Exception {
    MutableContent mc = rewriteContent(rewriter, s, null);
    String rewrittenContent = mc.getContent();

    // Strip around the HTML tags for convenience
    int htmlTagIndex = rewrittenContent.indexOf("<HTML>");
    if (htmlTagIndex != -1) {
      return rewrittenContent.substring(htmlTagIndex + 6,
          rewrittenContent.lastIndexOf("</HTML>"));
    }
    return rewrittenContent;
  }

  protected MutableContent rewriteContent(GadgetRewriter rewriter, String s,
      final String container) throws Exception {
    return rewriteContent(rewriter, s, container, false, false);
  }

  protected MutableContent rewriteContent(GadgetRewriter rewriter, String s,
      final String container, final boolean debug, final boolean ignoreCache)
      throws Exception {
    MutableContent mc = new MutableContent(parser, s);

    GadgetSpec spec = new GadgetSpec(SPEC_URL,
        "<Module><ModulePrefs title=''/><Content><![CDATA[" + s + "]]></Content></Module>");

    GadgetContext context = new GadgetContext() {
      @Override
      public Uri getUrl() {
        return SPEC_URL;
      }

      @Override
      public String getContainer() {
        return container;
      }

      @Override
      public boolean getDebug() {
        return debug;
      }

      @Override
      public boolean getIgnoreCache() {
        return ignoreCache;
      }
    };

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);
    rewriter.rewrite(gadget, mc);
    return mc;
  }

  private static class FakeRewriterFeatureFactory extends ContentRewriterFeature.Factory {
    private final ContentRewriterFeature.Config feature;

    public FakeRewriterFeatureFactory(ContentRewriterFeature.Config feature) {
      super(null, Providers.of(new ContentRewriterFeature.DefaultConfig(".*", "", "HTTP", "", false, false, false)));
      this.feature = feature;
    }

    @Override
    public ContentRewriterFeature.Config get(GadgetSpec spec) {
      return feature;
    }

    @Override
    public ContentRewriterFeature.Config get(HttpRequest request) {
      return feature;
    }
  }

  private static class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(RequestPipeline.class).toInstance(new RequestPipeline() {
        public HttpResponse execute(HttpRequest request) { return null; }
      });

      bind(GadgetSpecFactory.class).toInstance(new GadgetSpecFactory() {
        public GadgetSpec getGadgetSpec(GadgetContext context) {
          return null;
        }
        public Uri getGadgetUri(GadgetContext context) {
          return null;
        }
      });
    }
  }
}

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

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.AbstractContainerConfig;
import org.apache.shindig.config.ContainerConfig;
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
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.util.Set;

/**
 * Base class for testing content rewriting functionality
 */
public abstract class BaseRewriterTestCase extends EasyMockTestCase {
  public static final Uri SPEC_URL = Uri.parse("http://www.example.org/dir/g.xml");
  public static final String DEFAULT_PROXY_BASE = "http://www.test.com/dir/proxy?url=";
  public static final String DEFAULT_CONCAT_BASE = "http://www.test.com/dir/concat?";

  public static final String MOCK_CONTAINER = "mock";
  public static final String MOCK_PROXY_BASE = 
    replaceDefaultWithMockServer(DEFAULT_PROXY_BASE);
  public static final String MOCK_CONCAT_BASE = 
    replaceDefaultWithMockServer(DEFAULT_CONCAT_BASE);

  protected Set<String> tags;
  protected ContentRewriterFeature defaultRewriterFeature;
  protected ContentRewriterFeatureFactory rewriterFeatureFactory;
  protected LinkRewriter defaultLinkRewriter;
  protected GadgetHtmlParser parser;
  protected Injector injector;
  protected HttpResponse fakeResponse;
  protected ContainerConfig config;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rewriterFeatureFactory = new ContentRewriterFeatureFactory(null, ".*", "", "HTTP",
        "embed,img,script,link,style");
    defaultRewriterFeature = rewriterFeatureFactory.getDefault();
    tags = defaultRewriterFeature.getIncludedTags();
    defaultLinkRewriter = new ProxyingLinkRewriter(
        SPEC_URL,
        defaultRewriterFeature,
        DEFAULT_PROXY_BASE);
    injector = Guice.createInjector(new ParseModule(), new PropertiesModule(), new TestModule());
    parser = injector.getInstance(GadgetHtmlParser.class);
    fakeResponse = new HttpResponseBuilder().setHeader("Content-Type", "unknown")
        .setResponse(new byte[]{ (byte)0xFE, (byte)0xFF}).create();

    config = new AbstractContainerConfig() {
      @Override
      public Object getProperty(String container, String name) {
        if (MOCK_CONTAINER.equals(container)) {
          if (ContentRewriterUris.PROXY_BASE_CONFIG_PROPERTY.equals(name)) {
            return MOCK_PROXY_BASE;
          } else if (ContentRewriterUris.CONCAT_BASE_CONFIG_PROPERTY.equals(name)) {
            return MOCK_CONCAT_BASE;
          }
        }
        
        return null;
      }
    };
    
}

  public static GadgetSpec createSpecWithRewrite(String include, String exclude, String expires,
      Set<String> tags) throws GadgetException {
    String xml = "<Module>" +
                 "<ModulePrefs title=\"title\">" +
                 "<Optional feature=\"content-rewrite\">\n" +
                 "      <Param name=\"expires\">" + expires + "</Param>\n" +
                 "      <Param name=\"include-urls\">" + include + "</Param>\n" +
                 "      <Param name=\"exclude-urls\">" + exclude + "</Param>\n" +
                 "      <Param name=\"include-tags\">" + StringUtils.join(tags, ",") + "</Param>\n" +
                 "</Optional>" +
                 "</ModulePrefs>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>";
    return new GadgetSpec(SPEC_URL, xml);
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
  
  ContentRewriterFeatureFactory mockContentRewriterFeatureFactory(
      ContentRewriterFeature feature) {
    return new FakeRewriterFeatureFactory(feature);
  }

  String rewriteHelper(ContentRewriter rewriter, String s)
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

  MutableContent rewriteContent(ContentRewriter rewriter, String s, final String container)
      throws Exception {
    MutableContent mc = new MutableContent(parser, s);

    GadgetSpec spec = new GadgetSpec(SPEC_URL,
        "<Module><ModulePrefs title=''/><Content><![CDATA[" + s + "]]></Content></Module>");

    GadgetContext context = new GadgetContext() {
      @Override
      public URI getUrl() {
        return SPEC_URL.toJavaUri();
      }
      
      @Override
      public String getContainer() {
        return container;
      }
    };

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);
    rewriter.rewrite(gadget, mc);
    return mc;
  }

  private static class FakeRewriterFeatureFactory extends ContentRewriterFeatureFactory {
    private final ContentRewriterFeature feature;

    public FakeRewriterFeatureFactory(ContentRewriterFeature feature) {
      super(null, ".*", "", "HTTP", "");
      this.feature = feature;
    }

    @Override
    public ContentRewriterFeature get(GadgetSpec spec) {
      return feature;
    }

    @Override
    public ContentRewriterFeature get(HttpRequest request) {
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
      });
    }
  }
}

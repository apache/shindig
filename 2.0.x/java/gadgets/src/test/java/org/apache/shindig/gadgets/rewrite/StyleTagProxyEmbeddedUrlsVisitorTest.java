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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.AbstractContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlSerializer;
import org.apache.shindig.gadgets.uri.DefaultProxyUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests for StyleTagProxyEmbeddedUrlsVisitor.
 */
public class StyleTagProxyEmbeddedUrlsVisitorTest extends DomWalkerTestBase {
  protected static final String MOCK_CONTAINER = "mock";

  private static class FakeContainerConfig extends AbstractContainerConfig {
    private Map<String, Map<String, Object>> containers = new HashMap<String, Map<String, Object>>();

    private FakeContainerConfig(ContainerConfig defaultContainerConfig) {
      containers.put(ContainerConfig.DEFAULT_CONTAINER,
          defaultContainerConfig.getProperties(ContainerConfig.DEFAULT_CONTAINER));

      containers.put(MOCK_CONTAINER, ImmutableMap.<String, Object>builder()
          .put(DefaultProxyUriManager.PROXY_HOST_PARAM, "www.mock.com")
          .build());
    }

    @Override
    public Object getProperty(String container, String name) {
      Map<String, Object> data = containers.get(container);

      //if there is no value by this key inherit from default
      if (!data.containsKey(name)) {
        data = containers.get(ContainerConfig.DEFAULT_CONTAINER);
      }

      return data.get(name);
    }
  }

  private Injector injector;
  private CajaHtmlParser htmlParser;
  private CajaHtmlSerializer serializer;
  private ProxyUriManager proxyUriManager;

  @Before
  public void setUp() {
    super.setUp();
    injector = Guice.createInjector(
        new PropertiesModule(), new DefaultGuiceModule(), new OAuthModule());
    ParseModule.DOMImplementationProvider domImpl =
        new ParseModule.DOMImplementationProvider();
    htmlParser = new CajaHtmlParser(domImpl.get());
    serializer = new CajaHtmlSerializer();
    ContainerConfig config = new FakeContainerConfig(injector.getInstance(ContainerConfig.class));
    proxyUriManager = new DefaultProxyUriManager(config, null);
  }

  private static final String ORIGINAL = "<html><head>"
      + "<style>"
      + "@import url(/1.css);"
      + "P {color:blue;}"
      + "P {color:red;}"
      + "A {background: url(/2.jpg);}"
      + "</style>"
      + "</head><body><a href=\"hello\">Hello</a>"
      + "</body></html>";

  private static final String EXPECTED = "<html><head>"
      + "<style>"
      + "@import url('//localhost:8080/gadgets/proxy?container=default&"
      + "gadget=http%3A%2F%2F1.com%2F&debug=0&nocache=0"
      + "&url=http%3A%2F%2F1.com%2F1.css');\n"
      + "P {color:blue;}"
      + "P {color:red;}"
      + "A {background: url('//localhost:8080/gadgets/proxy?container=default"
      + "&gadget=http%3A%2F%2F1.com%2F&debug=0&nocache=0"
      + "&url=http%3A%2F%2F1.com%2F2.jpg');}"
      + "</style></head>"
      + "<body><a href=\"hello\">Hello</a>\n"
      + "</body></html>";

  @Test
  public void testImportsAndBackgroundUrlsInStyleTagDefaultContainer() throws Exception {
    testImportsAndBackgroundUrlsInStyleTag(ORIGINAL, EXPECTED, ContainerConfig.DEFAULT_CONTAINER);
  }

  @Test
  public void testImportsAndBackgroundUrlsInStyleTagMockContainer() throws Exception {
    testImportsAndBackgroundUrlsInStyleTag(ORIGINAL, EXPECTED.replace(
        "localhost:8080/gadgets/proxy?container=default", "www.mock.com/gadgets/proxy?container=mock"), 
        MOCK_CONTAINER);
  }
  
  private void testImportsAndBackgroundUrlsInStyleTag(String html, String expected, String container) 
      throws Exception {
    Document doc = htmlParser.parseDom(html);

    ContentRewriterFeature.Config config = injector.getInstance(
        ContentRewriterFeature.DefaultConfig.class);
    EasyMock.replay();

    StyleTagProxyEmbeddedUrlsVisitor visitor = new StyleTagProxyEmbeddedUrlsVisitor(
        config, proxyUriManager,
        injector.getInstance(CssResponseRewriter.class));

    Gadget gadget = DomWalker.makeGadget(new HttpRequest(Uri.parse("http://1.com/")).setContainer(
        container));

    NodeList list = doc.getElementsByTagName("style");
    visitor.revisit(gadget, ImmutableList.of(list.item(0)));
    EasyMock.verify();

    assertEquals(StringUtils.deleteWhitespace(expected),
        StringUtils.deleteWhitespace(serializer.serialize(doc)));
  }
}
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

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.DefaultGuiceModule;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.admin.GadgetAdminModule;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.oauth2.OAuth2MessageModule;
import org.apache.shindig.gadgets.oauth2.OAuth2Module;
import org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerModule;
import org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2PersistenceModule;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlSerializer;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature.Config;
import org.apache.shindig.gadgets.uri.DefaultProxyUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests for StyleTagProxyEmbeddedUrlsVisitor.
 */
public class StyleTagProxyEmbeddedUrlsVisitorTest extends DomWalkerTestBase {
  protected static final String MOCK_CONTAINER = "mock";
  private static final ImmutableMap<String, Object> MOCK_CONTAINER_CONFIG = ImmutableMap
      .<String, Object>builder()
      .put(ContainerConfig.CONTAINER_KEY, ImmutableList.of("mock"))
      .put(DefaultProxyUriManager.PROXY_HOST_PARAM, "www.mock.com")
      .build();

  private Injector injector;
  private CajaHtmlParser htmlParser;
  private CajaHtmlSerializer serializer;
  private ProxyUriManager proxyUriManager;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(
        new PropertiesModule(), new GadgetAdminModule(), new DefaultGuiceModule(),
        new OAuthModule(), new OAuth2Module(), new OAuth2PersistenceModule(), new OAuth2MessageModule(), new OAuth2HandlerModule());
    ParseModule.DOMImplementationProvider domImpl =
        new ParseModule.DOMImplementationProvider();
    htmlParser = new CajaHtmlParser(domImpl.get());
    serializer = new CajaHtmlSerializer();
    ContainerConfig config = injector.getInstance(ContainerConfig.class);
    config.newTransaction().addContainer(MOCK_CONTAINER_CONFIG).commit();
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

  private static final String NOREWRITE = "<html><head>"
      + "<style>"
      + "@import url('http://1.com/1.css');"
      + "P {color:blue;}"
      + "P {color:red;}"
      + "A {background: url('http://1.com/2.jpg');}"
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
    // TODO: IMPORTANT!  This test needs to not rely on the packaged shindig config, but rather
    //       mock the config with expected values, so that tests do not fail when people set
    //       alternative defaults.
    Config config = injector.getInstance(ContentRewriterFeature.DefaultConfig.class);
    EasyMock.replay();
    if (config.isRewriteEnabled())
      testImportsAndBackgroundUrlsInStyleTag(ORIGINAL, EXPECTED, ContainerConfig.DEFAULT_CONTAINER, config);
    else
      testImportsAndBackgroundUrlsInStyleTag(ORIGINAL, NOREWRITE, ContainerConfig.DEFAULT_CONTAINER, config);
  }

  @Test
  public void testImportsAndBackgroundUrlsInStyleTagMockContainer() throws Exception {
    // TODO: IMPORTANT!  This test needs to not rely on the packaged shindig config, but rather
    //       mock the config with expected values, so that tests do not fail when people set
    //       alternative defaults.
    Config config = injector.getInstance(ContentRewriterFeature.DefaultConfig.class);
    EasyMock.replay();

    if (config.isRewriteEnabled()) {
      testImportsAndBackgroundUrlsInStyleTag(ORIGINAL, EXPECTED.replace(
          "localhost:8080/gadgets/proxy?container=default", "www.mock.com/gadgets/proxy?container=mock"),
          MOCK_CONTAINER, config);
    } else {
      testImportsAndBackgroundUrlsInStyleTag(ORIGINAL, NOREWRITE, ContainerConfig.DEFAULT_CONTAINER, config);
    }

  }

  private void testImportsAndBackgroundUrlsInStyleTag(String html, String expected, String container, Config config)
      throws Exception {
    // TODO: IMPORTANT!  This test needs to not rely on the packaged shindig config, but rather
    //       mock the config with expected values, so that tests do not fail when people set
    //       alternative defaults.
    Document doc = htmlParser.parseDom(html);

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

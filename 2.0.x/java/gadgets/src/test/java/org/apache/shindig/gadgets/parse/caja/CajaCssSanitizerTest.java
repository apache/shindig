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
package org.apache.shindig.gadgets.parse.caja;

import com.google.caja.parser.css.CssTree;
import com.google.common.collect.ImmutableMap;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.AbstractContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.render.SanitizingProxyUriManager;
import org.apache.shindig.gadgets.uri.DefaultProxyUriManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for CajaCssSanitizer.
 */
public class CajaCssSanitizerTest extends EasyMockTestCase {
  private CajaCssParser parser;
  private CajaCssSanitizer sanitizer;
  private final Uri DUMMY = Uri.parse("http://www.example.org/base");
  private SanitizingProxyUriManager importRewriter;
  private SanitizingProxyUriManager imageRewriter;
  private GadgetContext gadgetContext;
  public static final String MOCK_CONTAINER = "mockContainer";

  private static class FakeContainerConfig extends AbstractContainerConfig {
    private Map<String, Map<String, Object>> containers =
        new HashMap<String, Map<String, Object>>();

    private FakeContainerConfig() {
      containers.put(ContainerConfig.DEFAULT_CONTAINER, ImmutableMap.<String, Object>builder()
          .put(DefaultProxyUriManager.PROXY_HOST_PARAM, "www.test.com")
          .put(DefaultProxyUriManager.PROXY_PATH_PARAM, "/dir/proxy")
          .build());
      containers.put(MOCK_CONTAINER, ImmutableMap.<String, Object>builder()
          .put(DefaultProxyUriManager.PROXY_HOST_PARAM, "www.mock.com")
          .build());
    }

    @Override
    public Object getProperty(String container, String name) {
      Map<String, Object> data = containers.get(container);

      // Inherit from default if there is no value for this key. 
      if (!data.containsKey(name)) {
        data = containers.get(ContainerConfig.DEFAULT_CONTAINER);
      }
      return data.get(name);
    }
  }

  @Before
  public void setUp() throws Exception {
    parser = new CajaCssParser();
    sanitizer = new CajaCssSanitizer(parser);

    ContainerConfig config = new FakeContainerConfig();
    ProxyUriManager proxyUriManager = new DefaultProxyUriManager(config, null);

    importRewriter = new SanitizingProxyUriManager(proxyUriManager, "text/css");
    imageRewriter = new SanitizingProxyUriManager(proxyUriManager, "image/*");
    gadgetContext = new GadgetContext() {
      @Override
      public String getContainer() {
        return MOCK_CONTAINER;
      }
    };
  }

  @Test
  public void testPreserveSafe() throws Exception {
    String css = ".xyz { font: bold;} A { color: #7f7f7f}";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, gadgetContext, importRewriter, imageRewriter);
    assertStyleEquals(css, styleSheet);
  }

  @Test
  public void testSanitizeFunctionCall() throws Exception {
    String css = ".xyz { font : iamevil(bold); }";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, gadgetContext, importRewriter, imageRewriter);
    assertStyleEquals(".xyz {}", styleSheet);
  }

  @Test
   public void testSanitizeUnsafeProperties() throws Exception {
    String css = ".xyz { behavior: url('xyz.htc'); -moz-binding:url(\"http://ha.ckers.org/xssmoz.xml#xss\") }";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, gadgetContext, importRewriter, imageRewriter);
    assertStyleEquals(".xyz {}", styleSheet);
  }

  @Test
  public void testSanitizeScriptUrls() throws Exception {
    String css = ".xyz { background: url('javascript:doevill'); background : url(vbscript:moreevil); }";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, gadgetContext, importRewriter, imageRewriter);
    assertStyleEquals(".xyz {}", styleSheet);
  }

  @Test
  public void testProxyUrls() throws Exception {
    String css = ".xyz { background: url('http://www.example.org/img.gif');}";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, gadgetContext, importRewriter, imageRewriter);
    assertStyleEquals(".xyz { " +
        "background: url('//www.mock.com/dir/proxy?container=mockContainer&gadget=http%3A%2F%2Fwww.example.org%2Fbase" +
        "&debug=0&nocache=0&url=http%3A%2F%2Fwww.example.org%2Fimg.gif&" +
        "sanitize=1&rewriteMime=image%2F%2a');}", styleSheet);
  }

  @Test
  public void testUrlEscapingMockContainer() throws Exception {
    String css = ".xyz { background: url('http://www.example.org/img.gif');}";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, gadgetContext, importRewriter, imageRewriter);
    assertEquals(".xyz{" +
        "background:url('//www.mock.com/dir/proxy?container=mockContainer&gadget=http%3A%2F%2Fwww.example.org%2Fbase" +
        "&debug=0&nocache=0&url=http%3A%2F%2Fwww.example.org%2Fimg.gif" +
        "&sanitize=1&rewriteMime=image%2F%2a');}",
        parser.serialize(styleSheet).replaceAll("\\s", ""));
  }

  @Test
  public void testUrlEscapingDefaultContainer() throws Exception {
    String css = ".xyz { background: url('http://www.example.org/img.gif');}";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    GadgetContext gadgetContext = new GadgetContext() {
      @Override
      public String getContainer() {
        return ContainerConfig.DEFAULT_CONTAINER;
      }
    };

    sanitizer.sanitize(styleSheet, DUMMY, gadgetContext, importRewriter, imageRewriter);
    assertEquals(".xyz{" +
        "background:url('//www.test.com/dir/proxy?container=default&gadget=http%3A%2F%2Fwww.example.org%2Fbase" +
        "&debug=0&nocache=0&url=http%3A%2F%2Fwww.example.org%2Fimg.gif" +
        "&sanitize=1&rewriteMime=image%2F%2a');}",
        parser.serialize(styleSheet).replaceAll("\\s", ""));
  }

  public void assertStyleEquals(String expected, CssTree.StyleSheet styleSheet) throws Exception {
    assertEquals(parser.serialize(parser.parseDom(expected)), parser.serialize(styleSheet));
  }
}

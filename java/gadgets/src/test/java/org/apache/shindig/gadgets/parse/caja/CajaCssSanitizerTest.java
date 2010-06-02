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

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.render.SanitizingProxyUriManager;
import org.apache.shindig.gadgets.uri.PassthruManager;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import com.google.caja.parser.css.CssTree;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class CajaCssSanitizerTest extends EasyMockTestCase {

  private CajaCssParser parser;
  private CajaCssSanitizer sanitizer;
  private final Uri DUMMY = Uri.parse("http://www.example.org/base");
  private ProxyUriManager passthruManager;
  private SanitizingProxyUriManager importRewriter;
  private SanitizingProxyUriManager imageRewriter;

  @Before
  public void setUp() throws Exception {
    parser = new CajaCssParser();
    sanitizer = new CajaCssSanitizer(parser);
    passthruManager = new PassthruManager("test.com", "/proxy/path");
    importRewriter = new SanitizingProxyUriManager(passthruManager, "text/css");
    imageRewriter = new SanitizingProxyUriManager(passthruManager, "image/*");
  }

  @Test
  public void testPreserveSafe() throws Exception {
    String css = ".xyz { font: bold;} A { color: #7f7f7f}";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, importRewriter, imageRewriter);
    assertStyleEquals(css, styleSheet);
  }

  @Test
  public void testSanitizeFunctionCall() throws Exception {
    String css = ".xyz { font : iamevil(bold); }";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, importRewriter, imageRewriter);
    assertStyleEquals(".xyz {}", styleSheet);
  }

  @Test
   public void testSanitizeUnsafeProperties() throws Exception {
    String css = ".xyz { behavior: url('xyz.htc'); -moz-binding:url(\"http://ha.ckers.org/xssmoz.xml#xss\") }";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, importRewriter, imageRewriter);
    assertStyleEquals(".xyz {}", styleSheet);
  }

  @Test
  public void testSanitizeScriptUrls() throws Exception {
    String css = ".xyz { background: url('javascript:doevill'); background : url(vbscript:moreevil); }";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, importRewriter, imageRewriter);
    assertStyleEquals(".xyz {}", styleSheet);
  }

  @Test
  public void testProxyUrls() throws Exception {
    String css = ".xyz { background: url('http://www.example.org/img.gif');}";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, importRewriter, imageRewriter);
    assertStyleEquals(
        ".xyz { background: url('http://test.com/proxy/path?url=" +
        "http%3A%2F%2Fwww.example.org%2Fimg.gif" +
        "&sanitize=1&rewriteMime=image%2F%2a');}", styleSheet);
  }

  @Test
  public void testUrlEscaping() throws Exception {
    String css = ".xyz { background: url('http://www.example.org/img.gif');}";
    CssTree.StyleSheet styleSheet = parser.parseDom(css);
    sanitizer.sanitize(styleSheet, DUMMY, importRewriter, imageRewriter);
    assertEquals(".xyz{background:url('http://test.com/proxy/path?url=http%3A%2F%2Fwww.example.org%2F" +
        "img.gif&sanitize=1&rewriteMime=image%2F%2a');}",
        parser.serialize(styleSheet).replaceAll("\\s", ""));
  }

  public void assertStyleEquals(String expected, CssTree.StyleSheet styleSheet) throws Exception {
    assertEquals(parser.serialize(parser.parseDom(expected)), parser.serialize(styleSheet));
  }
}

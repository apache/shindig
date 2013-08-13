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
package org.apache.shindig.gadgets.servlet;

import com.google.caja.lexer.CharProducer;
import com.google.caja.lexer.InputSource;
import com.google.caja.parser.ParseTreeNode;
import com.google.caja.reporting.EchoingMessageQueue;
import com.google.caja.reporting.MessageContext;
import com.google.caja.reporting.MessageQueue;
import com.google.caja.util.ContentType;

import java.io.PrintWriter;
import java.net.URI;

import junit.framework.TestCase;

/**
 * Tests equality of ModuleCacheKey{,s} instances.
 */
public class ModuleCacheTest extends TestCase {

  public final void testKeyEqualToKeyFromSameNode() throws Exception {
    assertKeyEquality(true, key("42"), key("42"));
    assertKeyEquality(true, key("<br>"), key("<br>"));
  }

  public final void testKeysDifferBasedOnContentType() throws Exception {
    assertKeysDiffer(key("foo", true), key("foo", false));
  }

  public final void testKeysDifferBasedOnNodeType() throws Exception {
    assertKeysDiffer(key("foo"), key("'foo'"));
    assertKeysDiffer(key("<div>"), key("div", true));
  }

  public final void testKeysDifferBasedOnNodeValue() throws Exception {
    assertKeysDiffer(key("'foo'"), key("'bar'"));
    assertKeysDiffer(key("break foo"), key("break"));
    assertKeysDiffer(key("break"), key("break foo"));
  }

  public final void testKeysDifferBasedOnChildren() throws Exception {
    assertKeysDiffer(key("return"), key("return 42"));
  }

  public final void testKeysDifferBasedOnElementName() throws Exception {
    assertKeysDiffer(key("<div>"), key("<span>"));
  }

  public final void testKeysDifferBasedOnAttributeName() throws Exception {
    assertKeysDiffer(key("<input type=text>"), key("<input name=text>"));
  }

  public final void testKeysDifferBasedOnAttributeValue() throws Exception {
    assertKeysDiffer(key("<input type=text>"), key("<input type=checkbox>"));
  }

  public final void testKeysDifferBasedOnText() throws Exception {
    assertKeysDiffer(key("<div>foo</div>"), key("<div>bar</div>"));
  }

  public final void testKeysDifferBasedOnCdataSection() throws Exception {
    assertKeysDiffer(key("<?xml version=\"1.0\"?><div><![CDATA[foo]]></div>"),
                     key("<?xml version=\"1.0\"?><div><![CDATA[bar]]></div>"));
  }

  private static void assertKeyEquality(
      boolean equal, ModuleCacheKey k, ModuleCacheKey j) {
    assertEquality(equal, k, j);
    assertEquality(equal, k.asSingleton(), j.asSingleton());
  }

  private static void assertEquality(boolean equal, Object a, Object b) {
    assertEquals(equal, a.equals(b));
    if (equal) {
      assertEquals(a.hashCode(), b.hashCode());
    }
  }

  private static void assertKeysDiffer(ModuleCacheKey k, ModuleCacheKey j) {
    assertKeyEquality(false, k, j);
  }

  private ModuleCacheKey key(String codeSnippet) throws Exception {
    boolean isHtml = codeSnippet.trim().startsWith("<");
    return key(codeSnippet, isHtml);
  }

  private ModuleCacheKey key(String codeSnippet, boolean isHtml) throws Exception {
    MessageQueue mq = new EchoingMessageQueue(
        new PrintWriter(System.err, true), new MessageContext());
    InputSource is = new InputSource(new URI("test:///" + getName()));
    ParseTreeNode node = CajaContentRewriter.parse(
        is, CharProducer.Factory.fromString(codeSnippet, is),
        isHtml ? "text/html" : "text/javascript",
        mq);
    return new ModuleCacheKey(isHtml ? ContentType.HTML : ContentType.JS, node);
  }
}

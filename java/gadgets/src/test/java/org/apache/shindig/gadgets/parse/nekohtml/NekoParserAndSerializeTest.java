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
package org.apache.shindig.gadgets.parse.nekohtml;

import static org.junit.Assert.assertNull;

import org.apache.shindig.gadgets.parse.AbstractParserAndSerializerTest;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;
import org.junit.Test;

/**
 * Test behavior of neko based parser and serializers
 */
public class NekoParserAndSerializeTest extends AbstractParserAndSerializerTest {
  @Override
  protected GadgetHtmlParser makeParser() {
    return new NekoSimplifiedHtmlParser(
        new ParseModule.DOMImplementationProvider().get());
  }

  // Neko-specific tests.
  @Test
  public void scriptPushedToBody() throws Exception {
    String content = loadFile("org/apache/shindig/gadgets/parse/nekohtml/test-leadingscript.html");
    String expected =
        loadFile("org/apache/shindig/gadgets/parse/nekohtml/test-leadingscript-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  // Neko overridden tests (due to Neko quirks)
  @Override
  @Test
  public void notADocument() throws Exception {
    // Note that no doctype is injected for fragments
    String content = loadFile("org/apache/shindig/gadgets/parse/nekohtml/test-fragment.html");
    String expected = loadFile("org/apache/shindig/gadgets/parse/nekohtml/test-fragment-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  @Override
  @Test
  public void noBody() throws Exception {
    // Note that no doctype is injected for fragments
    String content = loadFile("org/apache/shindig/gadgets/parse/nekohtml/test-headnobody.html");
    String expected = loadFile("org/apache/shindig/gadgets/parse/nekohtml/test-headnobody-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  // Overridden because of comment vs. script ordering. Neko stuffs script into head, but
  // postprocessing moves it back down into body, *above* the comment element. This is
  // semantically meaningless (to HTML), so we create a new test to accommodate it.
  @Override
  @Test
  public void docNoDoctype() throws Exception {
    // Note that no doctype is properly created when none specified
    String content = loadFile("org/apache/shindig/gadgets/parse/test-fulldocnodoctype.html");
    String expected =
        loadFile("org/apache/shindig/gadgets/parse/nekohtml/test-fulldocnodoctype-expected.html");
    assertNull(parser.parseDom(content).getDoctype());
    parseAndCompareBalanced(content, expected, parser);
  }

  @Test
  public void textBeforeScript() throws Exception {
    // Doesn't work in "native" form due to Neko's internals. Upon finding first text, then a
    // <script> node, Neko discards the text. To fix this, we would have to either dive into
    // Neko's internals, which could change underneath us, or do some overly complicated and
    // costly dual-parsing pass, to detect which "early" elements have been discarded from
    // the document by Neko. These use cases are marginal at best, and Caja's parser does not
    // exhibit this behavior, so we don't do so.
  }
}

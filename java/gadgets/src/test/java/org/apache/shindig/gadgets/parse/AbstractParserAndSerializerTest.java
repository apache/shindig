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
package org.apache.shindig.gadgets.parse;

import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Base test fixture for HTML parsing and serialization.
 */
public abstract class AbstractParserAndSerializerTest extends AbstractParsingTestBase {
  protected GadgetHtmlParser parser;

  protected abstract GadgetHtmlParser makeParser();

  @Before
  public void setUp() throws Exception {
    parser = makeParser();
  }

  @Test
  public void docWithDoctype() throws Exception {
    // Note that doctype is properly retained
    String content = loadFile("org/apache/shindig/gadgets/parse/test.html");
    String expected = loadFile("org/apache/shindig/gadgets/parse/test-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  @Test
  public void docNoDoctype() throws Exception {
    // Note that no doctype is properly created when none specified
    String content = loadFile("org/apache/shindig/gadgets/parse/test-fulldocnodoctype.html");
    String expected =
        loadFile("org/apache/shindig/gadgets/parse/test-fulldocnodoctype-expected.html");
    assertNull(parser.parseDom(content).getDoctype());
    parseAndCompareBalanced(content, expected, parser);
  }

  @Test
  public void docStartsWithHeader() throws Exception {
    String content = loadFile("org/apache/shindig/gadgets/parse/test-startswithcomment.html");
    String expected =
        loadFile("org/apache/shindig/gadgets/parse/test-startswithcomment-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  @Test
  public void notADocument() throws Exception {
    // Note that no doctype is injected for fragments
    String content = loadFile("org/apache/shindig/gadgets/parse/test-fragment.html");
    String expected = loadFile("org/apache/shindig/gadgets/parse/test-fragment-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  @Test
  public void notADocument2() throws Exception {
    // Note that no doctype is injected for fragments
    String content = loadFile("org/apache/shindig/gadgets/parse/test-fragment2.html");
    String expected = loadFile("org/apache/shindig/gadgets/parse/test-fragment2-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  @Test
  public void noBody() throws Exception {
    // Note that no doctype is injected for fragments
    String content = loadFile("org/apache/shindig/gadgets/parse/test-headnobody.html");
    String expected = loadFile("org/apache/shindig/gadgets/parse/test-headnobody-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  @Test
  public void ampersand() throws Exception {
    // Note that no doctype is injected for fragments
    String content = loadFile("org/apache/shindig/gadgets/parse/test-with-ampersands.html");
    String expected =
        loadFile("org/apache/shindig/gadgets/parse/test-with-ampersands-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }

  @Test
  public void textBeforeScript() throws Exception {
    String content = loadFile("org/apache/shindig/gadgets/parse/test-text-before-script.html");
    String expected =
        loadFile("org/apache/shindig/gadgets/parse/test-text-before-script-expected.html");
    parseAndCompareBalanced(content, expected, parser);
  }
}

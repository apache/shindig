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
package org.apache.shindig.gadgets.parse.caja;

import org.apache.shindig.gadgets.GadgetException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for VanillaCajaHtmlParser.
 */
public class VanillaCajaHtmlParserTest {
  private VanillaCajaHtmlParser parser;
  private VanillaCajaHtmlSerializer serializer;

  @Before
  public void setUp() throws Exception {
    DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
    // Require the traversal API
    DOMImplementation domImpl = registry.getDOMImplementation("XML 1.0 Traversal 2.0");
    parser = new VanillaCajaHtmlParser(domImpl, true);
    serializer = new VanillaCajaHtmlSerializer();
  }

  @Ignore
  @Test(expected = GadgetException.class)
  public void testEmptyDocument() throws Exception {
    boolean exceptionCaught = false;
    parser.parseDom("");
  }

  // Bad behavior by Caja DomParser. Bug to be raised with Caja team.
  // Caja should not parse such javascript as html. Ideally it should throw an
  // exception indicating non html content.
  // TODO: Update test case when the issue is fixed.
  @Test
  public void testNonHtml() throws Exception {
    String html = "var hello=\"world\";";
    String expected = "<html><head></head><body>var hello=&#34;world&#34;;"
                      + "</body></html>";
    assertEquals(expected, serializer.serialize(parser.parseDom(html)));
  }

  @Test
  public void testNoHead() throws Exception {
    String html = "<html><body><a href=\"hello\"></a></body></html>";
    String expected = "<html><head></head><body><a href=\"hello\"></a>"
                      + "</body></html>";
    assertEquals(expected, serializer.serialize(parser.parseDom(html)));
  }

  @Test
  public void testParseAndSerialize() throws Exception {
    String html = "<html><head><script src=\"1.js\"></script></head>"
                  + "<body><a href=\"hello\"></a></body></html>";
    String expected = "<html><head><script src=\"1.js\"></script></head>"
                      + "<body><a href=\"hello\"></a>"
                      + "</body></html>";
    assertEquals(expected, serializer.serialize(parser.parseDom(html)));
  }

  @Test
  public void testUnbalanced() throws Exception {
    String html = "<html><head><script src=\"1.js\"></script></head>"
                  + "<body><p><embed></p></embed></body></html>";
    String expected = "<html><head><script src=\"1.js\"></script></head>"
                      + "<body><p><embed /></p>"
                      + "</body></html>";
    assertEquals(expected, serializer.serialize(parser.parseDom(html)));
  }

  // Weird case of normalization. Chrome and Firefox do not seem to execute the
  // script since there is no closing </script> tag. Hence Caja is consistent
  // with modern browsers.
  @Test
  public void testBadTagBalancing() throws Exception {
    String html = "<html><head><script src=\"1.js\"></head>"
                  + "<body></body></html>";
    String expected = "<html><head><script src=\"1.js\">"
                      + "</head><body></body></html>"
                      + "</script></head><body></body></html>";
    assertEquals(expected, serializer.serialize(parser.parseDom(html)));
  }
}

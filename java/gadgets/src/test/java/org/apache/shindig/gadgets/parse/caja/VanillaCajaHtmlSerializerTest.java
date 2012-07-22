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

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import static org.junit.Assert.assertEquals;

/**
 * Tests for VanillaCajaHtmlSerializer.
 */
public class VanillaCajaHtmlSerializerTest {
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

  @Test
  public void testParseAndSerializeNonASCIINotEscaped() throws Exception {
    String html = "<html><head><script src=\"1.js\"></script></head>"
                  + "<body><a href=\"hello\">\\u200E\\u200F\\u2010\\u0410</a>"
                  + "</body></html>";
    assertEquals(html, serializer.serialize(parser.parseDom(html)));
  }

  @Test
  public void testParseAndSerializeCommentsNotRemoved() throws Exception {
    String html = "<html><head><script src=\"1.js\"></script></head>"
                  + "<body><div>before <!-- Test Data --> after \n"
                  + "<!-- [if IE ]>"
                  + "<link href=\"iecss.css\" rel=\"stylesheet\" type=\"text/css\">"
                  + "<![endif]-->"
                  + "</div></body></html>";
    // If we run the serializer with wantsComments set to false, all comments are removed from the
    // serialized html and the output is:
    // "<html><head><script src=\"1.js\"></script></head>"
    // + "<body><div>"
    // + "</div></body></html>"
    assertEquals(html, serializer.serialize(parser.parseDom(html)));
  }
}

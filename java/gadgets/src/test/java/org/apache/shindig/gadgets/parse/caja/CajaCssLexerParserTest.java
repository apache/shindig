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

import org.apache.shindig.common.cache.LruCacheProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Basic test of CSS lexer
 */
public class CajaCssLexerParserTest extends Assert {

  private CajaCssLexerParser cajaCssParser;

  private static final String CSS = "@import url('www.example.org/someother.css');\n" +
      ".xyz { background-image : url(http://www.example.org/someimage.gif); }\n" +
      "A { color : #7f7f7f }\n";

  @Before
  public void setUp() throws Exception {
    cajaCssParser = new CajaCssLexerParser();
  }

  @Test
  public void testBasicCssParse() throws Exception {
    String css = ".xyz { font : bold; } A { color : #7f7f7f }";
    List<Object> styleSheet = cajaCssParser.parse(css);
    assertEquals(cajaCssParser.serialize(styleSheet), css);
  }

  @Test
  public void testClone() throws Exception {
    // Set the cache so we force cloning
    cajaCssParser.setCacheProvider(new LruCacheProvider(100));

    // Compare the raw parsed structure to a cloned one
    List<Object> styleSheet = cajaCssParser.parseImpl(CSS);
    List<Object> styleSheet2 = cajaCssParser.parse(CSS);
    assertEquals(cajaCssParser.serialize(styleSheet), cajaCssParser.serialize(styleSheet2));
  }

  @Test
  public void testCache() throws Exception {
    cajaCssParser.setCacheProvider(new LruCacheProvider(100));
    // Ensure that we return cloned instances and not the original out of the cache. Cloned
    // instances intentionally do not compare equal but should produce the same output
    List<Object> styleSheet = cajaCssParser.parse(CSS);
    List<Object> styleSheet2 = cajaCssParser.parse(CSS);
    assertFalse(styleSheet.equals(styleSheet2));
    assertEquals(cajaCssParser.serialize(styleSheet), cajaCssParser.serialize(styleSheet2));
  }
}

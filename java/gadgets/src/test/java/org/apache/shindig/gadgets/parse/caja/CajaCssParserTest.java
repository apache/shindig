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

import junit.framework.TestCase;

import java.util.List;

/**
 * Basic CSS parse tests
 */
public class CajaCssParserTest extends TestCase {

  private CajaCssParser cajaCssParser;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    cajaCssParser = new CajaCssParser();
  }

  public void testBasicCssParse() throws Exception {
    String css = ".xyz { font : bold; } A { color : #7f7f7f }";
    CssTree.StyleSheet styleSheet = cajaCssParser.parseDom(css);
    List<CssTree.SimpleSelector> selectorList = CajaCssUtils.descendants(styleSheet,
        CssTree.SimpleSelector.class);
    assertEquals(2, selectorList.size());
    assertEquals(CssTree.SimpleSelector.class, selectorList.get(0).getClass());
  }
}

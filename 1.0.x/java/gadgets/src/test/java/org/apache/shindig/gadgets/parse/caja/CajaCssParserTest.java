/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.parse.caja;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.ParsedCssDeclaration;
import org.apache.shindig.gadgets.parse.ParsedCssRule;

import junit.framework.TestCase;

import java.util.List;

public class CajaCssParserTest extends TestCase {
  private final CajaCssParser csp = new CajaCssParser();
  
  public void testParseEveryTypeOfSelector() throws Exception {
    // List from http://www.w3.org/TR/REC-CSS2/selector.html
    List<ParsedCssRule> rules =
        csp.parse("*, E, E F, E > F, E:first-child, E:link, E:active, " +
                  "E:lang(c), E + F, E[foo], E[foo=\"warning\"], DIV.blah, " +
                  "E#myId { color: blue; }");
    assertNotNull(rules);
    assertEquals(1, rules.size());
    
    // Validate single resulting ParsedCssRule
    ParsedCssRule rule = rules.get(0);
    assertNotNull(rule.getSelectors());
    assertEquals(13, rule.getSelectors().size());
    
    // Selectors should come back in the order parsed as well
    // Shouldn't matter if they come out the way they went in
    // ie. if they're normalized
    assertEquals("*", rule.getSelectors().get(0));
    assertEquals("E", rule.getSelectors().get(1));
    assertTrue(rule.getSelectors().get(2).matches("E\\s+F"));
    assertTrue(rule.getSelectors().get(3).matches("E\\s+>\\s+F"));
    assertEquals("E:first-child", rule.getSelectors().get(4));
    assertEquals("E:link", rule.getSelectors().get(5));
    assertEquals("E:active", rule.getSelectors().get(6));
    assertEquals("E:lang(c)", rule.getSelectors().get(7));
    assertTrue(rule.getSelectors().get(8).matches("E\\s+\\+\\s+F"));
    assertTrue(rule.getSelectors().get(9).matches("E\\[\\s*foo\\s*\\]"));
    assertTrue(rule.getSelectors().get(10).matches(
        "E\\[\\s*foo\\s*=\\s*[\"']warning[\"']\\s*\\]"));
    assertEquals("DIV.blah", rule.getSelectors().get(11));
    assertEquals("E#myId", rule.getSelectors().get(12));
    
    // Declaration thrown in for good measure
    assertNotNull(rule.getDeclarations());
    assertEquals(1, rule.getDeclarations().size());
    ParsedCssDeclaration decl = rule.getDeclarations().get(0);
    assertEquals("color", decl.getName());
    assertEquals("blue", decl.getValue());
  }
  
  public void testParseWithNoDeclarations() throws Exception {
    List<ParsedCssRule> rules =
        csp.parse("#id { }");
    assertNotNull(rules);
    assertEquals(1, rules.size());
    
    ParsedCssRule rule = rules.get(0);
    assertNotNull(rule);
    assertNotNull(rule.getSelectors());
    assertEquals(1, rule.getSelectors().size());
    assertEquals("#id", rule.getSelectors().get(0));
    assertNotNull(rule.getDeclarations());
  }

  public void testParseEmptyContent() throws Exception {
    List<ParsedCssRule> rules =
        csp.parse("  \n\t  ");
    assertNotNull(rules);
    assertEquals(0, rules.size());
  }
  
  public void testParseMultipleRules() throws Exception {
    List<ParsedCssRule> rules =
        csp.parse("#id1 { font-size: 1; } #id2 { font-size: 2; } " +
                  "#id3 { font-size: 3; }");
    assertNotNull(rules);
    assertEquals(3, rules.size());
    
    // Scoped to hide each rule test from each other
    {
      ParsedCssRule rule = rules.get(0);
      assertNotNull(rule);
      assertNotNull(rule.getSelectors());
      assertEquals("#id1", rule.getSelectors().get(0));
      assertNotNull(rule.getDeclarations());
      assertEquals("font-size", rule.getDeclarations().get(0).getName());
      assertEquals("1", rule.getDeclarations().get(0).getValue());
    }
    
    {
      ParsedCssRule rule = rules.get(1);
      assertNotNull(rule);
      assertNotNull(rule.getSelectors());
      assertEquals("#id2", rule.getSelectors().get(0));
      assertNotNull(rule.getDeclarations());
      assertEquals("font-size", rule.getDeclarations().get(0).getName());
      assertEquals("2", rule.getDeclarations().get(0).getValue());
    }
    
    {
      ParsedCssRule rule = rules.get(2);
      assertNotNull(rule);
      assertNotNull(rule.getSelectors());
      assertEquals("#id3", rule.getSelectors().get(0));
      assertNotNull(rule.getDeclarations());
      assertEquals("font-size", rule.getDeclarations().get(0).getName());
      assertEquals("3", rule.getDeclarations().get(0).getValue());
    }
  }
  
  public void testParseCssNoTrailingSemicolon() throws Exception {
    List<ParsedCssRule> rules =
        csp.parse("#id { color:blue; font: verdana }");
    assertNotNull(rules);
    assertEquals(1, rules.size());
    
    ParsedCssRule rule = rules.get(0);
    assertNotNull(rule);
    assertNotNull(rule.getSelectors());
    assertEquals("#id", rule.getSelectors().get(0));
    assertNotNull(rule.getDeclarations());
    assertEquals(2, rule.getDeclarations().size());
    assertEquals("color", rule.getDeclarations().get(0).getName());
    assertEquals("blue", rule.getDeclarations().get(0).getValue());
    assertEquals("font", rule.getDeclarations().get(1).getName());
    assertEquals("verdana", rule.getDeclarations().get(1).getValue());
  }
  
  public void testParseInvalidCssNoDeclValue() throws Exception {
    try {
      String css = "#id { color: ; font-size: 10; }";
      csp.parse(css);
      fail("Should have failed to parse invalid CSS: " + css);
    } catch (GadgetException e) {
      // Expected condition.
    }
  }
  
  public void testParseInvalidCssNoClosingBrace() throws Exception {
    try {
      String css = "#id { color: blue; ";
      csp.parse(css);
      fail("Should have failed to parse invalid CSS: " + css);
    } catch (GadgetException e) {
      // Expected condition.
    }
  }
  
  public void testParseInvalidCssNoSelector() throws Exception {
    try {
      String css = "{ color: green; font: verdana; }";
      csp.parse(css);
      fail("Should have failed to parse invalid CSS: " + css);
    } catch (GadgetException e) {
      // Expected condition.
    }
  }
  
  public void testParseInvalidCssNoSeparator() throws Exception {
    try {
      String css = "#id { color blue; }";
      csp.parse(css);
      fail("Should have failed to parse invalid CSS: " + css);
    } catch (GadgetException e) {
      // Expected condition.
    }
  }
  
  public void testParseInlineGeneralDeclarations() throws Exception {
    List<ParsedCssDeclaration> decls =
        csp.parseInline("font-size: 10 em; color: green; font-color: #343434;");
    assertNotNull(decls);
    assertEquals(3, decls.size());
    assertEquals("font-size", decls.get(0).getName());
    assertEquals("10 em", decls.get(0).getValue());
    assertEquals("color", decls.get(1).getName());
    assertEquals("green", decls.get(1).getValue());
    assertEquals("font-color", decls.get(2).getName());
    assertEquals("#343434", decls.get(2).getValue());
  }
  
  public void testParseInlineNoDeclarations() throws Exception {
    List<ParsedCssDeclaration> decls =
        csp.parseInline("");
    assertNotNull(decls);
    assertEquals(0, decls.size());
  }
  
  public void testParseInlineNoEndingSemicolon() throws Exception {
    List<ParsedCssDeclaration> decls =
        csp.parseInline("color: green; font-size: 10");
    assertNotNull(decls);
    assertEquals(2, decls.size());
    assertEquals("color", decls.get(0).getName());
    assertEquals("green", decls.get(0).getValue());
    assertEquals("font-size", decls.get(1).getName());
    assertEquals("10", decls.get(1).getValue());
  }
  
  public void parseInlineNoSeparator() throws Exception {
    try {
      String iCss = "color green; font-size: 10;";
      csp.parseInline(iCss);
      fail("Should have failed to parse inline CSS: " + iCss);
    } catch (GadgetException e) {
      // Expected condition.
    }
  }
}

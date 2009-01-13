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
package org.apache.shindig.gadgets.parse;

import com.google.common.collect.Lists;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

import java.util.List;
import java.util.Set;

public class GadgetCssRuleTest extends TestCase {
  private String[] selectors = { "#id1", "  .class1 " };
  private String[][] decls =
      { { "color", "blue" }, { " font-size ", "10 em" } };

  private ParsedCssDeclaration makeParsedDecl(String name, String val) {
    ParsedCssDeclaration parsedDecl = EasyMock.createNiceMock(ParsedCssDeclaration.class);
    expect(parsedDecl.getName()).andReturn(name).anyTimes();
    expect(parsedDecl.getValue()).andReturn(val).anyTimes();
    replay(parsedDecl);
    return parsedDecl;
  }

  private ParsedCssRule makeParsedRule(String[] selectors, String[][] decls) {
    ParsedCssRule parsedMock = EasyMock.createNiceMock(ParsedCssRule.class);

    List<String> selectorList = Lists.newLinkedList();
    for (String sel : selectors) {
      selectorList.add(sel);
    }
    expect(parsedMock.getSelectors()).andReturn(selectorList).anyTimes();

    List<ParsedCssDeclaration> declList = Lists.newLinkedList();
    for (String[] decl : decls) {
      declList.add(makeParsedDecl(decl[0], decl[1]));
    }
    expect(parsedMock.getDeclarations()).andReturn(declList).anyTimes();

    replay(parsedMock);
    return parsedMock;
  }

  public void testCreatedFromParsed() {
    ParsedCssRule parsed = makeParsedRule(selectors, decls);
    GadgetCssRule rule = new GadgetCssRule(parsed);
    helperValidateCssRule(rule);
  }

  public void testCreatedManually() {
    GadgetCssRule rule = new GadgetCssRule();
    assertNotNull(rule.getSelectors());
    assertEquals(0, rule.getSelectors().size());
    assertNotNull(rule.getDeclarationKeys());
    assertEquals(0, rule.getDeclarationKeys().size());
    for (String sel : selectors) {
      assertTrue(rule.addSelector(sel, null));
    }
    for (String[] decl : decls) {
      rule.setDeclaration(decl[0], decl[1]);
    }
    helperValidateCssRule(rule);
  }

  private void helperValidateCssRule(GadgetCssRule rule) {
    assertTrue(rule.hasSelector("#id1"));
    assertTrue(rule.hasSelector(".class1"));
    assertFalse(rule.hasSelector("  .class1 "));
    assertFalse(rule.hasSelector("body"));

    List<String> seleList = rule.getSelectors();
    assertNotNull(seleList);
    assertEquals(2, seleList.size());
    assertEquals("#id1", seleList.get(0));
    assertEquals(".class1", seleList.get(1));

    assertTrue(rule.removeSelector(".class1"));
    assertTrue(rule.hasSelector("#id1"));
    assertFalse(rule.hasSelector(".class1"));
    assertFalse(rule.removeSelector("nonexistent"));

    assertTrue(rule.addSelector(" .new ", null));
    assertTrue(rule.hasSelector("#id1"));
    assertTrue(rule.hasSelector(".new"));
    assertFalse(rule.hasSelector(".class1"));
    assertFalse(rule.addSelector(".new", null));

    List<String> addSeleList = rule.getSelectors();
    assertNotNull(addSeleList);
    assertEquals(2, addSeleList.size());
    assertEquals("#id1", addSeleList.get(0));
    assertEquals(".new", addSeleList.get(1));

    assertTrue(rule.addSelector(".middle", ".new"));
    List<String> injectSeleList = rule.getSelectors();
    assertNotNull(injectSeleList);
    assertEquals(3, injectSeleList.size());
    assertEquals("#id1", injectSeleList.get(0));
    assertEquals(".middle", injectSeleList.get(1));
    assertEquals(".new", injectSeleList.get(2));
    assertFalse(rule.addSelector(".middle", ".new"));

    Set<String> declKeys = rule.getDeclarationKeys();
    assertEquals(2, declKeys.size());
    assertTrue(declKeys.contains("color"));
    assertEquals("blue", rule.getDeclarationValue("color"));
    assertTrue(declKeys.contains("font-size"));
    assertEquals("10 em", rule.getDeclarationValue("font-size"));

    assertTrue(rule.removeDeclaration("font-size"));
    Set<String> removeDeclKeys = rule.getDeclarationKeys();
    assertEquals(1, removeDeclKeys.size());
    assertTrue(removeDeclKeys.contains("color"));
    assertEquals("blue", rule.getDeclarationValue("color"));
    assertFalse(removeDeclKeys.contains("font-size"));
    assertFalse(rule.removeDeclaration("font-size"));

    rule.setDeclaration("color", "green");
    assertEquals("green", rule.getDeclarationValue("color"));
    assertEquals(1, rule.getDeclarationKeys().size());

    rule.setDeclaration("font", "verdana");
    assertEquals(2, rule.getDeclarationKeys().size());
    assertEquals("green", rule.getDeclarationValue("color"));
    assertEquals("verdana", rule.getDeclarationValue("font"));
  }
}

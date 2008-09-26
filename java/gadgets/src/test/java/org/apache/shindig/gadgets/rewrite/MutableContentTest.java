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
package org.apache.shindig.gadgets.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.shindig.gadgets.parse.GadgetHtmlNode;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.apache.shindig.gadgets.rewrite.MutableContent;

import org.junit.Before;
import org.junit.Test;

public class MutableContentTest {
  private MutableContent mhc;
  
  @Before
  public void setUp() throws Exception {
    // Note dependency on CajaHtmlParser - this isn't particularly ideal but is
    // sufficient given that this test doesn't exercise the parser extensively at all,
    // instead focusing on the additional utility provided by MutableHtmlContent
    mhc = new MutableContent(new CajaHtmlParser());
    mhc.setContent("DEFAULT VIEW");
  }
  
  @Test
  public void getContentAndParseTreeNoSets() throws Exception {
    String content = mhc.getContent();
    assertEquals("DEFAULT VIEW", content);
  
    GadgetHtmlNode root = mhc.getParseTree();
    assertEquals(1, root.getChildren().size());
    assertTrue(root.getChildren().get(0).isText());
    assertEquals(content, root.getChildren().get(0).getText());
  
    assertSame(content, mhc.getContent());
    assertSame(root, mhc.getParseTree());
  }
  
  @Test
  public void modifyContentReflectedInTree() throws Exception {
    mhc.setContent("NEW CONTENT");
    GadgetHtmlNode root = mhc.getParseTree();
    assertEquals(1, root.getChildren().size());
    assertEquals("NEW CONTENT", root.getChildren().get(0).getText());
  }
  
  @Test
  public void modifyTreeReflectedInContent() throws Exception {
    GadgetHtmlNode root = mhc.getParseTree();
  
    // First child should be text node per other tests. Modify it.
    root.getChildren().get(0).setText("FOO CONTENT");
    assertEquals("FOO CONTENT", mhc.getContent());
  
    // Do it again
    root.getChildren().get(0).setText("BAR CONTENT");
    assertEquals("BAR CONTENT", mhc.getContent());
  
    // GadgetHtmlNode hasn't changed because string hasn't changed
    assertSame(root, mhc.getParseTree());
  }
  
  @Test
  public void staleTreeEditsInvalidatedAfterContentSet() throws Exception {
    GadgetHtmlNode firstRoot = mhc.getParseTree();
  
    // Re-set content
    mhc.setContent("INVALIDATING CONTENT");
  
    // Should still be able to obtain this.
    GadgetHtmlNode secondRoot = mhc.getParseTree();
    assertNotSame(firstRoot, secondRoot);
  
    // Should be able to *obtain* first child node...
    GadgetHtmlNode firstTextNode = firstRoot.getChildren().get(0);
    try {
      // ...but not edit it.
      firstTextNode.setText("STALE-SET CONTENT");
      fail("Should not be able to modify stale parse tree");
    } catch (IllegalStateException e) {
      // Expected condition.
    }
  
    assertEquals("INVALIDATING CONTENT", secondRoot.getChildren().get(0).getText());
  
    // For good measure, modify secondRoot and get content
    secondRoot.getChildren().get(0).setText("NEW CONTENT");
    assertEquals("NEW CONTENT", mhc.getContent());
  }
}

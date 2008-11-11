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

import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class MutableContentTest {
  private MutableContent mhc;
  
  @Before
  public void setUp() throws Exception {
    // Note dependency on CajaHtmlParser - this isn't particularly ideal but is
    // sufficient given that this test doesn't exercise the parser extensively at all,
    // instead focusing on the additional utility provided by MutableHtmlContent
    Injector injector = Guice.createInjector(new ParseModule(), new PropertiesModule());
    mhc = new MutableContent(injector.getInstance(GadgetHtmlParser.class), "DEFAULT VIEW", null);
  }
  
  @Test
  public void getContentAndParseTreeNoSets() throws Exception {
    String content = mhc.getContent();
    assertEquals("DEFAULT VIEW", content);
  
    Document document = mhc.getDocument();
    assertEquals(2, document.getFirstChild().getChildNodes().getLength());
    assertTrue(document.getFirstChild().getChildNodes().item(1).getFirstChild().getNodeType() ==
        Node.TEXT_NODE);
    assertEquals(content, document.getFirstChild().getChildNodes().item(1).getTextContent());
  
    assertSame(content, mhc.getContent());
    assertSame(document, mhc.getDocument());
  }
  
  @Test
  public void modifyContentReflectedInTree() throws Exception {
    mhc.setContent("NEW CONTENT");
    Document document = mhc.getDocument();
    assertEquals(1, document.getChildNodes().getLength());
    assertEquals("NEW CONTENT", document.getChildNodes().item(0).getTextContent());
  }
  
  @Test
  public void modifyTreeReflectedInContent() throws Exception {
    Document document = mhc.getDocument();
  
    // First child should be text node per other tests. Modify it.
    document.getFirstChild().getFirstChild().setTextContent("FOO CONTENT");
    MutableContent.notifyEdit(document);
    assertTrue(mhc.getContent().contains("FOO CONTENT"));

    // Do it again
    document.getFirstChild().getFirstChild().setTextContent("BAR CONTENT");
    MutableContent.notifyEdit(document);
    assertTrue(mhc.getContent().contains("BAR CONTENT"));
  
    // GadgetHtmlNode hasn't changed because string hasn't changed
    assertSame(document, mhc.getDocument());
  }
}

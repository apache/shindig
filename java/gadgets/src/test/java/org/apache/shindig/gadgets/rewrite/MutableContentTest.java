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

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParseModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.InputStream;
import java.util.Arrays;

public class MutableContentTest {
  private MutableContent mhc;

  @Before
  public void setUp() throws Exception {
    Injector injector = Guice.createInjector(new ParseModule(), new PropertiesModule());
    mhc = new MutableContent(injector.getInstance(GadgetHtmlParser.class), "DEFAULT VIEW");
  }

  @Test
  public void getContentAndParseTreeNoSets() throws Exception {
    String content = mhc.getContent();
    assertEquals("DEFAULT VIEW", content);

    Document document = mhc.getDocument();
    assertEquals(2, document.getFirstChild().getChildNodes().getLength());
    Assert.assertSame(document.getFirstChild().getChildNodes().item(1).getFirstChild().getNodeType(), Node.TEXT_NODE);
    assertEquals(content, document.getFirstChild().getChildNodes().item(1).getTextContent());

    assertSame(content, mhc.getContent());
    assertTrue(Arrays.equals(
        content.getBytes("UTF8"), IOUtils.toByteArray(mhc.getContentBytes())));
    assertSame(document, mhc.getDocument());
    assertEquals(0, mhc.getNumChanges());
  }

  @Test
  public void modifyContentReflectedInTreeAndBytes() throws Exception {
    assertEquals(0, mhc.getNumChanges());
    mhc.setContent("NEW CONTENT");
    assertEquals(1, mhc.getNumChanges());
    assertEquals("NEW CONTENT", new String(IOUtils.toByteArray(mhc.getContentBytes()), "UTF8"));
    Document document = mhc.getDocument();
    assertEquals(1, document.getChildNodes().getLength());
    assertEquals("NEW CONTENT", document.getChildNodes().item(0).getTextContent());
    mhc.documentChanged();
    assertEquals(2, mhc.getNumChanges());
  }

  @Test
  public void modifyContentReflectedInTreeUtf8() throws Exception {
    String theContent = "N\uFFFDW C\uFFFDNT\uFFFDNT";

    assertEquals(0, mhc.getNumChanges());
    mhc.setContent(theContent);
    assertEquals(1, mhc.getNumChanges());
    assertEquals(theContent, new String(IOUtils.toByteArray(mhc.getContentBytes()), "UTF8"));
    Document document = mhc.getDocument();
    assertEquals(1, document.getChildNodes().getLength());
    assertEquals(theContent, document.getChildNodes().item(0).getTextContent());
    mhc.documentChanged();
    assertEquals(2, mhc.getNumChanges());
  }

  @Test
  public void modifyBytesReflectedInContentAndTree() throws Exception {
    assertEquals(0, mhc.getNumChanges());
    mhc.setContentBytes("NEW CONTENT".getBytes("UTF8"), Charsets.UTF_8);
    assertEquals(1, mhc.getNumChanges());
    Document document = mhc.getDocument();
    assertEquals(1, document.getChildNodes().getLength());
    assertEquals("NEW CONTENT", document.getChildNodes().item(0).getTextContent());
    assertEquals("NEW CONTENT", mhc.getContent());
    assertEquals(1, mhc.getNumChanges());
    InputStream is = mhc.getContentBytes();
    assertEquals("NEW CONTENT", new String(IOUtils.toByteArray(is), "UTF8"));
    assertEquals(1, mhc.getNumChanges());
  }

  @Test
  public void modifyTreeReflectedInContent() throws Exception {
    Document document = mhc.getDocument();

    // First child should be text node per other tests. Modify it.
    document.getFirstChild().getFirstChild().setTextContent("FOO CONTENT");
    assertEquals(0, mhc.getNumChanges());
    MutableContent.notifyEdit(document);
    assertEquals(1, mhc.getNumChanges());
    assertTrue(mhc.getContent().contains("FOO CONTENT"));

    // Do it again
    document.getFirstChild().getFirstChild().setTextContent("BAR CONTENT");
    MutableContent.notifyEdit(document);
    assertEquals(2, mhc.getNumChanges());
    assertTrue(mhc.getContent().contains("BAR CONTENT"));
    assertTrue(new String(IOUtils.toByteArray(mhc.getContentBytes()), "UTF8").contains("BAR CONTENT"));

    // GadgetHtmlNode hasn't changed because string hasn't changed
    assertSame(document, mhc.getDocument());
  }
}

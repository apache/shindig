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
package org.apache.shindig.gadgets.js;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

public class JsResponseBuilderTest {

  private JsResponseBuilder builder;

  @Before
  public void setUp() {
    builder = new JsResponseBuilder();
  }

  @Test
  public void testExterns() throws Exception {
    builder.appendExtern("b");
    builder.appendExtern("b");
    builder.appendExtern("c.d");
    builder.appendExtern("c.d");
    builder.appendExtern("e.prototype.f");
    builder.appendExtern("e.prototype.f");
    builder.appendRawExtern("var a");
    String eee = builder.build().getExterns();
    assertEquals(
        "var a;\n" +
        "var b = {};\n" +
        "var c = {};\nc.d = {};\n" +
        "var e = {};\ne.prototype.f = {};\n",
        builder.build().getExterns());
  }

  @Test
  public void skipsEmptyContent() throws Exception {
    builder.appendJs("number 1", "num1");
    builder.appendJs("", "num2");
    builder.appendJs("number 3", "num3");
    builder.prependJs("number 4", "num4");
    builder.prependJs("", "num5");
    Iterator<JsContent> allJsContent = builder.build().getAllJsContent().iterator();
    assertEquals("num4", allJsContent.next().getSource());
    assertEquals("num1", allJsContent.next().getSource());
    assertEquals("num3", allJsContent.next().getSource());
    assertFalse(allJsContent.hasNext());
  }
}

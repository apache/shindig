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

package org.apache.shindig.gadgets.spec;

import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.xml.XmlUtil;

import com.google.common.collect.Maps;

import org.junit.Test;
import org.w3c.dom.Element;

import java.net.URI;
import java.util.Map;

public class MessageBundleTest {
  private static final URI BUNDLE_URL = URI.create("http://example.org/m.xml");
  private static final Map<String, String> MESSAGES = Maps.newHashMap();
  private static final String XML;
  static {
    MESSAGES.put("hello", "world");
    MESSAGES.put("foo", "bar");
    StringBuilder buf = new StringBuilder();
    buf.append("<messagebundle>");
    for (Map.Entry<String, String> entry : MESSAGES.entrySet()) {
      buf.append("<msg name='").append(entry.getKey()).append("'>")
         .append(entry.getValue())
         .append("</msg>");
    }
    buf.append("</messagebundle>");
    XML = buf.toString();
  }

  @Test
  public void normalMessageBundleParsesOk() throws Exception {
    MessageBundle bundle = new MessageBundle(BUNDLE_URL, XML);
    assertEquals(MESSAGES, bundle.getMessages());
  }

  @Test(expected = SpecParserException.class)
  public void missingNameThrows() throws SpecParserException {
    String xml = "<messagebundle><msg>foo</msg></messagebundle>";
    MessageBundle bundle = new MessageBundle(BUNDLE_URL, xml);
  }

  @Test(expected = SpecParserException.class)
  public void malformedXmlThrows() throws SpecParserException {
    String xml = "</messagebundle>";
    MessageBundle bundle = new MessageBundle(BUNDLE_URL, xml);
  }

  @Test
  public void extractFromElement() throws Exception {
    Element element = XmlUtil.parse(XML);
    MessageBundle bundle = new MessageBundle(element);
    assertEquals(MESSAGES, bundle.getMessages());
  }

  @Test(expected = SpecParserException.class)
  public void extractFromElementsWithNoName() throws Exception {
    String xml = "<messagebundle><msg>foo</msg></messagebundle>";
    Element element = XmlUtil.parse(xml);
    MessageBundle bundle = new MessageBundle(element);
  }

  @Test
  public void toStringIsSane() throws Exception {
    MessageBundle b0 = new MessageBundle(BUNDLE_URL, XML);
    MessageBundle b1 = new MessageBundle(BUNDLE_URL, b0.toString());
    assertEquals(b0.getMessages(), b1.getMessages());
  }
}

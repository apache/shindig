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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;
import org.junit.Test;

public class LocaleSpecTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/foo.xml");

  @Test
  public void normalLocale() throws Exception {
    String xml = "<Locale" +
                 " lang=\"en\"" +
                 " country=\"US\"" +
                 " language_direction=\"rtl\"" +
                 " messages=\"http://example.org/msgs.xml\"/>";

    LocaleSpec locale = new LocaleSpec(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("en", locale.getLanguage());
    assertEquals("US", locale.getCountry());
    assertEquals("rtl", locale.getLanguageDirection());
    assertEquals("http://example.org/msgs.xml", locale.getMessages().toString());
    assertEquals(0, locale.getViews().size());
  }

  @Test
  public void viewLocale() throws Exception {
    String xml = "<Locale" +
                 " lang=\"en\"" +
                 " country=\"US\"" +
                 " language_direction=\"rtl\"" +
                 " messages=\"http://example.org/msgs.xml\"" +
                 " views=\"view1\"/>";

    LocaleSpec locale = new LocaleSpec(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("en", locale.getLanguage());
    assertEquals("US", locale.getCountry());
    assertEquals("rtl", locale.getLanguageDirection());
    assertEquals("http://example.org/msgs.xml", locale.getMessages().toString());
    assertEquals(1, locale.getViews().size());
    Object[] views = locale.getViews().toArray();
    assertEquals("view1",views[0].toString());
  }

  @Test
  public void relativeLocale() throws Exception {
    String xml = "<Locale messages=\"/test/msgs.xml\"/>";
    LocaleSpec locale = new LocaleSpec(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("http://example.org/test/msgs.xml", locale.getMessages().toString());
  }

  @Test
  public void defaultLanguageAndCountry() throws Exception {
    String xml = "<Locale/>";
    LocaleSpec locale = new LocaleSpec(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("all", locale.getLanguage());
    assertEquals("ALL", locale.getCountry());
  }

  @Test(expected = SpecParserException.class)
  public void invalidLanguageDirection() throws Exception {
    String xml = "<Locale language_direction=\"invalid\"/>";
    new LocaleSpec(XmlUtil.parse(xml), SPEC_URL);
  }

  @Test(expected = SpecParserException.class)
  public void invalidMessagesUrl() throws Exception {
    String xml = "<Locale messages=\"fobad@$%!fdf\"/>";
    new LocaleSpec(XmlUtil.parse(xml), SPEC_URL);
  }

  @Test
  public void nestedMessages() throws Exception {
    String msgName = "message name";
    String msgValue = "message value";
    String xml = "<Locale>" +
                 "<msg name=\"" + msgName + "\">" + msgValue + "</msg>" +
                 "</Locale>";
    LocaleSpec locale = new LocaleSpec(XmlUtil.parse(xml), SPEC_URL);
    assertEquals(msgValue, locale.getMessageBundle().getMessages().get(msgName));
  }

  @Test
  public void toStringIsSane() throws Exception {
    String xml = "<Locale lang='en' country='US' language_direction='rtl'" +
                 " messages='foo' views='view1, view2'>" +
                 "  <msg name='hello'>World</msg>" +
                 "  <msg name='foo'>Bar</msg>" +
                 "</Locale>";
    LocaleSpec loc = new LocaleSpec(XmlUtil.parse(xml), SPEC_URL);
    LocaleSpec loc2 = new LocaleSpec(XmlUtil.parse(loc.toString()), SPEC_URL);
    assertEquals(loc.getLanguage(), loc2.getLanguage());
    assertEquals(loc.getCountry(), loc2.getCountry());
    assertEquals(loc.getLanguageDirection(), loc2.getLanguageDirection());
    assertEquals(loc.getMessages(), loc2.getMessages());
    assertEquals(loc.getMessageBundle().getMessages(),
                 loc2.getMessageBundle().getMessages());
    assertEquals(loc.getViews().toString(),loc2.getViews().toString());
  }
}

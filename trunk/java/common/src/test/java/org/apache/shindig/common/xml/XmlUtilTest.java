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
package org.apache.shindig.common.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.uri.Uri;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

/**
 * Tests for XmlUtil
 */
public class XmlUtilTest {
  private final static String STRING_ATTR = "string";
  private final static String STRING_VALUE = "some string value";
  private final static String INT_ATTR = "int";
  private final static int INT_VALUE = 10;
  private final static String BOOL_TRUE_ATTR = "bool-true";
  private final static String BOOL_FALSE_ATTR = "bool-false";
  private final static String URI_ATTR = "uri";
  private final static Uri URI_VALUE = Uri.parse("http://example.org/file");
  private final static String URI_MALFORMED_ATTR = "uri-malformed";
  private final static String FAKE_ATTR = "fake";
  private final static String HTTPS_URI_ATTR = "httpsuri";
  private final static Uri HTTPS_URI_VALUE = Uri.parse("https://example.org");
  private final static String FTP_URI_ATTR = "ftpuri";
  private final static Uri FTP_URI_VALUE = Uri.parse("ftp://ftp.example.org");

  private final static String XML
      = "<Element " +
      STRING_ATTR + "='" + STRING_VALUE + "' " +
      INT_ATTR + "='" + INT_VALUE + "' " +
      BOOL_TRUE_ATTR + "='true' " +
      BOOL_FALSE_ATTR + "='false' " +
      URI_ATTR + "='" + URI_VALUE + "' " +
      URI_MALFORMED_ATTR + "='$#%$^$^$^$%$%!! ' " +
      HTTPS_URI_ATTR + "='" + HTTPS_URI_VALUE + "' " +
      FTP_URI_ATTR + "='" + FTP_URI_VALUE + "' " +
      "/>";

  private Element node;

  @Before
  public void makeElement() throws XmlException {
    node = XmlUtil.parse(XML);
  }

  @Test
  public void getAttribute() {
    assertEquals(STRING_VALUE, XmlUtil.getAttribute(node, STRING_ATTR));
    assertEquals(STRING_VALUE, XmlUtil.getAttribute(node, FAKE_ATTR, STRING_VALUE));
    assertNull("getAttribute must return null for undefined attributes.",
        XmlUtil.getAttribute(node, FAKE_ATTR));
  }

  @Test
  public void getIntAttribute() {
    assertEquals(INT_VALUE, XmlUtil.getIntAttribute(node, INT_ATTR));
    assertEquals(INT_VALUE, XmlUtil.getIntAttribute(node, FAKE_ATTR, INT_VALUE));
    assertEquals(INT_VALUE, XmlUtil.getIntAttribute(node, STRING_ATTR, INT_VALUE));
    assertEquals("getIntAttribute must return 0 for undefined attributes.",
        0, XmlUtil.getIntAttribute(node, FAKE_ATTR));
  }

  @Test
  public void getBoolAttribute() {
    assertTrue(XmlUtil.getBoolAttribute(node, BOOL_TRUE_ATTR));
    assertFalse(XmlUtil.getBoolAttribute(node, BOOL_FALSE_ATTR));
    assertTrue(XmlUtil.getBoolAttribute(node, FAKE_ATTR, true));
    assertFalse(XmlUtil.getBoolAttribute(node, FAKE_ATTR, false));
    assertFalse("getBoolAttribute must return false for undefined attributes.",
        XmlUtil.getBoolAttribute(node, FAKE_ATTR));
  }

  @Test
  public void getUriAttribute() {
    assertEquals(URI_VALUE, XmlUtil.getUriAttribute(node, URI_ATTR));
    assertEquals(URI_VALUE, XmlUtil.getUriAttribute(node, FAKE_ATTR, URI_VALUE));
    assertEquals(URI_VALUE, XmlUtil.getUriAttribute(node, URI_MALFORMED_ATTR, URI_VALUE));
    assertNull("getUriAttribute must return null for undefined attributes.",
        XmlUtil.getUriAttribute(node, FAKE_ATTR));
    assertEquals(FTP_URI_VALUE, XmlUtil.getUriAttribute(node, FTP_URI_ATTR));
  }

  @Test
  public void testHttpUriAttribute() {
    assertEquals(HTTPS_URI_VALUE, XmlUtil.getHttpUriAttribute(node, HTTPS_URI_ATTR, null));
    assertNull(XmlUtil.getHttpUriAttribute(node, FTP_URI_ATTR, null));
    assertEquals(HTTPS_URI_VALUE, XmlUtil.getHttpUriAttribute(node, FTP_URI_ATTR, null, HTTPS_URI_VALUE));
  }

  @Test(expected=XmlException.class)
  public void parseBadXmlThrows() throws XmlException {
    XmlUtil.parse("malformed xml");
  }
}

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
import static org.junit.Assert.fail;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlUtil;

import org.junit.Before;
import org.junit.Test;

public class OAuthServiceTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/g.xml");
  private OAuthService service;

  @Before
  public void setUp() {
    service = new OAuthService();
  }

  @Test
  public void testParseAuthorizeUrl() throws Exception {
    String xml = "<Authorization url='http://azn.example.com'/>";
    Uri url = service.parseAuthorizationUrl(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("http://azn.example.com", url.toString());
  }

  @Test(expected=SpecParserException.class)
  public void testParseAuthorizeUrl_nourl() throws Exception {
    String xml = "<Authorization/>";
    service.parseAuthorizationUrl(XmlUtil.parse(xml), SPEC_URL);
  }

  @Test
  public void testParseAuthorizeUrl_extraAttr() throws Exception {
    String xml = "<Authorization url='http://www.example.com' foo='bar'/>";
    Uri url = service.parseAuthorizationUrl(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("http://www.example.com", url.toString());
  }

  @Test
  public void testParseAuthorizeUrl_notHttp() throws Exception {
    OAuthService service = new OAuthService();
    String xml = "<Authorization url='ftp://www.example.com'/>";
    try {
      service.parseAuthorizationUrl(XmlUtil.parse(xml), SPEC_URL);
      fail("Should have rejected malformed Authorization element");
    } catch (SpecParserException e) {
      assertEquals("OAuth/Service/Authorization @url is not valid: ftp://www.example.com", e.getMessage());
    }
  }

  @Test
  public void testParseEndPoint() throws Exception {
    String xml = "<Request url='http://www.example.com'/>";
    OAuthService.EndPoint ep = service.parseEndPoint("Request", XmlUtil.parse(xml), SPEC_URL);
    assertEquals("http://www.example.com", ep.url.toString());
    assertEquals(OAuthService.Location.HEADER, ep.location);
    assertEquals(OAuthService.Method.GET, ep.method);
  }

  @Test
  public void testParseEndPoint_badlocation() throws Exception {
    try {
      String xml = "<Request url='http://www.example.com' method='GET' param_location='body'/>";
      service.parseEndPoint("Request", XmlUtil.parse(xml), SPEC_URL);
      fail("Should have thrown");
    } catch (SpecParserException e) {
      assertEquals("Unknown OAuth param_location: body", e.getMessage());
    }
  }

  @Test
  public void testParseEndPoint_nodefaults() throws Exception {
    String xml = "<Request url='http://www.example.com' method='GET' param_location='post-body'/>";
    OAuthService.EndPoint ep = service.parseEndPoint("Request", XmlUtil.parse(xml), SPEC_URL);
    assertEquals("http://www.example.com", ep.url.toString());
    assertEquals(OAuthService.Location.BODY, ep.location);
    assertEquals(OAuthService.Method.GET, ep.method);
  }

  @Test(expected=SpecParserException.class)
  public void testParseEndPoint_nourl() throws Exception {
    String xml = "<Request method='GET' param_location='post-body'/>";
    service.parseEndPoint("Request", XmlUtil.parse(xml), SPEC_URL);
  }

  @Test(expected=SpecParserException.class)
  public void testParseEndPoint_badurl() throws Exception {
    String xml = "<Request url='ftp://www.example.com' />";
    service.parseEndPoint("Request", XmlUtil.parse(xml), SPEC_URL);
  }

  @Test
  public void testParseService() throws Exception {
    String xml = "" +
        "<Service name='thename'>" +
        "   <Request url='http://request.example.com/foo'/>" +
        "   <Access url='http://access.example.com/bar'/>" +
        "   <Authorization url='http://azn.example.com/quux'/>" +
        "</Service>";
    OAuthService s = new OAuthService(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("thename", s.getName());
    assertEquals(OAuthService.Location.HEADER, s.getAccessUrl().location);
    assertEquals("http://azn.example.com/quux", s.getAuthorizationUrl().toString());
  }

  @Test
  public void testParseService_noname() throws Exception {
    String xml = "" +
        "<Service>" +
        "   <Request url='http://request.example.com/foo'/>" +
        "   <Access url='http://access.example.com/bar'/>" +
        "   <Authorization url='http://azn.example.com/quux'/>" +
        "</Service>";
    OAuthService s = new OAuthService(XmlUtil.parse(xml), SPEC_URL);
    assertEquals("", s.getName());
    assertEquals(OAuthService.Location.HEADER, s.getAccessUrl().location);
    assertEquals("http://azn.example.com/quux", s.getAuthorizationUrl().toString());
  }

  @Test
  public void testParseService_nodata() throws Exception {
    String xml = "<Service/>";
    try {
      new OAuthService(XmlUtil.parse(xml), SPEC_URL);
    } catch (SpecParserException e) {
      assertEquals("/OAuth/Service/Request is required", e.getMessage());
    }
  }

  @Test
  public void testParseService_reqonly() throws Exception {
    String xml = "<Service>" +
        "<Request url='http://www.example.com/request'/>" +
        "</Service>";
    try {
      new OAuthService(XmlUtil.parse(xml), SPEC_URL);
    } catch (SpecParserException e) {
      assertEquals("/OAuth/Service/Access is required", e.getMessage());
    }
  }
}

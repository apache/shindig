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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.URI;

import org.apache.shindig.common.xml.XmlUtil;
import org.junit.Before;
import org.junit.Test;

public class OAuthServiceTest {

  private OAuthService service;

  @Before
  public void setUp() {
    service = new OAuthService();
  }
  
  @Test
  public void testParseAuthorizeUrl() throws Exception {
    String xml = "<Authorization url='http://azn.example.com'/>";
    URI url = service.parseAuthorizationUrl(XmlUtil.parse(xml));
    assertEquals("http://azn.example.com", url.toString());
  }
  
  @Test
  public void testParseAuthorizeUrl_nourl() throws Exception {
    String xml = "<Authorization/>";
    try {
      service.parseAuthorizationUrl(XmlUtil.parse(xml));
      fail("Should have rejected malformed Authorization element");
    } catch (SpecParserException e) {
      assertEquals("OAuth/Service/Authorization @url is not valid: ", e.getMessage());
    }
  }
  
  @Test
  public void testParseAuthorizeUrl_extraAttr() throws Exception {
    String xml = "<Authorization url='http://www.example.com' foo='bar'/>";
    URI url = service.parseAuthorizationUrl(XmlUtil.parse(xml));
    assertEquals("http://www.example.com", url.toString());
  }
  
  @Test
  public void testParseAuthorizeUrl_notHttp() throws Exception {
    OAuthService service = new OAuthService();
    String xml = "<Authorization url='ftp://www.example.com'/>";
    try {
      service.parseAuthorizationUrl(XmlUtil.parse(xml));
      fail("Should have rejected malformed Authorization element");
    } catch (SpecParserException e) {
      assertEquals("OAuth/Service/Authorization @url is not valid: ftp://www.example.com", e.getMessage());
    }
  }
  
  @Test
  public void testParseEndPoint() throws Exception {
    String xml = "<Request url='http://www.example.com'/>";
    OAuthService.EndPoint ep = service.parseEndPoint("Request", XmlUtil.parse(xml));
    assertEquals("http://www.example.com", ep.url.toString());
    assertEquals(OAuthService.Location.header, ep.location);
    assertEquals(OAuthService.Method.POST, ep.method);
  }
  
  @Test
  public void testParseEndPoint_nodefaults() throws Exception {
    String xml = "<Request url='http://www.example.com' method='GET' param_location='body'/>";
    OAuthService.EndPoint ep = service.parseEndPoint("Request", XmlUtil.parse(xml));
    assertEquals("http://www.example.com", ep.url.toString());
    assertEquals(OAuthService.Location.body, ep.location);
    assertEquals(OAuthService.Method.GET, ep.method);    
  }
  
  @Test(expected=SpecParserException.class)
  public void testParseEndPoint_nourl() throws Exception {
    String xml = "<Request method='GET' param_location='body'/>";
    service.parseEndPoint("Request", XmlUtil.parse(xml));
  }
  
  @Test(expected=SpecParserException.class)
  public void testParseEndPoint_badurl() throws Exception {
    String xml = "<Request url='www.example.com' />";
    service.parseEndPoint("Request", XmlUtil.parse(xml));
  }
  
  @Test
  public void testParseService() throws Exception {
    String xml = "" +
    		"<Service name='thename'>" +
    		"   <Request url='http://request.example.com/foo'/>" +
    		"   <Access url='http://access.example.com/bar'/>" +
    		"   <Authorization url='http://azn.example.com/quux'/>" +
    		"</Service>";
    OAuthService s = new OAuthService(XmlUtil.parse(xml));
    assertEquals("thename", s.getName());
    assertEquals(OAuthService.Location.header, s.getAccessUrl().location);
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
    OAuthService s = new OAuthService(XmlUtil.parse(xml));
    assertEquals("", s.getName());
    assertEquals(OAuthService.Location.header, s.getAccessUrl().location);
    assertEquals("http://azn.example.com/quux", s.getAuthorizationUrl().toString());
  }
  
  @Test
  public void testParseService_nodata() throws Exception {
    String xml = "<Service/>";
    OAuthService s = new OAuthService(XmlUtil.parse(xml));
    assertEquals("", s.getName());
    assertNull(s.getAccessUrl());
    assertNull(s.getAuthorizationUrl());
    assertNull(s.getRequestUrl());
  }

  @Test
  public void testParseService_nameonly() throws Exception {
    String xml = "<Service name='foo'/>";
    OAuthService s = new OAuthService(XmlUtil.parse(xml));
    assertEquals("foo", s.getName());
    assertNull(s.getAccessUrl());
    assertNull(s.getAuthorizationUrl());
    assertNull(s.getRequestUrl());
  }

  @Test
  public void testParseService_reqonly() throws Exception {
    String xml = "<Service>" +
    		"<Request url='http://www.example.com/request'/>" +
    		"</Service>";
    OAuthService s = new OAuthService(XmlUtil.parse(xml));
    assertEquals("", s.getName());
    assertNull(s.getAccessUrl());
    assertNull(s.getAuthorizationUrl());
    assertEquals("http://www.example.com/request", s.getRequestUrl().url.toString());
  }
}

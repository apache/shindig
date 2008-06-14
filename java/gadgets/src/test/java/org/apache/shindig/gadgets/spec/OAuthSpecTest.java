/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.xml.XmlUtil;
import org.junit.Test;

/**
 * Tests for OAuthSpec
 */
public class OAuthSpecTest {

  @Test
  public void testOAuthSpec() throws Exception {
    String xml = "<OAuth><Service>" +
      "<Request url='http://www.example.com/request'/>" +
      "</Service></OAuth>";
    OAuthSpec oauth = new OAuthSpec(XmlUtil.parse(xml));
    assertEquals(1, oauth.getServices().size());
  }
  
  @Test
  public void testOAuthSpec_noservice() throws Exception {
    String xml = "<OAuth/>";
    OAuthSpec oauth = new OAuthSpec(XmlUtil.parse(xml));
    assertEquals(0, oauth.getServices().size());
  }
  
  
  @Test
  public void testOAuthSpec_threeservice() throws Exception {
    String xml = "<OAuth>" +
    		"<Service name='one'>" +
    		" <Request url='http://req.example.com' param_location='url' method='POST'/>" +
    		"</Service>" +
    		"<Service name='two'>" +
        " <Access url='http://two.example.com'/>" +
        "</Service>" +
    		"<Service name='three'>" +
        " <Request url='http://three.example.com' param_location='url' method='POST'/>" +
        "</Service>" +
    		"</OAuth>";
    OAuthSpec oauth = new OAuthSpec(XmlUtil.parse(xml));
    assertEquals("http://req.example.com",
        oauth.getServices().get("one").getRequestUrl().url.toString());
    assertEquals(OAuthService.Location.url,
        oauth.getServices().get("one").getRequestUrl().location);
    assertEquals("http://two.example.com",
        oauth.getServices().get("two").getAccessUrl().url.toString());
    assertEquals(OAuthService.Method.POST,
        oauth.getServices().get("three").getRequestUrl().method);
    assertNull(oauth.getServices().get("three").getAccessUrl());
    assertNull(oauth.getServices().get("three").getAuthorizationUrl());
  }
}

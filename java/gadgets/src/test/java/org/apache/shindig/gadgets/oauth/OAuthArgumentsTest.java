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
package org.apache.shindig.gadgets.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.shindig.common.testing.FakeHttpServletRequest;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.OAuthArguments.UseToken;
import org.apache.shindig.gadgets.spec.Auth;
import org.apache.shindig.gadgets.spec.Preload;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests parameter parsing
 */
public class OAuthArgumentsTest {
 
  @Test
  public void testInitFromPreload() throws Exception {
    String xml = "<Preload href='http://www.example.com' " +
    		"oauth_service_name='service' " +
    		"OAUTH_TOKEN_NAME='token' " +
    		"OAUTH_REQuest_token='requesttoken' " +
    		"oauth_request_token_secret='tokensecret' " +
    		"OAUTH_USE_TOKEN='never' " +
    		"/>";

    Preload preload = new Preload(XmlUtil.parse(xml));
    OAuthArguments params = new OAuthArguments(preload);
    assertEquals("service", params.getServiceName());
    assertEquals("token", params.getTokenName());
    assertEquals("requesttoken", params.getRequestToken());
    assertEquals("tokensecret", params.getRequestTokenSecret());
    assertEquals(UseToken.NEVER, params.getUseToken());
    assertNull(params.getOrigClientState());
    assertFalse(params.getBypassSpecCache());
  }
  
  private FakeHttpServletRequest makeDummyRequest() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    req.setParameter("OAUTH_USE_TOKEN", true, "never");
    req.setParameter("OAUTH_SERVICE_NAME", true, "service");
    req.setParameter("OAUTH_TOKEN_NAME", true, "token");
    req.setParameter("OAUTH_REQUEST_TOKEN", true, "reqtoken");
    req.setParameter("OAUTH_REQUEST_TOKEN_SECRET", true, "secret");
    req.setParameter("oauthState", true, "state");
    req.setParameter("bypassSpecCache", true, "1");
    req.setParameter("signOwner", true, "false");
    req.setParameter("signViewer", true, "false");
    return req;
  }
  
  @Test
  public void testInitFromRequest() throws Exception {
    HttpServletRequest req = makeDummyRequest();
    
    OAuthArguments args = new OAuthArguments(Auth.SIGNED, req);
    assertEquals(UseToken.NEVER, args.getUseToken());
    assertEquals("service", args.getServiceName());
    assertEquals("token", args.getTokenName());
    assertEquals("reqtoken", args.getRequestToken());
    assertEquals("secret", args.getRequestTokenSecret());
    assertEquals("state", args.getOrigClientState());
    assertEquals(true, args.getBypassSpecCache());
    assertEquals(false, args.getSignOwner());
    assertEquals(false, args.getSignViewer());
  }
  
  @Test
  public void testInitFromRequest_defaults() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();
    OAuthArguments args = new OAuthArguments(Auth.SIGNED, req);
    assertEquals(UseToken.NEVER, args.getUseToken());
    assertEquals("", args.getServiceName());
    assertEquals("", args.getTokenName());
    assertEquals(null, args.getRequestToken());
    assertEquals(null, args.getRequestTokenSecret());
    assertEquals(null, args.getOrigClientState());
    assertEquals(false, args.getBypassSpecCache());
    assertEquals(true, args.getSignOwner());
    assertEquals(true, args.getSignViewer());
  }
  
  @Test
  public void testInitFromRequest_oauthDefaults() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    OAuthArguments args = new OAuthArguments(Auth.OAUTH, req);
    assertEquals(UseToken.ALWAYS, args.getUseToken());
  }
  
  @Test
  public void testNoArgConstructorDefaults() throws Exception {
    OAuthArguments args = new OAuthArguments();
    assertEquals(UseToken.ALWAYS, args.getUseToken());
    assertEquals("", args.getServiceName());
    assertEquals("", args.getTokenName());
    assertEquals(null, args.getRequestToken());
    assertEquals(null, args.getRequestTokenSecret());
    assertEquals(null, args.getOrigClientState());
    assertEquals(false, args.getBypassSpecCache());
    assertEquals(false, args.getSignOwner());
    assertEquals(false, args.getSignViewer());
  }
  
  @Test
  public void testGetAndSet() throws Exception {
    OAuthArguments args = new OAuthArguments();
    args.setBypassSpecCache(true);
    assertEquals(true, args.getBypassSpecCache());
    
    args.setOrigClientState("thestate");
    assertEquals("thestate", args.getOrigClientState());
    
    args.setRequestToken("rt");
    assertEquals("rt", args.getRequestToken());
    
    args.setRequestTokenSecret("rts");
    assertEquals("rts", args.getRequestTokenSecret());
    
    args.setServiceName("s");
    assertEquals("s", args.getServiceName());
    
    args.setSignOwner(true);
    assertEquals(true, args.getSignOwner());
    
    args.setSignViewer(true);
    assertEquals(true, args.getSignViewer());
    
    args.setUseToken(UseToken.IF_AVAILABLE);
    assertEquals(UseToken.IF_AVAILABLE, args.getUseToken());
  }
  
  @Test
  public void testCopyConstructor() throws Exception {
    HttpServletRequest req = makeDummyRequest();
    OAuthArguments args = new OAuthArguments(Auth.OAUTH, req);
    args = new OAuthArguments(args);
    assertEquals(UseToken.NEVER, args.getUseToken());
    assertEquals("service", args.getServiceName());
    assertEquals("token", args.getTokenName());
    assertEquals("reqtoken", args.getRequestToken());
    assertEquals("secret", args.getRequestTokenSecret());
    assertEquals("state", args.getOrigClientState());
    assertEquals(true, args.getBypassSpecCache());
    assertEquals(false, args.getSignOwner());
    assertEquals(false, args.getSignViewer());
  }
  
  @Test
  public void testParseUseToken() throws Exception {
    FakeHttpServletRequest req = new FakeHttpServletRequest();
    req.setParameter("OAUTH_USE_TOKEN", "ALWAYS");
    OAuthArguments args = new OAuthArguments(Auth.SIGNED, req);
    assertEquals(UseToken.ALWAYS, args.getUseToken());
    
    req.setParameter("OAUTH_USE_TOKEN", "if_available");
    args = new OAuthArguments(Auth.SIGNED, req);
    assertEquals(UseToken.IF_AVAILABLE, args.getUseToken());     
    
    req.setParameter("OAUTH_USE_TOKEN", "never");
    args = new OAuthArguments(Auth.SIGNED, req);
    assertEquals(UseToken.NEVER, args.getUseToken());
    
    req.setParameter("OAUTH_USE_TOKEN", "");
    args = new OAuthArguments(Auth.SIGNED, req);
    assertEquals(UseToken.NEVER, args.getUseToken());
    
    req.setParameter("OAUTH_USE_TOKEN", "");
    args = new OAuthArguments(Auth.OAUTH, req);
    assertEquals(UseToken.ALWAYS, args.getUseToken());
    
    try {
      req.setParameter("OAUTH_USE_TOKEN", "stuff");
      new OAuthArguments(Auth.OAUTH, req);
      fail("Should have thrown");
    } catch (GadgetException e) {
      // good.
    }
  }
}

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
package org.apache.shindig.auth;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.FakeHttpServletRequest;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

public class AuthInfoTest extends Assert {

  @Test
  public void testToken() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();
    SecurityToken token = new FakeGadgetToken();
    
    AuthInfo info = new AuthInfo(req).setSecurityToken(token);
    
    assertEquals(token, info.getSecurityToken());
    // This should work when creating a new AuthInfo from the same request
    assertEquals(token, new AuthInfo(req).getSecurityToken());
  }

  @Test
  public void testAuthType() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();

    AuthInfo info = new AuthInfo(req).setAuthType("FakeAuth");

    assertEquals("FakeAuth", info.getAuthType());
    // This should work when creating a new AuthInfo from the same request
    assertEquals("FakeAuth", new AuthInfo(req).getAuthType());
  }

  @Test
  public void testBinding() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();
    SecurityToken token = new FakeGadgetToken();
    new AuthInfo(req).setSecurityToken(token).setAuthType("FakeAuth");
    
    Injector injector = Guice.createInjector(new TestModule(req));
    AuthInfo injected = injector.getInstance(AuthInfo.class);
    assertEquals(token, injected.getSecurityToken());
    assertEquals("FakeAuth", injected.getAuthType());
  }
  
  private static class TestModule extends AbstractModule {
    private HttpServletRequest req;
    
    public TestModule(HttpServletRequest req) {
      this.req = req;
    }
    @Override
    protected void configure() {
      bind(HttpServletRequest.class).toInstance(req);
    }
    
  }
}

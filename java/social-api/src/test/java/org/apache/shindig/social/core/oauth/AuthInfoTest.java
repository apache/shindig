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
package org.apache.shindig.social.core.oauth;

import junit.framework.TestCase;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.testing.FakeHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

public class AuthInfoTest extends TestCase {

  public void testToken() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();
    SecurityToken token = new FakeGadgetToken();
    AuthInfo.setSecurityToken(req, token);
    assertEquals(token, AuthInfo.getSecurityToken(req));
  }
  
  public void testAuthType() throws Exception {
    HttpServletRequest req = new FakeHttpServletRequest();
    AuthInfo.setAuthType(req, "FakeAuth");
    assertEquals("FakeAuth", AuthInfo.getAuthType(req));
  }
}

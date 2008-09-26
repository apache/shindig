/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.servlet;

import static org.easymock.EasyMock.expect;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.AuthInfo;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetContext;

import java.util.Locale;

public class HttpGadgetContextTest extends ServletTestFixture {
  public void testIgnoreCacheParam() {
    expect(request.getParameter("nocache")).andReturn(Integer.toString(Integer.MAX_VALUE));
    replay();
    GadgetContext context = new HttpGadgetContext(request);
    assertEquals(true, context.getIgnoreCache());
  }

  public void testLocale() {
    expect(request.getParameter("lang")).andReturn(Locale.CHINA.getLanguage());
    expect(request.getParameter("country")).andReturn(Locale.CHINA.getCountry());
    replay();
    GadgetContext context = new HttpGadgetContext(request);
    assertEquals(Locale.CHINA, context.getLocale());
  }

  public void testDebug() {
    expect(request.getParameter("debug")).andReturn("1");
    replay();
    GadgetContext context = new HttpGadgetContext(request);
    assertEquals(true, context.getDebug());
  }

  public void testGetParameter() {
    expect(request.getParameter("foo")).andReturn("bar");
    replay();
    GadgetContext context = new HttpGadgetContext(request);
    assertEquals("bar", context.getParameter("foo"));
  }

  public void testGetHost() {
    expect(request.getHeader("Host")).andReturn("foo.org");
    replay();
    GadgetContext context = new HttpGadgetContext(request);
    assertEquals("foo.org", context.getHost());
  }

  public void testGetSecurityToken() throws Exception {
    SecurityToken expected = new AnonymousSecurityToken();
    expect(request.getAttribute(AuthInfo.Attribute.SECURITY_TOKEN.getId())).andReturn(expected);
    replay();
    GadgetContext context = new HttpGadgetContext(request);
    assertEquals(expected, context.getToken());
  }
}
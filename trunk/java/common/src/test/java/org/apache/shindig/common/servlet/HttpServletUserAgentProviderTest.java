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
package org.apache.shindig.common.servlet;

import com.google.inject.Provider;

import static org.easymock.EasyMock.expect;

import org.apache.shindig.common.EasyMockTestCase;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

/**
 * Simple test for HttpServletUserAgentProvider.
 */
public class HttpServletUserAgentProviderTest extends EasyMockTestCase {
  private UserAgent.Parser parser = new PassThroughUAParser();

  @Test
  public void testProviderWorks() {
    String agentVersion = "AGENT_VERSION";
    HttpServletRequest req = mock(HttpServletRequest.class);
    expect(req.getHeader("User-Agent")).andReturn(agentVersion).once();
    replay();
    HttpServletUserAgentProvider provider = new HttpServletUserAgentProvider(
        parser, new HttpServletRequestProvider(req));
    UserAgent entry = provider.get();
    assertEquals(UserAgent.Browser.OTHER, entry.getBrowser());
    assertEquals(agentVersion, entry.getVersion());
    verify();
  }

  @Test
  public void testNoRequestGetsNull() {
    HttpServletUserAgentProvider provider = new HttpServletUserAgentProvider(
        parser, new HttpServletRequestProvider(null));
    assertNull(provider.get());
  }

  @Test
  public void testNoUserAgentGetsNull() {
    HttpServletRequest req = mock(HttpServletRequest.class);
    expect(req.getHeader("User-Agent")).andReturn(null).once();
    replay();
    HttpServletUserAgentProvider provider = new HttpServletUserAgentProvider(
        parser, new HttpServletRequestProvider(req));
    assertNull(provider.get());
    verify();
  }

  private static class HttpServletRequestProvider implements Provider<HttpServletRequest> {
    private HttpServletRequest req;

    private HttpServletRequestProvider(HttpServletRequest req) {
      this.req = req;
    }

    public HttpServletRequest get() {
      return req;
    }
  }

  private static class PassThroughUAParser implements UserAgent.Parser {
    public UserAgent parse(String agentVersion) {
      return new UserAgent(UserAgent.Browser.OTHER, agentVersion);
    }
  }
}

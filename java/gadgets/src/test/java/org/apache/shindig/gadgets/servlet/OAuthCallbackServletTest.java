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
package org.apache.shindig.gadgets.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for OAuth callback servlet.
 */
public class OAuthCallbackServletTest {

  private final ServletTestFixture fixture = new ServletTestFixture();

  @Test
  public void testServlet() throws Exception {
    OAuthCallbackServlet servlet = new OAuthCallbackServlet();
    fixture.replay();
    servlet.doGet(fixture.request, fixture.recorder);
    fixture.verify();
    assertEquals("text/html; charset=UTF-8", fixture.recorder.getContentType());
    String body = fixture.recorder.getResponseAsString();
    assertTrue("body is " + body, body.indexOf("window.close()") != -1);
  }
}

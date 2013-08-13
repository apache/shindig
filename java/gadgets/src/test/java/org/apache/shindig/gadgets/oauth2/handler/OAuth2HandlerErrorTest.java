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
package org.apache.shindig.gadgets.oauth2.handler;

import org.apache.shindig.gadgets.oauth2.OAuth2Error;

import org.junit.Assert;
import org.junit.Test;

public class OAuth2HandlerErrorTest {
  @Test
  public void testOAuth2HandlerError1() throws Exception {
    final OAuth2Error error = OAuth2Error.AUTHENTICATION_PROBLEM;
    final String contextMessage = "";
    final Exception cause = new Exception();

    final OAuth2HandlerError result = new OAuth2HandlerError(error, contextMessage, cause);

    Assert.assertNotNull(result);
    Assert.assertEquals("", result.getContextMessage());
    Assert.assertEquals(
            "org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerError : AUTHENTICATION_PROBLEM :  :  : :java.lang.Exception",
            result.toString());
  }

  @Test
  public void testGetCause1() throws Exception {
    final OAuth2HandlerError fixture = new OAuth2HandlerError(OAuth2Error.AUTHENTICATION_PROBLEM,
            "", new Exception());

    final Exception result = fixture.getCause();

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getMessage());
    Assert.assertEquals(null, result.getLocalizedMessage());
    Assert.assertEquals("java.lang.Exception", result.toString());
    Assert.assertEquals(null, result.getCause());
  }

  @Test
  public void testGetContextMessage1() throws Exception {
    final OAuth2HandlerError fixture = new OAuth2HandlerError(OAuth2Error.AUTHENTICATION_PROBLEM,
            "", new Exception());

    final String result = fixture.getContextMessage();

    Assert.assertEquals("", result);
  }

  @Test
  public void testGetError1() throws Exception {
    final OAuth2HandlerError fixture = new OAuth2HandlerError(OAuth2Error.AUTHENTICATION_PROBLEM,
            "", new Exception());

    final OAuth2Error result = fixture.getError();

    Assert.assertNotNull(result);
    Assert.assertEquals("authentication_problem", result.getErrorCode());
    Assert.assertEquals("AUTHENTICATION_PROBLEM", result.name());
    Assert.assertEquals(2, result.ordinal());
    Assert.assertEquals("AUTHENTICATION_PROBLEM", result.toString());
  }

  @Test
  public void testToString1() throws Exception {
    final OAuth2HandlerError fixture = new OAuth2HandlerError(OAuth2Error.AUTHENTICATION_PROBLEM,
            "", new Exception());

    final String result = fixture.toString();

    Assert.assertEquals(
            "org.apache.shindig.gadgets.oauth2.handler.OAuth2HandlerError : AUTHENTICATION_PROBLEM :  :  : :java.lang.Exception",
            result);
  }
}

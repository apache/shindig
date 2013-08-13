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
package org.apache.shindig.gadgets.oauth2;

import org.junit.Assert;
import org.junit.Test;

public class OAuth2RequestExceptionTest {
  @Test
  public void testOAuth2RequestException_1() throws Exception {
    final OAuth2Error error = OAuth2Error.AUTHENTICATION_PROBLEM;
    final String errorText = "";
    final Throwable cause = new Throwable();

    final OAuth2RequestException result = new OAuth2RequestException(error, errorText, cause);

    Assert.assertNotNull(result);
  }

  @Test
  public void testGetError_1() throws Exception {
    final OAuth2RequestException fixture = new OAuth2RequestException(
        OAuth2Error.AUTHENTICATION_PROBLEM, "", new Throwable());

    final OAuth2Error result = fixture.getError();

    Assert.assertNotNull(result);
    Assert.assertEquals("authentication_problem", result.getErrorCode());
    Assert.assertEquals("AUTHENTICATION_PROBLEM", result.name());
    Assert.assertEquals(2, result.ordinal());
    Assert.assertEquals("AUTHENTICATION_PROBLEM", result.toString());
  }

}

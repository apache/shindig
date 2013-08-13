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
package org.apache.shindig.gadgets.oauth2.persistence;

import org.junit.Assert;
import org.junit.Test;

public class OAuth2PersistenceExceptionTest {
  @Test
  public void testOAuth2PersistenceException_1() throws Exception {
    final Exception cause = new Exception();

    final OAuth2PersistenceException result = new OAuth2PersistenceException(cause);

    Assert.assertNotNull(result);
    Assert.assertEquals("java.lang.Exception", result.getMessage());
    Assert.assertEquals("java.lang.Exception", result.getLocalizedMessage());
    Assert
        .assertEquals(
            "org.apache.shindig.gadgets.oauth2.persistence.OAuth2PersistenceException: java.lang.Exception",
            result.toString());
  }
}

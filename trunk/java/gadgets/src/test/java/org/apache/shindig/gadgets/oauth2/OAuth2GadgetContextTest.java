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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OAuth2GadgetContextTest extends MockUtils {
  private static SecurityToken securityToken;
  private static OAuth2Arguments arguments;
  private static Uri gadgetUri;

  @Before
  public void setUp() throws Exception {
    OAuth2GadgetContextTest.securityToken = MockUtils.getDummySecurityToken(MockUtils.USER,
        MockUtils.USER, MockUtils.GADGET_URI1);
    OAuth2GadgetContextTest.arguments = MockUtils.getDummyArguments();
    OAuth2GadgetContextTest.gadgetUri = Uri.parse(MockUtils.GADGET_URI1);
  }

  @Test
  public void testOAuth2GadgetContext_1() throws Exception {
    final OAuth2GadgetContext result = new OAuth2GadgetContext(
        OAuth2GadgetContextTest.securityToken, OAuth2GadgetContextTest.arguments,
        OAuth2GadgetContextTest.gadgetUri);

    Assert.assertNotNull(result);
    Assert.assertEquals(false, result.getCajoled());
    Assert.assertEquals("", result.getContainer());
    Assert.assertEquals(false, result.getDebug());
    Assert.assertEquals(null, result.getHost());
    Assert.assertEquals(false, result.getIgnoreCache());
    Assert.assertNotNull(result.getLocale());
    Assert.assertEquals(0, result.getModuleId());
    Assert.assertNotNull(result.getRenderingContext());
    Assert.assertEquals(null, result.getRepository());
    Assert.assertEquals(false, result.getSanitize());
    Assert.assertEquals(MockUtils.SCOPE, result.getScope());
    Assert.assertEquals(OAuth2GadgetContextTest.securityToken, result.getToken());
    Assert.assertEquals(MockUtils.GADGET_URI1, result.getUrl().toString());
    Assert.assertEquals(null, result.getUserAgent());
    Assert.assertEquals(null, result.getUserIp());
    Assert.assertNotNull(result.getUserPrefs());
  }
}

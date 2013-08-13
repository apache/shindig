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
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GadgetOAuth2TokenStoreTest extends MockUtils {
  private static GadgetOAuth2TokenStore gts;
  private static SecurityToken securityToken;
  private static Uri gadgetUri = Uri.parse(MockUtils.GADGET_URI1);
  private static OAuth2Arguments arguments;

  @Before
  public void setUp() throws Exception {
    GadgetOAuth2TokenStoreTest.securityToken = MockUtils.getDummySecurityToken(MockUtils.USER,
        MockUtils.USER, MockUtils.GADGET_URI1);
    GadgetOAuth2TokenStoreTest.arguments = MockUtils.getDummyArguments();

    final OAuth2Store store = MockUtils.getDummyStore();
    final GadgetSpecFactory specFactory = MockUtils.getDummySpecFactory();

    GadgetOAuth2TokenStoreTest.gts = new GadgetOAuth2TokenStore(store, specFactory);
  }

  @Test
  public void testGadgetOAuth2TokenStore_1() throws Exception {
    Assert.assertNotNull(GadgetOAuth2TokenStoreTest.gts);
  }

  @Test
  public void testGetOAuth2Accessor_1() throws Exception {
    final OAuth2Accessor result = GadgetOAuth2TokenStoreTest.gts.getOAuth2Accessor(null,
        GadgetOAuth2TokenStoreTest.arguments, GadgetOAuth2TokenStoreTest.gadgetUri);
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isErrorResponse());
    Assert.assertEquals(OAuth2Error.GET_OAUTH2_ACCESSOR_PROBLEM, result.getError());
    Assert.assertTrue(result.getErrorContextMessage().startsWith(
        "OAuth2Accessor missing a param"));
  }

  @Test
  public void testGetOAuth2Accessor_2() throws Exception {
    final OAuth2Accessor result = GadgetOAuth2TokenStoreTest.gts.getOAuth2Accessor(
        GadgetOAuth2TokenStoreTest.securityToken, GadgetOAuth2TokenStoreTest.arguments, null);
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isErrorResponse());
    Assert.assertEquals(OAuth2Error.GET_OAUTH2_ACCESSOR_PROBLEM, result.getError());
    Assert.assertTrue(result.getErrorContextMessage().startsWith(
        "OAuth2Accessor missing a param"));
  }

  @Test
  public void testGetOAuth2Accessor_3() throws Exception {
    final OAuth2Accessor result = GadgetOAuth2TokenStoreTest.gts.getOAuth2Accessor(
        GadgetOAuth2TokenStoreTest.securityToken, GadgetOAuth2TokenStoreTest.arguments,
        Uri.parse("bad"));

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isErrorResponse());
    Assert.assertEquals(OAuth2Error.NO_GADGET_SPEC, result.getError());
    Assert.assertTrue(result.getErrorContextMessage().startsWith("gadgetUri ="));
  }

  @Test
  public void testGetOAuth2Accessor_4() throws Exception {
    final OAuth2Accessor result = GadgetOAuth2TokenStoreTest.gts.getOAuth2Accessor(
        GadgetOAuth2TokenStoreTest.securityToken, GadgetOAuth2TokenStoreTest.arguments,
        Uri.parse(MockUtils.GADGET_URI1));

    Assert.assertNotNull(result);
    Assert.assertFalse(result.isErrorResponse());
    Assert.assertEquals(null, result.getAccessToken());
    Assert.assertEquals(MockUtils.AUTHORIZE_URL, result.getAuthorizationUrl());
    Assert.assertEquals(OAuth2Message.BASIC_AUTH_TYPE, result.getClientAuthenticationType());
    Assert.assertEquals(MockUtils.CLIENT_ID1, result.getClientId());
    Assert.assertEquals(MockUtils.GADGET_URI1, result.getGadgetUri());
    Assert.assertEquals(OAuth2Message.AUTHORIZATION, result.getGrantType());
    Assert.assertEquals(MockUtils.REDIRECT_URI, result.getRedirectUri());
    Assert.assertEquals(null, result.getRefreshToken());
    Assert.assertEquals(MockUtils.SCOPE, result.getScope());
    Assert.assertEquals(MockUtils.SERVICE_NAME, result.getServiceName());
    Assert.assertEquals(MockUtils.TOKEN_URL, result.getTokenUrl());
    Assert.assertEquals(OAuth2Accessor.Type.CONFIDENTIAL, result.getType());
    Assert.assertEquals(MockUtils.USER, result.getUser());
    Assert.assertTrue(result.isValid());
    Assert.assertFalse(result.isAllowModuleOverrides());
    Assert.assertFalse(result.isErrorResponse());
    Assert.assertFalse(result.isRedirecting());
    Assert.assertFalse(result.isUrlParameter());
    Assert.assertTrue(result.isAuthorizationHeader());

  }

  @Test
  public void testGetOAuth2Store_1() throws Exception {
    final OAuth2Store result = GadgetOAuth2TokenStoreTest.gts.getOAuth2Store();

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.getDummyStore(), result);
  }

}

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

import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Accessor;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OAuth2ClientTest extends MockUtils {
  private static OAuth2Client client1;
  private static OAuth2Client client2;

  @Before
  public void setUp() throws Exception {
    OAuth2ClientTest.client1 = MockUtils.getClient_Code_Confidential();
    OAuth2ClientTest.client2 = MockUtils.getClient_Code_Public();
  }

  @Test
  public void testOAuth2Client_1() throws Exception {
    final OAuth2Client result = new OAuth2Client(MockUtils.getDummyEncrypter());

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getAuthorizationUrl());
    Assert.assertEquals(null, result.getClientAuthenticationType());
    Assert.assertEquals(null, result.getClientId());
    Assert.assertEquals(null, result.getClientSecret());
    Assert.assertEquals(null, result.getEncryptedSecret());
    Assert.assertEquals(null, result.getGadgetUri());
    Assert.assertEquals("NONE", result.getGrantType());
    Assert.assertEquals(null, result.getRedirectUri());
    Assert.assertEquals(null, result.getServiceName());
    Assert.assertEquals(null, result.getTokenUrl());
    Assert.assertEquals(false, result.isAllowModuleOverride());
    Assert.assertEquals(false, result.isAuthorizationHeader());
    Assert.assertEquals(false, result.isUrlParameter());
    Assert
    .assertEquals(
        "org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2ClientImpl: serviceName = null , redirectUri = null , gadgetUri = null , clientId = null , grantType = NONE , type = UNKNOWN , grantType = NONE , tokenUrl = null , authorizationUrl = null , this.clientAuthenticationType = null , this.sharedToken = false, this.allowedDomains = []",
        result.toString());
  }

  @Test
  public void testEquals_1() throws Exception {

    final OAuth2Client obj = new OAuth2Client(MockUtils.getDummyEncrypter());
    obj.setAuthorizationUrl(MockUtils.AUTHORIZE_URL);
    obj.setClientAuthenticationType(OAuth2Message.BASIC_AUTH_TYPE);
    obj.setServiceName(MockUtils.SERVICE_NAME);
    obj.setRedirectUri(MockUtils.REDIRECT_URI);
    obj.setGrantType(OAuth2Message.AUTHORIZATION);
    obj.setAllowModuleOverride(true);
    obj.setAuthorizationHeader(true);
    obj.setTokenUrl(MockUtils.TOKEN_URL);
    obj.setGadgetUri(MockUtils.GADGET_URI1);
    obj.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    obj.setUrlParameter(false);
    obj.setClientSecret(MockUtils.CLIENT_SECRET1.getBytes("UTF-8"));
    obj.setClientId(MockUtils.CLIENT_ID1);

    final boolean result = OAuth2ClientTest.client1.equals(obj);

    Assert.assertTrue(result);
  }

  @Test
  public void testEquals_2() throws Exception {
    final Object obj = new Object();

    boolean result = OAuth2ClientTest.client1.equals(obj);

    Assert.assertFalse(result);

    result = OAuth2ClientTest.client1.equals(OAuth2ClientTest.client2);

    Assert.assertFalse(result);
  }

  @Test
  public void testEquals_3() throws Exception {
    final boolean result = OAuth2ClientTest.client1.equals(null);

    Assert.assertFalse(result);
  }

  @Test
  public void testGetAuthorizationUrl_1() throws Exception {
    final String result = OAuth2ClientTest.client1.getAuthorizationUrl();

    Assert.assertNotNull(result);
    Assert.assertEquals(MockUtils.AUTHORIZE_URL, result);
  }

  @Test
  public void testGetClientAuthenticationType_1() throws Exception {
    final String result = OAuth2ClientTest.client1.getClientAuthenticationType();

    Assert.assertNotNull(result);
    Assert.assertEquals(OAuth2Message.BASIC_AUTH_TYPE, result);
  }

  @Test
  public void testGetClientId_1() throws Exception {
    final String result = OAuth2ClientTest.client1.getClientId();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.CLIENT_ID1, result);
  }

  @Test
  public void testGetClientSecret_1() throws Exception {
    final byte[] result = OAuth2ClientTest.client1.getClientSecret();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.CLIENT_SECRET1, new String(result, "UTF-8"));
  }

  @Test
  public void testGetEncryptedSecret_1() throws Exception {
    final byte[] result = OAuth2ClientTest.client1.getEncryptedSecret();

    Assert.assertNotNull(result);

    Assert.assertEquals("dmjfouTfdsfu2", new String(result, "UTF-8"));
  }

  @Test
  public void testGetEncrypter_1() throws Exception {
    final OAuth2Encrypter result = OAuth2ClientTest.client1.getEncrypter();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.getDummyEncrypter(), result);
  }

  @Test
  public void testGetGadgetUri_1() throws Exception {
    final String result = OAuth2ClientTest.client1.getGadgetUri();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.GADGET_URI1, result);
  }

  @Test
  public void testGetGrantType_1() throws Exception {
    final String result = OAuth2ClientTest.client1.getGrantType();

    Assert.assertNotNull(result);

    Assert.assertEquals(OAuth2Message.AUTHORIZATION_CODE, result);
  }

  @Test
  public void testGetRedirectUri_1() throws Exception {
    final String result = OAuth2ClientTest.client1.getRedirectUri();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.REDIRECT_URI, result);
  }

  @Test
  public void testGetServiceName_1() throws Exception {
    final String result = OAuth2ClientTest.client1.getServiceName();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.SERVICE_NAME, result);
  }

  @Test
  public void testGetTokenUrl_1() throws Exception {
    final String result = OAuth2ClientTest.client1.getTokenUrl();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.TOKEN_URL, result);
  }

  @Test
  public void testGetType_1() throws Exception {
    final org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type result = OAuth2ClientTest.client1
        .getType();

    Assert.assertNotNull(result);

    Assert.assertEquals(OAuth2Accessor.Type.CONFIDENTIAL, result);
  }

  @Test
  public void testHashCode_1() throws Exception {
    final int result = OAuth2ClientTest.client1.hashCode();

    Assert.assertEquals(-1410040560, result);
  }

  @Test
  public void testHashCode_2() throws Exception {
    final int result = OAuth2ClientTest.client2.hashCode();

    Assert.assertEquals(-1410040559, result);
  }

  @Test
  public void testHashCode_3() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri((String) null);
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");

    final int result = fixture.hashCode();

    Assert.assertEquals(0, result);
  }

  @Test
  public void testIsAllowModuleOverride_1() throws Exception {
    final boolean result = OAuth2ClientTest.client1.isAllowModuleOverride();

    Assert.assertTrue(result);
  }

  @Test
  public void testIsAllowModuleOverride_2() throws Exception {
    final boolean result = OAuth2ClientTest.client2.isAllowModuleOverride();

    Assert.assertFalse(result);
  }

  @Test
  public void testIsAuthorizationHeader_1() throws Exception {
    final boolean result = OAuth2ClientTest.client1.isAuthorizationHeader();

    Assert.assertTrue(result);
  }

  @Test
  public void testIsAuthorizationHeader_2() throws Exception {
    final boolean result = OAuth2ClientTest.client2.isAuthorizationHeader();

    Assert.assertFalse(result);
  }

  @Test
  public void testIsUrlParameter_1() throws Exception {
    final boolean result = OAuth2ClientTest.client1.isUrlParameter();

    Assert.assertFalse(result);
  }

  @Test
  public void testIsUrlParameter_2() throws Exception {
    final boolean result = OAuth2ClientTest.client2.isUrlParameter();

    Assert.assertTrue(result);
  }

  @Test
  public void testSetAllowModuleOverride_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final boolean alllowModuleOverride = true;

    fixture.setAllowModuleOverride(alllowModuleOverride);
  }

  @Test
  public void testSetAuthorizationHeader_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final boolean authorizationHeader = true;

    fixture.setAuthorizationHeader(authorizationHeader);
  }

  @Test
  public void testSetAuthorizationUrl_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final String authorizationUrl = "";

    fixture.setAuthorizationUrl(authorizationUrl);
  }

  @Test
  public void testSetClientAuthenticationType_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final String clientAuthenticationType = "";

    fixture.setClientAuthenticationType(clientAuthenticationType);
  }

  @Test
  public void testSetClientId_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final String clientId = "";

    fixture.setClientId(clientId);
  }

  @Test
  public void testSetClientSecret_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final byte[] secret = new byte[] {};

    fixture.setClientSecret(secret);
  }

  @Test
  public void testSetClientSecret_2() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final byte[] secret = new byte[] {};

    fixture.setClientSecret(secret);
  }

  @Test
  public void testSetEncryptedSecret_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final byte[] encryptedSecret = new byte[] {};

    fixture.setEncryptedSecret(encryptedSecret);
  }

  @Test
  public void testSetEncryptedSecret_2() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final byte[] encryptedSecret = new byte[] {};

    fixture.setEncryptedSecret(encryptedSecret);
  }

  @Test
  public void testSetGadgetUri_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final String gadgetUri = "";

    fixture.setGadgetUri(gadgetUri);
  }

  @Test
  public void testSetGrantType_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final String grantType = "";

    fixture.setGrantType(grantType);
  }

  @Test
  public void testSetRedirectUri_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final String redirectUri = "";

    fixture.setRedirectUri(redirectUri);
  }

  @Test
  public void testSetServiceName_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final String serviceName = "";

    fixture.setServiceName(serviceName);
  }

  @Test
  public void testSetTokenUrl_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final String tokenUrl = "";

    fixture.setTokenUrl(tokenUrl);
  }

  @Test
  public void testSetType_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type type = org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL;

    fixture.setType(type);
  }

  @Test
  public void testSetUrlParameter_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");
    final boolean urlParameter = true;

    fixture.setUrlParameter(urlParameter);
  }

  @Test
  public void testToString_1() throws Exception {
    final OAuth2Client fixture = new OAuth2Client(MockUtils.getDummyEncrypter());
    fixture.setAuthorizationUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setServiceName("");
    fixture.setRedirectUri("");
    fixture.setGrantType("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setAllowModuleOverride(true);
    fixture.setAuthorizationHeader(true);
    fixture.setTokenUrl("");
    fixture.setGadgetUri("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Accessor.Type.CONFIDENTIAL);
    fixture.setUrlParameter(true);
    fixture.setClientSecret(new byte[] {});
    fixture.setClientId("");

    final String result = fixture.toString();

    Assert.assertNotNull(result);
  }
}

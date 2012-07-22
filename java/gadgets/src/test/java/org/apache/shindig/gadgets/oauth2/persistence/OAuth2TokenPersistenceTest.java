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

import static org.junit.Assert.assertArrayEquals;

import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.apache.shindig.gadgets.oauth2.OAuth2Message;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class OAuth2TokenPersistenceTest extends MockUtils {
  private static OAuth2TokenPersistence accessToken;
  private static OAuth2TokenPersistence refreshToken;

  @Before
  public void setUp() throws Exception {
    OAuth2TokenPersistenceTest.accessToken = MockUtils.getAccessToken();
    OAuth2TokenPersistenceTest.refreshToken = MockUtils.getRefreshToken();
  }

  @Test
  public void testOAuth2TokenPersistence_1() throws Exception {
    final OAuth2TokenPersistence result = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());

    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getEncryptedMacSecret());
    Assert.assertEquals(null, result.getEncryptedSecret());
    Assert.assertEquals(0L, result.getExpiresAt());
    Assert.assertEquals(null, result.getGadgetUri());
    Assert.assertEquals(0L, result.getIssuedAt());
    Assert.assertEquals(null, result.getMacAlgorithm());
    Assert.assertEquals(null, result.getMacExt());
    Assert.assertEquals(null, result.getMacSecret());
    Assert.assertEquals(null, result.getScope());
    Assert.assertEquals(null, result.getSecret());
    Assert.assertEquals(null, result.getServiceName());
    Assert.assertEquals("Bearer", result.getTokenType());
    Assert.assertEquals(null, result.getType());
    Assert.assertEquals(null, result.getUser());
    Assert
        .assertEquals(
            "org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2TokenImpl: serviceName = null , user = null , gadgetUri = null , scope = null , tokenType = Bearer , issuedAt = 0 , expiresAt = 0 , type = null",
            result.toString());
  }

  @Test
  public void testEquals_1() throws Exception {
    final OAuth2TokenPersistence obj = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    obj.setServiceName(MockUtils.SERVICE_NAME);
    obj.setUser(MockUtils.USER);
    obj.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    obj.setIssuedAt(0L);
    obj.setEncryptedMacSecret(new byte[] {});
    obj.setMacAlgorithm("");
    obj.setScope(MockUtils.SCOPE);
    obj.setExpiresAt(1L);
    obj.setMacSecret(new byte[] {});
    obj.setSecret(new byte[] {});
    obj.setGadgetUri(MockUtils.GADGET_URI1);
    obj.setMacExt("");
    obj.setTokenType(OAuth2Message.BEARER_TOKEN_TYPE);

    final boolean result = OAuth2TokenPersistenceTest.accessToken.equals(obj);

    Assert.assertTrue(result);
  }

  @Test
  public void testEquals_2() throws Exception {
    final boolean result = OAuth2TokenPersistenceTest.accessToken
        .equals(OAuth2TokenPersistenceTest.refreshToken);

    Assert.assertFalse(result);
  }

  @Test
  public void testEquals_3() throws Exception {
    Assert.assertFalse(OAuth2TokenPersistenceTest.accessToken.equals(new Object()));
  }

  @Test
  public void testEquals_4() throws Exception {
    Assert.assertFalse(OAuth2TokenPersistenceTest.accessToken.equals(null));
  }

  @Test
  public void testGetEncryptedMacSecret_1() throws Exception {
    final byte[] result = OAuth2TokenPersistenceTest.accessToken.getEncryptedMacSecret();

    Assert.assertNotNull(result);
    Assert.assertEquals("", new String(result, "UTF-8"));
  }

  @Test
  public void testGetEncryptedSecret_1() throws Exception {
    final byte[] result = OAuth2TokenPersistenceTest.accessToken.getEncryptedSecret();

    Assert.assertNotNull(result);
    Assert.assertEquals("bddfttTfdsfu", new String(result, "UTF-8"));
  }

  @Test
  public void testGetExpiresAt_1() throws Exception {
    final long result = OAuth2TokenPersistenceTest.accessToken.getExpiresAt();

    Assert.assertEquals(1L, result);
  }

  @Test
  public void testGetGadgetUri_1() throws Exception {
    final String result = OAuth2TokenPersistenceTest.accessToken.getGadgetUri();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.GADGET_URI1, result);
  }

  @Test
  public void testGetIssuedAt_1() throws Exception {
    final long result = OAuth2TokenPersistenceTest.accessToken.getIssuedAt();

    Assert.assertEquals(0L, result);
  }

  @Test
  public void testGetMacAlgorithm_1() throws Exception {
    final String result = OAuth2TokenPersistenceTest.accessToken.getMacAlgorithm();

    Assert.assertNotNull(result);

    Assert.assertEquals("", result);
  }

  @Test
  public void testGetMacExt_1() throws Exception {
    final String result = OAuth2TokenPersistenceTest.accessToken.getMacExt();

    Assert.assertNotNull(result);

    Assert.assertEquals("", result);
  }

  @Test
  public void testGetMacSecret_1() throws Exception {
    final byte[] result = OAuth2TokenPersistenceTest.accessToken.getMacSecret();

    Assert.assertNotNull(result);

    Assert.assertEquals("", new String(result, "UTF-8"));
  }

  @Test
  public void testGetProperties_1() throws Exception {
    final Map<String, String> result = OAuth2TokenPersistenceTest.accessToken.getProperties();

    Assert.assertNotNull(result);
  }

  @Test
  public void testGetScope_1() throws Exception {
    final String result = OAuth2TokenPersistenceTest.accessToken.getScope();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.SCOPE, result);
  }

  @Test
  public void testGetSecret_1() throws Exception {
    final byte[] result = OAuth2TokenPersistenceTest.accessToken.getSecret();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.ACCESS_SECRET, new String(result, "UTF-8"));
  }

  @Test
  public void testGetServiceName_1() throws Exception {
    final String result = OAuth2TokenPersistenceTest.accessToken.getServiceName();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.SERVICE_NAME, result);
  }

  @Test
  public void testGetTokenType_1() throws Exception {
    final String result = OAuth2TokenPersistenceTest.accessToken.getTokenType();

    Assert.assertNotNull(result);

    Assert.assertEquals(OAuth2Message.BEARER_TOKEN_TYPE, result);
  }

  @Test
  public void testGetType_1() throws Exception {
    final org.apache.shindig.gadgets.oauth2.OAuth2Token.Type result = OAuth2TokenPersistenceTest.accessToken
        .getType();

    Assert.assertNotNull(result);

    Assert.assertEquals(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS, result);
  }

  @Test
  public void testGetType_2() throws Exception {
    final org.apache.shindig.gadgets.oauth2.OAuth2Token.Type result = OAuth2TokenPersistenceTest.refreshToken
        .getType();

    Assert.assertNotNull(result);

    Assert.assertEquals(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.REFRESH, result);
  }

  @Test
  public void testGetUser_1() throws Exception {
    final String result = OAuth2TokenPersistenceTest.accessToken.getUser();

    Assert.assertNotNull(result);

    Assert.assertEquals(MockUtils.USER, result);
  }

  @Test
  public void testHashCode_1() throws Exception {
    final int result = OAuth2TokenPersistenceTest.accessToken.hashCode();

    Assert.assertEquals(-1087355025, result);
  }

  @Test
  public void testHashCode_2() throws Exception {
    final int result = OAuth2TokenPersistenceTest.refreshToken.hashCode();

    Assert.assertEquals(-1380171248, result);
  }

  @Test
  public void testHashCode_3() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri((String) null);
    fixture.setMacExt("");
    fixture.setTokenType("");

    final int result = fixture.hashCode();

    Assert.assertEquals(0, result);
  }

  @Test
  public void testSetEncryptedMacSecret_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final byte[] encryptedSecret = new byte[] {};

    fixture.setEncryptedMacSecret(encryptedSecret);
  }

  @Test
  public void testSetEncryptedMaxSecret_2() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final byte[] encryptedSecret = new byte[] {};

    fixture.setEncryptedMacSecret(encryptedSecret);
  }

  @Test
  public void testSetEncryptedSecret_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final byte[] encryptedSecret = new byte[] {};

    fixture.setEncryptedSecret(encryptedSecret);
  }

  @Test
  public void testSetEncryptedSecret_2() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final byte[] encryptedSecret = new byte[] {};

    fixture.setEncryptedSecret(encryptedSecret);
  }

  @Test
  public void testSetExpiresAt_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final long expiresAt = 1L;

    fixture.setExpiresAt(expiresAt);
  }

  @Test
  public void testSetGadgetUri_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final String gadgetUri = "";

    fixture.setGadgetUri(gadgetUri);
  }

  @Test
  public void testSetIssuedAt_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final long issuedAt = 1L;

    fixture.setIssuedAt(issuedAt);
  }

  @Test
  public void testSetMacAlgorithm_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final String algorithm = "";

    fixture.setMacAlgorithm(algorithm);
  }

  @Test
  public void testSetMacExt_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final String macExt = "";

    fixture.setMacExt(macExt);
  }

  @Test
  public void testSetMacSecret_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final byte[] secret = new byte[] {};

    fixture.setMacSecret(secret);
  }

  @Test
  public void testSetMacSecret_2() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final byte[] secret = new byte[] {};

    fixture.setMacSecret(secret);
  }

  @Test
  public void testSetScope_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final String scope = "";

    fixture.setScope(scope);
  }

  @Test
  public void testSetSecret_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final byte[] secret = new byte[] {};

    fixture.setSecret(secret);
  }

  @Test
  public void testSetSecret_2() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence();
    fixture.setServiceName("");
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");

    byte[] secret = "abcdef".getBytes();
    fixture.setEncryptedSecret( secret );
    assertArrayEquals(secret, fixture.getSecret());
    fixture.setEncryptedMacSecret(secret);
    assertArrayEquals(secret, fixture.getMacSecret());

    byte[] secret2 = "zyxwvu".getBytes();
    fixture.setSecret( secret2 );
    assertArrayEquals( secret2, fixture.getEncryptedSecret() );
    fixture.setMacSecret( secret2 );
    assertArrayEquals( secret2, fixture.getEncryptedMacSecret() );

  }

  @Test
  public void testSetServiceName_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final String serviceName = "";

    fixture.setServiceName(serviceName);
  }

  @Test
  public void testSetTokenType_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final String tokenType = "";

    fixture.setTokenType(tokenType);
  }

  @Test
  public void testSetType_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final org.apache.shindig.gadgets.oauth2.OAuth2Token.Type type = org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS;

    fixture.setType(type);
  }

  @Test
  public void testSetUser_1() throws Exception {
    final OAuth2TokenPersistence fixture = new OAuth2TokenPersistence(MockUtils.getDummyEncrypter());
    fixture.setServiceName("");
    fixture.setEncryptedSecret(new byte[] {});
    fixture.setUser("");
    fixture.setType(org.apache.shindig.gadgets.oauth2.OAuth2Token.Type.ACCESS);
    fixture.setIssuedAt(1L);
    fixture.setEncryptedMacSecret(new byte[] {});
    fixture.setMacAlgorithm("");
    fixture.setScope("");
    fixture.setExpiresAt(1L);
    fixture.setMacSecret(new byte[] {});
    fixture.setSecret(new byte[] {});
    fixture.setGadgetUri("");
    fixture.setMacExt("");
    fixture.setTokenType("");
    final String user = "";

    fixture.setUser(user);

  }

  @Test
  public void testToString_1() throws Exception {
    final String result = OAuth2TokenPersistenceTest.accessToken.toString();

    Assert.assertNotNull(result);

    Assert
        .assertEquals(
            "org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2TokenImpl: serviceName = serviceName , user = testUser , gadgetUri = http://www.example.com/1 , scope = testScope , tokenType = Bearer , issuedAt = 0 , expiresAt = 1 , type = ACCESS",
            result);
  }
}

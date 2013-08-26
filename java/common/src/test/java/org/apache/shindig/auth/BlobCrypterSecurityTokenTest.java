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
package org.apache.shindig.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.auth.AbstractSecurityToken.Keys;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.common.util.HMACType;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests BlobCrypterSecurityToken
 */
public class BlobCrypterSecurityTokenTest {

  private static final String CONTAINER = "container";
  private static final String DOMAIN = "example.com";

  private FakeTimeSource timeSource = new FakeTimeSource();
  private BasicBlobCrypter crypter;
  private BasicBlobCrypter crypter256;
  private BasicBlobCrypter crypter384;
  private BasicBlobCrypter crypter512;
  private List<BasicBlobCrypter> crypters = new ArrayList<BasicBlobCrypter>();

  @Before
  public void setUp() {
    crypter = new BasicBlobCrypter(Crypto.getRandomBytes(20),HMACType.HMACSHA1);
    crypter256 = new BasicBlobCrypter(Crypto.getRandomBytes(20),HMACType.HMACSHA256);
    crypter384 = new BasicBlobCrypter(Crypto.getRandomBytes(20),HMACType.HMACSHA384);
    crypter512 = new BasicBlobCrypter(Crypto.getRandomBytes(20),HMACType.HMACSHA512);
    crypters.add(crypter);
    crypters.add(crypter256);
    crypters.add(crypter384);
    crypters.add(crypter512);
    crypter.timeSource = timeSource;
  }

  @Test
  public void testNullValues() throws Exception {
    for (BasicBlobCrypter crypter: crypters) {
      BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(CONTAINER, DOMAIN, null, null);
      String token = t.getContainer() + ":" + crypter.wrap(t.toMap());
      assertTrue("should start with container: " + token, token.startsWith("container:"));
      String[] fields = StringUtils.split(token, ':');
      BlobCrypterSecurityToken t2 = new BlobCrypterSecurityToken(CONTAINER, DOMAIN, null, crypter.unwrap(fields[1]));

      assertNull(t2.getAppId(), t2.getAppId());
      assertNull(t2.getAppUrl(), t2.getAppUrl());
      assertEquals(DOMAIN, t2.getDomain());
      assertEquals(0, t2.getModuleId());
      assertNull(t2.getOwnerId(), t2.getOwnerId());
      assertNull(t2.getViewerId(), t2.getViewerId());
      assertNull(t2.getTrustedJson(), t2.getTrustedJson());
      assertNull(t2.getUpdatedToken(), t2.getUpdatedToken());
      assertEquals(CONTAINER, t2.getContainer());
      assertNull(t2.getActiveUrl(), t2.getActiveUrl());
    }
  }

  @Test
  public void testRealValues() throws Exception {
    for (BasicBlobCrypter crypter: crypters) {
      Map<String, String> values = new HashMap<String, String>();
      values.put(Keys.APP_URL.getKey(), "http://www.example.com/gadget.xml");
      values.put(Keys.MODULE_ID.getKey(), Long.toString(12345L, 10));
      values.put(Keys.OWNER.getKey(), "owner");
      values.put(Keys.VIEWER.getKey(), "viewer");
      values.put(Keys.TRUSTED_JSON.getKey(), "trusted");

      BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(CONTAINER, DOMAIN, null, values);
      String token = t.getContainer() + ":" + crypter.wrap(t.toMap());
      assertTrue("should start with container: " + token, token.startsWith("container:"));
      String[] fields = StringUtils.split(token, ':');
      BlobCrypterSecurityToken t2 = new BlobCrypterSecurityToken(CONTAINER, DOMAIN, "active", crypter.unwrap(fields[1]));
      assertEquals("http://www.example.com/gadget.xml", t2.getAppId());
      assertEquals("http://www.example.com/gadget.xml", t2.getAppUrl());
      assertEquals(DOMAIN, t2.getDomain());
      assertEquals(12345L, t2.getModuleId());
      assertEquals("owner", t2.getOwnerId());
      assertEquals("viewer", t2.getViewerId());
      assertEquals("trusted", t2.getTrustedJson());
      assertEquals(CONTAINER, t2.getContainer());
      assertEquals("active", t2.getActiveUrl());
    }
  }

}

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.shindig.auth.AbstractSecurityToken.Keys;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Tests for BlobCrypterSecurityTokenCodec
 */
public class BlobCrypterSecurityTokenCodecTest {

  private BlobCrypterSecurityTokenCodec codec;
  private FakeTimeSource timeSource;
  private ContainerConfig config;

  @Before
  public void setUp() throws Exception {
    config = new BasicContainerConfig();
    config
        .newTransaction()
        .addContainer(makeContainer("default"))
        .addContainer(makeContainer("container"))
        .addContainer(makeContainer("example"))
        .commit();
    codec = new BlobCrypterSecurityTokenCodec(config);
    timeSource = new FakeTimeSource();
  }

  protected Map<String, Object> makeContainer(String container) {
    return ImmutableMap.<String, Object>of(ContainerConfig.CONTAINER_KEY,
        ImmutableList.of(container),
        BlobCrypterSecurityTokenCodec.SECURITY_TOKEN_KEY,
        getContainerKey(container),
        BlobCrypterSecurityTokenCodec.SIGNED_FETCH_DOMAIN,
        container + ".com");
  }

  protected String getContainerKey(String container) {
    return "KEY FOR CONTAINER " + container;
  }

  protected BlobCrypter getBlobCrypter(String key) {
    BasicBlobCrypter c = new BasicBlobCrypter(key);
    c.timeSource = timeSource;
    return c;
  }

  @Test
  public void testCreateToken() throws Exception {
    Map<String, String> values = new HashMap<String, String>();
    values.put(Keys.APP_URL.getKey(), "http://www.example.com/gadget.xml");
    values.put(Keys.MODULE_ID.getKey(), Long.toString(12345L, 10));
    values.put(Keys.OWNER.getKey(), "owner");
    values.put(Keys.VIEWER.getKey(), "viewer");
    values.put(Keys.TRUSTED_JSON.getKey(), "trusted");

    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken("container", null, null, values);
    String encrypted = t.getContainer() + ":" + getBlobCrypter(getContainerKey("container")).wrap(t.toMap());

    SecurityToken t2 = codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));

    assertEquals("http://www.example.com/gadget.xml", t2.getAppId());
    assertEquals("http://www.example.com/gadget.xml", t2.getAppUrl());
    assertEquals("container.com", t2.getDomain());
    assertEquals(12345L, t2.getModuleId());
    assertEquals("owner", t2.getOwnerId());
    assertEquals("viewer", t2.getViewerId());
    assertEquals("trusted", t2.getTrustedJson());
  }

  @Test
  public void testUnknownContainer() throws Exception {
    Map<String, String> values = new HashMap<String, String>();
    values.put(Keys.APP_URL.getKey(), "http://www.example.com/gadget.xml");
    values.put(Keys.MODULE_ID.getKey(), Long.toString(12345L, 10));
    values.put(Keys.OWNER.getKey(), "owner");
    values.put(Keys.VIEWER.getKey(), "viewer");
    values.put(Keys.TRUSTED_JSON.getKey(), "trusted");

    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken("container", null, null, values);
    String encrypted = t.getContainer() + ":" + getBlobCrypter(getContainerKey("container")).wrap(t.toMap());
    encrypted = encrypted.replace("container:", "other:");

    try {
      codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));
      fail("should have reported that container was unknown");
    } catch (SecurityTokenException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Unknown container"));
    }
  }

  @Test
  public void testWrongContainer() throws Exception {
    Map<String, String> values = new HashMap<String, String>();
    values.put(Keys.APP_URL.getKey(), "http://www.example.com/gadget.xml");
    values.put(Keys.MODULE_ID.getKey(), Long.toString(12345L, 10));
    values.put(Keys.OWNER.getKey(), "owner");
    values.put(Keys.VIEWER.getKey(), "viewer");
    values.put(Keys.TRUSTED_JSON.getKey(), "trusted");

    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken("container", null, null, values);
    String encrypted = t.getContainer() + ":" + getBlobCrypter(getContainerKey("container")).wrap(t.toMap());
    encrypted = encrypted.replace("container:", "example:");

    try {
      codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));
      fail("should have tried to decrypt with wrong key");
    } catch (SecurityTokenException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Invalid token signature"));
    }
  }

  @Test
  public void testExpired() throws Exception {
    Map<String, String> values = new HashMap<String, String>();
    values.put(Keys.APP_URL.getKey(), "http://www.example.com/gadget.xml");
    values.put(Keys.MODULE_ID.getKey(), Long.toString(12345L, 10));
    values.put(Keys.OWNER.getKey(), "owner");
    values.put(Keys.VIEWER.getKey(), "viewer");
    values.put(Keys.TRUSTED_JSON.getKey(), "trusted");

    BlobCrypterSecurityToken token = new BlobCrypterSecurityToken("container", null, null, values);
    token.setTimeSource(timeSource);
    timeSource.incrementSeconds(-1 * (codec.getTokenTimeToLive("container") + 181)); // one hour plus clock skew
    String encrypted = codec.encodeToken(token);
    try {
      codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));
      fail("should have expired");
    } catch (SecurityTokenException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Blob expired"));
    }
  }

  @Test
  public void testMalformed() throws Exception {
    try {
      codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, "foo"));
      fail("should have tried to decrypt with wrong key");
    } catch (SecurityTokenException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Invalid security token foo"));
    }
  }

  @Test
  public void testAnonymous() throws Exception {
    SecurityToken t = codec.createToken(
        ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, "   "));
    assertTrue(t.isAnonymous());

    Map<String, String> empty = ImmutableMap.of();
    t = codec.createToken(empty);
    assertTrue(t.isAnonymous());
  }

  @Test
  public void testChangingContainers() throws Exception {
    String newContainer = "newcontainer";
    Map<String, String> values = new HashMap<String, String>();
    values.put(Keys.APP_URL.getKey(), "http://www.example.com/gadget.xml");
    values.put(Keys.MODULE_ID.getKey(), Long.toString(12345L, 10));
    values.put(Keys.OWNER.getKey(), "owner");
    values.put(Keys.VIEWER.getKey(), "viewer");
    values.put(Keys.TRUSTED_JSON.getKey(), "trusted");

    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(newContainer, null, null, values);
    String encrypted = t.getContainer() + ":" + getBlobCrypter(getContainerKey(newContainer)).wrap(t.toMap());

    // fails when trying to create a token for a non-existing container
    try {
      codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));
      fail("Should have thrown a SecurityTokenException");
    } catch (SecurityTokenException e) {
      // pass
    }
    // add the container, now it should succeed
    config.newTransaction().addContainer(makeContainer(newContainer)).commit();
    codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));
    // remove the token, now it should fail again
    config.newTransaction().removeContainer(newContainer).commit();
    try {
      codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));
      fail("Should have thrown a SecurityTokenException");
    } catch (SecurityTokenException e) {
      // pass
    }
  }

  @Test
  public void testGetTokenTimeToLive() throws Exception {
    Builder<String, Object> builder = ImmutableMap.builder();
    Map<String, Object> container = builder.putAll(makeContainer("tokenTest"))
            .put(SecurityTokenCodec.SECURITY_TOKEN_TTL_CONFIG, Integer.valueOf(300)).build();

    config.newTransaction().addContainer(container).commit();
    assertEquals("Token TTL matches what is set in the container config", 300,
            codec.getTokenTimeToLive("tokenTest"));
    assertEquals("Token TTL matches the default TTL", AbstractSecurityToken.DEFAULT_MAX_TOKEN_TTL,
            codec.getTokenTimeToLive());
  }
}

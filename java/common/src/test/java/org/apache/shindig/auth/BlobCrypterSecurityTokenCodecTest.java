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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.shindig.auth.AbstractSecurityToken.Keys;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.config.ContainerConfig;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
        .addContainer(makeContainer("keyOnlyNoFile", true))
        .commit();
    codec = new CodecWithLoadStubbedOut(config);
    timeSource = new FakeTimeSource();
  }

  protected Map<String, Object> makeContainer(String container) {
    return makeContainer(container, false);
  }

  protected Map<String, Object> makeContainer(String container, boolean insertKey) {
    if (insertKey) {
      return ImmutableMap.<String, Object>of(ContainerConfig.CONTAINER_KEY,
          ImmutableList.of(container),
          BlobCrypterSecurityTokenCodec.SECURITY_TOKEN_KEY_FILE,
          container,
          BlobCrypterSecurityTokenCodec.SECURITY_TOKEN_KEY,
          getContainerKey(container),
          BlobCrypterSecurityTokenCodec.SIGNED_FETCH_DOMAIN,
          container + ".com");
    } else {
      return ImmutableMap.<String, Object>of(ContainerConfig.CONTAINER_KEY,
              ImmutableList.of(container),
              BlobCrypterSecurityTokenCodec.SECURITY_TOKEN_KEY_FILE,
              container,
              BlobCrypterSecurityTokenCodec.SIGNED_FETCH_DOMAIN,
              container + ".com");
    }
  }

  protected String getContainerKey(String container) {
    return "KEY FOR CONTAINER " + container;
  }

  protected BlobCrypter getBlobCrypter(String fileName) {
    BasicBlobCrypter c = new BasicBlobCrypter(CharsetUtil.getUtf8Bytes(fileName));
    c.timeSource = timeSource;
    return c;
  }

  /**
   * Stubs out loading the key file.
   */
  private class CodecWithLoadStubbedOut extends BlobCrypterSecurityTokenCodec {

    public CodecWithLoadStubbedOut(ContainerConfig config) {
      super(config);
    }

    /**
     * @param file the location of the file.
     * @return a crypter based on the name of the file passed in, rather than the contents.
     * @throws IOException when passed a filename with 'fail' in it.
     */
    @Override
    protected String getKeyFromFile(String file) throws IOException {
      if (file.contains("fail")) {
        throw new IOException("Load failed: " + file);
      }
      return getContainerKey(file);
    }
  }

  @Test
  public void testCreateTokenUsingKeyFile() throws Exception {
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
    timeSource.incrementSeconds(-1 * (3600 + 181)); // one hour plus clock skew
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
  public void testLoadFailure() throws Exception {
    config.newTransaction().addContainer(makeContainer("failure")).commit();

    try {
      new CodecWithLoadStubbedOut(config);
      fail("Should have failed to load crypter");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Load failed"));
    }
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
  public void testCreateTokenUsingKey() throws Exception {
    Map<String, String> values = new HashMap<String, String>();
    values.put(Keys.APP_URL.getKey(), "http://www.example.com/gadget.xml");
    values.put(Keys.MODULE_ID.getKey(), Long.toString(12345L, 10));
    values.put(Keys.OWNER.getKey(), "owner");
    values.put(Keys.VIEWER.getKey(), "viewer");
    values.put(Keys.TRUSTED_JSON.getKey(), "trusted");

    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken("keyOnlyNoFile", null, null, values);
    String encrypted = t.getContainer() + ":" + getBlobCrypter(getContainerKey("keyOnlyNoFile")).wrap(t.toMap());

    SecurityToken t2 = codec.createToken(ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));

    assertEquals("http://www.example.com/gadget.xml", t2.getAppId());
    assertEquals("http://www.example.com/gadget.xml", t2.getAppUrl());
    assertEquals("keyOnlyNoFile.com", t2.getDomain());
    assertEquals(12345L, t2.getModuleId());
    assertEquals("owner", t2.getOwnerId());
    assertEquals("viewer", t2.getViewerId());
    assertEquals("trusted", t2.getTrustedJson());
  }
}

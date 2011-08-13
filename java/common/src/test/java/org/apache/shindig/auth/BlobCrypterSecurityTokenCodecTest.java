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

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.config.BasicContainerConfig;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Tests for BlobCrypterSecurityTokenCodec
 */
public class BlobCrypterSecurityTokenCodecTest {

  private BlobCrypterSecurityTokenCodec codec;
  private final FakeTimeSource timeSource = new FakeTimeSource();
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
    codec = new CodecWithLoadStubbedOut(config);
  }
  
  protected Map<String, Object> makeContainer(String container) {
    return ImmutableMap.<String, Object>of(ContainerConfig.CONTAINER_KEY,
        ImmutableList.of(container),
        BlobCrypterSecurityTokenCodec.SECURITY_TOKEN_KEY_FILE,
        getContainerKey(container),
        BlobCrypterSecurityTokenCodec.SIGNED_FETCH_DOMAIN,
        container + ".com");
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
    protected BlobCrypter loadCrypter(String file) throws IOException {
      if (file.contains("fail")) {
        throw new IOException("Load failed: " + file);
      }
      return getBlobCrypter(file);
    }
  }

  @Test
  public void testCreateToken() throws Exception {
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(
        getBlobCrypter(getContainerKey("container")), "container", null);
    t.setAppUrl("http://www.example.com/gadget.xml");
    t.setModuleId(12345L);
    t.setOwnerId("owner");
    t.setViewerId("viewer");
    t.setTrustedJson("trusted");
    String encrypted = t.encrypt();

    SecurityToken t2 = codec.createToken(
        ImmutableMap.of(SecurityTokenCodec.SECURITY_TOKEN_NAME, encrypted));

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
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(
        getBlobCrypter(getContainerKey("container")), "container", null);
    t.setAppUrl("http://www.example.com/gadget.xml");
    t.setModuleId(12345L);
    t.setOwnerId("owner");
    t.setViewerId("viewer");
    t.setTrustedJson("trusted");
    String encrypted = t.encrypt();
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
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(
        getBlobCrypter(getContainerKey("container")), "container", null);
    t.setAppUrl("http://www.example.com/gadget.xml");
    t.setModuleId(12345L);
    t.setOwnerId("owner");
    t.setViewerId("viewer");
    t.setTrustedJson("trusted");
    String encrypted = t.encrypt();
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
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(
        getBlobCrypter(getContainerKey("container")), "container", null);
    t.setAppUrl("http://www.example.com/gadget.xml");
    t.setModuleId(12345L);
    t.setOwnerId("owner");
    t.setViewerId("viewer");
    t.setTrustedJson("trusted");
    String encrypted = t.encrypt();

    timeSource.incrementSeconds(3600 + 181); // one hour plus clock skew
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
    BlobCrypterSecurityToken t = new BlobCrypterSecurityToken(
        getBlobCrypter(getContainerKey(newContainer)), newContainer, null);
    t.setAppUrl("http://www.example.com/gadget.xml");
    t.setModuleId(12345L);
    t.setOwnerId("owner");
    t.setViewerId("viewer");
    t.setTrustedJson("trusted");
    String encrypted = t.encrypt();

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
  public void testGetTokenExpiration() throws Exception {
    Assert.assertNull(codec.getTokenExpiration(null));
  }
}

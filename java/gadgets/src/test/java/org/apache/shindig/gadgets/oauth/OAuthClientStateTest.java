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
package org.apache.shindig.gadgets.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.util.FakeTimeSource;
import org.junit.Before;
import org.junit.Test;

public class OAuthClientStateTest {

  private FakeTimeSource timeSource;
  private BasicBlobCrypter crypter;

  @Before
  public void setUp() throws Exception {
    crypter = new BasicBlobCrypter("abcdefghijklmnop".getBytes());
    timeSource = new FakeTimeSource();
    crypter.timeSource = timeSource;
  }

  private void assertEmpty(OAuthClientState state) {
    assertTrue(state.isEmpty());
    assertNull(state.getRequestToken());
    assertNull(state.getRequestTokenSecret());
    assertNull(state.getAccessToken());
    assertNull(state.getAccessTokenSecret());
    assertNull(state.getOwner());
  }

  @Test
  public void testEncryptEmpty() throws Exception {
    OAuthClientState state = new OAuthClientState(crypter);
    assertEmpty(state);
    String encrypted = state.getEncryptedState();
    state = new OAuthClientState(crypter, encrypted);
    assertEmpty(state);
  }

  @Test
  public void testValuesSet() throws Exception {
    OAuthClientState state = new OAuthClientState(crypter);
    state.setAccessToken("atoken");
    state.setAccessTokenSecret("atokensecret");
    state.setOwner("owner");
    state.setRequestToken("reqtoken");
    state.setRequestTokenSecret("reqtokensecret");
    String encrypted = state.getEncryptedState();
    state = new OAuthClientState(crypter, encrypted);
    assertEquals("atoken", state.getAccessToken());
    assertEquals("atokensecret", state.getAccessTokenSecret());
    assertEquals("owner", state.getOwner());
    assertEquals("reqtoken", state.getRequestToken());
    assertEquals("reqtokensecret", state.getRequestTokenSecret());
  }

  @Test
  public void testNullConstructorArg() throws Exception {
    OAuthClientState state = new OAuthClientState(crypter, null);
    assertEmpty(state);
  }

  @Test
  public void testExpired() throws Exception {
    OAuthClientState state = new OAuthClientState(crypter);
    timeSource.incrementSeconds(-1 * (3600 + 180 + 1)); // expiry time + skew.
    state.setTimeSource(timeSource);
    state.setRequestToken("reqtoken");
    String encrypted = state.getEncryptedState();
    state = new OAuthClientState(crypter, encrypted);
    assertNull(state.getRequestToken());
  }

  @Test
  public void testNullValue() throws Exception {
    OAuthClientState state = new OAuthClientState(crypter);
    state.setRequestToken("reqtoken");
    state.setRequestToken(null);
    state.setOwner("owner");
    String encrypted = state.getEncryptedState();
    state = new OAuthClientState(crypter, encrypted);
    assertNull(state.getRequestToken());
    assertEquals("owner", state.getOwner());
  }
}

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
package org.apache.shindig.common.crypto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.common.util.HMACType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.util.Map;

public class BlobCrypterTest {

  private BasicBlobCrypter crypter;
  private FakeTimeSource timeSource;

  public BlobCrypterTest() {
    crypter = new BasicBlobCrypter("0123456789abcdef".getBytes(),HMACType.HMACSHA1);
    timeSource = new FakeTimeSource();
    crypter.timeSource = timeSource;
  }

  @Test
  public void testEncryptAndDecrypt() throws Exception {
    checkString("");
    checkString("a");
    checkString("ab");
    checkString("dfkljdasklsdfklasdjfklajsdfkljasdklfjasdkljfaskldjf");
    checkString(Crypto.getRandomString(500));
    checkString("foo bar baz");
    checkString("foo\nbar\nbaz");
  }

  private void checkString(String string) throws Exception {
    Map<String, String> in = Maps.newHashMap();
    if (string != null) {
      in.put("a", string);
    }
    String blob = crypter.wrap(in);
    Map<String, String> out = crypter.unwrap(blob);
    assertEquals(string, out.get("a"));
  }

  @Test
  public void testDecryptGarbage() throws Exception {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i < 100; ++i) {
      assertThrowsBlobCrypterException(sb.toString());
      sb.append('a');
    }
  }

  private void assertThrowsBlobCrypterException(String in) {
    try {
      crypter.unwrap(in);
      fail("Should have thrown BlobCrypterException for input " + in);
    } catch (BlobCrypterException e) {
      // Good.
    }
  }

  @Test
  public void testManyEntries() throws Exception {
    Map<String, String> in = Maps.newHashMap();
    for (int i=0; i < 1000; i++) {
      in.put(Integer.toString(i), Integer.toString(i));
    }
    String blob = crypter.wrap(in);
    Map<String, String> out = crypter.unwrap(blob);
    for (int i=0; i < 1000; i++) {
      assertEquals(out.get(Integer.toString(i)), Integer.toString(i));
    }
  }

  @Test(expected=BlobCrypterException.class)
  public void testTamperIV() throws Exception {
    Map<String, String> in = ImmutableMap.of("a","b");

    String blob = crypter.wrap(in);
    byte[] blobBytes = Base64.decodeBase64(blob.getBytes());
    blobBytes[0] ^= 0x01;
    String tampered = new String(Base64.encodeBase64(blobBytes));
    crypter.unwrap(tampered);
  }

  @Test(expected=BlobCrypterException.class)
  public void testTamperData() throws Exception {
    Map<String, String> in = ImmutableMap.of("a","b");
    String blob = crypter.wrap(in);
    byte[] blobBytes = Base64.decodeBase64(blob.getBytes());
    blobBytes[30] ^= 0x01;
    String tampered = new String(Base64.encodeBase64(blobBytes));
    crypter.unwrap(tampered);
  }

  @Test(expected=BlobCrypterException.class)
  public void testTamperMac() throws Exception {
    Map<String, String> in = ImmutableMap.of("a","b");

    String blob = crypter.wrap(in);
    byte[] blobBytes = Base64.decodeBase64(blob.getBytes());
    blobBytes[blobBytes.length-1] ^= 0x01;
    String tampered = new String(Base64.encodeBase64(blobBytes));
    crypter.unwrap(tampered);
  }

  @Test
  public void testFixedKey() throws Exception {
    BlobCrypter alt = new BasicBlobCrypter("0123456789abcdef".getBytes(),HMACType.HMACSHA1);
    Map<String, String> in = ImmutableMap.of("a","b");

    String blob = crypter.wrap(in);
    Map<String, String> out = alt.unwrap(blob);
    assertEquals("b", out.get("a"));
  }

  @Test(expected=BlobCrypterException.class)
  public void testBadKey() throws Exception {
    BlobCrypter alt = new BasicBlobCrypter("1123456789abcdef".getBytes(),HMACType.HMACSHA1);
    Map<String, String> in = ImmutableMap.of("a","b");

    String blob = crypter.wrap(in);
    alt.unwrap(blob);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testShortKeyFails() throws Exception {
    new BasicBlobCrypter("0123456789abcde".getBytes());
  }
}

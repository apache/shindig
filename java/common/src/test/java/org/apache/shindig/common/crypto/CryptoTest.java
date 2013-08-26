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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.GeneralSecurityException;
import java.util.regex.Pattern;

import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.common.util.HMACType;
import org.junit.Test;

public class CryptoTest {
  private BasicBlobCrypter crypter;

  public CryptoTest() {
    crypter = new BasicBlobCrypter("0123456789abcdef".getBytes());
    crypter.timeSource = new FakeTimeSource();
  }

  @Test
  public void testHmacSha1() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = {
        -21, 2, 47, -101, 9, -40, 18, 43, 76, 117,
        -51, 115, -122, -91, 39, 26, -18, 122, 30, 90,
    };
    byte[] hmac = Crypto.hmacSha(key.getBytes(), val.getBytes(),HMACType.HMACSHA1.getName());
    assertArrayEquals(expected, hmac);
  }

  @Test
  public void testHmacSha1Verify() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = {
        -21, 2, 47, -101, 9, -40, 18, 43, 76, 117,
        -51, 115, -122, -91, 39, 26, -18, 122, 30, 90,
    };
    Crypto.hmacShaVerify(key.getBytes(), val.getBytes(), expected,HMACType.HMACSHA1.getName());
  }

  @Test
  public void testHmacSha256() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = { 69, -128, -5, 20, 94, -46, -40, 46, 43, -24, -76, -93,
        -28, -70, 3, 93, 101, 124, 111, -56, 124, -38, 103, 41, 83, -53, -45,
        36, -21, 73, -10, -32, };
    byte[] hmac = Crypto.hmacSha(key.getBytes(), val.getBytes(),HMACType.HMACSHA256.getName());
    assertArrayEquals(expected, hmac);
  }

  @Test
  public void testHmacSha256Verify() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = { 69, -128, -5, 20, 94, -46, -40, 46, 43, -24, -76, -93,
        -28, -70, 3, 93, 101, 124, 111, -56, 124, -38, 103, 41, 83, -53, -45,
        36, -21, 73, -10, -32, };
    Crypto.hmacShaVerify(key.getBytes(), val.getBytes(), expected,HMACType.HMACSHA256.getName());
  }

  @Test
  public void testHmacSha384() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = { 66, -117, 24, -112, 19, -58, 80, 27, -117, 23, 107, 41,
        -118, -3, 100, -61, 42, 77, 50, 70, -28, 85, -39, -55, 47, 42, 106,
        116, -26, 72, 76, -101, 67, -37, -56, 5, -85, 117, -51, -95, -18, -100,
        81, 69, 9, 105, 70, 99, };
    byte[] hmac = Crypto.hmacSha(key.getBytes(), val.getBytes(),HMACType.HMACSHA384.getName());
    assertArrayEquals(expected, hmac);
  }

  @Test
  public void testHmacSha384Verify() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = { 66, -117, 24, -112, 19, -58, 80, 27, -117, 23, 107, 41,
        -118, -3, 100, -61, 42, 77, 50, 70, -28, 85, -39, -55, 47, 42, 106,
        116, -26, 72, 76, -101, 67, -37, -56, 5, -85, 117, -51, -95, -18, -100,
        81, 69, 9, 105, 70, 99, };
    Crypto.hmacShaVerify(key.getBytes(), val.getBytes(), expected,HMACType.HMACSHA384.getName());
  }

  @Test
  public void testHmacSha512() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = { -40, -114, 57, 41, -97, -13, 13, 106, -71, 72, -54, 97,
        -50, -109, -115, -24, -68, 82, 73, -97, 46, -21, -128, -40, 73, 41, 43,
        61, 20, 35, 79, 90, -27, 83, -1, -64, -128, 49, -118, -117, 34, -63,
        -51, 87, -85, 120, -9, -107, 29, 106, -48, 51, 105, -56, 86, -52, 18,
        -45, -81, -6, 0, 16, 67, 90, };
    byte[] hmac = Crypto.hmacSha(key.getBytes(), val.getBytes(),HMACType.HMACSHA512.getName());
    assertArrayEquals(expected, hmac);
  }

  @Test
  public void testHmacSha512Verify() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = { -40, -114, 57, 41, -97, -13, 13, 106, -71, 72, -54, 97,
        -50, -109, -115, -24, -68, 82, 73, -97, 46, -21, -128, -40, 73, 41, 43,
        61, 20, 35, 79, 90, -27, 83, -1, -64, -128, 49, -118, -117, 34, -63,
        -51, 87, -85, 120, -9, -107, 29, 106, -48, 51, 105, -56, 86, -52, 18,
        -45, -81, -6, 0, 16, 67, 90, };
    Crypto.hmacShaVerify(key.getBytes(), val.getBytes(), expected,HMACType.HMACSHA512.getName());
  }
  @Test(expected = GeneralSecurityException.class)
  public void testHmacSha1VerifyTampered() throws Exception {
    String key = "abcd1234";
    String val = "your mother is a hedgehog";
    byte[] expected = {
        -21, 2, 47, -101, 9, -40, 18, 43, 76, 117,
        -51, 115, -122, -91, 39, 0, -18, 122, 30, 90,
    };
    Crypto.hmacShaVerify(key.getBytes(), val.getBytes(), expected,HMACType.HMACSHA1.getName());
  }

  @Test
  public void testAes128Cbc() throws Exception {
    byte[] key = Crypto.getRandomBytes(Crypto.CIPHER_KEY_LEN);
    for (byte i=0; i < 50; i++) {
      byte[] orig = new byte[i];
      for (byte j=0; j < i; j++) {
        orig[j] = j;
      }
      byte[] cipherText = Crypto.aes128cbcEncrypt(key, orig);
      byte[] plainText = Crypto.aes128cbcDecrypt(key, cipherText);
      assertArrayEquals("Array of length " + i, orig, plainText);
    }
  }

  @Test
  public void testRandomDigits() throws Exception {
    Pattern digitPattern = Pattern.compile("^\\d+$");
    String digits = Crypto.getRandomDigits(100);
    assertEquals(100, digits.length());
    assertTrue("Should be only digits: " + digits, digitPattern.matcher(digits).matches());
  }
}

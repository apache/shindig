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

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.base.Preconditions;

import com.google.common.primitives.Bytes;
import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.common.util.CharsetUtil;
import org.apache.shindig.common.util.TimeSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shindig.common.util.Utf8UrlCoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * Simple implementation of BlobCrypter.
 */
public class BasicBlobCrypter implements BlobCrypter {

  // Labels for key derivation
  private static final byte CIPHER_KEY_LABEL = 0;
  private static final byte HMAC_KEY_LABEL = 1;

  /** minimum length of master key */
  public static final int MASTER_KEY_MIN_LEN = 16;

  public TimeSource timeSource = new TimeSource();
  private byte[] cipherKey;
  private byte[] hmacKey;

  /**
   * Creates a crypter based on a key in a file.  The key is the first line
   * in the file, whitespace trimmed from either end, as UTF-8 bytes.
   *
   * The following *nix command line will create an excellent key:
   * <pre>
   * dd if=/dev/random bs=32 count=1  | openssl base64 > /tmp/key.txt
   * </pre>
   *
   * @throws IOException if the file can't be read.
   */
  public BasicBlobCrypter(File keyfile) throws IOException {
    BufferedReader reader = null;
    try {
      FileInputStream openFile = new FileInputStream(keyfile);
      reader = new BufferedReader(
          new InputStreamReader(openFile, Charsets.UTF_8));
      init(reader.readLine());
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        // oh well.
      }
    }
  }

  /**
   * Builds a BlobCrypter from the specified master key
   *
   * @param masterKey
   */
  public BasicBlobCrypter(byte[] masterKey) {
    init(masterKey);
  }

  /**
   * Builds a BlobCrypter from the specified master key
   *
   * @param masterKey
   */
  public BasicBlobCrypter(String masterKey) {
    init(masterKey);
  }

  private void init(String masterKey) {
    if (masterKey == null) {
      throw new IllegalArgumentException("Unexpectedly empty masterKey:" + masterKey);
    }
    masterKey = masterKey.trim();
    byte[] keyBytes = CharsetUtil.getUtf8Bytes(masterKey);
    init(keyBytes);
  }

  private void init(byte[] masterKey) {
    Preconditions.checkArgument(masterKey.length >= MASTER_KEY_MIN_LEN,
        "Master key needs at least %s bytes", MASTER_KEY_MIN_LEN);

    cipherKey = deriveKey(CIPHER_KEY_LABEL, masterKey, Crypto.CIPHER_KEY_LEN);
    hmacKey = deriveKey(HMAC_KEY_LABEL, masterKey, 0);
  }

  /**
   * Generates unique keys from a master key.
   *
   * @param label type of key to derive
   * @param masterKey master key
   * @param len length of key needed, less than 20 bytes.  20 bytes are
   * returned if len is 0.
   *
   * @return a derived key of the specified length
   */
  private byte[] deriveKey(byte label, byte[] masterKey, int len) {
    byte[] base = Bytes.concat(new byte[] { label }, masterKey);
    byte[] hash = DigestUtils.sha(base);
    if (len == 0) {
      return hash;
    }
    byte[] out = new byte[len];
    System.arraycopy(hash, 0, out, 0, out.length);
    return out;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.util.BlobCrypter#wrap(java.util.Map)
   */
  public String wrap(Map<String, String> in) throws BlobCrypterException {
    try {
      byte[] encoded = serialize(in);
      byte[] cipherText = Crypto.aes128cbcEncrypt(cipherKey, encoded);
      byte[] hmac = Crypto.hmacSha1(hmacKey, cipherText);
      byte[] b64 = Base64.encodeBase64URLSafe(Bytes.concat(cipherText, hmac));
      return CharsetUtil.newUtf8String(b64);
    } catch (GeneralSecurityException e) {
      throw new BlobCrypterException(e);
    }
  }

  /**
   * Encode the input for transfer.  We use something a lot like HTML form
   * encodings.
   * @param in map of parameters to encode
   */
  private byte[] serialize(Map<String, String> in) {
    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, String> val : in.entrySet()) {
      sb.append(Utf8UrlCoder.encode(val.getKey()));
      sb.append('=');
      sb.append(Utf8UrlCoder.encode(val.getValue()));
      sb.append('&');
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.length() - 1);  // Remove the last &
    }
    return CharsetUtil.getUtf8Bytes(sb.toString());
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.util.BlobCrypter#unwrap(java.lang.String, int)
   */
  public Map<String, String> unwrap(String in) throws BlobCrypterException {
    try {
      byte[] bin = Base64.decodeBase64(CharsetUtil.getUtf8Bytes(in));
      byte[] hmac = new byte[Crypto.HMAC_SHA1_LEN];
      byte[] cipherText = new byte[bin.length-Crypto.HMAC_SHA1_LEN];
      System.arraycopy(bin, 0, cipherText, 0, cipherText.length);
      System.arraycopy(bin, cipherText.length, hmac, 0, hmac.length);
      Crypto.hmacSha1Verify(hmacKey, cipherText, hmac);
      byte[] plain = Crypto.aes128cbcDecrypt(cipherKey, cipherText);
      Map<String, String> out = deserialize(plain);
      return out;
    } catch (GeneralSecurityException e) {
      throw new BlobCrypterException("Invalid token signature", e);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new BlobCrypterException("Invalid token format", e);
    } catch (NegativeArraySizeException e) {
      throw new BlobCrypterException("Invalid token format", e);
    }

  }

  private Map<String, String> deserialize(byte[] plain) {
    String base = CharsetUtil.newUtf8String(plain);
    // replaces [&=] regex
    String[] items = StringUtils.splitPreserveAllTokens(base, "&=");
    Map<String, String> map = Maps.newHashMapWithExpectedSize(items.length);
    for (int i=0; i < items.length; ) {
      String key = Utf8UrlCoder.decode(items[i++]);
      String val = Utf8UrlCoder.decode(items[i++]);
      map.put(key, val);
    }
    return map;
  }
}

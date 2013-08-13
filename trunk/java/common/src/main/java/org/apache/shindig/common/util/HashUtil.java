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
package org.apache.shindig.common.util;

import com.google.common.base.Preconditions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Routines for producing hashes.
 */
public final class HashUtil {
  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  private HashUtil() {}
  /**
   * Produces a checksum for the given input data. Currently uses a hexified
   * message digest.
   *
   * @param data
   * @return The checksum.
   */
  public static String checksum(byte[] data) {
    byte[] hashBytes = getMessageDigest().digest(Preconditions.checkNotNull(data));
    return bytesToHex(hashBytes);
  }

  /**
   * Converts a byte array into a hex string.
   *
   * @param hashBytes The byte array to convert.
   * @return The hex string.
   */
  public static String bytesToHex(byte[] hashBytes) {
    char[] hex = new char[2 * hashBytes.length];

    // Convert to hex. possibly change to base64 in the future for smaller
    // signatures.

    int offset = 0;
    for (byte b : hashBytes) {
      hex[offset++] = HEX_CHARS[(b & 0xF0) >>> 4]; // upper 4 bits
      hex[offset++] = HEX_CHARS[(b & 0x0F)];       // lower 4 bits
    }
    return new String(hex);
  }

  /**
   * Produces a raw checksum for the given input data.  Currently uses a message digest
   *
   * @param data
   * @return The checksum.
   */
  public static String rawChecksum(byte[] data) {
    return new String(getMessageDigest().digest(Preconditions.checkNotNull(data)));
  }

  /**
   * Provides a {@link MessageDigest} object for calculating checksums.
   *
   * @return A MessageDigest object.
   */
  public static MessageDigest getMessageDigest() {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException noMD5) {
      try {
        md = MessageDigest.getInstance("SHA");
      } catch (NoSuchAlgorithmException noSha) {
        throw new RuntimeException("No suitable MessageDigest found!", noSha);
      }
    }
    return md;
  }
}

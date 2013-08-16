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

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.shindig.common.logging.i18n.MessageKeys;

import com.google.inject.Inject;
import java.util.logging.Level;

public class GenericDigestUtils extends DigestUtils {
  private static final String CLASSNAME = GenericDigestUtils.class.getName();
  private static final Logger LOG = Logger.getLogger(CLASSNAME,
      MessageKeys.MESSAGES);
  private static final int STREAM_BUFFER_LENGTH = 1024;

  private static byte[] digest(MessageDigest digest, InputStream data)
      throws IOException {
    byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
    int read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);

    while (read > -1) {
      digest.update(buffer, 0, read);
      read = data.read(buffer, 0, STREAM_BUFFER_LENGTH);
    }

    return digest.digest();
  }

  private static byte[] getBytesUtf8(String data) {
    return StringUtils.getBytesUtf8(data);
  }

  public static MessageDigest getDigest(String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private static String type = DigestType.SHA.getName();

  public static byte[] digest(byte[] data) {
    return getDigest(type).digest(data);
  }

  public static byte[] digest(InputStream data) throws IOException {
    return digest(getDigest(type), data);
  }

  public static String digestHex(String data) {
    return Hex.encodeHexString(digest(getBytesUtf8(data)));
  }

  public static String digestHex(byte[] data) {
    return Hex.encodeHexString(getDigest(type).digest(data));
  }

  public static String digestHex(InputStream data) throws IOException {
    return Hex.encodeHexString(digest(getDigest(type), data));
  }

  public static byte[] digest(String data) {
    return digest(getBytesUtf8(data));
  }

  @Inject(optional = true)
  public static void setType(
      @com.google.inject.name.Named("shindig.crypto.preferredHashAlgorithm") String type) {
    if (LOG.isLoggable(Level.INFO)) {
      LOG.log(Level.INFO, "shindig.crypto.preferredHashAlgorithm: " + type);
    }
    if (type != null) {
      GenericDigestUtils.type = type;
    }
  }
}

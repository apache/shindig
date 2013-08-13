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

import com.google.common.base.Charsets;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;

/**
 * Utilities for dealing with character set encoding.
 */
public final class CharsetUtil {
  private CharsetUtil() {}

  /**
   * A clean version of String#getBytes that does not throw exceptions for
   * the UTF-8 Charset.
   *
   * Replace all Callers with getBytes(Charsets.UTF_8) once
   * we move to Java 6.
   *
   * @param s a string to convert
   * @return UTF-8 byte array for the input string.
   */
  public static byte[] getUtf8Bytes(String s) {
    if (s == null) {
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
    ByteBuffer bb = Charsets.UTF_8.encode(s);
    return ArrayUtils.subarray(bb.array(), 0, bb.limit());

  }

  /**
   * A clean version of new String(byte[], "UTF-8")
   *
   * Replace all callers with new String(b, Charsets.UTF_8) when
   * we move to Java 6.
   *
   * @param b a byte array of UTF-8 bytes
   * @return a UTF-8 encoded string
   */
  public static String newUtf8String(byte[] b) {
    return Charsets.UTF_8.decode(ByteBuffer.wrap(b)).toString();
  }
}

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

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import junitx.framework.ArrayAssert;

/**
 * Tests for CharsetUtil.
 */
public class CharsetUtilTest {

  @Test
  public void testGetUtf8String() {
    ArrayAssert.assertEquals(new byte[] { 0x69, 0x6e }, CharsetUtil.getUtf8Bytes("in"));
    ArrayAssert.assertEquals(ArrayUtils.EMPTY_BYTE_ARRAY, CharsetUtil.getUtf8Bytes(null));
    testStringOfLength(0);
    testStringOfLength(10);
    testStringOfLength(100);
    testStringOfLength(1000);
  }

  private void testStringOfLength(int len) {
    StringBuilder sb = new StringBuilder();
    for (int i=0; i < len; ++i) {
      sb.append('a');
    }
    byte[] out = CharsetUtil.getUtf8Bytes(sb.toString());
    assertEquals(len, out.length);
    for (int i=0; i < len; ++i) {
      assertEquals('a', out[i]);
    }
  }


  private static final byte[] LATIN1_UTF8_DATA = {
    'G', 'a', 'm', 'e', 's', ',', ' ', 'H', 'Q', ',', ' ', 'M', 'a', 'n', 'g', (byte)0xC3,
    (byte) 0xA1, ',', ' ', 'A', 'n', 'i', 'm', 'e', ' ', 'e', ' ', 't', 'u', 'd', 'o', ' ',
    'q', 'u', 'e', ' ', 'u', 'm', ' ', 'b', 'o', 'm', ' ', 'n', 'e', 'r', 'd', ' ', 'a', 'm', 'a'
  };

  private static final String LATIN1_STRING
      = "Games, HQ, Mang\u00E1, Anime e tudo que um bom nerd ama";

  @Test
  public void testLatin1() {
    ArrayAssert.assertEquals(LATIN1_UTF8_DATA, CharsetUtil.getUtf8Bytes(LATIN1_STRING));
  }
}

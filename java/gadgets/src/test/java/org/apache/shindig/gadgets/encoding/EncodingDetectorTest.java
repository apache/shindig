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
package org.apache.shindig.gadgets.encoding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EncodingDetectorTest {

  @Test
  public void asciiAssumesUtf8() throws Exception {
    byte[] data = "Hello, world".getBytes("US-ASCII");
    assertEquals("UTF-8", EncodingDetector.detectEncoding(data, true));
  }

  @Test
  public void detectedUtf8WithByteOrderMark() {
    byte[] data = new byte[] {
        (byte)0xEF, (byte)0xBB, (byte)0xBF, 'h', 'e', 'l', 'l', 'o'
    };

    assertEquals("UTF-8", EncodingDetector.detectEncoding(data, true));
  }

  @Test
  public void assumeLatin1OnInvalidUtf8() throws Exception {
    byte[] data = "\u4F60\u597D".getBytes("BIG5");

    assertEquals("ISO-8859-1", EncodingDetector.detectEncoding(data, true));
  }

  @Test
  public void doNotAssumeLatin1OnInvalidUtf8() throws Exception {
    byte[] data = ("\u6211\u662F\u4E00\u4E2A\u4E0D\u5584\u4E8E\u8BB2\u8BDD\u7684\u4EBA\uFF0C" +
                   "\u552F\u5176\u4E0D\u5584\u4E8E\u8BB2\u8BDD\uFF0C\u6709\u601D\u60F3\u8868" +
                   "\u8FBE\u4E0D\u51FA\uFF0C\u6709\u611F\u60C5\u65E0\u6CD5\u503E\u5410")
                   .getBytes("GB18030");

    assertEquals("GB18030", EncodingDetector.detectEncoding(data, false));
  }

  @Test
  public void longUtf8StringIsUtf8() throws Exception {
    byte[] data = ("\u6211\u662F\u4E00\u4E2A\u4E0D\u5584\u4E8E\u8BB2\u8BDD\u7684\u4EBA\uFF0C" +
                   "\u552F\u5176\u4E0D\u5584\u4E8E\u8BB2\u8BDD\uFF0C\u6709\u601D\u60F3\u8868" +
                   "\u8FBE\u4E0D\u51FA\uFF0C\u6709\u611F\u60C5\u65E0\u6CD5\u503E\u5410")
                   .getBytes("UTF-8");

    assertEquals("UTF-8", EncodingDetector.detectEncoding(data, true));
  }

  @Test
  public void shortUtf8StringIsUtf8() throws Exception {
    byte[] data = "Games, HQ, Mang\u00E1, Anime e tudo que um bom nerd ama".getBytes("UTF-8");

    assertEquals("UTF-8", EncodingDetector.detectEncoding(data, true));
  }
}

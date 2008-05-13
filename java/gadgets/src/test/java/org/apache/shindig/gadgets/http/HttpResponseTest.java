/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.common.util.InputStreamConsumer;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HttpResponseTest extends TestCase {
  private Map<String, List<String>> headers;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    headers = new HashMap<String, List<String>>();
  }

  private void addHeader(String name, String value) {
    java.util.List<String> existing = headers.get(name);
    if (existing == null) {
      existing = new LinkedList<String>();
      headers.put(name, existing);
    }
    existing.add(value);
  }

  public void testGetEncoding() throws Exception {
    addHeader("Content-Type", "text/plain; charset=TEST-CHARACTER-SET");
    HttpResponse response = new HttpResponse(200, new byte[0], headers);
    assertEquals("TEST-CHARACTER-SET", response.getEncoding());
  }

  public void testEncodingDetectionUtf8WithBom() throws Exception {
    // Input is UTF-8 with BOM.
    byte[] data = new byte[] {
      (byte)0xEF, (byte)0xBB, (byte)0xBF, 'h', 'e', 'l', 'l', 'o'
    };
    addHeader("Content-Type", "text/plain; charset=UTF-8");
    HttpResponse response = new HttpResponse(200, data, headers);
    assertEquals("hello", response.getResponseAsString());
  }

  public void testEncodingDetectionLatin1() throws Exception {
    // Input is a basic latin-1 string with 1 non-UTF8 compatible char.
    byte[] data = new byte[] {
      'h', (byte)0xE9, 'l', 'l', 'o'
    };
    addHeader("Content-Type", "text/plain; charset=iso-8859-1");
    HttpResponse response = new HttpResponse(200, data, headers);
    assertEquals("h\u00E9llo", response.getResponseAsString());
  }

  public void testEncodingDetectionBig5() throws Exception {
    byte[] data = new byte[] {
      (byte)0xa7, (byte)0x41, (byte)0xa6, (byte)0x6e
    };
    addHeader("Content-Type", "text/plain; charset=BIG5");
    HttpResponse response = new HttpResponse(200, data, headers);
    String resp = response.getResponseAsString();
    assertEquals("\u4F60\u597D", response.getResponseAsString());
  }

  public void testPreserveBinaryData() throws Exception {
    byte[] data = new byte[] {
        (byte)0x00, (byte)0xDE, (byte)0xEA, (byte)0xDB, (byte)0xEE, (byte)0xF0
    };
    addHeader("Content-Type", "application/octet-stream");
    HttpResponse response = new HttpResponse(200, data, headers);
    byte[] out = InputStreamConsumer.readToByteArray(response.getResponse());
    assertTrue(Arrays.equals(data, out));
  }
}

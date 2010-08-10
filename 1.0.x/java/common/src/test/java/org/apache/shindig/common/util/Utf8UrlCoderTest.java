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

import org.junit.Test;

public class Utf8UrlCoderTest {
  private static final String RAW_SIMPLE = "Hello, world!";
  private static final String ENCODED_SIMPLE = "Hello%2C+world%21";
  private static final String RAW_COMPLEX = "\u4F60\u597D";
  private static final String ENCODED_COMPLEX = "%E4%BD%A0%E5%A5%BD";

  @Test
  public void encodeSimple() {
    assertEquals(ENCODED_SIMPLE, Utf8UrlCoder.encode(RAW_SIMPLE));
  }

  @Test
  public void decodeSimple() {
    assertEquals(RAW_SIMPLE, Utf8UrlCoder.decode(ENCODED_SIMPLE));
  }

  @Test
  public void encodeComplex() {
    assertEquals(ENCODED_COMPLEX, Utf8UrlCoder.encode(RAW_COMPLEX));
  }

  @Test
  public void decodeComplex() {
    assertEquals(RAW_COMPLEX, Utf8UrlCoder.decode(ENCODED_COMPLEX));
  }
}

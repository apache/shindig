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
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Date;
import java.util.Locale;

public class DateUtilTest {

  String[] rfc1123text = new String[] {
    "Tue, 27 May 2008 05:12:50 GMT",
    "Wed, 28 May 2008 04:40:48 GMT",
    "Mon, 30 Jun 3090 03:29:55 GMT",
    "Fri, 06 Jun 1670 01:57:27 GMT",
  };

  String[] iso8601text = new String[] {
          "2008-05-27T05:12:50.000Z",
          "2008-05-28T04:40:48.000Z",
          "3090-06-30T03:29:55.000Z",
          "1670-06-06T01:57:27.000Z"
   };

  Date[] timeStamps = new Date[] {
    new Date(1211865170000L),
    new Date(1211949648000L),
    new Date(35359385395000L),
    new Date(-9453535353000L),
  };

  @Test
  public void parse() {
    for (int i = 0, j = rfc1123text.length; i < j; ++i) {
      assertEquals(timeStamps[i].getTime(), DateUtil.parseRfc1123Date(rfc1123text[i]).getTime());
    }
  }

  @Test
  public void format() {
    for (int i = 0, j = timeStamps.length; i < j; ++i) {
      assertEquals(rfc1123text[i], DateUtil.formatRfc1123Date(timeStamps[i].getTime()));
    }
  }

  @Test
  public void formatIso8601() {
      for (int i = 0, j = timeStamps.length; i < j; ++i) {
          assertEquals(iso8601text[i], DateUtil.formatIso8601Date(timeStamps[i].getTime()));
      }
  }

  @Test
  public void formatRfc1123Date() {
    for (int i = 0, j = timeStamps.length; i < j; ++i) {
      assertEquals(rfc1123text[i], DateUtil.formatRfc1123Date(timeStamps[i]));
    }
  }

  @Test
  public void formatIso8601Date() {
      for (int i = 0, j = timeStamps.length; i < j; ++i) {
          assertEquals(iso8601text[i], DateUtil.formatIso8601Date(timeStamps[i]));
      }
  }

  @Test
  public void parseMalformedRfc1123() {
    assertNull(DateUtil.parseRfc1123Date("Invalid date format"));
  }

  @Test
  public void parseMalformedIso8691() {
      assertNull(DateUtil.parseIso8601DateTime("invalid date format"));
  }

  @Test
  public void parseWrongTimeZone() {
    String expires = "Mon, 12 May 2008 09:23:29 PDT";
    assertNull(DateUtil.parseRfc1123Date(expires));
  }

  @Test
  public void parseRfc1036() {
    // We don't support this, though RFC 2616 suggests we should
    String expires = "Sunday, 06-Nov-94 08:49:37 GMT";
    assertNull(DateUtil.parseRfc1123Date(expires));
  }

  @Test
  public void parseAsctime() {
    // We don't support this, though RFC 2616 suggests we should
    String expires = "Sun Nov  6 08:49:37 1994";
    assertNull(DateUtil.parseRfc1123Date(expires));
  }

  @Test
  public void formatInWrongLocale() {
    Locale orig = Locale.getDefault();
    try {
      Locale.setDefault(Locale.ITALY);
      formatRfc1123Date();
    } finally {
      Locale.setDefault(orig);
    }
  }
}

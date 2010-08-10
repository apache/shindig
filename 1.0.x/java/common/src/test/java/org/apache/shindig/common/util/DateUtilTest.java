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

  String[] text = new String[] {
    "Tue, 27 May 2008 05:12:50 GMT",
    "Wed, 28 May 2008 04:40:48 GMT",
    "Mon, 30 Jun 3090 03:29:55 GMT",
    "Fri, 06 Jun 1670 01:57:27 GMT",
  };

  Date[] timeStamps = new Date[] {
    new Date(1211865170000L),
    new Date(1211949648000L),
    new Date(35359385395000L),
    new Date(-9453535353000L),
  };

  @Test
  public void parse() {
    for (int i = 0, j = text.length; i < j; ++i) {
      assertEquals(timeStamps[i].getTime(), DateUtil.parseDate(text[i]).getTime());
    }
  }

  @Test
  public void format() {
    for (int i = 0, j = timeStamps.length; i < j; ++i) {
      assertEquals(text[i], DateUtil.formatDate(timeStamps[i].getTime()));
    }
  }

  @Test
  public void formatDate() {
    for (int i = 0, j = timeStamps.length; i < j; ++i) {
      assertEquals(text[i], DateUtil.formatDate(timeStamps[i]));
    }
  }

  @Test
  public void parseMalformed() {
    assertNull(DateUtil.parseDate("Invalid date format"));
  }

  @Test
  public void parseWrongTimeZone() {
    String expires = "Mon, 12 May 2008 09:23:29 PDT";
    assertNull(DateUtil.parseDate(expires));
  }

  @Test
  public void parseRfc1036() {
    // We don't support this, though RFC 2616 suggests we should
    String expires = "Sunday, 06-Nov-94 08:49:37 GMT";
    assertNull(DateUtil.parseDate(expires));
  }

  @Test
  public void parseAsctime() {
    // We don't support this, though RFC 2616 suggests we should
    String expires = "Sun Nov  6 08:49:37 1994";
    assertNull(DateUtil.parseDate(expires));
  }

  @Test
  public void formatInWrongLocale() {
    Locale orig = Locale.getDefault();
    try {
      Locale.setDefault(Locale.ITALY);
      formatDate();
    } finally {
      Locale.setDefault(orig);
    }
  }
}

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
package org.apache.shindig.gadgets.http;

import junit.framework.TestCase;

import java.util.Date;
import java.util.Locale;

public class HttpUtilTest extends TestCase {
  
  public void testFormatInWrongLocale() {
    Locale orig = Locale.getDefault();
    try {
      Locale.setDefault(Locale.ITALY);
      testFormatDate();
    } finally {
      Locale.setDefault(orig);
    }
  }
  
  public void testParseDate_rfc1123() {
    String expires = "Sun, 06 Nov 1994 08:49:37 GMT";
    Date date = HttpUtil.parseDate(expires);
    assertEquals(784111777000L, date.getTime());
    
    date = HttpUtil.parseDate("Mon, 12 May 2008 17:00:18 GMT");
    assertEquals(1210611618000L, date.getTime());
  }

  public void testParseDate_wrongTimeZone() {
    String expires = "Mon, 12 May 2008 09:23:29 PDT";
    assertNull(HttpUtil.parseDate(expires));
  }
  
  public void testParseDate_rfc1036() {
    // We don't support this, though RFC 2616 suggests we should
    String expires = "Sunday, 06-Nov-94 08:49:37 GMT";
    assertNull(HttpUtil.parseDate(expires));
  }
  
  public void testParseDate_asctime() {
    // We don't support this, though RFC 2616 suggests we should
    String expires = "Sun Nov  6 08:49:37 1994";
    assertNull(HttpUtil.parseDate(expires));
  }

  public void testFormatDate() {
    Date date = new Date(784111777000L);
    assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", HttpUtil.formatDate(date));
    
    date = new Date(1210611618000L);
    assertEquals("Mon, 12 May 2008 17:00:18 GMT", HttpUtil.formatDate(date));
  }

}

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

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.Locale;

/**
 * Date parsing and writing utilities.
 */
public class DateUtil {

  private static DateTimeFormatter rfc1123DateFormat = DateTimeFormat
      .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
      .withLocale(Locale.US)
      .withZone(DateTimeZone.UTC);

  private DateUtil() {}

  /**
   * Parses an RFC1123 format date.  Returns null if the date fails to parse for
   * any reason.
   *
   * @param dateStr
   * @return the date
   */
  public static Date parseDate(String dateStr) {
    try {
      return rfc1123DateFormat.parseDateTime(dateStr).toDate();
    } catch (Exception e) {
      // Don't care.
      return null;
    }
  }

  /**
   * Formats an RFC 1123 format date.
   */
  public static String formatDate(Date date) {
    return formatDate(date.getTime());
  }

  /**
   * Formats an RFC 1123 format date.
   */
  public static String formatDate(long timeStamp) {
    return rfc1123DateFormat.print(timeStamp);
  }
}

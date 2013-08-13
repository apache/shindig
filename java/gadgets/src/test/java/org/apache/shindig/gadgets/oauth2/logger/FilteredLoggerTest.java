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
package org.apache.shindig.gadgets.oauth2.logger;

import java.util.ResourceBundle;
import java.util.logging.Level;

import org.apache.shindig.gadgets.oauth2.OAuth2Error;
import org.junit.Assert;
import org.junit.Test;

public class FilteredLoggerTest {
  @Test
  public void testFilteredLogger_1() throws Exception {
    final String className = "";

    final FilteredLogger result = new FilteredLogger(className);

    Assert.assertNotNull(result);
    Assert.assertEquals(false, result.isLoggable());
  }

  @Test
  public void testEntering_1() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final String sourceClass = "";
    final String sourceMethod = "";

    fixture.entering(sourceClass, sourceMethod);
  }

  @Test
  public void testEntering_2() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final String sourceClass = "";
    final String sourceMethod = "";
    final Object param = new Object();

    fixture.entering(sourceClass, sourceMethod, param);
  }

  @Test
  public void testEntering_3() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final String sourceClass = "";
    final String sourceMethod = "";
    final Object[] params = new Object[] {};

    fixture.entering(sourceClass, sourceMethod, params);
  }

  @Test
  public void testExiting_1() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final String sourceClass = "";
    final String sourceMethod = "";

    fixture.exiting(sourceClass, sourceMethod);
  }

  @Test
  public void testExiting_2() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final String sourceClass = "";
    final String sourceMethod = "";
    final Object result = new Object();

    fixture.exiting(sourceClass, sourceMethod, result);
  }

  @Test
  public void testFilterSecrets_1() throws Exception {
    final String in = "a";

    final String result = FilteredLogger.filterSecrets(in);

    Assert.assertEquals("a", result);
  }

  @Test
  public void testFilterSecrets_2() throws Exception {
    final String in = null;

    final String result = FilteredLogger.filterSecrets(in);

    Assert.assertEquals("", result);
  }

  @Test
  public void testFilterSecrets_3() throws Exception {
    final String in = "";

    final String result = FilteredLogger.filterSecrets(in);

    Assert.assertEquals("", result);
  }

  @Test
  public void testFilterSecrets_4() throws Exception {
    final String in = "?access_token=XXX";

    final String result = FilteredLogger.filterSecrets(in);

    Assert.assertEquals("?access_token=REMOVED", result);
  }

  @Test
  public void testFilterSecrets_5() throws Exception {
    final String in = "Authorization: XXX";

    final String result = FilteredLogger.filterSecrets(in);

    Assert.assertEquals("Authorization:REMOVED", result);
  }

  @Test
  public void testGetFilteredLogger_1() throws Exception {
    final String className = "";

    final FilteredLogger result = FilteredLogger.getFilteredLogger(className);

    Assert.assertNotNull(result);
    Assert.assertEquals(false, result.isLoggable());
  }

  @Test
  public void testGetResourceBundle_1() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");

    final ResourceBundle result = fixture.getResourceBundle();

    Assert.assertNotNull(result);
  }

  @Test
  public void testIsLoggable_1() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");

    final boolean result = fixture.isLoggable();

    Assert.assertEquals(false, result);
  }

  @Test
  public void testIsLoggable_2() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");

    final boolean result = fixture.isLoggable();

    Assert.assertEquals(false, result);
  }

  @Test
  public void testIsLoggable_3() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final Level logLevel = Level.FINE;

    final boolean result = fixture.isLoggable(logLevel);

    Assert.assertFalse(result);
  }

  @Test
  public void testLog_1() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final String msg = "";
    final Object param = new Object();

    fixture.log(msg, param);
  }

  @Test
  public void testLog_2() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final String msg = "";
    final Throwable thrown = new Throwable();

    fixture.log(msg, thrown);

  }

  @Test
  public void testLog_3() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final String msg = "";
    final Object[] params = new Object[] {};

    fixture.log(msg, params);
  }

  @Test
  public void testLog_4() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final Level logLevel = Level.FINE;
    final String msg = "";
    final Object param = new Object();

    fixture.log(logLevel, msg, param);
  }

  @Test
  public void testLog_5() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final Level logLevel = Level.FINE;
    final String msg = "";
    final Throwable thrown = new Throwable();

    fixture.log(logLevel, msg, thrown);
  }

  @Test
  public void testLog_6() throws Exception {
    final FilteredLogger fixture = FilteredLogger.getFilteredLogger("");
    final Level logLevel = Level.FINE;
    final String msg = "";
    final Object[] params = new Object[] {};

    fixture.log(logLevel, msg, params);
  }
}

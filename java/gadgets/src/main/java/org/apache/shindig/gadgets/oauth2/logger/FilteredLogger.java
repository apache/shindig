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

import org.apache.shindig.gadgets.oauth2.OAuth2Error;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wraps a {@link Logger} with functions to remove OAuth2 secrets so they don't show up in trace
 * logs.
 *
 */
public class FilteredLogger {
  private static final Level DEFAULT_LOG_LEVEL = Level.FINEST;

  private static final Pattern[] filters = new Pattern[] {
          Pattern.compile("(?<=access_token=)[^=& \t\r\n]*"),
          Pattern.compile("(?<=refresh_token=)[^=& \t\r\n]*"),
          Pattern.compile("(?<=Authorization:)[^\t\r\n]*"),
          Pattern.compile("(?<=client_id:)[^\t\r\n]*"),
          Pattern.compile("(?<=client_id=)[^=& \t\r\n]*"),
          Pattern.compile("(?<=client_secret=)[^=& \t\r\n]*"),
          Pattern.compile("(?<=client_secret:)[^\t\r\n]*") };

  private static String filteredParam(final Object param) {
    final String paramString;
    if (param != null) {
      paramString = FilteredLogger.filterSecrets(param.toString());
    } else {
      paramString = "";
    }

    return paramString;
  }

  private static String[] filteredParams(final Object[] params) {
    final String[] paramStrings;
    if (params != null) {
      paramStrings = new String[params.length];
      int i = 0;
      for (final Object param : params) {
        if (param != null) {
          paramStrings[i] = FilteredLogger.filteredParam(param.toString());
        } else {
          paramStrings[i] = "";
        }
        i++;
      }
    } else {
      paramStrings = new String[] {};
    }

    return paramStrings;
  }

  public static String filterSecrets(final String in) {
    String ret = in;
    if (ret != null && ret.length() > 0) {
      for (final Pattern pattern : FilteredLogger.filters) {
        final Matcher m = pattern.matcher(ret);
        ret = m.replaceAll("REMOVED");
      }
    }

    if (ret == null) {
      ret = "";
    }

    return ret;
  }

  public static FilteredLogger getFilteredLogger(final String className) {
    return new FilteredLogger(className);
  }

  private final Logger logger;

  protected FilteredLogger(final String className) {
    this.logger = java.util.logging.Logger.getLogger(className, OAuth2Error.MESSAGES);
  }

  public void entering(final String sourceClass, final String sourceMethod) {
    this.logger.entering(sourceClass, sourceMethod);
  }

  public void entering(final String sourceClass, final String sourceMethod, final Object param) {
    this.logger.entering(sourceClass, sourceMethod, FilteredLogger.filteredParam(param));
  }

  public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
    this.logger.entering(sourceClass, sourceMethod, FilteredLogger.filteredParams(params));
  }

  public ResourceBundle getResourceBundle() {
    return this.logger.getResourceBundle();
  }

  public boolean isLoggable() {
    return this.isLoggable(FilteredLogger.DEFAULT_LOG_LEVEL);
  }

  public boolean isLoggable(final Level logLevel) {
    return this.logger.isLoggable(logLevel);
  }

  public void log(final Level logLevel, final String msg, final Object param) {
    this.logger.log(logLevel, FilteredLogger.filterSecrets(msg),
            FilteredLogger.filteredParam(param));
  }

  public void log(final Level logLevel, final String msg, final Object[] params) {
    this.logger.log(logLevel, FilteredLogger.filterSecrets(msg),
            FilteredLogger.filteredParams(params));
  }

  public void log(final Level logLevel, final String msg, final Throwable thrown) {
    this.logger.log(logLevel, FilteredLogger.filterSecrets(msg), thrown);
  }

  public void log(final String msg, final Object param) {
    this.log(FilteredLogger.DEFAULT_LOG_LEVEL, msg, param);
  }

  public void log(final String msg, final Object[] params) {
    this.log(FilteredLogger.DEFAULT_LOG_LEVEL, msg, params);
  }

  public void log(final String msg, final Throwable thrown) {
    this.logger.log(FilteredLogger.DEFAULT_LOG_LEVEL, msg, thrown);
  }

  public void exiting(final String sourceClass, final String sourceMethod) {
    this.logger.exiting(sourceClass, sourceMethod);
  }

  public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
    this.logger.exiting(sourceClass, sourceMethod, FilteredLogger.filteredParam(result));
  }
}

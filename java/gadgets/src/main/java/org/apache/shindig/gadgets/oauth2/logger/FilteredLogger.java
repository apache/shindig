/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.shindig.gadgets.oauth2.logger;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.shindig.gadgets.oauth2.OAuth2Error;

/**
 * Wraps a {@link Logger} with functions to remove OAuth2 secrets so they don't
 * show up in trace logs.
 *
 */
public class FilteredLogger {
  private static final Level DEFAULT_LOG_LEVEL = Level.FINEST;

  private static final Pattern REMOVE_SECRETS1 = Pattern.compile("(?<=access_token=)[^=& \t\r\n]*");
  private static final Pattern REMOVE_SECRETS2 = Pattern.compile("(Authorization:)[^\t\r\n]*");

  private static String filteredMsg(final String msg) {
    return FilteredLogger.filterSecrets(msg);
  }

  private static String filteredParam(final Object param) {
    final String _param;
    if (param != null) {
      _param = FilteredLogger.filterSecrets(param.toString());
    } else {
      _param = "";
    }

    return _param;
  }

  private static String[] filteredParams(final Object[] params) {
    final String[] _params;
    if (params != null) {
      _params = new String[params.length];
      int i = 0;
      for (final Object param : params) {
        if (param != null) {
          _params[i] = FilteredLogger.filteredMsg(param.toString());
        } else {
          _params[i] = "";
        }
        i++;
      }
    } else {
      _params = new String[] {};
    }

    return _params;
  }

  public static String filterSecrets(String in) {
    if ((in != null) && (in.length() > 0)) {
      Matcher m = FilteredLogger.REMOVE_SECRETS1.matcher(in);
      final String ret = m.replaceAll("REMOVED");
      m = FilteredLogger.REMOVE_SECRETS2.matcher(ret);
      return m.replaceAll("REMOVED");
    }

    return "";
  }

  public static FilteredLogger getFilteredLogger(final String className) {
    return new FilteredLogger(className);
  }

  private final Logger logger;

  protected FilteredLogger(final String className) {
    this.logger = java.util.logging.Logger.getLogger(className, OAuth2Error.MESSAGES);
  }

  public void entering(String sourceClass, String sourceMethod) {
    this.logger.entering(sourceClass, sourceMethod);
  }

  public void entering(String sourceClass, String sourceMethod, Object param) {
    this.logger.entering(sourceClass, sourceMethod, FilteredLogger.filteredParam(param));
  }

  public void entering(String sourceClass, String sourceMethod, Object[] params) {
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

  public void log(Level logLevel, String msg, Object param) {
    this.logger.log(logLevel, FilteredLogger.filteredMsg(msg), FilteredLogger.filteredParam(param));
  }

  public void log(Level logLevel, String msg, Object[] params) {
    this.logger.log(logLevel, FilteredLogger.filteredMsg(msg),
        FilteredLogger.filteredParams(params));
  }

  public void log(Level logLevel, String msg, Throwable thrown) {
    this.logger.log(logLevel, FilteredLogger.filterSecrets(msg), thrown);
  }

  public void log(String msg, Object param) {
    this.log(FilteredLogger.DEFAULT_LOG_LEVEL, msg, param);
  }

  public void log(String msg, Object[] params) {
    this.log(FilteredLogger.DEFAULT_LOG_LEVEL, msg, params);
  }

  public void log(String msg, Throwable thrown) {
    this.logger.log(FilteredLogger.DEFAULT_LOG_LEVEL, msg, thrown);
  }

  public void exiting(String sourceClass, String sourceMethod) {
    this.logger.exiting(sourceClass, sourceMethod);
  }

  public void exiting(String sourceClass, String sourceMethod, Object result) {
    this.logger.exiting(sourceClass, sourceMethod, FilteredLogger.filteredParam(result));
  }
}
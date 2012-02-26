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
package org.apache.shindig.common.servlet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple class defining basic User-Agent parsing.
 * Defines an interface for a Parser, a list of common Browsers,
 * and an Entry that is consumed by code providing UA-specific behavior.
 */
public final class UserAgent {
  private final Browser browser;
  private final String version;
  private static final Pattern VERSION_NUMBER_REGEX = Pattern.compile(".*?([0-9]+(\\.[0-9]+)?).*");

  public UserAgent(Browser browser, String version) {
    this.browser = browser;
    this.version = version;
  }

  /**
   * @return Identifying browser string.
   */
  public Browser getBrowser() {
    return browser;
  }

  /**
   * @return Version string of user agent.
   */
  public String getVersion() {
    return version != null ? version.trim() : null;
  }

  /**
   * @return Numeric version number, if parseable. Otherwise -1.
   */
  public double getVersionNumber() {
    // Attempt to retrieve the numeric part of a version string.
    Matcher matcher = VERSION_NUMBER_REGEX.matcher(getVersion());
    if (!matcher.matches()) {
      return -1;
    }
    String matched = matcher.group(1);
    return Double.parseDouble(matched);
  }

  public interface Parser {
    UserAgent parse(String userAgent);
  }

  public enum Browser {
    MSIE,
    FIREFOX,
    SAFARI,
    WEBKIT,
    CHROME,
    OPERA,
    HTML5,  // A faux-Browser useful for directly referencing HTML5 JS capability vs. "legacy".
    OTHER
  }
}

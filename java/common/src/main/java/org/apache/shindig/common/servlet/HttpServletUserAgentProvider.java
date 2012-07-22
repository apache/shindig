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

import com.google.inject.Inject;
import com.google.inject.Provider;


import javax.servlet.http.HttpServletRequest;

/**
 * Simple provider of UserAgent information from an HttpServletRequest.
 * Uses an injected UserAgent.Parser to generate a UserAgent.Entry.
 */
public class HttpServletUserAgentProvider implements Provider<UserAgent> {
  private final UserAgent.Parser uaParser;
  private final Provider<HttpServletRequest> reqProvider;

  @Inject
  public HttpServletUserAgentProvider(UserAgent.Parser uaParser,
      Provider<HttpServletRequest> reqProvider) {
    this.uaParser = uaParser;
    this.reqProvider = reqProvider;
  }

  public UserAgent get() {
    HttpServletRequest req = reqProvider.get();
    if (req != null) {
      String userAgent = req.getHeader("User-Agent");
      if (userAgent != null) {
        return uaParser.parse(userAgent);
      }
    }
    return null;
  }
}

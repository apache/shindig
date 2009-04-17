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
package org.apache.shindig.gadgets.servlet;

import com.google.common.collect.Maps;

import org.apache.shindig.common.util.Utf8UrlCoder;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * A proxy request wrapper that supports chained request syntax (e.g.
 * "http://shindig/proxy/additional=parameters/http://remotehost/file").
 */
public class ProxyServletRequest extends HttpServletRequestWrapper {
  protected static final Pattern CHAINED_SYNTAX_PATTERN
      = Pattern.compile("^[^?]+/proxy/([^?/]*)/(.*)$");
  protected static final Pattern PARAMETER_PAIR_PATTERN
      = Pattern.compile("([^&=]+)=([^&=]*)");

  protected final boolean usingChainedSyntax;
  protected final Map<String, String> extractedParameters;

  public ProxyServletRequest(HttpServletRequest request) {
    super(request);
    Matcher chainedMatcher
        = CHAINED_SYNTAX_PATTERN.matcher(request.getRequestURI());
    usingChainedSyntax = chainedMatcher.matches();
    if (usingChainedSyntax) {
      extractedParameters = Maps.newHashMap();

      Matcher paramMatcher
          = PARAMETER_PAIR_PATTERN.matcher(chainedMatcher.group(1));
      while (paramMatcher.find()) {
        extractedParameters.put(Utf8UrlCoder.decode(paramMatcher.group(1)),
                                Utf8UrlCoder.decode(paramMatcher.group(2)));
      }

      String urlParam = Utf8UrlCoder.decode(chainedMatcher.group(2));
      if (request.getQueryString() != null) {
        urlParam += "?" + request.getQueryString();
      }
      extractedParameters.put(ProxyBase.URL_PARAM, urlParam);
                              
    } else {
      extractedParameters = Collections.emptyMap();
    }
  }

  /**
   * @return True if the request is using the chained syntax form.
   */
  public boolean isUsingChainedSyntax() {
    return usingChainedSyntax;
  }

  @Override
  public String getParameter(String name) {
    if (usingChainedSyntax) {
      return extractedParameters.get(name);
    } else {
      return super.getParameter(name);
    }
  }
}

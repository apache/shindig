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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * A proxy request wrapper that supports chained request syntax (e.g.
 * "http://shindig/proxy/additional=parameters/http://remotehost/file").
 */
public class ProxyServletRequest extends HttpServletRequestWrapper {
  protected static Pattern chainedSyntaxPattern = Pattern.compile("^[^?]+/proxy/([^?/]*)/(.*)$");
  protected static Pattern parameterPairPattern = Pattern.compile("([^&=]+)=([^&=]*)");

  protected boolean usingChainedSyntax;
  protected Map<String, String> extractedParameters;

  public ProxyServletRequest(HttpServletRequest request) {
    super(request);
    Matcher chainedSyntaxMatcher = chainedSyntaxPattern.matcher(request.getRequestURI());
    usingChainedSyntax = chainedSyntaxMatcher.matches();
    if (usingChainedSyntax) {
      extractedParameters = new HashMap<String, String>();

      Matcher parameterPairMatcher = parameterPairPattern.matcher(chainedSyntaxMatcher.group(1));
      while (parameterPairMatcher.find()) {
        try {
          extractedParameters.put(URLDecoder.decode(parameterPairMatcher.group(1), "UTF-8"),
                                  URLDecoder.decode(parameterPairMatcher.group(2), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
      }

      extractedParameters.put(ProxyHandler.URL_PARAM, chainedSyntaxMatcher.group(2));
    }
  }

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

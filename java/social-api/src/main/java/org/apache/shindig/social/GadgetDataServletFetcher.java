/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.apache.shindig.social;

import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.servlet.ParameterFetcher;

import com.google.common.collect.Maps;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Default implementation for the GadgetDataServlet parameter fetcher. Do not
 * change unless you have a compelling need to pass more parameters into the
 * createResponse method.
 */
public class GadgetDataServletFetcher implements ParameterFetcher {
  public Map<String, String> fetch(HttpServletRequest req) {
    final Map<String, String> params = Maps.newHashMapWithExpectedSize(2);
    params.put(SecurityTokenDecoder.SECURITY_TOKEN_NAME, req.getParameter("st"));
    params.put(GadgetDataServlet.REQUEST_PARAMETER_NAME, req.getParameter(GadgetDataServlet.REQUEST_PARAMETER_NAME));
    return params;
  }
}


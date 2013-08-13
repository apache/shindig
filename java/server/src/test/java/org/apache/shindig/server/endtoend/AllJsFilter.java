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
package org.apache.shindig.server.endtoend;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.common.servlet.InjectedFilter;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.features.FeatureRegistryProvider;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

public class AllJsFilter extends InjectedFilter {

  private String allFeatures;

  @Inject
  public void setFeatureRegistryProvider(FeatureRegistryProvider provider) {
    try {
      FeatureRegistry registry = provider.get(null);
      Set<String> allFeatureNames = registry.getAllFeatureNames();

      // TODO(felix8a): Temporary hack for caja
      HashSet<String> someFeatureNames = new HashSet<String>(allFeatureNames);
      someFeatureNames.remove("es53-guest-frame");
      someFeatureNames.remove("es53-guest-frame.opt");
      someFeatureNames.remove("es53-taming-frame");
      someFeatureNames.remove("es53-taming-frame.opt");

      allFeatures = Joiner.on(':').join(someFeatureNames);
    } catch (GadgetException e) {
      e.printStackTrace();
    }
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {
    if (!(request instanceof HttpServletRequest && response instanceof HttpServletResponse)) {
      throw new ServletException("Only HTTP!");
    }

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse resp = (HttpServletResponse) response;

    String requestURI = req.getRequestURI();
    if (!requestURI.contains("all-features-please.js")) {
      chain.doFilter(request, response);
    } else {
      String newURI = requestURI.replace("all-features-please.js", allFeatures + ".js") + "?" + req.getQueryString();
      resp.sendRedirect(newURI);
    }
  }

  public void destroy() {
  }
}


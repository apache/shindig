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


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet filter to generate and check ETags in HTTP responses.
 *
 * An ETag is calculated for the servlet's output. If its value matches the
 * value provided in the request's "If-None-Match" header, a 304 Not Modified
 * response is returned; otherwise, the value is added to the response's "ETag"
 * header.
 *
 * Note that when this filter is applied, the response body cannot be streamed.
 */
public class ETagFilter implements Filter {

  public void init(FilterConfig filterConfig) {
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      ETaggingHttpResponse taggingResponse = createTaggingResponse(request, response);
      try {
        chain.doFilter(request, taggingResponse);
      } finally {
        // Write to the output even if there was an exception, as it would have
        // done without this filter.
        taggingResponse.writeToOutput();
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  protected ETaggingHttpResponse createTaggingResponse(
      ServletRequest request, ServletResponse response) {
    return new ETaggingHttpResponse((HttpServletRequest) request, (HttpServletResponse) response);
  }
}

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
package org.apache.shindig.gadgets.js;

import org.apache.shindig.gadgets.uri.UriStatus;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.servlet.http.HttpServletResponse;

/**
 * Returns a 304 Not Modified response if the request is for a versioned
 * resource and contains an If-Modified-Since header. This works in this way
 * because we rely on cache busting for versioned resources.
 */
public class IfModifiedSinceProcessor implements JsProcessor {

  public static final int DEFAULT_VERSIONED_MAXAGE = -1;

  private int versionedMaxAge = DEFAULT_VERSIONED_MAXAGE;

  @Inject(optional=true)
  public void setVersionedMaxAge(@Named("shindig.jscontent.versioned.maxage") Integer maxAge) {
    if (maxAge != null) {
      versionedMaxAge = maxAge;
    }
  }

  public boolean process(JsRequest request, JsResponseBuilder builder) {
    if (request.isInCache() &&
        request.getJsUri().getStatus() == UriStatus.VALID_VERSIONED) {
      builder.setStatusCode(HttpServletResponse.SC_NOT_MODIFIED);
      builder.setCacheTtlSecs(versionedMaxAge);
      return false;
    }
    return true;
  }

}

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

import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;

/**
 * Data about a JavaScript request.
 * 
 * This class is instantiated via {@link JsRequestBuilder}.
 */
public class JsRequest {

  private final JsUri jsUri;
  private final String host;
  private final boolean inCache;

  JsRequest(JsUri jsUri, String host, boolean inCache) {
    this.jsUri = jsUri;
    this.host = host;
    this.inCache = inCache;
  }
 
  /**
   * Returns this request's {@link JsUri}.
   */
  public JsUri getJsUri() {
    return jsUri;
  }

  /**
   * Returns the host this request was directed to.
   */
  public String getHost() {
    return host;
  }

  /**
   * Returns whether the client has this JS code in the cache.
   */
  public boolean isInCache() {
    return inCache;
  }
}

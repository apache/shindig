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

package org.apache.shindig.gadgets.js;

import com.google.inject.Inject;

import org.apache.shindig.gadgets.servlet.JsHandler;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.apache.shindig.gadgets.uri.UriStatus;

/**
 * Retrieves the requested Javascript code using a {@link JsHandler}.
 */
public class GetJsContentProcessor implements JsProcessor {

  private final JsHandler jsHandler;

  @Inject
  public GetJsContentProcessor(JsHandler jsHandler) {
    this.jsHandler = jsHandler;
  }
  
  public boolean process(JsRequest request, JsResponseBuilder builder) throws JsException {
    // Get JavaScript content from features aliases request.
    JsUri jsUri = request.getJsUri();
    JsResponse handlerResponse =
        jsHandler.getJsContent(jsUri, request.getHost());
    builder.setProxyCacheable(handlerResponse.isProxyCacheable());    
    setResponseCacheTtl(builder, jsUri.getStatus());
    builder.appendJs(handlerResponse.allJs());
    return true;
  }

  /**
   * Sets the cache TTL depending on the value of the {@link UriStatus} object.
   *
   * @param resp The {@link JsResponseBuilder} object.
   * @param vstatus The {@link UriStatus} object.
   */
  protected void setResponseCacheTtl(JsResponseBuilder resp, UriStatus vstatus) {
    switch (vstatus) {
      case VALID_VERSIONED:
        // Versioned files get cached indefinitely
        resp.setCacheTtlSecs(-1);
        break;
      case VALID_UNVERSIONED:
        // Unversioned files get cached for 1 hour.
        resp.setCacheTtlSecs(60 * 60);
        break;
      case INVALID_VERSION:
        // URL is invalid in some way, likely version mismatch.
        // Indicate no-cache forcing subsequent requests to regenerate URLs.
        resp.setCacheTtlSecs(0);
        break;
    }
  }

}

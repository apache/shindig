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
package org.apache.shindig.gadgets.render;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.uri.ProxyUriManager;

import java.util.List;

/**
 * Forcible rewrite the link through the proxy and force sanitization with
 * an expected mime type.
 *
 * @since 2.0.0
 */
public class SanitizingProxyUriManager implements ProxyUriManager {
  private final ProxyUriManager wrapped;
  private final String expectedMime;

  public SanitizingProxyUriManager(ProxyUriManager wrapped, String expectedMime) {
    this.wrapped = wrapped;
    this.expectedMime = expectedMime;
  }

  public ProxyUri process(Uri uri) throws GadgetException {
    return wrapped.process(uri);
  }

  public List<Uri> make(List<ProxyUri> ctx, Integer forcedRefresh) {
    // Just wraps the original ProxyUriManager and adds a few query params.
    for (ProxyUri proxyUri : ctx) {
      proxyUri.setSanitizeContent(true);
      if (expectedMime != null) {
        proxyUri.setRewriteMimeType(expectedMime);
      }
    }

    return wrapped.make(ctx, forcedRefresh);
  }
}

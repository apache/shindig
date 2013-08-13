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
package org.apache.shindig.gadgets.uri;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import com.google.common.collect.Lists;

public class PassthruManager implements ProxyUriManager {
  private UriStatus expectStatus = UriStatus.VALID_VERSIONED;

  private boolean doProxy = false;
  private String proxyHost = null;
  private String proxyPath = null;

  public PassthruManager() {
    // Regular no-proxy mode.
  }

  public PassthruManager(String proxyHost, String proxyPath) {
    this.proxyHost = proxyHost;
    this.proxyPath = proxyPath;
    this.doProxy = true;
  }

  public List<Uri> make(List<ProxyUri> resource, Integer forcedRefresh) {
    List<Uri> ctx = Lists.newArrayListWithCapacity(resource.size());
    for (ProxyUri res : resource) {
      ctx.add(getUri(res));
    }
    return ImmutableList.copyOf(ctx);
  }

  private Uri getUri(ProxyUri src) {
    if (!doProxy) {
      return src.getResource();
    }
    UriBuilder builder =
        new UriBuilder().setScheme("http").setAuthority(proxyHost).setPath(proxyPath)
          .addQueryParameter(Param.URL.getKey(), src.getResource().toString());
    if (src.sanitizeContent()) {
      builder.addQueryParameter(Param.SANITIZE.getKey(), "1");
    }
    if (src.getRewriteMimeType() != null) {
      builder.addQueryParameter(Param.REWRITE_MIME_TYPE.getKey(), src.getRewriteMimeType());
    }
    return builder.toUri();
  }

  public ProxyUri process(Uri uri) throws GadgetException {
    String proxied = uri.getQueryParameter(Param.URL.getKey());
    ProxyUri proxyUri = new ProxyUri(expectStatus,
        proxied != null ? Uri.parse(proxied) : null, uri);
    proxyUri.setHtmlTagContext(uri.getQueryParameter(Param.HTML_TAG_CONTEXT.getKey()));
    return proxyUri;
  }

  public void expectStatus(UriStatus status) {
    this.expectStatus = status;
  }
}

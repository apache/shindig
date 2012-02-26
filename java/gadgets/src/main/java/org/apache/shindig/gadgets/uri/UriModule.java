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

import com.google.inject.AbstractModule;
import com.google.inject.util.Providers;

/**
 * Provides default configuration and bindings for Uri classes.
 *
 * @since 2.0.0
 */
public class UriModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(IframeUriManager.class).to(DefaultIframeUriManager.class);
    bind(IframeUriManager.Versioner.class).to(AllJsIframeVersioner.class);

    bind(JsUriManager.class).to(DefaultJsUriManager.class);
    bind(JsUriManager.Versioner.class).to(DefaultJsVersioner.class);

    bind(OAuthUriManager.class).to(DefaultOAuthUriManager.class);

    bind(ProxyUriManager.class).to(DefaultProxyUriManager.class);
    bind(ProxyUriManager.Versioner.class)
        .toProvider(Providers.<ProxyUriManager.Versioner>of(null));

    bind(ConcatUriManager.class).to(DefaultConcatUriManager.class);
    bind(ConcatUriManager.Versioner.class)
        .toProvider(Providers.<ConcatUriManager.Versioner>of(null));
  }

}

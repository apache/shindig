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
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.GadgetTestFixture;
import org.apache.shindig.gadgets.GadgetTokenDecoder;
import org.apache.shindig.gadgets.SigningFetcherFactory;


public abstract class HttpTestFixture extends GadgetTestFixture {
  public final ProxyHandler proxyHandler;
  public final GadgetRenderer gadgetRenderer;
  public final JsonRpcHandler jsonRpcHandler;
  public final SigningFetcherFactory signingFetcherFactory
      = mock(SigningFetcherFactory.class);
  public final UrlGenerator urlGenerator = mock(UrlGenerator.class);
  public final GadgetTokenDecoder gadgetTokenDecoder
      = mock(GadgetTokenDecoder.class);

  public HttpTestFixture() {
    super();
    proxyHandler = new ProxyHandler(
        fetcher,
        gadgetTokenDecoder,
        signingFetcherFactory,
        null);
    gadgetRenderer = new GadgetRenderer(
          gadgetServer, registry, containerConfig, urlGenerator);
    jsonRpcHandler = new JsonRpcHandler(executor, gadgetServer, urlGenerator);
  }
}

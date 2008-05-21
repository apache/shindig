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

package org.apache.shindig.gadgets;

import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.rewrite.NoOpContentRewriter;

import java.util.concurrent.Executor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class GadgetTestFixture extends EasyMockTestCase {
  public final HttpServletRequest request = mock(HttpServletRequest.class);
  public final HttpServletResponse response = mock(HttpServletResponse.class);
  public final SecurityTokenDecoder securityTokenDecoder
      = mock(SecurityTokenDecoder.class);
  public final GadgetServer gadgetServer;
  public final ContentFetcherFactory fetcherFactory
      = mock(ContentFetcherFactory.class);
  public final HttpFetcher fetcher = mock(HttpFetcher.class);
  public final GadgetBlacklist blacklist = mock(GadgetBlacklist.class);
  public final GadgetSpecFactory specFactory =
      new BasicGadgetSpecFactory(fetcher, new NoOpContentRewriter(), true, 0);
  public final MessageBundleFactory bundleFactory =
      new BasicMessageBundleFactory(fetcher, 0);
  public GadgetFeatureRegistry registry;
  public ContainerConfig containerConfig;
  public final Executor executor = new Executor() {
    public void execute(Runnable r) {
      r.run();
    }
  };


  public GadgetTestFixture() {
    try {
      registry = new GadgetFeatureRegistry(null, fetcher);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to create feature registry");
    }

    try {
      containerConfig = new ContainerConfig(null);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to create container config");
    }

    gadgetServer = new GadgetServer(executor, registry, blacklist,
        fetcherFactory, specFactory, bundleFactory);
  }
}

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

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.DefaultCacheProvider;
import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.oauth.OAuthFetcher;
import org.apache.shindig.gadgets.rewrite.CaptureRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.ContentRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.DefaultContentRewriterRegistry;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

// DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
public abstract class GadgetTestFixture extends EasyMockTestCase {
  // TODO: Remove all of these.

  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final GadgetServer gadgetServer;
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final ContentFetcherFactory fetcherFactory = mock(ContentFetcherFactory.class);
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final HttpFetcher fetcher = mock(HttpFetcher.class);
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final OAuthFetcher oauthFetcher = mock(OAuthFetcher.class);
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final GadgetBlacklist blacklist = mock(GadgetBlacklist.class);
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  private final CacheProvider cacheProvider = new DefaultCacheProvider();
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final MessageBundleFactory bundleFactory =
      new BasicMessageBundleFactory(fetcher, cacheProvider, 0, 0L, 0L);
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final GadgetFeatureRegistry registry;
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final ContainerConfig containerConfig = mock(ContainerConfig.class);
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final CaptureRewriter rewriter = new CaptureRewriter();
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final ContentRewriterRegistry rewriterRegistry
      = new DefaultContentRewriterRegistry(Arrays.<ContentRewriter>asList(rewriter), null);
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final FakeTimeSource timeSource = new FakeTimeSource();
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final ExecutorService executor = new TestExecutorService();
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public final GadgetSpecFactory specFactory = new BasicGadgetSpecFactory(
      fetcher, cacheProvider, executor, 0, 0L, 0L);
  // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
  public GadgetTestFixture() {
    try {
      // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
      registry = new GadgetFeatureRegistry(null, fetcher);
      // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
      gadgetServer = new GadgetServer(executor, registry, blacklist,
          containerConfig, rewriterRegistry, null, fetcherFactory, specFactory, bundleFactory);
      // DO NOT ADD ANYTHING ELSE TO THIS CLASS. IT IS GOING AWAY SOON!!!
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

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
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthFetcher;
import org.apache.shindig.gadgets.rewrite.DefaultContentRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;

import java.util.concurrent.ExecutorService;

public abstract class GadgetTestFixture extends EasyMockTestCase {
  // TODO: Remove all of these.
  public final GadgetServer gadgetServer;
  public final ContentFetcherFactory fetcherFactory = mock(ContentFetcherFactory.class);
  public final HttpFetcher fetcher = mock(HttpFetcher.class);
  public final OAuthFetcher oauthFetcher = mock(OAuthFetcher.class);
  public final GadgetBlacklist blacklist = mock(GadgetBlacklist.class);
  private final CacheProvider cacheProvider = new DefaultCacheProvider();
  public final MessageBundleFactory bundleFactory =
      new BasicMessageBundleFactory(fetcher, cacheProvider, 0, 0L, 0L);
  public final GadgetFeatureRegistry registry;
  public final ContainerConfig containerConfig = mock(ContainerConfig.class);
  public final CaptureRewriter rewriter = new CaptureRewriter();
  public final FakeTimeSource timeSource = new FakeTimeSource();
  public final ExecutorService executor = new TestExecutorService();
  public final GadgetSpecFactory specFactory = new BasicGadgetSpecFactory(
      fetcher, cacheProvider, executor, 0, 0L, 0L);

  public GadgetTestFixture() {
    try {
      registry = new GadgetFeatureRegistry(null, fetcher);
      gadgetServer = new GadgetServer(executor, registry, blacklist,
          containerConfig, new DefaultContentRewriterRegistry(rewriter, null),
          null, fetcherFactory, specFactory, bundleFactory);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static class CaptureRewriter implements ContentRewriter {
    private boolean rewroteView = false;
    private boolean rewroteResponse = false;

    public void rewrite(HttpRequest request, HttpResponse original, MutableContent content) {
      rewroteResponse = true;
    }

    public boolean responseWasRewritten() {
      return rewroteResponse;
    }

    public void rewrite(Gadget gadget) {
      rewroteView = true;
    }

    public boolean viewWasRewritten() {
      return rewroteView;
    }
  }
}

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
import org.apache.shindig.common.SecurityTokenDecoder;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.DefaultCacheProvider;
import org.apache.shindig.common.util.FakeTimeSource;
import org.apache.shindig.gadgets.http.ContentFetcherFactory;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.oauth.OAuthFetcher;
import org.apache.shindig.gadgets.rewrite.BasicContentRewriterRegistry;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.NoOpContentRewriter;
import org.apache.shindig.gadgets.servlet.GadgetRenderingTask;
import org.apache.shindig.gadgets.servlet.HttpServletResponseRecorder;
import org.apache.shindig.gadgets.servlet.HttpUtil;
import org.apache.shindig.gadgets.servlet.JsonRpcHandler;
import org.apache.shindig.gadgets.servlet.UrlGenerator;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class GadgetTestFixture extends EasyMockTestCase {
  public final HttpServletRequest request = mock(HttpServletRequest.class);
  public final HttpServletResponse response = mock(HttpServletResponse.class);
  public final HttpServletResponseRecorder recorder = new HttpServletResponseRecorder(response);
  public final SecurityTokenDecoder securityTokenDecoder
      = mock(SecurityTokenDecoder.class);
  public final GadgetServer gadgetServer;
  public final ContentFetcherFactory fetcherFactory
      = mock(ContentFetcherFactory.class);
  public final HttpFetcher fetcher = mock(HttpFetcher.class);
  public final SigningFetcher signingFetcher = mock(SigningFetcher.class);
  public final OAuthFetcher oauthFetcher = mock(OAuthFetcher.class);
  public final ContentFetcherFactory contentFetcherFactory = mock(ContentFetcherFactory.class);
  public final GadgetBlacklist blacklist = mock(GadgetBlacklist.class);
  private final CacheProvider cacheProvider = new DefaultCacheProvider();
  public final MessageBundleFactory bundleFactory =
      new BasicMessageBundleFactory(fetcher, cacheProvider, 0, 0L, 0L);
  public final GadgetFeatureRegistry registry;
  public final ContainerConfig containerConfig = mock(ContainerConfig.class);

  public final GadgetRenderingTask gadgetRenderer;
  public final JsonRpcHandler jsonRpcHandler;
  public final UrlGenerator urlGenerator = mock(UrlGenerator.class);
  public final LockedDomainService lockedDomainService = mock(LockedDomainService.class);
  public final ContentRewriter rewriter = new NoOpContentRewriter();
  public final FakeTimeSource timeSource = new FakeTimeSource();
  public final ExecutorService executor = new AbstractExecutorService() {
    private boolean shutdown;

    public void execute(Runnable command) {
      command.run();
    }

    public boolean isTerminated() {
      return shutdown;
    }

    public boolean isShutdown() {
      return shutdown;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return true;
    }

    public void shutdown() {
      shutdown = true;
    }

    public List<Runnable> shutdownNow() {
      shutdown();
      return Collections.emptyList();
    }
  };
  public final GadgetSpecFactory specFactory = new BasicGadgetSpecFactory(
      fetcher, cacheProvider, new BasicContentRewriterRegistry(null), executor, 0, 0L, 0L);


  public GadgetTestFixture() {
    try {
      HttpUtil.setTimeSource(timeSource);
      registry = new GadgetFeatureRegistry(null, fetcher);
      gadgetServer = new GadgetServer(executor, registry, blacklist,
          fetcherFactory, specFactory, bundleFactory);
      gadgetRenderer = new GadgetRenderingTask(gadgetServer, bundleFactory,
          registry, containerConfig, urlGenerator, securityTokenDecoder, lockedDomainService);
      jsonRpcHandler = new JsonRpcHandler(executor, gadgetServer, urlGenerator);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

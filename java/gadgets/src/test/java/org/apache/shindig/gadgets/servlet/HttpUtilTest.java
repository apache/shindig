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
package org.apache.shindig.gadgets.servlet;

import static org.easymock.EasyMock.expect;

import org.apache.shindig.config.AbstractContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class HttpUtilTest extends ServletTestFixture {
  private final static String CONTAINER = "container";
  private final static String FEATURE_0 = "featureZero";
  private final static String FEATURE_1 = "feature-One";
  private final FakeContainerConfig containerConfig = new FakeContainerConfig();

  private final GadgetContext context = mock(GadgetContext.class);

  public void testSetCachingHeaders() {
    HttpUtil.setCachingHeaders(recorder);
    replay();

    checkCacheControlHeaders(HttpUtil.DEFAULT_TTL, false);
  }

  public void testSetCachingHeadersNoProxy() {
    HttpUtil.setCachingHeaders(recorder, true);
    replay();

    checkCacheControlHeaders(HttpUtil.DEFAULT_TTL, true);
  }

  public void testSetCachingHeadersAllowProxy() {
    HttpUtil.setCachingHeaders(recorder, false);
    replay();

    checkCacheControlHeaders(HttpUtil.DEFAULT_TTL, false);
  }

  public void testSetCachingHeadersFixedTtl() {
    int ttl = 10;
    HttpUtil.setCachingHeaders(recorder, ttl);
    replay();

    checkCacheControlHeaders(ttl, false);
  }

  public void testSetCachingHeadersWithTtlAndNoProxy() {
    int ttl = 20;
    HttpUtil.setCachingHeaders(recorder, ttl, true);
    replay();

    checkCacheControlHeaders(ttl, true);
  }

  public void testSetCachingHeadersNoCache() {
    HttpUtil.setCachingHeaders(recorder, 0);
    replay();

    checkCacheControlHeaders(0, true);
  }

  public void testGetJsConfig() {
    Map<String, String> features = ImmutableMap.of(
        FEATURE_0, "config",
        FEATURE_1, "other config"
    );

    Set<String> needed = ImmutableSet.of(FEATURE_0, FEATURE_1);

    expect(context.getContainer()).andReturn(CONTAINER);

    containerConfig.data.put("gadgets.features", features);
    replay();

    assertEquals(features,  HttpUtil.getJsConfig(containerConfig, context, needed));
  }

  public void testGetJsConfigNoFeatures() {
    expect(context.getContainer()).andReturn(CONTAINER);

    replay();

    Map<String, Object> results = HttpUtil.getJsConfig(containerConfig, context,
        Collections.<String>emptySet());

    assertEquals("Results should be empty when there are no features", 0, results.size());
  }

  private static class FakeContainerConfig extends AbstractContainerConfig {
    protected final Map<String, Object> data = Maps.newHashMap();

    @Override
    public Object getProperty(String container, String name) {
      return data.get(name);
    }

  }
}

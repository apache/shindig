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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.AuthType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link CacheKeyBuilder}.
 *
 * <p>These tests are critical in that when the implementation of the CacheKeyBuilder changes,
 * the tests seen below <em>must</em> still pass, unmodified.
 *
 * <p>This ensures smooth rolling restart of the high-load servers with persistent cache, for which
 * changing the caching scheme across runs would generate lots of traffic due to artificial
 * cache misses.
 */
public class CacheKeyBuilderTest extends Assert {

  private CacheKeyBuilder builder;

  @Before
  public void setUp() {
    builder = new CacheKeyBuilder()
        .setLegacyParam(0, Uri.parse("http://example.com"))
        .setLegacyParam(1, AuthType.SIGNED);
  }

  @Test
  public void testBuilder() {
    assertEquals("http://example.com:signed:0:0:0:0:0:0:0", builder.build());
  }

  @Test
  public void testOwner() {
    builder.setLegacyParam(2, "owner");
    assertEquals("http://example.com:signed:owner:0:0:0:0:0:0", builder.build());
  }

  @Test
  public void testViewer() {
    builder.setLegacyParam(3, "viewer");
    assertEquals("http://example.com:signed:0:viewer:0:0:0:0:0", builder.build());
  }

  @Test
  public void testTokenOwner() {
    builder.setLegacyParam(4, "token");
    assertEquals("http://example.com:signed:0:0:token:0:0:0:0", builder.build());
  }

  @Test
  public void testAppUrl() {
    builder.setLegacyParam(5, "appurl");
    assertEquals("http://example.com:signed:0:0:0:appurl:0:0:0", builder.build());
  }

  @Test
  public void testInstanceId() {
    builder.setLegacyParam(6, "id");
    assertEquals("http://example.com:signed:0:0:0:0:id:0:0", builder.build());
  }

  @Test
  public void testServiceName() {
    builder.setLegacyParam(7, "srv");
    assertEquals("http://example.com:signed:0:0:0:0:0:srv:0", builder.build());
  }

  @Test
  public void testTokenName() {
    builder.setLegacyParam(8, "token");
    assertEquals("http://example.com:signed:0:0:0:0:0:0:token", builder.build());
  }

  // The additional parameters, proxy image dimensions
  @Test
  public void testParam() {
    builder.setParam("rh", 1);
    assertEquals("http://example.com:signed:0:0:0:0:0:0:0:rh=1", builder.build());
  }

  @Test
  public void testResizeParams() {
    builder.setParam("rh", 1);
    builder.setParam("rq", 2);
    builder.setParam("rw", 3);
    assertEquals("http://example.com:signed:0:0:0:0:0:0:0:rh=1:rq=2:rw=3", builder.build());
  }
}

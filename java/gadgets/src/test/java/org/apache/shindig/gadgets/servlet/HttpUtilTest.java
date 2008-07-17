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
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HttpUtilTest {
  private final static String CONTAINER = "container";
  private final static String FEATURE_0 = "featureZero";
  private final static String FEATURE_1 = "feature-One";

  private final ContainerConfig config = createNiceMock(ContainerConfig.class);
  private final GadgetContext context = createNiceMock(GadgetContext.class);

  private final ServletTestFixture fixture = new ServletTestFixture();

  @Test
  public void setCachingHeaders() {
    HttpUtil.setCachingHeaders(fixture.recorder);
    fixture.replay();

    fixture.checkCacheControlHeaders(HttpUtil.DEFAULT_TTL, false);
  }

  @Test
  public void setCachingHeadersNoProxy() {
    HttpUtil.setCachingHeaders(fixture.recorder, true);
    fixture.replay();

    fixture.checkCacheControlHeaders(HttpUtil.DEFAULT_TTL, true);
  }

  @Test
  public void setCachingHeadersAllowProxy() {
    HttpUtil.setCachingHeaders(fixture.recorder, false);
    fixture.replay();

    fixture.checkCacheControlHeaders(HttpUtil.DEFAULT_TTL, false);
  }

  @Test
  public void setCachingHeadersFixedTtl() {
    int ttl = 10;
    HttpUtil.setCachingHeaders(fixture.recorder, ttl);
    fixture.replay();

    fixture.checkCacheControlHeaders(ttl, false);
  }

  @Test
  public void setCachingHeadersWithTtlAndNoProxy() {
    int ttl = 20;
    HttpUtil.setCachingHeaders(fixture.recorder, ttl, true);
    fixture.replay();

    fixture.checkCacheControlHeaders(ttl, true);
  }

  @Test
  public void setCachingHeadersNoCache() {
    HttpUtil.setCachingHeaders(fixture.recorder, 0);
    fixture.replay();

    fixture.checkCacheControlHeaders(0, true);
  }

  private void assertJsonEquals(JSONObject lhs, JSONObject rhs) throws JSONException {
    for (String key : JSONObject.getNames(lhs)) {
      Object obj = lhs.get(key);
      if (obj instanceof String) {
        assertEquals(obj, rhs.get(key));
      } else if (obj instanceof JSONObject) {
        assertJsonEquals((JSONObject)obj, rhs.getJSONObject(key));
      } else {
        fail("Unsupported type: " + obj.getClass());
      }
    }
  }

  @Test
  public void getJsConfig() throws JSONException {
    JSONObject features = new JSONObject()
        .put(FEATURE_0, "config")
        .put(FEATURE_1, "other config");

    Set<String> needed = new HashSet<String>();
    needed.add(FEATURE_0);
    needed.add(FEATURE_1);

    expect(context.getContainer()).andReturn(CONTAINER);
    expect(config.getJsonObject(CONTAINER, "gadgets.features"))
        .andReturn(features);

    replay(context);
    replay(config);

    assertJsonEquals(features, HttpUtil.getJsConfig(config, context, needed));
  }

  @Test
  public void getJsConfigNoFeatures() {
    expect(context.getContainer()).andReturn(CONTAINER);
    expect(config.getJsonObject(CONTAINER, "gadgets.features"))
        .andReturn(null);

    replay(context);
    replay(config);

    JSONObject results = HttpUtil.getJsConfig(config, context, Collections.<String>emptySet());

    assertEquals("Results should be empty when there are no features", 0, results.length());
  }
}
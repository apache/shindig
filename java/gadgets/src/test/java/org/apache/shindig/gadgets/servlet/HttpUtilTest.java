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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.shindig.gadgets.ContainerConfig;
import org.apache.shindig.gadgets.GadgetContext;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

public class HttpUtilTest {

  @Test
  public void setCachingHeaders() {
    HttpServletResponse response = createNiceMock(HttpServletResponse.class);
    int ttl = 10;
    response.setDateHeader(eq("Expires"), anyLong());
    response.setHeader("Cache-Control", "public,max-age=" + ttl);
    response.setDateHeader(eq("Last-Modified"), anyLong());
    replay(response);
    HttpUtil.setCachingHeaders(response, ttl);
    verify(response);
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
    String feature0 = "featureZero";
    String feature1 = "featureOne";

    JSONObject features = new JSONObject()
        .put(feature0, "config")
        .put(feature1, "other config");

    String container = "container";

    ContainerConfig config = createNiceMock(ContainerConfig.class);
    GadgetContext context = createNiceMock(GadgetContext.class);
    Set<String> needed = new HashSet<String>();
    needed.add(feature0);
    needed.add(feature1);

    expect(context.getContainer()).andReturn(container);
    expect(config.getJsonObject(container, "gadgets.features"))
        .andReturn(features);

    replay(context);
    replay(config);

    assertJsonEquals(features, HttpUtil.getJsConfig(config, context, needed));
  }
}

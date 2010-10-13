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

import com.google.caja.util.Lists;

import junit.framework.TestCase;

import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class JsUriManagerTest extends TestCase {
  private static final UriStatus STATUS = UriStatus.VALID_UNVERSIONED;
  private static final List<String> LIBS = Lists.newArrayList("feat1", "feat2");
  private static final String CONTAINER_VALUE = "ig";
  private static final String ONLOAD_VALUE = "ol";

  @Test
  public void testJsUriNormal() throws Exception {
    UriBuilder builder = new UriBuilder();
    builder.setScheme("http");
    builder.setAuthority("localohst");
    builder.setPath("/gadgets/js/feature.js");
    builder.addQueryParameter(Param.CONTAINER.getKey(), CONTAINER_VALUE);
    builder.addQueryParameter(Param.CONTAINER_MODE.getKey(), "1");
    builder.addQueryParameter(Param.JSLOAD.getKey(), "1");
    builder.addQueryParameter(Param.NO_CACHE.getKey(), "1");
    builder.addQueryParameter(Param.ONLOAD.getKey(), ONLOAD_VALUE);

    JsUriManager.JsUri jsUri = new JsUriManager.JsUri(STATUS, builder.toUri(), LIBS);
    assertEquals(RenderingContext.CONTAINER, jsUri.getContext());
    assertEquals(CONTAINER_VALUE, jsUri.getContainer());
    assertTrue(jsUri.isJsload());
    assertTrue(jsUri.isNoCache());
    assertEquals(ONLOAD_VALUE, jsUri.getOnload());
    assertEquals(LIBS, Lists.newArrayList(jsUri.getLibs()));
  }

  @Test
  public void testJsUriNullUri() throws Exception {
    JsUriManager.JsUri jsUri = new JsUriManager.JsUri(STATUS, null,
        Collections.<String>emptyList()); // Null URI.
    assertEquals(RenderingContext.GADGET, jsUri.getContext());
    assertEquals(ContainerConfig.DEFAULT_CONTAINER, jsUri.getContainer());
    assertFalse(jsUri.isJsload());
    assertFalse(jsUri.isNoCache());
    assertNull(ONLOAD_VALUE, jsUri.getOnload());
    assertTrue(jsUri.getLibs().isEmpty());
  }
}

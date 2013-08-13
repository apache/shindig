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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.JsCompileMode;
import org.apache.shindig.gadgets.RenderingContext;
import org.apache.shindig.gadgets.uri.UriCommon.Param;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class JsUriManagerTest extends UriManagerTestBase {
  private static final UriStatus STATUS = UriStatus.VALID_UNVERSIONED;
  private static final List<String> LIBS = Lists.newArrayList("feat1", "feat2");
  private static final List<String> HAVE = Lists.newArrayList("have1", "have2");
  private static final String CONTAINER_VALUE = "ig";
  private static final String ONLOAD_VALUE = "ol";

  @Test
  public void newJsUriWithOriginalUri() throws Exception {
    Uri uri = newTestUriBuilder(RenderingContext.CONTAINER,
        JsCompileMode.CONCAT_COMPILE_EXPORT_ALL).toUri();
    JsUriManager.JsUri jsUri = new JsUriManager.JsUri(STATUS, uri, LIBS, HAVE);
    assertEquals(RenderingContext.CONTAINER, jsUri.getContext());
    assertEquals(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL, jsUri.getCompileMode());
    assertEquals(CONTAINER_VALUE, jsUri.getContainer());
    assertTrue(jsUri.isJsload());
    assertTrue(jsUri.isNoCache());
    assertTrue(jsUri.isNohint());
    assertEquals(ONLOAD_VALUE, jsUri.getOnload());
    assertEquals(LIBS, Lists.newArrayList(jsUri.getLibs()));
    assertEquals(HAVE, Lists.newArrayList(jsUri.getLoadedLibs()));
    assertEquals(uri, jsUri.getOrigUri());
  }

  @Test
  public void newJsUriWithConfiguredGadgetContext() throws Exception {
    Uri uri = newTestUriBuilder(RenderingContext.CONFIGURED_GADGET,
        JsCompileMode.CONCAT_COMPILE_EXPORT_ALL).toUri();
    JsUriManager.JsUri jsUri = new JsUriManager.JsUri(STATUS, uri, LIBS, HAVE);
    assertEquals(RenderingContext.CONFIGURED_GADGET, jsUri.getContext());
    assertEquals(JsCompileMode.CONCAT_COMPILE_EXPORT_ALL, jsUri.getCompileMode());
    assertEquals(CONTAINER_VALUE, jsUri.getContainer());
    assertTrue(jsUri.isJsload());
    assertTrue(jsUri.isNoCache());
    assertTrue(jsUri.isNohint());
    assertEquals(ONLOAD_VALUE, jsUri.getOnload());
    assertEquals(LIBS, Lists.newArrayList(jsUri.getLibs()));
    assertEquals(HAVE, Lists.newArrayList(jsUri.getLoadedLibs()));
    assertEquals(uri, jsUri.getOrigUri());
  }

  @Test
  public void newJsUriWithEmptyOriginalUri() throws Exception {
    JsUriManager.JsUri jsUri = new JsUriManager.JsUri(STATUS, null,
        Collections.<String>emptyList(), null); // Null URI.
    assertEquals(RenderingContext.GADGET, jsUri.getContext());
    assertEquals(ContainerConfig.DEFAULT_CONTAINER, jsUri.getContainer());
    assertEquals(JsCompileMode.COMPILE_CONCAT, jsUri.getCompileMode());
    assertFalse(jsUri.isJsload());
    assertFalse(jsUri.isNoCache());
    assertFalse(jsUri.isNohint());
    assertNull(jsUri.getOnload());
    assertTrue(jsUri.getLibs().isEmpty());
    assertTrue(jsUri.getLoadedLibs().isEmpty());
    assertNull(jsUri.getOrigUri());
  }

  @Test
  public void newJsUriCopyOfOtherJsUri() throws Exception {
    Uri uri = newTestUriBuilder(RenderingContext.CONTAINER,
        JsCompileMode.CONCAT_COMPILE_EXPORT_ALL).toUri();
    JsUriManager.JsUri jsUri = new JsUriManager.JsUri(STATUS, uri, LIBS, HAVE);
    JsUriManager.JsUri jsUriCopy = new JsUriManager.JsUri(jsUri);
    assertEquals(jsUri, jsUriCopy);
  }

  private UriBuilder newTestUriBuilder(RenderingContext context,
      JsCompileMode compileMode) {
    UriBuilder builder = new UriBuilder();
    builder.setScheme("http");
    builder.setAuthority("localhost");
    builder.setPath("/gadgets/js/feature.js");
    builder.addQueryParameter(Param.CONTAINER.getKey(), CONTAINER_VALUE);
    builder.addQueryParameter(Param.CONTAINER_MODE.getKey(), context.getParamValue());
    builder.addQueryParameter(Param.COMPILE_MODE.getKey(), compileMode.getParamValue());
    builder.addQueryParameter(Param.JSLOAD.getKey(), "1");
    builder.addQueryParameter(Param.NO_CACHE.getKey(), "1");
    builder.addQueryParameter(Param.NO_HINT.getKey(), "1");
    builder.addQueryParameter(Param.ONLOAD.getKey(), ONLOAD_VALUE);
    return builder;
  }
}

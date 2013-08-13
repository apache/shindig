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
package org.apache.shindig.gadgets.js;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetException.Code;
import org.apache.shindig.gadgets.features.FeatureRegistry;
import org.apache.shindig.gadgets.uri.JsUriManager;
import org.apache.shindig.gadgets.uri.JsUriManager.JsUri;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import com.google.caja.util.Lists;

import javax.servlet.http.HttpServletRequest;


/**
 * Tests for {@link JsRequestBuilder}.
 */
public class JsRequestBuilderTest {
  private static final String HOST_HEADER_KEY = "Host";
  private static final String IMS_HEADER_KEY = "If-Modified-Since";
  private static final String HOST = "localhost";

  private IMocksControl control;
  private JsUriManager jsUriManager;
  private JsUri jsUri;
  private HttpServletRequest request;
  private JsRequestBuilder builder;
  private FeatureRegistry registry;

  @Before
  public void setUp() {
    control = EasyMock.createControl();
    jsUriManager = control.createMock(JsUriManager.class);
    jsUri = control.createMock(JsUri.class);
    request = control.createMock(HttpServletRequest.class);
    registry = control.createMock(FeatureRegistry.class);
    builder = new JsRequestBuilder(jsUriManager, registry);

    EasyMock.expect(request.getScheme()).andReturn("http");
    EasyMock.expect(request.getServerPort()).andReturn(80);
    EasyMock.expect(request.getServerName()).andReturn("HOST");
    EasyMock.expect(request.getRequestURI()).andReturn("/foo");
    EasyMock.expect(request.getQueryString()).andReturn("");
  }

  @Test
  public void testCreateRequestNotInCache() throws Exception {
    EasyMock.expect(jsUriManager.processExternJsUri(EasyMock.isA(Uri.class))).andReturn(jsUri);
    EasyMock.expect(request.getHeader(HOST_HEADER_KEY)).andReturn(HOST);
    EasyMock.expect(request.getHeader(IMS_HEADER_KEY)).andReturn(null);
    control.replay();
    JsRequest jsRequest = builder.build(request);
    control.verify();
    assertSame(jsUri, jsRequest.getJsUri());
    assertEquals(HOST, jsRequest.getHost());
    assertFalse(jsRequest.isInCache());
  }

  @Test
  public void testCreateRequestInCache() throws Exception {
    EasyMock.expect(jsUriManager.processExternJsUri(EasyMock.isA(Uri.class))).andReturn(jsUri);
    EasyMock.expect(request.getHeader(HOST_HEADER_KEY)).andReturn(HOST);
    EasyMock.expect(request.getHeader(IMS_HEADER_KEY)).andReturn("today");
    control.replay();
    JsRequest jsRequest = builder.build(request);
    control.verify();
    assertSame(jsUri, jsRequest.getJsUri());
    assertEquals(HOST, jsRequest.getHost());
    assertTrue(jsRequest.isInCache());
  }

  @Test
  public void testCreateRequestThrowsExceptionOnParseError() throws Exception {
    EasyMock.expect(jsUriManager.processExternJsUri(EasyMock.isA(Uri.class))).andThrow(
        new GadgetException(Code.INVALID_PARAMETER));
    control.replay();
    try {
      builder.build(request);
      fail("Should have thrown a GadgetException.");
    } catch (GadgetException e) {
      // pass
    }
    control.verify();
  }

  @Test
  public void testCreateRequestComputesDeps() throws Exception {
    List<String> requested = Lists.newArrayList("req1", "req2");
    List<String> loaded = Lists.newArrayList("load1", "load2");
    List<String> fullClosure =
        Lists.newArrayList("dep-s1", "dep1", "dep2", "dep-s2", "load1", "load2", "req1", "req2");
    List<String> loadedClosure =
        Lists.newArrayList("dep-s1", "dep-s2", "load1", "load2");
    EasyMock.expect(registry.getFeatures(requested)).andReturn(fullClosure);
    EasyMock.expect(registry.getFeatures(loaded)).andReturn(loadedClosure);
    EasyMock.expect(jsUri.getLibs()).andReturn(requested);
    EasyMock.expect(jsUri.getLoadedLibs()).andReturn(loaded);
    EasyMock.expect(jsUriManager.processExternJsUri(EasyMock.isA(Uri.class))).andReturn(jsUri);
    EasyMock.expect(request.getHeader(IMS_HEADER_KEY)).andReturn(null);
    EasyMock.expect(request.getHeader(HOST_HEADER_KEY)).andReturn(HOST);
    control.replay();
    JsRequest jsRequest = builder.build(request);
    assertSame(jsUri, jsRequest.getJsUri());
    assertEquals(HOST, jsRequest.getHost());

    List<String> allMatch =
        Lists.newArrayList("dep-s1", "dep1", "dep2", "dep-s2", "load1", "load2", "req1", "req2");
    assertEquals(allMatch, jsRequest.getAllFeatures());

    List<String> loadedMatch =
        Lists.newArrayList("dep-s1", "dep-s2", "load1", "load2");
    assertEquals(loadedMatch, jsRequest.getLoadedFeatures());

    List<String> newMatch =
        Lists.newArrayList("dep1", "dep2", "req1", "req2");
    assertEquals(newMatch, jsRequest.getNewFeatures());

    // Verify calls at the end, since they're made lazily in the context of .getFeatures() calls.
    control.verify();
  }
}

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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.LruCacheProvider;
import org.apache.shindig.common.cache.SoftExpiringCache;
import org.apache.shindig.common.testing.ImmediateExecutorService;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.easymock.EasyMock;
import org.junit.Test;


/**
 * Tests for DefaultGadgetSpecFactory
 */
public class DefaultGadgetSpecFactoryTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private static final String LOCAL_CONTENT = "Hello, local content!";
  private static final String ALT_LOCAL_CONTENT = "Hello, local content!";
  private static final String RAWXML_CONTENT = "Hello, rawxml content!";
  private static final String LOCAL_SPEC_XML
      = "<Module>" +
        "  <ModulePrefs title='GadgetSpecFactoryTest'/>" +
        "  <Content type='html'>" + LOCAL_CONTENT + "</Content>" +
        "</Module>";
  private static final String ALT_LOCAL_SPEC_XML
      = "<Module>" +
        "  <ModulePrefs title='GadgetSpecFactoryTest'/>" +
        "  <Content type='html'>" + ALT_LOCAL_CONTENT + "</Content>" +
        "</Module>";
  private static final String RAWXML_SPEC_XML
      = "<Module>" +
        "  <ModulePrefs title='GadgetSpecFactoryTest'/>" +
        "  <Content type='html'>" + RAWXML_CONTENT + "</Content>" +
        "</Module>";

  private static final GadgetContext RAWXML_GADGET_CONTEXT = new GadgetContext() {
    @Override
    public boolean getIgnoreCache() {
      // This should be ignored by calling code.
      return false;
    }

    @Override
    public Uri getUrl() {
      return SPEC_URL;
    }

    @Override
    public String getParameter(String param) {
      if (param.equals(DefaultGadgetSpecFactory.RAW_GADGETSPEC_XML_PARAM_NAME)) {
        return RAWXML_SPEC_XML;
      }
      return null;
    }
  };

  private static final int MAX_AGE = 10000;

  private final CountingExecutor executor = new CountingExecutor();

  private final RequestPipeline pipeline = EasyMock.createNiceMock(RequestPipeline.class);

  private final CacheProvider cacheProvider = new LruCacheProvider(5);

  private final DefaultGadgetSpecFactory specFactory
      = new DefaultGadgetSpecFactory(executor, pipeline, cacheProvider, MAX_AGE);

  private static HttpRequest createIgnoreCacheRequest() {
    return new HttpRequest(SPEC_URL)
        .setIgnoreCache(true)
        .setGadget(SPEC_URL)
        .setContainer(ContainerConfig.DEFAULT_CONTAINER);
  }

  private static HttpRequest createCacheableRequest() {
    return new HttpRequest(SPEC_URL)
        .setGadget(SPEC_URL)
        .setContainer(ContainerConfig.DEFAULT_CONTAINER);
  }

  private static GadgetContext createContext(final Uri uri, final boolean ignoreCache) {
    return new GadgetContext() {
      @Override
      public Uri getUrl() {
        return uri;
      }

      @Override
      public boolean getIgnoreCache() {
        return ignoreCache;
      }
    };
  }

  @Test
  public void specFetched() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    HttpResponse response = new HttpResponse(LOCAL_SPEC_XML);
    expect(pipeline.execute(request)).andReturn(response);
    replay(pipeline);

    GadgetSpec spec = specFactory.getGadgetSpec(createContext(SPEC_URL, true));

    assertEquals(LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void specFetchedWithBom() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    HttpResponse response = new HttpResponse("&#xFEFF;" + LOCAL_SPEC_XML);
    expect(pipeline.execute(request)).andReturn(response);
    replay(pipeline);

    GadgetSpec spec = specFactory.getGadgetSpec(createContext(SPEC_URL, true));

    assertEquals(LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test(expected = GadgetException.class)
  public void specFetchedEmptyContent() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    HttpResponse response = new HttpResponse("");
    expect(pipeline.execute(request)).andReturn(response);
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, true));
  }

  @Test(expected = GadgetException.class)
  public void malformedGadgetSpecIsCachedAndThrows2() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    expect(pipeline.execute(request)).andReturn(new HttpResponse("")).once();
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, true));
  }

  @Test
  public void specFetchedWithBomChar() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    HttpResponse response = new HttpResponse('\uFEFF' + LOCAL_SPEC_XML);
    expect(pipeline.execute(request)).andReturn(response);
    replay(pipeline);

    GadgetSpec spec = specFactory.getGadgetSpec(createContext(SPEC_URL, true));

    assertEquals(LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  // TODO: Move these tests into AbstractSpecFactoryTest
  @Test
  public void specRefetchedAsync() throws Exception {
    HttpRequest request = createCacheableRequest();
    HttpResponse response = new HttpResponse(ALT_LOCAL_SPEC_XML);
    expect(pipeline.execute(request)).andReturn(response);
    replay(pipeline);

    specFactory.cache.addElement(
        SPEC_URL.toString(), new GadgetSpec(SPEC_URL, LOCAL_SPEC_XML), -1);

    GadgetSpec spec = specFactory.getGadgetSpec(createContext(SPEC_URL, false));

    assertEquals(LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());

    spec = specFactory.getGadgetSpec(createContext(SPEC_URL, false));

    assertEquals(ALT_LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());

    assertEquals(1, executor.runnableCount);
  }

  @Test
  public void specFetchedFromParam() throws Exception {
    // Set up request as if it's a regular spec request, and ensure that
    // the return value comes from rawxml, not the pipeline.
    HttpRequest request = createIgnoreCacheRequest();
    HttpResponse response = new HttpResponse(LOCAL_SPEC_XML);
    expect(pipeline.execute(request)).andReturn(response);
    replay(pipeline);

    GadgetSpec spec = specFactory.getGadgetSpec(RAWXML_GADGET_CONTEXT);

    assertEquals(RAWXML_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
    assertEquals(DefaultGadgetSpecFactory.RAW_GADGET_URI, spec.getUrl());
  }

  @Test
  public void staleSpecIsRefetched() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    HttpRequest retriedRequest = createCacheableRequest();

    HttpResponse expiredResponse = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .setResponse(LOCAL_SPEC_XML.getBytes("UTF-8"))
        .create();
    HttpResponse updatedResponse = new HttpResponse(ALT_LOCAL_SPEC_XML);
    expect(pipeline.execute(request)).andReturn(expiredResponse).once();
    expect(pipeline.execute(retriedRequest)).andReturn(updatedResponse).once();
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, true));

    SoftExpiringCache.CachedObject<Object> inCache = specFactory.cache.getElement(SPEC_URL.toString());
    specFactory.cache.addElement(SPEC_URL.toString(), inCache.obj, -1);

    GadgetSpec spec = specFactory.getGadgetSpec(createContext(SPEC_URL, false));

    assertEquals(ALT_LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void staleSpecReturnedFromCacheOnError() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    HttpRequest retriedRequest = createCacheableRequest();

    HttpResponse expiredResponse = new HttpResponseBuilder()
        .setResponse(LOCAL_SPEC_XML.getBytes("UTF-8"))
        .addHeader("Pragma", "no-cache")
        .create();
    expect(pipeline.execute(request)).andReturn(expiredResponse);
    expect(pipeline.execute(retriedRequest)).andReturn(HttpResponse.notFound()).once();
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, true));

    SoftExpiringCache.CachedObject<Object> inCache = specFactory.cache.getElement(SPEC_URL.toString());
    specFactory.cache.addElement(SPEC_URL.toString(), inCache.obj, -1);

    GadgetSpec spec = specFactory.getGadgetSpec(createContext(SPEC_URL, false));

    assertEquals(ALT_LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void ttlPropagatesToPipeline() throws Exception {
    CapturingPipeline capturingPipeline = new CapturingPipeline();

    GadgetSpecFactory forcedCacheFactory = new DefaultGadgetSpecFactory(
        new ImmediateExecutorService(), capturingPipeline, cacheProvider, 10000);

    forcedCacheFactory.getGadgetSpec(createContext(SPEC_URL, false));

    assertEquals(10, capturingPipeline.request.getCacheTtl());
  }

  @Test
  public void specRequestMarkedWithAnonymousToken() throws Exception {
    CapturingPipeline capturingPipeline = new CapturingPipeline();

    GadgetSpecFactory factory = new DefaultGadgetSpecFactory(
        new CountingExecutor(), capturingPipeline, cacheProvider, 10000);

    factory.getGadgetSpec(createContext(SPEC_URL, false));

    SecurityToken st = capturingPipeline.request.getSecurityToken();
    assertNotNull(st);
    assertTrue( st.isAnonymous() );
    assertEquals( SPEC_URL.toString(), st.getAppUrl() );
  }


  @Test(expected = GadgetException.class)
  public void badFetchThrows() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    expect(pipeline.execute(request)).andReturn(HttpResponse.error());
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, true));
  }

  public void badFetchThrowsExceptionOverridingCache() throws Exception {
    HttpRequest firstRequest = createCacheableRequest();
    expect(pipeline.execute(firstRequest)).andReturn(new HttpResponse(LOCAL_SPEC_XML)).times(2);
    HttpRequest secondRequest = createIgnoreCacheRequest();
    expect(pipeline.execute(secondRequest)).andReturn(HttpResponse.error()).once();
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, false));

    try {
      specFactory.getGadgetSpec(createContext(SPEC_URL, true));
    } catch (GadgetException e) {
      // Expected condition.
    }

    // Now make sure the cache wasn't populated w/ the error.
    specFactory.getGadgetSpec(createContext(SPEC_URL, false));
  }

  @Test(expected = GadgetException.class)
  public void malformedGadgetSpecThrows() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    expect(pipeline.execute(request)).andReturn(new HttpResponse("malformed junk"));
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, true));
  }

  @Test(expected = GadgetException.class)
  public void malformedGadgetSpecIsCachedAndThrows() throws Exception {
    HttpRequest request = createCacheableRequest();
    expect(pipeline.execute(request)).andReturn(new HttpResponse("malformed junk")).once();
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, false));
  }

  @Test(expected = GadgetException.class)
  public void throwingPipelineRethrows() throws Exception {
    HttpRequest request = createIgnoreCacheRequest();
    expect(pipeline.execute(request)).andThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT));
    replay(pipeline);

    specFactory.getGadgetSpec(createContext(SPEC_URL, true));
  }

  @Test(expected = SpecParserException.class)
  public void negativeCachingEnforced() throws Exception {
    specFactory.cache.addElement(SPEC_URL.toString(), new SpecParserException("broken"), 1000);
    specFactory.getGadgetSpec(createContext(SPEC_URL, false));
  }

  private static class CountingExecutor extends ImmediateExecutorService {
    int runnableCount = 0;

    @Override
    public void execute(Runnable r) {
      runnableCount++;
      r.run();
    }
  }

  private static class CapturingPipeline implements RequestPipeline {
    HttpRequest request;

    public HttpResponse execute(HttpRequest request) {
      this.request = request;
      return new HttpResponse(LOCAL_SPEC_XML);
    }
  }
}

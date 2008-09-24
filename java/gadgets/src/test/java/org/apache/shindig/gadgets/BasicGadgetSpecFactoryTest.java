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

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.DefaultCacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.testing.TestExecutorService;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * Tests for BasicGadgetSpecFactory
 */
public class BasicGadgetSpecFactoryTest {
  private final static Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private final static Uri REMOTE_URL = Uri.parse("http://example.org/remote.html");
  private final static String LOCAL_CONTENT = "Hello, local content!";
  private final static String ALT_LOCAL_CONTENT = "Hello, local content!";
  private final static String REMOTE_CONTENT = "Hello, remote content!";
  private final static String RAWXML_CONTENT = "Hello, rawxml content!";
  private final static String LOCAL_SPEC_XML
  = "<Module>" +
  "  <ModulePrefs title='GadgetSpecFactoryTest'/>" +
  "  <Content type='html'>" + LOCAL_CONTENT + "</Content>" +
  "</Module>";
  private final static String ALT_LOCAL_SPEC_XML
  = "<Module>" +
  "  <ModulePrefs title='GadgetSpecFactoryTest'/>" +
  "  <Content type='html'>" + ALT_LOCAL_CONTENT + "</Content>" +
  "</Module>";
  private final static String REMOTE_SPEC_XML
  = "<Module>" +
  "  <ModulePrefs title='GadgetSpecFactoryTest'/>" +
  "  <Content type='html' href='" + REMOTE_URL + "'/>" +
  "</Module>";
  private final static String RAWXML_SPEC_XML
  = "<Module>" +
  "  <ModulePrefs title='GadgetSpecFactoryTest'/>" +
  "  <Content type='html'>" + RAWXML_CONTENT + "</Content>" +
  "</Module>";
  private final static String URL_SPEC_XML
  = "<Module>" +
  "  <ModulePrefs title='GadgetSpecFactoryTest'/>" +
  "  <Content type='url' href='" + REMOTE_URL + "'/>" +
  "</Module>";

  private final static GadgetContext NO_CACHE_CONTEXT = new GadgetContext() {
    @Override
    public boolean getIgnoreCache() {
      return true;
    }
    @Override
    public URI getUrl() {
      return SPEC_URL.toJavaUri();
    }
  };
  
  private final static GadgetContext RAWXML_GADGET_CONTEXT = new GadgetContext() {
    @Override
    public boolean getIgnoreCache() {
      // This should be ignored by calling code.
      return false;
    }
    
    @Override
    public URI getUrl() {
      return SPEC_URL.toJavaUri();
    }
    
    @Override
    public String getParameter(String param) {
      if (param.equals(BasicGadgetSpecFactory.RAW_GADGETSPEC_XML_PARAM_NAME)) {
        return RAWXML_SPEC_XML;
      }
      return null;
    }
  };
  private final static ExecutorService FAKE_EXECUTOR = new TestExecutorService();

  private final HttpFetcher fetcher = EasyMock.createNiceMock(HttpFetcher.class);
  
  private final CacheProvider cacheProvider = new DefaultCacheProvider();

  private final BasicGadgetSpecFactory specFactory
      = new BasicGadgetSpecFactory(fetcher, cacheProvider, FAKE_EXECUTOR, 5, -1000, 1000);

  @Test
  public void specFetched() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    HttpResponse response = new HttpResponse(LOCAL_SPEC_XML);
    expect(fetcher.fetch(request)).andReturn(response);
    replay(fetcher);

    GadgetSpec spec = specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), true);

    assertEquals(LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void specFetchedWithContext() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    HttpResponse response = new HttpResponse(LOCAL_SPEC_XML);
    expect(fetcher.fetch(request)).andReturn(response);
    replay(fetcher);

    GadgetSpec spec = specFactory.getGadgetSpec(NO_CACHE_CONTEXT);

    assertEquals(LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }
  
  @Test
  public void specFetchedFromParam() throws Exception {
    // Set up request as if it's a regular spec request, and ensure that
    // the return value comes from rawxml, not the fetcher.
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(false);
    HttpResponse response = new HttpResponse(LOCAL_SPEC_XML);
    expect(fetcher.fetch(request)).andReturn(response);
    replay(fetcher);
    
    GadgetSpec spec = specFactory.getGadgetSpec(RAWXML_GADGET_CONTEXT);
    
    assertEquals(RAWXML_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
    assertEquals(BasicGadgetSpecFactory.RAW_GADGET_URI, spec.getUrl());
  }

  @Test
  public void staleSpecIsRefetched() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    HttpRequest retriedRequest = new HttpRequest(SPEC_URL).setIgnoreCache(false);
    HttpResponse expiredResponse = new HttpResponseBuilder()
        .addHeader("Pragma", "no-cache")
        .setResponse(LOCAL_SPEC_XML.getBytes("UTF-8"))
        .create();
    HttpResponse updatedResponse = new HttpResponse(ALT_LOCAL_SPEC_XML);
    expect(fetcher.fetch(request)).andReturn(expiredResponse).once();
    expect(fetcher.fetch(retriedRequest)).andReturn(updatedResponse).once();
    replay(fetcher);

    specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), true);
    GadgetSpec spec = specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), false);

    assertEquals(ALT_LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void staleSpecReturnedFromCacheOnError() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    HttpRequest retriedRequest = new HttpRequest(SPEC_URL).setIgnoreCache(false);    
    HttpResponse expiredResponse = new HttpResponseBuilder()
        .setResponse(LOCAL_SPEC_XML.getBytes("UTF-8"))
        .addHeader("Pragma", "no-cache")
        .create();
    expect(fetcher.fetch(request)).andReturn(expiredResponse);
    expect(fetcher.fetch(retriedRequest)).andReturn(HttpResponse.notFound()).once();
    replay(fetcher);

    specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), true);
    GadgetSpec spec = specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), false);

    assertEquals(ALT_LOCAL_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void externalContentFetched() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    HttpResponse response = new HttpResponse(REMOTE_SPEC_XML);
    HttpRequest viewRequest = new HttpRequest(REMOTE_URL).setIgnoreCache(true);
    HttpResponse viewResponse = new HttpResponse(REMOTE_CONTENT);
    expect(fetcher.fetch(request)).andReturn(response);
    expect(fetcher.fetch(viewRequest)).andReturn(viewResponse);
    replay(fetcher);

    GadgetSpec spec = specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), true);

    assertEquals(REMOTE_CONTENT, spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test
  public void typeUrlNotFetchedRemote() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    HttpResponse response = new HttpResponse(URL_SPEC_XML);
    expect(fetcher.fetch(request)).andReturn(response);
    replay(fetcher);

    GadgetSpec spec = specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), true);

    assertEquals(REMOTE_URL.toJavaUri(), spec.getView(GadgetSpec.DEFAULT_VIEW).getHref());
    assertEquals("", spec.getView(GadgetSpec.DEFAULT_VIEW).getContent());
  }

  @Test(expected = GadgetException.class)
  public void badFetchThrows() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    expect(fetcher.fetch(request)).andReturn(HttpResponse.error());
    replay(fetcher);

    specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), true);
  }

  @Test(expected = GadgetException.class)
  public void badRemoteContentThrows() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    HttpResponse response = new HttpResponse(REMOTE_SPEC_XML);
    HttpRequest viewRequest = new HttpRequest(REMOTE_URL).setIgnoreCache(true);
    expect(fetcher.fetch(request)).andReturn(response);
    expect(fetcher.fetch(viewRequest)).andReturn(HttpResponse.error());
    replay(fetcher);

    specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), true);
  }

  @Test(expected = GadgetException.class)
  public void throwingFetcherRethrows() throws Exception {
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    expect(fetcher.fetch(request)).andThrow(
        new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT));
    replay(fetcher);

    specFactory.getGadgetSpec(SPEC_URL.toJavaUri(), true);
  }
}

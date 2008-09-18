/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.rewrite.ContentRewriter;
import org.apache.shindig.gadgets.rewrite.RewriterResults;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import org.easymock.classextension.EasyMock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Sanity test to ensure that Guice injection of a default
 * ContentRewriter leads to its use by an AbstractHttpCache
 * in properly rewriting cacheable content.
 */
public class AbstractHttpCacheTest extends TestCase {
  private Injector injector;
  
  @Override
  protected void setUp() throws Exception {
    injector = Guice.createInjector(new TestCacheModule());
  }
  
  public void testCache() {
    // Setup: could move this elsewhere, but no real need right now.
    HttpCacheKey key = EasyMock.createNiceMock(HttpCacheKey.class);
    expect(key.isCacheable()).andReturn(true).anyTimes();
    HttpRequest request = EasyMock.createNiceMock(HttpRequest.class);
    expect(request.getIgnoreCache()).andReturn(false).anyTimes();
    expect(request.getCacheTtl()).andReturn(Integer.MAX_VALUE).anyTimes();
    replay(key, request);
    HttpResponse response = new HttpResponseBuilder().setHttpStatusCode(200)
        .setResponse("foo".getBytes()).setCacheTtl(Integer.MAX_VALUE).create();
    
    // Actual test.
    AbstractHttpCache ahc = injector.getInstance(TestHttpCache.class);
    HttpResponse added = ahc.addResponse(key, request, response);
    assertNotSame(added, response);
    
    // Not rewritten (anymore).
    assertEquals("foo", added.getResponseAsString());
    assertSame(added, ahc.getResponse(key, request));
    assertEquals(response, ahc.removeResponse(key));
  }

  private static class TestHttpCache extends AbstractHttpCache {
    private final Map<String, HttpResponse> map;
    
    public TestHttpCache() {
      super();
      map = new HashMap<String, HttpResponse>();
    }
    
    @Override
    public void addResponseImpl(String key, HttpResponse response) {
      map.put(key, response);
    }
    
    @Override
    public HttpResponse getResponseImpl(String key) {
      return map.get(key);
    }
    
    @Override
    public HttpResponse removeResponseImpl(String key) {
      return map.remove(key);
    }
  }
  
  private static String PFX_STR = "--prefixtest--";
  private static class TestContentRewriter implements ContentRewriter {
    public RewriterResults rewrite(Gadget gadget) {
      gadget.setContent(PFX_STR + gadget.getContent());
      return null;
    }
    
    public RewriterResults rewrite(HttpRequest req, HttpResponse resp, MutableContent c) {
      c.setContent(PFX_STR + c.getContent());
      return null;
    }
  }
  
  private static class TestCacheModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ContentRewriter.class).to(TestContentRewriter.class);
    }
  }
}

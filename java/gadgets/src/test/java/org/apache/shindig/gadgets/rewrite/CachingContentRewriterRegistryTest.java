/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class CachingContentRewriterRegistryTest {
  private static final Uri SPEC_URL = Uri.parse("http://example.org/gadget.xml");
  private final CaptureRewriter captureRewriter = new CaptureRewriter();
  private final List<CaptureRewriter> rewriters
      = Lists.newArrayList(captureRewriter, new ModifyingCaptureContentRewriter());
  private final List<ContentRewriter> contentRewriters
      = Lists.<ContentRewriter>newArrayList(rewriters);
  private final FakeCacheProvider provider = new FakeCacheProvider();
  private final CachingContentRewriterRegistry registry
      = new CachingContentRewriterRegistry(contentRewriters, null, provider, 100, 0);

  @Test
  public void gadgetGetsCached() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);
    GadgetContext context = new GadgetContext();
    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);

    registry.rewriteGadget(gadget, body);

    // TODO: We're not actually testing the TTL of the entries here.
    assertEquals(1, provider.readCount);
    assertEquals(1, provider.writeCount);
  }

  @Test
  public void gadgetFetchedFromCache() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);

    GadgetContext context = new GadgetContext();

    for (int i = 0; i < 3; ++i) {
      Gadget gadget = new Gadget()
          .setContext(context)
          .setSpec(spec);
      registry.rewriteGadget(gadget, body);
    }

    assertEquals(3, provider.readCount);
    assertEquals(1, provider.writeCount);
  }

  @Test
  public void noCacheGadgetDoesNotGetCached() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);
    GadgetContext context = new GadgetContext() {
      @Override
      public boolean getIgnoreCache() {
        return true;
      }
    };

    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);

    registry.rewriteGadget(gadget, body);

    assertTrue("Rewriting not performed on uncacheable content.",
        rewriters.get(0).viewWasRewritten());
    assertEquals(0, provider.readCount);
    assertEquals(0, provider.writeCount);
  }

  @Test
  public void httpResponseGetsCached() throws Exception {
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(SPEC_URL);
    HttpResponse response = new HttpResponse(body);

    registry.rewriteHttpResponse(request, response);

    assertEquals(1, provider.readCount);
    assertEquals(1, provider.writeCount);
  }

  @Test
  public void httpResponseFetchedFromCache() throws Exception {
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(SPEC_URL);
    HttpResponse response = new HttpResponse(body);

    registry.rewriteHttpResponse(request, response);
    registry.rewriteHttpResponse(request, response);
    registry.rewriteHttpResponse(request, response);

    assertEquals(3, provider.readCount);
    assertEquals(1, provider.writeCount);
  }

  @Test
  public void noCacheHttpResponseDoesNotGetCached() throws Exception {
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(SPEC_URL).setIgnoreCache(true);
    HttpResponse response = new HttpResponse(body);

    registry.rewriteHttpResponse(request, response);

    assertTrue("Rewriting not performed on uncacheable content.",
        rewriters.get(0).responseWasRewritten());
    assertEquals(0, provider.readCount);
    assertEquals(0, provider.writeCount);
  }

  @Test
  public void changingRewritersBustsCache() {
    // What we're testing here is actually impossible (you can't swap the registry on a running
    // application), but there's a good chance that implementations will be using a shared cache,
    // which persists across server restarts / reconfigurations. This verifies that the entries in
    // the cache will be invalidated if the rewriters change.

    // Just HTTP here; we'll assume this code is common between both methods.
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(SPEC_URL);
    HttpResponse response = new HttpResponse(body);

    registry.rewriteHttpResponse(request, response);

    assertEquals(1, provider.readCount);
    assertEquals(1, provider.writeCount);

    // The new registry is created using one additional rewriter, but the same cache.
    contentRewriters.add(new CaptureRewriter());
    ContentRewriterRegistry newRegistry
        = new CachingContentRewriterRegistry(contentRewriters, null, provider, 100, 0);

    newRegistry.rewriteHttpResponse(request, response);

    assertEquals(2, provider.readCount);
    assertEquals(2, provider.writeCount);
    assertFalse("Cache was written using identical keys.",
        provider.keys.get(0).equals(provider.keys.get(1)));
  }

  @Test
  public void rewriteBelowMinCacheDoesntWriteToCache() throws Exception {
    registry.setMinCacheTtl(1000);
    captureRewriter.setCacheTtl(500);

    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content/></Module>";
    GadgetSpec spec = new GadgetSpec(SPEC_URL, xml);
    GadgetContext context = new GadgetContext();

    // We have to re-create Gadget objects because they get mutated directly, which is really
    // inconsistent with the behavior of rewriteHttpResponse.
    Gadget gadget = new Gadget()
        .setContext(context)
        .setSpec(spec);
    registry.rewriteGadget(gadget, body);

    assertEquals(1, provider.readCount);
    assertEquals(0, provider.writeCount);
  }

  private static class FakeCacheProvider implements CacheProvider {
    private final Map<String, Object> entries = Maps.newHashMap();
    private final List<String> keys = Lists.newLinkedList();
    private int readCount = 0;
    private int writeCount = 0;
    private final Cache<String, Object> cache = new Cache<String, Object>() {
      public void addElement(String key, Object value) {
        entries.put(key, value);
        keys.add(key);
        writeCount++;
      }

      public Object getElement(String key) {
        readCount++;
        return entries.get(key);
      }

      public Object removeElement(String key) {
        return entries.remove(key);
      }
    };

    public <K, V> Cache<K, V> createCache(int capacity, String name) {
      throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> createCache(int capacity) {
      return (Cache<K, V>)cache;
    }
  }

  private static class ModifyingCaptureContentRewriter extends CaptureRewriter {

    @Override
    public RewriterResults rewrite(HttpRequest request, HttpResponse original,
        MutableContent content) {
      super.rewrite(request, original, content);
      content.setContent(content.getContent() + "-modified");
      return RewriterResults.cacheableIndefinitely();
    }

    @Override
    public RewriterResults rewrite(Gadget gadget, MutableContent content) {
      super.rewrite(gadget, content);
      content.setContent(content.getContent() + "-modified");
      return RewriterResults.cacheableIndefinitely();
    }

  }

}

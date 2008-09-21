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

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.MutableContent;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.emory.mathcs.backport.java.util.Collections;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class CachingContentRewriterRegistryTest {
  private final List<CaptureRewriter> rewriters
      = Lists.newArrayList(new CaptureRewriter(), new ModifyingCaptureContentRewriter());
  private final FakeCacheProvider provider = new FakeCacheProvider();
  private final ContentRewriterRegistry registry
      = new CachingContentRewriterRegistry(rewriters, null, provider, 100);
  private final IMocksControl control = EasyMock.createNiceControl();
  private final ContainerConfig config = control.createMock(ContainerConfig.class);

  @Test
  @SuppressWarnings("unchecked")
  public void gadgetGetsCached() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content>" + body + "</Content></Module>";
    GadgetSpec spec = new GadgetSpec(URI.create("#"), xml);
    GadgetContext context = new GadgetContext();
    Gadget gadget = new Gadget(context, spec, Collections.emptyList(), config, null);

    control.replay();

    registry.rewriteGadget(gadget);

    // TODO: We're not actually testing the TTL of the entries here.
    assertEquals(1, provider.readCount);
    assertEquals(1, provider.writeCount);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void gadgetFetchedFromCache() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content>" + body + "</Content></Module>";
    GadgetSpec spec = new GadgetSpec(URI.create("#"), xml);

    GadgetContext context = new GadgetContext();

    control.replay();

    // We have to re-create Gadget objects because they get mutated directly, which is really
    // inconsistent with the behavior of rewriteHttpResponse.
    Gadget gadget = new Gadget(context, spec, Collections.emptyList(), config, null);
    registry.rewriteGadget(gadget);
    gadget = new Gadget(context, spec, Collections.emptyList(), config, null);
    registry.rewriteGadget(gadget);
    gadget = new Gadget(context, spec, Collections.emptyList(), config, null);
    registry.rewriteGadget(gadget);

    // TODO: We're not actually testing the TTL of the entries here.
    assertEquals(3, provider.readCount);
    assertEquals(1, provider.writeCount);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void noCacheGadgetDoesNotGetCached() throws Exception {
    String body = "Hello, world";
    String xml = "<Module><ModulePrefs title=''/><Content>" + body + "</Content></Module>";
    GadgetSpec spec = new GadgetSpec(URI.create("#"), xml);
    GadgetContext context = new GadgetContext() {
      @Override
      public boolean getIgnoreCache() {
        return true;
      }
    };

    Gadget gadget = new Gadget(context, spec, Collections.emptyList(), config, null);

    control.replay();

    registry.rewriteGadget(gadget);

    assertTrue("Rewriting not performed on uncacheable content.",
        rewriters.get(0).viewWasRewritten());
    assertEquals(0, provider.readCount);
    assertEquals(0, provider.writeCount);
  }

  @Test
  public void httpResponseGetsCached() throws Exception {
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(Uri.parse("#"));
    HttpResponse response = new HttpResponse(body);

    registry.rewriteHttpResponse(request, response);

    assertEquals(1, provider.readCount);
    assertEquals(1, provider.writeCount);
  }

  @Test
  public void httpResponseFetchedFromCache() throws Exception {
    String body = "Hello, world";
    HttpRequest request = new HttpRequest(Uri.parse("#"));
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
    HttpRequest request = new HttpRequest(Uri.parse("#")).setIgnoreCache(true);
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
    HttpRequest request = new HttpRequest(Uri.parse("#"));
    HttpResponse response = new HttpResponse(body);

    registry.rewriteHttpResponse(request, response);

    assertEquals(1, provider.readCount);
    assertEquals(1, provider.writeCount);

    // The new registry is created using one additional rewriter, but the same cache.
    rewriters.add(new CaptureRewriter());
    ContentRewriterRegistry newRegistry
        = new CachingContentRewriterRegistry(rewriters, null, provider, 100);

    newRegistry.rewriteHttpResponse(request, response);

    assertEquals(2, provider.readCount);
    assertEquals(2, provider.writeCount);
    assertFalse("Cache was written using identical keys.",
        provider.keys.get(0).equals(provider.keys.get(1)));
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
    public RewriterResults rewrite(Gadget gadget) {
      super.rewrite(gadget);
      gadget.setContent(gadget.getContent() + "-modified");
      return RewriterResults.cacheableIndefinitely();
    }
  }
}

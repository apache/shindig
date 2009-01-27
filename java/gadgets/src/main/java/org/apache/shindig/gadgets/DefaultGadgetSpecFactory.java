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

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.SoftExpiringCache;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.net.URI;
import java.util.logging.Logger;

/**
 * Default implementation of a gadget spec factory.
 */
@Singleton
public class DefaultGadgetSpecFactory implements GadgetSpecFactory {
  public static final String CACHE_NAME = "gadgetSpecs";
  static final String RAW_GADGETSPEC_XML_PARAM_NAME = "rawxml";
  static final Uri RAW_GADGET_URI = Uri.parse("http://localhost/raw.xml");
  static final String ERROR_SPEC = "<Module><ModulePrefs title='Error'/><Content/></Module>";
  static final String ERROR_KEY = "parse.exception";
  static final Logger LOG = Logger.getLogger(DefaultGadgetSpecFactory.class.getName());

  private final RequestPipeline pipeline;
  private final SoftExpiringCache<Uri, GadgetSpec> cache;
  private final long refresh;

  @Inject
  public DefaultGadgetSpecFactory(RequestPipeline pipeline,
                                  CacheProvider cacheProvider,
                                  @Named("shindig.cache.xml.refreshInterval") long refresh) {
    this.pipeline = pipeline;
    Cache<Uri, GadgetSpec> baseCache = cacheProvider.createCache(CACHE_NAME);
    this.cache = new SoftExpiringCache<Uri, GadgetSpec>(baseCache);
    this.refresh = refresh;
  }

  public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
    String rawxml = context.getParameter(RAW_GADGETSPEC_XML_PARAM_NAME);
    if (rawxml != null) {
      // Set URI to a fixed, safe value (localhost), preventing a gadget rendered
      // via raw XML (eg. via POST) to be rendered on a locked domain of any other
      // gadget whose spec is hosted non-locally.
      return new GadgetSpec(RAW_GADGET_URI, rawxml);
    }
    return getGadgetSpec(context.getUrl(), context.getContainer(), context.getIgnoreCache());
  }

  /**
   * Retrieves a gadget specification from the cache or from the Internet.
   * TODO: This should be removed. Too much context is missing from this request.
   */
  public GadgetSpec getGadgetSpec(URI gadgetUri, boolean ignoreCache) throws GadgetException {
    return getGadgetSpec(gadgetUri, ContainerConfig.DEFAULT_CONTAINER, ignoreCache);
  }

  private GadgetSpec getGadgetSpec(URI gadgetUri, String container, boolean ignoreCache)
      throws GadgetException {
    Uri uri = Uri.fromJavaUri(gadgetUri);
    if (ignoreCache) {
      return fetchObjectAndCache(uri, container, ignoreCache);
    }

    SoftExpiringCache.CachedObject<GadgetSpec> cached = cache.getElement(uri);

    GadgetSpec spec = null;
    if (cached == null || cached.isExpired) {
      try {
        spec = fetchObjectAndCache(uri, container, ignoreCache);
      } catch (GadgetException e) {
        // Enforce negative caching.
        if (cached != null) {
          spec = cached.obj;
          Preconditions.checkNotNull(spec);
        } else {
          // We create this dummy spec to avoid the cost of re-parsing when a remote site is out.
          spec = new GadgetSpec(uri, ERROR_SPEC);
          spec.setAttribute(ERROR_KEY, e);
        }
        LOG.info("GadgetSpec fetch failed for " + uri + " - using cached.");
        cache.addElement(uri, spec, refresh);
      }
    } else {
      spec = cached.obj;
    }

    GadgetException exception = (GadgetException) spec.getAttribute(ERROR_KEY);
    if (exception != null) {
      throw exception;
    }
    return spec;
  }

  /**
   * Retrieves a gadget specification from the Internet, processes its views and
   * adds it to the cache.
   */
  private GadgetSpec fetchObjectAndCache(Uri url, String container, boolean ignoreCache)
      throws GadgetException {
    HttpRequest request = new HttpRequest(url)
        .setIgnoreCache(ignoreCache)
        .setGadget(url)
        .setContainer(container);

    // Since we don't allow any variance in cache time, we should just force the cache time
    // globally. This ensures propagation to shared caches when this is set.
    request.setCacheTtl((int) (refresh / 1000));

    HttpResponse response = pipeline.execute(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                                "Unable to retrieve gadget xml. HTTP error " +
                                response.getHttpStatusCode());
    }

    GadgetSpec spec = new GadgetSpec(url, response.getResponseAsString());
    cache.addElement(url, spec, refresh);
    return spec;
  }
}

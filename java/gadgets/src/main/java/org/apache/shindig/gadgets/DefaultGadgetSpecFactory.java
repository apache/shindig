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
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.ApplicationManifest;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.w3c.dom.Element;

import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * Default implementation of a gadget spec factory.
 */
@Singleton
public class DefaultGadgetSpecFactory implements GadgetSpecFactory {
  public static final String CACHE_NAME = "gadgetSpecs";

  static final String VERSION_PARAM = "version";
  static final String LABEL_PARAM = "label";
  static final String DEFAULT_LABEL = "production";
  static final String RAW_GADGETSPEC_XML_PARAM_NAME = "rawxml";
  static final Uri RAW_GADGET_URI = Uri.parse("http://localhost/raw.xml");

  private final Logger logger = Logger.getLogger(DefaultGadgetSpecFactory.class.getName());
  private final ExecutorService executor;
  private final RequestPipeline pipeline;
  final SoftExpiringCache<Uri, Object> cache;
  private final long refresh;

  @Inject
  public DefaultGadgetSpecFactory(ExecutorService executor,
                                  RequestPipeline pipeline,
                                  CacheProvider cacheProvider,
                                  @Named("shindig.cache.xml.refreshInterval") long refresh) {
    this.executor = executor;
    this.pipeline = pipeline;
    Cache<Uri, Object> baseCache = cacheProvider.createCache(CACHE_NAME);
    this.cache = new SoftExpiringCache<Uri, Object>(baseCache);
    this.refresh = refresh;
  }

  public GadgetSpec getGadgetSpec(GadgetContext context) throws GadgetException {
    String rawxml = context.getParameter(RAW_GADGETSPEC_XML_PARAM_NAME);
    if (rawxml != null) {
      // Set URI to a fixed, safe value (localhost), preventing a gadget rendered
      // via raw XML (eg. via POST) to be rendered on a locked domain of any other
      // gadget whose spec is hosted non-locally.
      try {
        return new GadgetSpec(RAW_GADGET_URI, XmlUtil.parse(rawxml), rawxml);
      } catch (XmlException e) {
        throw new SpecParserException(e);
      }
    }

    return fetchObject(context.getUrl(), context, false);
  }

  private GadgetSpec getSpecFromManifest(ApplicationManifest manifest, GadgetContext context)
      throws GadgetException {
    String version = context.getParameter(VERSION_PARAM);

    if (version == null) {
      // TODO: The label param should only be used for metadata calls. This should probably be
      // exposed up a layer in the stack, perhaps at the interface level.
      String label = Objects.firstNonNull(context.getParameter(LABEL_PARAM), DEFAULT_LABEL);

      version = manifest.getVersion(label);

      if (version == null) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
            "Unable to find a suitable version for the given manifest.");
      }
    }

    Uri specUri = manifest.getGadget(version);

    if (specUri == null) {
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          "No gadget spec available for the given version.");
    }

    return fetchObject(specUri, context, true);
  }

  private GadgetSpec fetchObject(Uri uri, GadgetContext context, boolean noManifests)
      throws GadgetException {

    Object obj = null;
    if (!context.getIgnoreCache()) {
      SoftExpiringCache.CachedObject<Object> cached = cache.getElement(uri);
      if (cached != null) {
        obj = cached.obj;
        if (cached.isExpired) {
          // We write to the cache to avoid any race conditions with multiple writers.
          // This causes a double write, but that's better than a write per thread or synchronizing
          // this block.
          cache.addElement(uri, obj, refresh);
          executor.execute(new ObjectUpdater(uri, context, obj));
        }
      }
    }

    if (obj == null) {
      try {
        obj = fetchFromNetwork(uri, context);
      } catch (GadgetException e) {
        obj = e;
      }

      cache.addElement(uri, obj, refresh);
    }

    if (obj instanceof GadgetSpec) {
      return (GadgetSpec) obj;
    }

    if (obj instanceof ApplicationManifest) {
      if (noManifests) {
        throw new SpecParserException("Manifests may not reference other manifests.");
      }

      return getSpecFromManifest((ApplicationManifest) obj, context);
    }

    if (obj instanceof GadgetException) {
      throw (GadgetException) obj;
    }

    // Some big bug.
    throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
        "Unknown object type stored for input URI " + uri);
  }

  /**
   * Retrieves a gadget specification from the Internet, processes its views and
   * adds it to the cache.
   */
  private Object fetchFromNetwork(Uri uri, GadgetContext context) throws GadgetException {
    HttpRequest request = new HttpRequest(uri)
        .setIgnoreCache(context.getIgnoreCache())
        .setGadget(uri)
        .setContainer(context.getContainer());

    // Since we don't allow any variance in cache time, we should just force the cache time
    // globally. This ensures propagation to shared caches when this is set.
    request.setCacheTtl((int) (refresh / 1000));

    HttpResponse response = pipeline.execute(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      throw new GadgetException(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
                                "Unable to retrieve gadget xml. HTTP error " +
                                response.getHttpStatusCode());
    }

    try {
      String content = response.getResponseAsString();
      Element element = XmlUtil.parse(content);
      if (ApplicationManifest.NAMESPACE.equals(element.getNamespaceURI())) {
        return new ApplicationManifest(uri, element);
      }
      return new GadgetSpec(uri, element, content);
    } catch (XmlException e) {
      throw new SpecParserException(e);
    }
  }

  private class ObjectUpdater implements Runnable {
    private final Uri uri;
    private final GadgetContext context;
    private final Object old;

    public ObjectUpdater(Uri uri, GadgetContext context, Object old) {
      this.uri = uri;
      this.context = context;
      this.old = old;
    }

    public void run() {
      try {
        Object newObject = fetchFromNetwork(uri, context);
        cache.addElement(uri, newObject, refresh);
      } catch (GadgetException e) {
        if (old != null) {
          logger.info("Failed to update " + uri + ". Using cached version.");
          cache.addElement(uri, old, refresh);
        } else {
          logger.info("Failed to update " + uri + ". Applying negative cache.");
          cache.addElement(uri, e, refresh);
        }
      }
    }
  }
}

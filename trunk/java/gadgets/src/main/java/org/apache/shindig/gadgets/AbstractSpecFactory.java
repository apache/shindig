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

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.SoftExpiringCache;
import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.xml.XmlException;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.SpecParserException;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basis for implementing GadgetSpec and MessageBundle factories.
 *
 * Automatically updates objects as needed asynchronously to provide optimal throughput.
 */
public abstract class AbstractSpecFactory<T> {
  //class name for logging purpose
  private static final String classname = AbstractSpecFactory.class.getName();
  private static final Logger LOG = Logger.getLogger(classname,MessageKeys.MESSAGES);
  private final Class<T> clazz;
  private final ExecutorService executor;
  private final RequestPipeline pipeline;
  final SoftExpiringCache<String, Object> cache;
  private final long refresh;

  /**
   * @param clazz the class for spec objects.
   * @param executor for asynchronously updating specs
   * @param pipeline the request pipeline for fetching new specs
   * @param cache a cache for parsed spec objects
   * @param refresh the frequency at which to update specs, independent of cache expiration policy
   */
  public AbstractSpecFactory(Class<T> clazz, ExecutorService executor, RequestPipeline pipeline,
      Cache<String, Object> cache, long refresh) {
    this.clazz = clazz;
    this.executor = executor;
    this.pipeline = pipeline;
    this.cache = new SoftExpiringCache<String, Object>(cache);
    this.refresh = refresh;
  }

  /**
   * Attempt to fetch a spec, either from cache or from the network.
   *
   * Note that the {@code query} passed here will always be passed, unmodified, to
   * {@link #parse(String, Query)}. This can be used to carry additional context information
   * during parsing.
   */
  protected T getSpec(Query query) throws GadgetException {
    Object obj = null;
    if (!query.ignoreCache) {
      SoftExpiringCache.CachedObject<Object> cached = cache.getElement(query.specUri.toString());
      if (cached != null) {
        obj = cached.obj;
        if (cached.isExpired) {
          // We write to the cache to avoid any race conditions with multiple writers.
          // This causes a double write, but that's better than a write per thread or synchronizing
          // this block.
          cache.addElement(query.specUri.toString(), obj, refresh);
          executor.execute(new SpecUpdater(query, obj));
        }
      }
    }

    if (obj == null) {
      boolean bypassCache = false;
      try {
        obj = fetchFromNetwork(query);
      } catch (SpecRetrievalFailedException e) {
        // Don't cache the resulting exception.
        // The underlying RequestPipeline may (and should) cache non-OK HTTP responses
        // independently, and may do so for the same spec in different ways depending
        // on context. There's no computational benefit to caching this exception in
        // the spec cache since we won't try to re-parse the data anyway, as we would
        // an OK response with a faulty spec.
        bypassCache = true;
        obj = e;
      } catch (GadgetException e) {
        obj = e;
      }
      if (!bypassCache) {
        cache.addElement(query.specUri.toString(), obj, refresh);
      }
    }

    if (obj instanceof GadgetException) {
      throw (GadgetException) obj;
    }

    // If there's a bug that puts the wrong object in here, we'll get a ClassCastException.
    return clazz.cast(obj);
  }

  /**
   * Retrieves a spec from the network, parses, and adds it to the cache.
   */
  protected T fetchFromNetwork(Query query) throws SpecRetrievalFailedException, GadgetException {
    HttpRequest request = new HttpRequest(query.specUri)
        .setIgnoreCache(query.ignoreCache)
        .setGadget(query.gadgetUri)
        .setContainer(query.container)
        .setSecurityToken( new AnonymousSecurityToken("", 0L, query.gadgetUri.toString()));

    // Since we don't allow any variance in cache time, we should just force the cache time
    // globally. This ensures propagation to shared caches when this is set.
    request.setCacheTtl((int) (refresh / 1000));

    HttpResponse response = pipeline.execute(request);
    if (response.getHttpStatusCode() != HttpResponse.SC_OK) {
      int retcode = response.getHttpStatusCode();
      if (retcode == HttpResponse.SC_INTERNAL_SERVER_ERROR) {
        // Convert external "internal error" to gateway error:
        retcode = HttpResponse.SC_BAD_GATEWAY;
      }
      throw new SpecRetrievalFailedException(query.specUri, retcode);
    }

    try {
      String content = response.getResponseAsString();
      return parse(content, query);
    } catch (XmlException e) {
      throw new SpecParserException(e);
    }
  }

  /**
   * Parse and return a new spec object from the network.
   *
   * @param content the content located at specUri
   * @param query same as was passed {@link #getSpec(Query)}
   */
  protected abstract T parse(String content, Query query) throws XmlException, GadgetException;

  /**
   * Holds information used to fetch a spec.
   */
  protected static class Query {
    private Uri specUri = null;
    private String container = ContainerConfig.DEFAULT_CONTAINER;
    private Uri gadgetUri = null;
    private boolean ignoreCache = false;

    // Expose public constructor
    public Query() {
    }

    public Query setSpecUri(Uri specUri) {
      this.specUri = specUri;
      return this;
    }

    public Query setContainer(String container) {
      this.container = container;
      return this;
    }

    public Query setGadgetUri(Uri gadgetUri) {
      this.gadgetUri = gadgetUri;
      return this;
    }

    public Query setIgnoreCache(boolean ignoreCache) {
      this.ignoreCache = ignoreCache;
      return this;
    }

    public Uri getSpecUri() {
      return specUri;
    }

    public String getContainer() {
      return container;
    }

    public Uri getGadgetUri() {
      return gadgetUri;
    }

    public boolean getIgnoreCache() {
      return ignoreCache;
    }
  }

  private class SpecUpdater implements Runnable {
    private final Query query;
    private final Object old;

    public SpecUpdater(Query query, Object old) {
      this.query = query;
      this.old = old;
    }

    public void run() {
      try {
        T newSpec = fetchFromNetwork(query);
        cache.addElement(query.specUri.toString(), newSpec, refresh);
      } catch (SpecRetrievalFailedException se) {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, classname, "SpecUpdater", MessageKeys.UPDATE_SPEC_FAILURE_APPLY_NEG_CACHE, new Object[] {
              query.specUri,
              se.getHttpStatusCode(),
              se.getMessage()
          });
        }
      } catch (GadgetException e) {
        if (old != null) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "SpecUpdater", MessageKeys.UPDATE_SPEC_FAILURE_USE_CACHE_VERSION, new Object[] {
                query.specUri,
                e.getHttpStatusCode(),
                e.getMessage()
            });
          }
          cache.addElement(query.specUri.toString(), old, refresh);
        } else {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "SpecUpdater", MessageKeys.UPDATE_SPEC_FAILURE_APPLY_NEG_CACHE, new Object[] {
                query.specUri,
                e.getHttpStatusCode(),
                e.getMessage()
            });
          }
          cache.addElement(query.specUri.toString(), e, refresh);
        }
      }
    }
  }

  protected static class SpecRetrievalFailedException extends GadgetException {
    public SpecRetrievalFailedException(Uri specUri, int code) {
      super(GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT,
            "Unable to retrieve spec for " + specUri + ". HTTP error " + code, code);
    }
  }
}

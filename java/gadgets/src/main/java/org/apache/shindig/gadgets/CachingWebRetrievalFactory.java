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

import java.util.logging.Logger;

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.TtlCache;
import org.apache.shindig.gadgets.GadgetException;

/**
 * Base class for a factory which utilizes a time-to-live cache under the hood.
 * The provided logic is that if ignoreCache is specified, the requested object
 * will be freshly retrieved and cached. Otherwise, the object is returned from
 * the cache if it's either still valid (below its TTL), or if the raw retrieval
 * operation failed.
 * 
 * The class is templatized by T = type of the object to be retrieved and cached;
 * Q = type used to query for T, and K = type of the cache key.
 * 
 * Subclasses must implement two methods:
 * retrieveRawObject(), which uses the query param to retrieve an object of type T,
 * along with its expiration time in milliseconds.
 * getCacheKeyFromQueryObj(), which computes a cache key for the query param.
 * 
 * Still, due to the templatization, this class is admittedly not as easy as it
 * perhaps ought to be to read. As such, it's due for cleanup and possible removal.
 * 
 * @param <T> Type of object to retrieve and cache.
 * @param <Q> Type of query parameter used to fetch the cached object. May not be
 * the same as the cache key (K), since K may not contain enough information for the
 * retrieval operation.
 * @param <K> Type of the key used to cache object of type T.
 */
public abstract class CachingWebRetrievalFactory<T, Q, K> {
  // Subclasses must override these.
  protected abstract FetchedObject<T> retrieveRawObject(Q queryObj, boolean ignoreCache) throws GadgetException;
  protected abstract K getCacheKeyFromQueryObj(Q queryObj);
  protected abstract Logger getLogger();
  
  private final TtlCache<K, T> ttlCache;

  protected CachingWebRetrievalFactory(CacheProvider cacheProvider,
      int capacity, long minTtl, long maxTtl) {
    this.ttlCache = new TtlCache<K, T>(cacheProvider, capacity, minTtl, maxTtl);
  }
  
  protected T doCachedFetch(Q queryObj, boolean ignoreCache) throws GadgetException {
    if (ignoreCache) {
      return fetchObjectAndCache(queryObj, ignoreCache);
    }
    
    TtlCache.CachedObject<T> cached = null;
    K cacheKey = getCacheKeyFromQueryObj(queryObj);
    
    synchronized(ttlCache) {
      cached = ttlCache.getElementWithExpiration(cacheKey);
    }
    
    if (cached.obj == null || cached.isExpired) {
      try {
        return fetchObjectAndCache(queryObj, false);
      } catch (GadgetException e) {
        // Failed to re-fetch raw object. Use cached object if it exists.
        if (cached.obj == null) {
          throw e;
        } else {
          getLogger().info("Object fetch failed for " + cacheKey + " -  using cached.");
        }
      }
    }
    
    return cached.obj;
  }
  
  private T fetchObjectAndCache(Q queryObj, boolean ignoreCache) throws GadgetException {
    FetchedObject<T> fetched = retrieveRawObject(queryObj, ignoreCache);
    
    synchronized(ttlCache) {
      ttlCache.addElement(getCacheKeyFromQueryObj(queryObj), fetched.fetchedObj, fetched.expirationTime);
    }
    
    return fetched.fetchedObj;
  }
  
  protected static class FetchedObject<T> {
    private T fetchedObj;
    private long expirationTime;
    
    public FetchedObject(T fetchedObj, long expirationTime) {
      this.fetchedObj = fetchedObj;
      this.expirationTime = expirationTime;
    }
  }
}

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

import java.net.URI;
import java.util.logging.Logger;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.gadgets.GadgetException;

public abstract class CachingWebRetrievalFactory<T, Q> {
  // Subclasses must override these.
  protected abstract FetchedObject<T> fetchFromWeb(Q queryObj, boolean ignoreCache) throws GadgetException;
  protected abstract URI getCacheKeyFromQueryObj(Q queryObj);
  
  private static final Logger logger = Logger.getLogger(CachingWebRetrievalFactory.class.getName());
  private final Cache<URI, TimeoutPair<T>> cache;
  private final long minTtl, maxTtl;

  protected CachingWebRetrievalFactory(CacheProvider cacheProvider,
      int capacity, long minTtl, long maxTtl) {
    this.cache = cacheProvider.createCache(capacity);
    this.minTtl = minTtl;
    this.maxTtl = maxTtl;
  }
  
  protected T doCachedFetch(Q queryObj, boolean ignoreCache) throws GadgetException {
    if (ignoreCache) {
      return fetchObjectAndCache(queryObj, ignoreCache);
    }
    
    T resultObj = null;
    long expiration = -1;
    URI cacheKey = getCacheKeyFromQueryObj(queryObj);
    
    synchronized(cache) {
      TimeoutPair<T> cachedEntry = cache.getElement(cacheKey);
      if (cachedEntry != null) {
        resultObj = cachedEntry.cachedObj;
        expiration = cachedEntry.timeout;
      }
    }
    
    // If the obj is not in the cache or has expired, fetch it from its URI.
    long now = System.currentTimeMillis();
    
    if (resultObj == null || expiration < now) {
      try {
        return fetchObjectAndCache(queryObj, false);
      } catch (GadgetException e) {
        if (resultObj == null) {
          throw e;
        } else {
          logger.info("Object fetch failed for " + cacheKey + " -  using cached ");
          // Try again later...
          synchronized (cache) {
            cache.addElement(cacheKey, new TimeoutPair<T>(resultObj, now + minTtl));
          }
        }
      }
    }
    
    return resultObj;
  }
  
  private T fetchObjectAndCache(Q queryObj, boolean ignoreCache) throws GadgetException {
    FetchedObject<T> fetched = fetchFromWeb(queryObj, ignoreCache);
    
    long now = System.currentTimeMillis();
    long expiration = fetched.expirationTime;
    expiration = Math.max(now + minTtl, Math.min(now + maxTtl, expiration));
    synchronized (cache) {
      cache.addElement(getCacheKeyFromQueryObj(queryObj),
          new TimeoutPair<T>(fetched.fetchedObj, expiration));
    }
    
    return fetched.fetchedObj;
  }
  
  protected static class FetchedObject<T> {
    private T fetchedObj;
    private long expirationTime;
    
    protected FetchedObject(T fetchedObj, long expirationTime) {
      this.fetchedObj = fetchedObj;
      this.expirationTime = expirationTime;
    }
  }
  
  private static class TimeoutPair<T> {
    private T cachedObj;
    private long timeout;

    private TimeoutPair(T cachedObj, long timeout) {
      this.cachedObj = cachedObj;
      this.timeout = timeout;
    }
  }
}

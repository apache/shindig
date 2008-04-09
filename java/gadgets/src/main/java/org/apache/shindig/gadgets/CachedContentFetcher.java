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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches the results of a request indefinitely.
 *
 * Currently just uses a map for the cache. This means it will grow infinitely.
 */
@Singleton
public class CachedContentFetcher extends RemoteContentFetcher {

  private final Map<URI, RemoteContent> cache;

  /** {@inheritDoc} */
  @Override
  public RemoteContent fetch(RemoteContentRequest request)
      throws GadgetException {
    if (request.getOptions().ignoreCache) {
      return nextFetcher.fetch(request);
    }
    RemoteContent result = cache.get(request.getUri());
    if (result == null) {
      result = nextFetcher.fetch(request);
      synchronized (cache) {
        cache.put(request.getUri(),result);
      }
    }
    return result;
  }

  @Inject
  public CachedContentFetcher(RemoteContentFetcher nextFetcher) {
    super(nextFetcher);
    cache = new HashMap<URI, RemoteContent>();
  }
}

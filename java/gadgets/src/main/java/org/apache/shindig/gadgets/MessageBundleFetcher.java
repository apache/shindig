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

import org.apache.shindig.gadgets.spec.MessageBundle;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Message bundle retrieval implementation that retrieves by the following:
 *
 * - Consulting the in-memory cache (just a Map)
 * - If not found, making a RemoteContentRequest for the bundle
 * - Parsing bundle
 * - Storing to the cache
 */
public class MessageBundleFetcher implements DataFetcher<MessageBundle> {
  private final RemoteContentFetcher fetcher;
  private final Map<URI, MessageBundle> cache;

  private final static Logger logger
      = Logger.getLogger("org.apache.shindig.gadgets");

  /** {@inheritDoc} */
  public void invalidate(URI url) {
    synchronized (cache) {
      cache.remove(url);
    }
  }

  /** {@inheritDoc} */
  public MessageBundle fetch(URI url, boolean forceReload)
      throws GadgetException {
    MessageBundle bundle = null;
    if (!forceReload) {
      cache.get(url);
    }
    if (bundle == null) {
      RemoteContentRequest request = new RemoteContentRequest(url);
      RemoteContent response = fetcher.fetch(request);

      if (response.getHttpStatusCode() == RemoteContent.SC_OK) {
        bundle = new MessageBundle(response.getResponseAsString());
        cache.put(url, bundle);
      } else {
        String error = "Unable to get content from " + url.toString();
        logger.info(error);
        throw new GadgetException(
            GadgetException.Code.FAILED_TO_RETRIEVE_CONTENT, error);
      }
    }
    return bundle;
  }

  /**
   * @param fetcher The fetcher to use for remote resource retrieval.
   */
  public MessageBundleFetcher(RemoteContentFetcher fetcher) {
    this.fetcher = fetcher;
    cache = new HashMap<URI, MessageBundle>();
  }
}

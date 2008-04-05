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
package org.apache.shindig.gadgets;

import java.util.Map;


/**
 * Fetches data over HTTP.
 *
 * Subclasses can use a chain-of-responsibility pattern to add functionality
 * to the fetching process.  For example, a SigningFetcher can talk to a
 * CachingFetcher can talk to a ThrottlingFetcher that talks to a
 * RemoteFetcher that gets the actual data.
 */
public abstract class RemoteContentFetcher {

  /** next fetcher in the chain, may be null */
  protected RemoteContentFetcher nextFetcher;

  protected RemoteContentFetcher(RemoteContentFetcher nextFetcher) {
    this.nextFetcher = nextFetcher;
  }

  /**
   * Fetch HTTP content.
   *
   * @param request The request to fetch.
   * @return RemoteContent
   * @throws GadgetException
   */
  public abstract RemoteContent fetch(RemoteContentRequest request)
      throws GadgetException;
  
  /**
   * @return the next fetcher in the chain
   */
  public RemoteContentFetcher getNextFetcher() {
    return nextFetcher;
  }
  
  /**
   * @return additional data to embed in responses sent from the JSON proxy.
   */
  public Map<String, String> getResponseMetadata() {
    return null;
  }
  
}

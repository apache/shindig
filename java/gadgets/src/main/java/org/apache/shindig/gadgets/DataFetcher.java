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

import java.net.URI;

/**
 * Interface for retrieving a specific type of content from external resources
 *
 * Implementations may provide their own caching layer and define how resources
 * are retrieved.
 *
 * @param <T> Type of data that this fetcher fetches.
 */
public interface DataFetcher<T> {
  /**
   * Retrieve an object from the fetcher.
   *
   * @param url The url where the object can be located.
   * @param forceReload True to disable caches and go directly to the source.
   * @return The object, if it can be retrieved.
   * @throws GadgetException If the object can't be retrieved or is not a valid
   *     format for this data type.
   */
  public T fetch(URI url, boolean forceReload) throws GadgetException;

  /**
   * Invalidates any cache entries for the given url.
   *
   * @param url
   * @throws GadgetException
   */
  public void invalidate(URI url) throws GadgetException;
}

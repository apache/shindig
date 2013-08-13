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
package org.apache.shindig.gadgets.http;

import org.apache.shindig.gadgets.GadgetException;

import com.google.inject.ImplementedBy;

/**
 * Perform an request for the given resource. Does not perform caching, authentication, or stats.
 * This class should only be used to implement network-level fetching of resources. While we use
 * HTTP to represent the transport layer, it's important to note that this fetcher may be used for
 * other types of URI-based resources and does not necessarily require HTTP.
 */
@ImplementedBy(BasicHttpFetcher.class)
public interface HttpFetcher {

  /**
   * Fetch HTTP content.
   *
   * @param request The request to fetch.
   * @return An HTTP response from the relevant resource, including error conditions.
   * @throws GadgetException In the event of a failure that can't be mapped to an HTTP result code.
   */
  HttpResponse fetch(HttpRequest request) throws GadgetException;
}

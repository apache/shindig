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
 * Implements a complete HTTP request pipeline. Performs caching, authentication, and serves as an
 * injection point for any custom request pipeline injection.
 *
 * NOTE: When using cache, please ensure that you are checking response.isStrictNoCache() before
 * serving out. Because cache may have private contents, even though marked stale.
 * @see {AbstractHttpCache} for details.
 */
@ImplementedBy(DefaultRequestPipeline.class)
public interface RequestPipeline {

  /**
   * Execute the given request.
   *
   * TODO: This should throw a custom exception type.
   */
  HttpResponse execute(HttpRequest request) throws GadgetException;
}

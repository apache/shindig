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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.inject.ImplementedBy;

/**
 * Performs rewriting operations by invoking one or more {@link org.apache.shindig.gadgets.rewrite.ResponseRewriter}s.
 *
 * @since 2.0.0
 */
@ImplementedBy(DefaultResponseRewriterRegistry.class)
public interface ResponseRewriterRegistry {

  /**
   * Rewrites an {@code HttpResponse} object with the given request as context,
   * using the registered rewriters.
   * @param req Request object for context.
   * @param resp Original response object.
   * @param gadget Gadget that may have been making the request.
   * @return Rewritten response object, or resp if not modified.
   * @throws RewritingException In case of errors.
   */
  HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp, Gadget gadget)
    throws RewritingException;
}

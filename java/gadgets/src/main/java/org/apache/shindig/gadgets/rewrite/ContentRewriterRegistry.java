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
package org.apache.shindig.gadgets.rewrite;

import com.google.inject.ImplementedBy;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;

@ImplementedBy(BasicContentRewriterRegistry.class)
public interface ContentRewriterRegistry {
  /**
   * Rewrites a {@code Gadget} object given the registered rewriters.
   * @param gadget Gadget object to rewrite
   * @return True if rewriting occurred
   * @throws GadgetException Potentially passed through from rewriters
   */
  public boolean rewriteGadget(Gadget gadget) throws GadgetException;
  
  /**
   * Rewrites an {@code HttpResponse} object with the given request as context,
   * using the registered rewriters.
   * @param req Request object for context.
   * @param resp Original response object.
   * @return Rewritten response object, or resp if not modified.
   */
  public HttpResponse rewriteHttpResponse(HttpRequest req, HttpResponse resp);
}

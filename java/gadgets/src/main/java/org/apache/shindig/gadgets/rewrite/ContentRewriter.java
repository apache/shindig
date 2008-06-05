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

import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.GadgetSpec;

import java.io.Reader;
import java.io.Writer;
import java.net.URI;

/**
 * Standard interface for content rewriters
 */
public interface ContentRewriter {

  /**
   * Rewrite the original content located at source
   * @param request  Originating request
   * @param original Original content
   * @return A rewritten copy of the original or null if no rewriting occurred
   */
  public HttpResponse rewrite(HttpRequest request, HttpResponse original);

  /**
   * Rewrite the original gadget content located at source
   * @param spec     GadgetSpec to use for rewriting rules. May be null
   * @param original Original content
   * @param mimeType A string containing the mime type of the content, may
   *                 contain other content as allowed in the HTTP Content-Type
   *                 header
   * @return A rewritten copy of the original or null if no rewriting occurred
   */
  public String rewriteGadgetView(GadgetSpec spec, String original, String mimeType);
}

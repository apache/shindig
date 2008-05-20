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

import java.io.Reader;
import java.io.Writer;
import java.net.URI;

/**
 * Standard interface for content rewriters
 */
public interface ContentRewriter {

  /**
   * Rewrite the original content located at source
   * @param request   Originating request
   * @param original Original content
   * @return A rewritten copy of the original or null if no rewriting occurred
   */
  public HttpResponse rewrite(HttpRequest request, HttpResponse original);

  /**
   * Rewrite the original content located at source
   * @param source   Location of the original content
   * @param original Original content
   * @param mimeType A string containing the mime type of the content, may
   *                 contain other content as allowed in the HTTP Content-Type
   *                 header
   * @return A rewritten copy of the original or null if no rewriting occurred
   */
  public String rewrite(URI source, String original, String mimeType);

  /**
   * Rewrite the content in the original response located at source
   * @param source   Location of the original content
   * @param original Original content
   * @param mimeType A string containing the mime type of the content, may
   *                 contain other content as allowed in the HTTP Content-Type
   *                 header
   * @param rewritten Target of rewritten content, not written to if no
   *                rewriting is done.
   * @return true if rewrite occurred, false otherwise
   */
  public boolean rewrite(URI source, Reader original, String mimeType,
      Writer rewritten);
  
}

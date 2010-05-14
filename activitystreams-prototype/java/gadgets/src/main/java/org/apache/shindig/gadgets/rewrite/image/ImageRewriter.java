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
package org.apache.shindig.gadgets.rewrite.image;

import com.google.inject.ImplementedBy;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpRequest;

/**
 * Rewrite an image when it is read from the network
 */
@ImplementedBy(BasicImageRewriter.class)
public interface ImageRewriter {

  /**
   * Take an HttpResponse object and rewrite it if it contains image data. If
   * it does not contain image data it should return the original content
   * unchanged.
   *
   * @param request the HTTP request that caused the response
   * @param response the response to rewrite
   * @return either a rewritten image if the rewriting has been triggered; or the original
   *         unchanged content
   */
  HttpResponse rewrite(HttpRequest request, HttpResponse response);

  /**
   * @return the number of total original image bytes read
   */
  long getOriginalImageBytes();

  /**
   * @return the number of total rewritten image bytes generated
   */
  long getRewrittenImageBytes();
}

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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;

import java.util.Set;

/**
 * No-Op implementation of the invalidation service
 */
public class NoOpInvalidationService implements InvalidationService {

  public void invalidateApplicationResources(Set<Uri> uris, SecurityToken token) {
    // No op
  }

  public void invalidateUserResources(Set<String> opensocialIds, SecurityToken token) {
    // No op
  }

  public boolean isValid(HttpRequest request, HttpResponse response) {
    return true;
  }

  public HttpResponse markResponse(HttpRequest request, HttpResponse response) {
    return response;
  }
}

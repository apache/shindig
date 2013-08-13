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

import com.google.common.base.Strings;
import org.apache.shindig.auth.AuthenticationMode;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.protocol.BaseRequestItem;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Handle cache invalidation API calls
 *
 * TODO : Sync with spec. Propose cache as service name, invalidate as operation
 * viewer is always invalidated if available
 */
@Service(name = "cache")
public class InvalidationHandler {

  public static final String KEYS_PARAM = "invalidationKeys";

  private final InvalidationService invalidation;


  @Inject
  public InvalidationHandler(InvalidationService invalidation) {
    this.invalidation = invalidation;
  }

  @Operation(httpMethods = {"POST","GET"}, path = "/invalidate")
  public void invalidate(BaseRequestItem request) {
    if (Strings.isNullOrEmpty(request.getToken().getAppId()) &&
        Strings.isNullOrEmpty(request.getToken().getAppUrl())) {
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
          "Cannot invalidate content without specifying application");
    }

    // Is the invalidation call from the application backend. If not we don't allow
    // invalidation of resources or users other than @viewer
    boolean isBackendInvalidation = AuthenticationMode.OAUTH_CONSUMER_REQUEST.name().equals(
        request.getToken().getAuthenticationMode());

    List<String> keys = request.getListParameter(KEYS_PARAM);
    Set<String> userIds = Sets.newHashSet();
    Set<Uri> resources = Sets.newHashSet();

    // Assume the the viewer content is being invalidated if it is available
    if (!Strings.isNullOrEmpty(request.getToken().getViewerId())) {
      userIds.add(request.getToken().getViewerId());
    }
    if (keys != null) {
      for (String key : keys) {
        String lowerKey = key.toLowerCase();
        if (lowerKey.startsWith("http")) {
          // Assume key is a gadget spec, message bundle or other resource
          // owned by the gadget
          if (!isBackendInvalidation) {
            throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
                "Cannot flush application resources from a gadget. " +
                    "Must use OAuth consumer request");
          }
          resources.add(Uri.parse(key));
        } else {
          if ("@viewer".equals(key)) {
            // Viewer is invalidated by default if available
            continue;
          }
          if (!isBackendInvalidation && !key.equals(request.getToken().getViewerId())) {
            throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
                "Cannot invalidate the content for a user other than the viewer from a gadget.");
          }
          userIds.add(key);
        }
      }
    }
    invalidation.invalidateApplicationResources(resources, request.getToken());
    invalidation.invalidateUserResources(userIds, request.getToken());
  }
}

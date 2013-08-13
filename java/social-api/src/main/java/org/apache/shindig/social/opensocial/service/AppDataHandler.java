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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.UserId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

/**
 * Handles REST/RPC requests for AppData
 */
@Service(name = "appdata", path = "/{userId}+/{groupId}/{appId}")
public class AppDataHandler {

  private final AppDataService service;

  @Inject
  public AppDataHandler(AppDataService service) {
    this.service = service;
  }

  /**
   * Allowed endpoints /appdata/{userId}/{groupId}/{appId} - fields={field1, field2}
   *
   * examples: /appdata/john.doe/@friends/app?fields=count /appdata/john.doe/@self/app
   *
   * The post data should be a regular json object. All of the fields vars will be pulled from the
   * values and set on the person object. If there are no fields vars then all of the data will be
   * overridden.
   */
  @Operation(httpMethods = "DELETE")
  public Future<?> delete(SocialRequestItem request)
      throws ProtocolException {

    Set<UserId> userIds = request.getUsers();

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

    return service.deletePersonData(userIds.iterator().next(), request.getGroup(),
        request.getAppId(), request.getFields(), request.getToken());
  }

  /**
   * Allowed endpoints /appdata/{userId}/{groupId}/{appId} - fields={field1, field2}
   *
   * examples: /appdata/john.doe/@friends/app?fields=count /appdata/john.doe/@self/app
   *
   * The post data should be a regular json object. All of the fields vars will be pulled from the
   * values and set on the person object. If there are no fields vars then all of the data will be
   * overridden.
   */
  @Operation(httpMethods = "PUT", bodyParam = "data")
  public Future<?> update(SocialRequestItem request) throws ProtocolException {
    return create(request);
  }

  /**
   * /appdata/{userId}/{groupId}/{appId} - fields={field1, field2}
   *
   * examples: /appdata/john.doe/@friends/app?fields=count /appdata/john.doe/@self/app
   *
   * The post data should be a regular json object. All of the fields vars will be pulled from the
   * values and set. If there are no fields vars then all of the data will be overridden.
   */
  @Operation(httpMethods = "POST", bodyParam = "data")
  public Future<?> create(SocialRequestItem request) throws ProtocolException {
    Set<UserId> userIds = request.getUsers();

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");

    @SuppressWarnings("unchecked")
    // As of today, this is the only format supported by the AppData protocol
    Map<String, Object> values = request.getTypedParameter("data", Map.class);
    for (String key : values.keySet()) {
      if (!isValidKey(key)) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,
            "One or more of the app data keys are invalid: " + key);
      }
    }

    return service.updatePersonData(userIds.iterator().next(), request.getGroup(),
        request.getAppId(), request.getFields(), values, request.getToken());
  }

  /**
   * /appdata/{userId}+/{groupId}/{appId} - fields={field1, field2}
   *
   * examples: /appdata/john.doe/@friends/app?fields=count /appdata/john.doe/@self/app
   */
  @Operation(httpMethods = "GET")
  public Future<?> get(SocialRequestItem request) throws ProtocolException {
    Set<UserId> userIds = request.getUsers();

    // Preconditions
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");

    return service.getPersonData(userIds, request.getGroup(),
        request.getAppId(), request.getFields(), request.getToken());
  }

  /**
   * Determines whether the input is a valid key. Valid keys match the regular expression [\w\-\.]+.
   * The logic is not done using java.util.regex.* as that is 20X slower.
   *
   * @param key the key to validate.
   * @return true if the key is a valid appdata key, false otherwise.
   */
  public static boolean isValidKey(String key) {
    if (key == null || key.length() == 0) {
      return false;
    }
    for (int i = 0; i < key.length(); ++i) {
      char c = key.charAt(i);
      if ((c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
          (c >= '0' && c <= '9') ||
          (c == '-') ||
          (c == '_') ||
          (c == '.')) {
        continue;
      }
      return false;
    }
    return true;
  }

}


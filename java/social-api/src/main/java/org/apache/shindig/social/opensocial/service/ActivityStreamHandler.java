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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RequestItem;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.spi.ActivityStreamService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

/**
 * <p>ActivityStreamHandler class.</p>
 */
@Service(name = "activitystreams", path="/{userId}+/{groupId}/{appId}/{activityId}+")
public class ActivityStreamHandler {

  private final ActivityStreamService service;
  private final ContainerConfig config;

  /**
   * <p>Constructor for ActivityStreamHandler.</p>
   *
   * @param service a {@link org.apache.shindig.social.opensocial.spi.ActivityStreamService} object.
   * @param config a {@link org.apache.shindig.config.ContainerConfig} object.
   */
  @Inject
  public ActivityStreamHandler(ActivityStreamService service, ContainerConfig config) {
    this.service = service;
    this.config = config;
  }

  /**
   * Allowed end-points /activitystreams/{userId}/@self/{appId}/{activityId}+
   *
   * Examples: /activitystreams/john.doe/@self/1/object1,object2
   *
   * @param request a {@link org.apache.shindig.social.opensocial.service.SocialRequestItem} object.
   * @return a {@link java.util.concurrent.Future} object.
   * @throws org.apache.shindig.protocol.ProtocolException if any.
   */
  @Operation(httpMethods="DELETE")
  public Future<?> delete(SocialRequestItem request)
      throws ProtocolException {

    Set<UserId> userIds = request.getUsers();
    Set<String> activityIds = ImmutableSet.copyOf(request.getListParameter("activityId"));

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");
    HandlerPreconditions.requireNotEmpty(activityIds, "At least one activity ID must be specified");

    return service.deleteActivityEntries(Iterables.getOnlyElement(userIds), request.getGroup(),
        request.getAppId(), activityIds, request.getToken());
  }

  /**
   * Allowed end-points /activitystreams/{userId}/@self/{appId}/{activityId}
   *
   * Examples: /activitystreams/john.doe/@self/1/object2 - postBody is an activity object
   *
   * @param request a {@link org.apache.shindig.social.opensocial.service.SocialRequestItem} object.
   * @return a {@link java.util.concurrent.Future} object.
   * @throws org.apache.shindig.protocol.ProtocolException if any.
   */
  @Operation(httpMethods="PUT", bodyParam = "activity")
  public Future<?> update(SocialRequestItem request) throws ProtocolException {
    Set<UserId> userIds = request.getUsers();
    List<String> activityIds = request.getListParameter("activityId");

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");
    HandlerPreconditions.requireSingular(activityIds, "Must specify exactly one activity ID");

    return service.updateActivityEntry(Iterables.getOnlyElement(userIds), request.getGroup(),
        request.getAppId(), request.getFields(),
        request.getTypedParameter("activity", ActivityEntry.class),
        activityIds.iterator().next(),
        request.getToken());
  }

  /**
   * Allowed end-points /activitystreams/{userId}/@self/{appId}
   *
   * Examples: /activitystreams/john.doe/@self/{appId} - postBody is an activity object
   *
   * @param request a {@link org.apache.shindig.social.opensocial.service.SocialRequestItem} object.
   * @return a {@link java.util.concurrent.Future} object.
   * @throws org.apache.shindig.protocol.ProtocolException if any.
   */
  @Operation(httpMethods="POST", bodyParam = "activity")
  public Future<?> create(SocialRequestItem request) throws ProtocolException {
    Set<UserId> userIds = request.getUsers();
    List<String> activityIds = request.getListParameter("activityId");

    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    HandlerPreconditions.requireSingular(userIds, "Multiple userIds not supported");
    HandlerPreconditions.requireEmpty(activityIds, "Cannot specify activity ID in create");

    return service.createActivityEntry(Iterables.getOnlyElement(userIds), request.getGroup(),
        request.getAppId(), request.getFields(),
        request.getTypedParameter("activity", ActivityEntry.class),
        request.getToken());
  }

  /**
   * Allowed end-points:
   *   /activitystreams/{userId}/{groupId}/{optionalActvityId}+
   *   /activitystreams/{userId}+/{groupId}
   *
   * Examples:
   *   /activitystreams/john.doe/@self/1
   *   /activitystreams/john.doe,jane.doe/@friends
   *
   * @param request a {@link org.apache.shindig.social.opensocial.service.SocialRequestItem} object.
   * @return a {@link java.util.concurrent.Future} object.
   * @throws org.apache.shindig.protocol.ProtocolException if any.
   */
  @Operation(httpMethods="GET")
  public Future<?> get(SocialRequestItem request)
      throws ProtocolException {
    Set<UserId> userIds = request.getUsers();
    Set<String> optionalActivityIds = ImmutableSet.copyOf(request.getListParameter("activityId"));

    CollectionOptions options = new CollectionOptions(request);

    // Preconditions
    HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
    if (userIds.size() > 1 && !optionalActivityIds.isEmpty()) {
      throw new IllegalArgumentException("Cannot fetch activities by ID for multiple users");
    }

    if (!optionalActivityIds.isEmpty()) {
      if (optionalActivityIds.size() == 1) {
        return service.getActivityEntry(userIds.iterator().next(), request.getGroup(),
            request.getAppId(), request.getFields(), optionalActivityIds.iterator().next(),
            request.getToken());
      } else {
        return service.getActivityEntries(userIds.iterator().next(), request.getGroup(),
            request.getAppId(), request.getFields(), options, optionalActivityIds, request.getToken());
      }
    }

    return service.getActivityEntries(userIds, request.getGroup(),
        request.getAppId(),
        // TODO: add pagination and sorting support
        // getSortBy(params), getFilterBy(params), getStartIndex(params), getCount(params),
        request.getFields(), options, request.getToken());
  }

  /**
   * Return a list of supported fields for the ActivityStreams endpoint
   *
   * @param request a {@link org.apache.shindig.protocol.RequestItem} object.
   * @return a List of supported fields
   */
  @Operation(httpMethods = "GET", path="/@supportedFields")
  public List<Object> supportedFields(RequestItem request) {
    // TODO: Would be nice if name in config matched name of service.
    String container = Objects.firstNonNull(request.getToken().getContainer(), ContainerConfig.DEFAULT_CONTAINER);
    return config.getList(container,
        "${Cur['gadgets.features'].opensocial.supportedFields.activityEntry}");
  }
}

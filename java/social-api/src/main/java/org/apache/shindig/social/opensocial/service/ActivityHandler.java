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
package org.apache.shindig.social.opensocial.service;

import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

public class ActivityHandler extends DataRequestHandler {
  private final ActivityService service;

  private static final String ACTIVITY_ID_PATH
      = "/activities/{userId}+/{groupId}/{appId}/{activityId}+";

  @Inject
  public ActivityHandler(ActivityService service) {
    this.service = service;
  }

  /**
   * Allowed end-points /activities/{userId}/@self/{actvityId}+
   *
   * examples: /activities/john.doe/@self/1
   */
  protected Future<? extends ResponseItem> handleDelete(RequestItem request) {
    request.applyUrlTemplate(ACTIVITY_ID_PATH);

    Set<UserId> userIds = request.getUsers();
    Set<String> activityIds = Sets.newLinkedHashSet(request.getListParameter("activityId"));

    Preconditions.requireNotEmpty(userIds, "No userId specified");
    Preconditions.requireSingular(userIds, "Multiple userIds not supported");

    return service.deleteActivities(userIds.iterator().next(), request.getGroup(),
        request.getAppId(), activityIds, request.getToken());
  }

  /**
   * Allowed end-points /activities/{userId}/@self
   *
   * examples: /activities/john.doe/@self - postBody is an activity object
   */
  protected Future<? extends ResponseItem> handlePut(RequestItem request) {
    return handlePost(request);
  }

  /**
   * Allowed end-points /activities/{userId}/@self
   *
   * examples: /activities/john.doe/@self - postBody is an activity object
   */
  protected Future<? extends ResponseItem> handlePost(RequestItem request) {
    request.applyUrlTemplate(ACTIVITY_ID_PATH);

    Set<UserId> userIds = request.getUsers();
    List<String> activityIds = request.getListParameter("activityId");

    Preconditions.requireNotEmpty(userIds, "No userId specified");
    Preconditions.requireSingular(userIds, "Multiple userIds not supported");
    // TODO(lryan) This seems reasonable to allow on PUT but we don't have an update verb.
    Preconditions.requireEmpty(activityIds, "Cannot specify activityId in create");

    return service.createActivity(userIds.iterator().next(), request.getGroup(),
        request.getAppId(), request.getFields(),
        request.getTypedParameter("activity", Activity.class),
        request.getToken());
  }

  /**
   * Allowed end-points /activities/{userId}/{groupId}/{optionalActvityId}+
   * /activities/{userId}+/{groupId}
   *
   * examples: /activities/john.doe/@self/1 /activities/john.doe/@self
   * /activities/john.doe,jane.doe/@friends
   */
  protected Future<? extends ResponseItem> handleGet(RequestItem request) {
    request.applyUrlTemplate(ACTIVITY_ID_PATH);

    Set<UserId> userIds = request.getUsers();
    Set<String> optionalActivityIds = Sets.newLinkedHashSet(request.getListParameter("activityId"));

    // Preconditions
    Preconditions.requireNotEmpty(userIds, "No userId specified");
    if (userIds.size() > 1 && !optionalActivityIds.isEmpty()) {
      throw new IllegalArgumentException("Cannot fetch same activityIds for multiple userIds");
    }

    if (!optionalActivityIds.isEmpty()) {
      if (optionalActivityIds.size() == 1) {
        return service.getActivity(userIds.iterator().next(), request.getGroup(),
            request.getAppId(), request.getFields(), optionalActivityIds.iterator().next(),
            request.getToken());
      } else {
        return service.getActivities(userIds.iterator().next(), request.getGroup(),
            request.getAppId(), request.getFields(), optionalActivityIds, request.getToken());
      }
    }

    return service.getActivities(userIds, request.getGroup(),
        request.getAppId(),
        // TODO: add pagination and sorting support
        // getSortBy(params), getFilterBy(params), getStartIndex(params), getCount(params),
        request.getFields(), request.getToken());
  }

}



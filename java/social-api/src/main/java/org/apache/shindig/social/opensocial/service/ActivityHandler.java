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

import com.google.inject.Inject;

import java.util.concurrent.Future;

public class ActivityHandler extends DataRequestHandler {
  private ActivityService service;

  // TODO: The appId should come from the url. The spec needs to be fixed!
  private static final String ACTIVITY_ID_PATH = "/activities/{userId}/{groupId}/{activityId}";
  // Note: not what the spec says
  private static final String GROUP_PATH = "/activities/{userId}/{groupId}/{appId}"; 

  @Inject
  public ActivityHandler(ActivityService service) {
    this.service = service;
  }

  /**
   * /activities/{userId}/@self/{actvityId}
   *
   * examples:
   * /activities/john.doe/@self/1
   */
  protected Future<? extends ResponseItem> handleDelete(RequestItem request) {
    request.parseUrlWithTemplate(ACTIVITY_ID_PATH);

    return service.deleteActivity(request.getUser(), request.getGroup(),
        request.getAppId(), request.getParameters().get("activityId"), request.getToken());
  }

  /**
   * /activities/{userId}/@self
   *
   * examples:
   * /activities/john.doe/@self
   * - postBody is an activity object
   */
  protected Future<? extends ResponseItem> handlePut(RequestItem request) {
    return handlePost(request);
  }

  /**
   * /activities/{userId}/@self
   *
   * examples:
   * /activities/john.doe/@self
   * - postBody is an activity object
   */
  protected Future<? extends ResponseItem> handlePost(RequestItem request) {
    request.parseUrlWithTemplate(GROUP_PATH);

    return service.createActivity(request.getUser(), request.getGroup(),
        request.getAppId(), request.getFields(), request.getPostData(Activity.class),
        request.getToken());
  }

  /**
   * /activities/{userId}/{groupId}/{optionalActvityId}
   *
   * examples:
   * /activities/john.doe/@self/1
   * /activities/john.doe/@self
   * /activities/john.doe/@friends
   */
  protected Future<? extends ResponseItem> handleGet(RequestItem request) {
    request.parseUrlWithTemplate(ACTIVITY_ID_PATH);
    String optionalActivityId = request.getParameters().get("activityId");

    if (optionalActivityId != null) {
      return service.getActivity(request.getUser(), request.getGroup(),request.getAppId(),
          request.getFields(), optionalActivityId, request.getToken());
    }

    return service.getActivities(request.getUser(), request.getGroup(), request.getAppId(),
        // TODO: add pagination and sorting support
        // getOrderBy(params), getFilterBy(params), getStartIndex(params), getCount(params),
        request.getFields(), request.getToken());
  }

}



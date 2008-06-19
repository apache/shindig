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
package org.apache.shindig.social.dataservice;

import com.google.inject.Inject;

import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.model.ActivityImpl;
import org.apache.shindig.social.opensocial.model.Activity;

public class ActivityHandler extends DataRequestHandler {
  private ActivityService service;

  @Inject
  public ActivityHandler(ActivityService service) {
    this.service = service;
  }

  protected ResponseItem handleDelete(RequestItem request) {
    return new ResponseItem<Object>(ResponseError.BAD_REQUEST,
        "You can't delete activities. ", null);
  }

  /**
   * /activities/{userId}/@self
   *
   * examples:
   * /activities/john.doe/@self
   * - postBody is an activity object
   */
  protected ResponseItem handlePut(RequestItem request) {
    return handlePost(request);
  }

  /**
   * /activities/{userId}/@self
   *
   * examples:
   * /activities/john.doe/@self
   * - postBody is an activity object
   */
  protected ResponseItem handlePost(RequestItem request) {
    String[] segments = getParamsFromRequest(request);

    UserId userId = UserId.fromJson(segments[0]);
    GroupId groupId = GroupId.fromJson(segments[1]);
    // TODO: Should we pass the groupId through to the service?

    String jsonActivity = request.getParameters().get("entry");
    Activity activity = converter.convertToObject(jsonActivity, Activity.class);

    return service.createActivity(userId, activity, request.getToken());
  }

  /**
   * /activities/{userId}/{groupId}/{optionalActvityId}
   *
   * examples:
   * /activities/john.doe/@self/1
   * /activities/john.doe/@self
   * /activities/john.doe/@friends
   */
  protected ResponseItem handleGet(RequestItem request) {
    String[] segments = getParamsFromRequest(request);

    UserId userId = UserId.fromJson(segments[0]);
    GroupId groupId = GroupId.fromJson(segments[1]);
    String optionalActivityId = null;
    if (segments.length > 2) {
      optionalActivityId = segments[2];
    }

    // TODO: Filter by fields
    // TODO: do we need to add pagination and sorting support?
    if (optionalActivityId != null) {
      return service.getActivity(userId, groupId, optionalActivityId, request.getToken());
    }
    return service.getActivities(userId, groupId, request.getToken());
  }

}



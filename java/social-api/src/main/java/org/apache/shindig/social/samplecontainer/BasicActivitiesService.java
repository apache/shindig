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
package org.apache.shindig.social.samplecontainer;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.dataservice.ActivityService;
import org.apache.shindig.social.dataservice.RestfulCollection;
import org.apache.shindig.social.dataservice.GroupId;
import org.apache.shindig.social.dataservice.UserId;
import org.apache.shindig.social.opensocial.ActivitiesService;
import org.apache.shindig.social.opensocial.model.Activity;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class BasicActivitiesService implements ActivitiesService,
    ActivityService {
  private XmlStateFileFetcher fetcher;

  @Inject
  public BasicActivitiesService(XmlStateFileFetcher fetcher) {
    this.fetcher = fetcher;
    fetcher.loadDefaultStateFileIfNoneLoaded();
  }

  public ResponseItem<List<Activity>> getActivities(List<String> ids,
      SecurityToken token) {
    Map<String, List<Activity>> allActivities = fetcher.getActivities();

    List<Activity> activities = Lists.newArrayList();

    for (String id : ids) {
      List<Activity> personActivities = allActivities.get(id);
      if (personActivities != null) {
        activities.addAll(personActivities);
      }
    }

    // TODO: Sort them
    return new ResponseItem<List<Activity>>(activities);
  }

  public ResponseItem<Activity> getActivity(String id, String activityId,
      SecurityToken token) {
    List<Activity> allActivities = getActivities(
        Lists.newArrayList(id), token).getResponse();

    for (Activity activity : allActivities) {
      if (activity.getId().equals(activityId)) {
        return new ResponseItem<Activity>(activity);
      }
    }
    return new ResponseItem<Activity>(ResponseError.BAD_REQUEST,
        "Activity not found", null);
  }

  public ResponseItem createActivity(String personId, Activity activity,
      SecurityToken token) {
    // TODO: Validate the activity and do any template expanding
    activity.setUserId(personId);
    activity.setPostedTime(new Date().getTime());

    fetcher.createActivity(personId, activity);
    return new ResponseItem<JSONObject>(new JSONObject());
  }

  // New interface methods

  public ResponseItem<RestfulCollection<Activity>> getActivities(UserId userId,
      GroupId groupId, SecurityToken token) {
    List<String> ids = Lists.newArrayList();
    switch (groupId.getType()) {
      case all:
      case friends:
        List<String> friendIds = fetcher.getFriendIds().get(userId.getUserId(token));
        if (friendIds != null) {
          ids.addAll(friendIds);
        }
        break;
      case self:
        ids.add(userId.getUserId(token));
    }

    Map<String, List<Activity>> allActivities = fetcher.getActivities();
    List<Activity> activities = Lists.newArrayList();

    for (String id : ids) {
      List<Activity> personActivities = allActivities.get(id);
      if (personActivities != null) {
        activities.addAll(personActivities);
      }
    }

    // TODO: Sort them
    return new ResponseItem<RestfulCollection<Activity>>(
        new RestfulCollection<Activity>(activities));
  }

  public ResponseItem<Activity> getActivity(UserId userId,
      GroupId groupId, String activityId,
      SecurityToken token) {
    RestfulCollection<Activity> allActivities = getActivities(userId, groupId,
        token).getResponse();
    for (Activity activity : allActivities.getEntry()) {
      if (activity.getId().equals(activityId)) {
        return new ResponseItem<Activity>(activity);
      }
    }
    return new ResponseItem<Activity>(ResponseError.BAD_REQUEST,
        "Activity not found", null);
  }

  public ResponseItem createActivity(UserId personId, Activity activity,
      SecurityToken token) {
    // TODO: Validate the activity and do any template expanding
    activity.setUserId(personId.getUserId(token));
    activity.setPostedTime(new Date().getTime());

    fetcher.createActivity(personId.getUserId(token), activity);
    return new ResponseItem<JSONObject>(new JSONObject());
  }

}

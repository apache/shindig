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
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.dataservice.ActivityService;
import org.apache.shindig.social.dataservice.GroupId;
import org.apache.shindig.social.dataservice.RestfulCollection;
import org.apache.shindig.social.dataservice.UserId;
import org.apache.shindig.social.opensocial.model.Activity;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class BasicActivitiesService implements ActivityService {
  private XmlStateFileFetcher fetcher;

  @Inject
  public BasicActivitiesService(XmlStateFileFetcher fetcher) {
    this.fetcher = fetcher;
    fetcher.loadDefaultStateFileIfNoneLoaded();
  }

  public Future<ResponseItem<RestfulCollection<Activity>>> getActivities(UserId userId,
      GroupId groupId, String appId, Set<String> fields, SecurityToken token) {
    return ImmediateFuture.newInstance(getActivitiesInternal(userId, groupId, token));
  }

  private ResponseItem<RestfulCollection<Activity>> getActivitiesInternal(UserId userId,
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

    // TODO: Sort them, respect the fields param etc
    return new ResponseItem<RestfulCollection<Activity>>(
        new RestfulCollection<Activity>(activities));
  }

  public Future<ResponseItem<Activity>> getActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, String activityId, SecurityToken token) {

    RestfulCollection<Activity> allActivities = getActivitiesInternal(userId, groupId, token)
        .getResponse();
    for (Activity activity : allActivities.getEntry()) {
      if (activity.getId().equals(activityId)) {
        return ImmediateFuture.newInstance(new ResponseItem<Activity>(activity));
      }
    }
    return ImmediateFuture.newInstance(new ResponseItem<Activity>(ResponseError.BAD_REQUEST,
        "Activity not found", null));
  }

  public Future<ResponseItem<Object>> deleteActivity(UserId userId, GroupId groupId, String appId,
      String activityId, SecurityToken token) {
    fetcher.deleteActivity(userId.getUserId(token), activityId);
    return ImmediateFuture.newInstance(new ResponseItem<Object>(new Object()));
  }

  public Future<ResponseItem<Object>> createActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Activity activity, SecurityToken token) {

    // TODO: Validate the activity, respect the fields param, and do any template expanding
    activity.setUserId(userId.getUserId(token));
    activity.setPostedTime(new Date().getTime());

    fetcher.createActivity(userId.getUserId(token), activity);
    return ImmediateFuture.newInstance(new ResponseItem<Object>(new Object()));
  }

}

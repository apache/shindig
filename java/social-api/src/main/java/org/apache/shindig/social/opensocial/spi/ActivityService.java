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
package org.apache.shindig.social.opensocial.spi;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.inject.ImplementedBy;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * The ActivityService interface defines the service provider interface to retrieve activities from
 * the underlying SNS.
 */
@ImplementedBy(JsonDbOpensocialService.class)
public interface ActivityService {

  /**
   * Returns a list of activities that correspond to the passed in users and group.
   *
   * @param userIds The set of ids of the people to fetch activities for.
   * @param groupId Indicates whether to fetch activities for a group.
   * @param appId   The app id.
   * @param fields  The fields to return. Empty set implies all
   * @param token   A valid SecurityToken
   * @return a response item with the list of activities.
   */
  Future<RestfulCollection<Activity>> getActivities(Set<UserId> userIds,
      GroupId groupId, String appId, Set<String> fields, SecurityToken token)
      throws SocialSpiException;

  /**
   * Returns a set of activities for the passed in user and group that corresponds to a list of
   * activityIds.
   *
   * @param userId      The set of ids of the people to fetch activities for.
   * @param groupId     Indicates whether to fetch activities for a group.
   * @param appId       The app id.
   * @param fields      The fields to return. Empty set implies all
   * @param activityIds The set of activity ids to fetch.
   * @param token       A valid SecurityToken
   * @return a response item with the list of activities.
   */
  Future<RestfulCollection<Activity>> getActivities(UserId userId, GroupId groupId,
      String appId, Set<String> fields, Set<String> activityIds, SecurityToken token)
      throws SocialSpiException;


  /**
   * Returns a set of activities for the passed in user and group that corresponds to a single of
   * activityId
   *
   * @param userId     The set of ids of the people to fetch activities for.
   * @param groupId    Indicates whether to fetch activities for a group.
   * @param appId      The app id.
   * @param fields     The fields to return. Empty set implies all
   * @param activityId The activity id to fetch.
   * @param token      A valid SecurityToken
   * @return a response item with the list of activities.
   */
  Future<Activity> getActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, String activityId, SecurityToken token)
      throws SocialSpiException;

  /**
   * Deletes the activity for the passed in user and group that corresponds to the activityId.
   *
   * @param userId      The user.
   * @param groupId     The group.
   * @param appId       The app id.
   * @param activityIds A list of activity ids to delete.
   * @param token       A valid SecurityToken.
   * @return a response item containing any errors
   */
  Future<Void> deleteActivities(UserId userId, GroupId groupId, String appId,
      Set<String> activityIds, SecurityToken token) throws SocialSpiException;

  /**
   * Creates the passed in activity for the passed in user and group. Once createActivity is called,
   * getActivities will be able to return the Activity.
   *
   * @param userId   The id of the person to create the activity for.
   * @param groupId  The group.
   * @param appId    The app id.
   * @param fields   The fields to return.
   * @param activity The activity to create.
   * @param token    A valid SecurityToken
   * @return a response item containing any errors
   */
  Future<Void> createActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Activity activity, SecurityToken token) throws SocialSpiException;
}

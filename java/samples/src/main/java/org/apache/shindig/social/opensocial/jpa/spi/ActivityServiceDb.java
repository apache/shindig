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
package org.apache.shindig.social.opensocial.jpa.spi;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;

import java.util.Set;
import java.util.concurrent.Future;

/**
 *
 */
public class ActivityServiceDb implements ActivityService {

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#createActivity(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, org.apache.shindig.social.opensocial.model.Activity, org.apache.shindig.auth.SecurityToken)
   */
  public Future<Void> createActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Activity activity, SecurityToken token) throws SocialSpiException {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#deleteActivities(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, org.apache.shindig.auth.SecurityToken)
   */
  public Future<Void> deleteActivities(UserId userId, GroupId groupId, String appId,
      Set<String> activityIds, SecurityToken token) throws SocialSpiException {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#getActivities(java.util.Set, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, org.apache.shindig.auth.SecurityToken)
   */
  public Future<RestfulCollection<Activity>> getActivities(Set<UserId> userIds, GroupId groupId,
      String appId, Set<String> fields, SecurityToken token) throws SocialSpiException {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#getActivities(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, java.util.Set, org.apache.shindig.auth.SecurityToken)
   */
  public Future<RestfulCollection<Activity>> getActivities(UserId userId, GroupId groupId,
      String appId, Set<String> fields, Set<String> activityIds, SecurityToken token)
      throws SocialSpiException {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#getActivity(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, java.lang.String, org.apache.shindig.auth.SecurityToken)
   */
  public Future<Activity> getActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, String activityId, SecurityToken token) throws SocialSpiException {
    // TODO Auto-generated method stub
    return null;
  }

}

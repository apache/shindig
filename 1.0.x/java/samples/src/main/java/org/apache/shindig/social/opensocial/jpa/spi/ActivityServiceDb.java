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
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.opensocial.jpa.ActivityDb;
import org.apache.shindig.social.opensocial.jpa.ApplicationDataMapDb;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.RestfulCollection;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.inject.Inject;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 *
 */
public class ActivityServiceDb implements ActivityService {

  private EntityManager entityManager;

  @Inject
  public ActivityServiceDb(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

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
    return ImmediateFuture.newInstance(new RestfulCollection<Activity>(getActivities(userId, activityIds, token)));
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#getActivity(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, java.lang.String, org.apache.shindig.auth.SecurityToken)
   */
  public Future<Activity> getActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, String activityId, SecurityToken token) throws SocialSpiException {
    Activity activity = getActivities(userId, activityId,  token);
    if ( activity != null  ) {
      return ImmediateFuture.newInstance(activity);
    }
    throw new SocialSpiException(ResponseError.BAD_REQUEST,"Cant find activity");
  }

  
  /**
   * @param userId
   * @param groupId
   * @param appId
   * @param token
   * @return
   */
  private Activity getActivities(UserId userId, String activityId,
      SecurityToken token) {
    Query q = entityManager.createNamedQuery(ActivityDb.FINDBY_ACTIVITY_ID);
    String uid = SPIUtils.getUserList(userId, token);
    q.setParameter(ActivityDb.PARAM_USERID, uid);
    q.setParameter(ActivityDb.PARAM_ACTIVITYID, activityId);
    q.setFirstResult(1);
    q.setMaxResults(1);
    List<?> activities = q.getResultList();
    if ( activities != null && activities.size() > 0 ) {
      return (Activity) activities.get(1);
    }
    return null;
  }


  /**
   * @param userId
   * @param groupId
   * @param appId
   * @param token
   * @return
   */
  private List<Activity> getActivities(UserId userId, Set<String> activityIds,
      SecurityToken token) {
    StringBuilder sb = new StringBuilder();
    sb.append(ActivityDb.JPQL_FINDBY_ACTIVITIES);
    List<String> paramList = SPIUtils.toList(activityIds);
    String uid = SPIUtils.getUserList(userId, token);
    int lastPos = JPQLUtils.addInClause(sb, "a", "id", 1, paramList.size());
    sb.append(" and a.userid = ?").append(lastPos);
    lastPos++;
    paramList.add(uid);
    List<Activity> a = JPQLUtils.getListQuery(entityManager, sb.toString(), paramList, null);
    return a;
  }

}

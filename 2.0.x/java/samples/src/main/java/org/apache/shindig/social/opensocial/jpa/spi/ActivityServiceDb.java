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

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.jpa.ActivityDb;
import org.apache.shindig.social.opensocial.jpa.MediaItemDb;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;

/**
 * The Class ActivityServiceDb.
 */
public class ActivityServiceDb implements ActivityService {

  /** The entity manager. */
  private EntityManager entityManager;

  /**
   * Instantiates a new activity service db.
   * 
   * @param entityManager the entity manager
   */
  @Inject
  public ActivityServiceDb(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#createActivity(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, org.apache.shindig.social.opensocial.model.Activity, org.apache.shindig.auth.SecurityToken)
   */
  public Future<Void> createActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Activity activity, SecurityToken token) throws ProtocolException {
    String uid = SPIUtils.getUserList(userId, token);

    try {
      // Map activity into a new ActivityDb instance
      // TODO Could we use dozer to do this mapping instead, for future-proofing reasons?
      ActivityDb activityDb = new ActivityDb();
      activityDb.setPostedTime(new Date().getTime());
      activityDb.setAppId(appId);
      activityDb.setUserId(uid);
      activityDb.setId(activity.getId());
      activityDb.setBodyId(activity.getBodyId());
      activityDb.setBody(activity.getBody());
      activityDb.setExternalId(activity.getExternalId());
      activityDb.setTitleId(activity.getTitleId());
      activityDb.setTitle(activity.getTitle());
      activityDb.setUpdated(new Date());
      activityDb.setPriority(activity.getPriority());
      activityDb.setStreamFaviconUrl(activity.getStreamFaviconUrl());
      activityDb.setStreamSourceUrl(activity.getStreamSourceUrl());
      activityDb.setStreamTitle(activity.getStreamTitle());
      activityDb.setStreamUrl(activity.getStreamUrl());
      activityDb.setUrl(activity.getUrl());
      if(activity.getMediaItems() != null) {
        List<MediaItem> mediaItems = new ArrayList<MediaItem>();
        for(MediaItem m : activity.getMediaItems()) {
          MediaItemDb mediaItem = new MediaItemDb();
          mediaItem.setMimeType(m.getMimeType());
          mediaItem.setType(m.getType());
          mediaItem.setUrl(m.getUrl());
          mediaItems.add(mediaItem);
        }
        activityDb.setMediaItems(mediaItems);
      }
      if (activity.getTemplateParams() != null) {
        activityDb.setTemplateParams(activity.getTemplateParams());
      }

      // TODO How should transactions be managed? Should samples be using warp-persist instead?
      if (!entityManager.getTransaction().isActive()) {
        entityManager.getTransaction().begin();
      }
      entityManager.persist(activityDb);
      entityManager.getTransaction().commit();

    } catch (Exception e) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create activity", e);
    }

    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#deleteActivities(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, org.apache.shindig.auth.SecurityToken)
   */
  public Future<Void> deleteActivities(UserId userId, GroupId groupId, String appId,
      Set<String> activityIds, SecurityToken token) throws ProtocolException {
    // TODO Auto-generated method stub
    return null;
  }
  
  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#getActivities(java.util.Set, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, org.apache.shindig.social.opensocial.spi.CollectionOptions, org.apache.shindig.auth.SecurityToken)
   */
  public Future<RestfulCollection<Activity>> getActivities(Set<UserId> userIds,
      GroupId groupId, String appId, Set<String> fields,
      CollectionOptions options, SecurityToken token) throws ProtocolException {

    // TODO currently the implementation of this method ignores the fields variable. Is this correct?

    List<Activity> plist = null;
    int lastPos = 1;

    StringBuilder sb = new StringBuilder();
    // sanitize the list to get the uid's and remove duplicates
    List<String> paramList = SPIUtils.getUserList(userIds, token);
    // select the group Id as this will drive the query
    switch (groupId.getType()) {
    case all:
      // select all contacts
      sb.append("");
      lastPos = JPQLUtils.addInClause(sb, "p", "id", lastPos, paramList.size());
      break;
    case friends:
      // select all friends (subset of contacts)
      sb.append(ActivityDb.JPQL_FINDACTIVITY_BY_FRIENDS);
      lastPos = JPQLUtils.addInClause(sb, "p", "id", lastPos, paramList.size());
      sb.append(")) ");
      // TODO Group by doesn't work in HSQLDB or Derby - causes a "Not in aggregate function or group by clause" jdbc exception
      // sb.append(" group by p ");
      break;
    case groupId:
      // select those in the group
      // TODO Needs implementing and then have a unit test created to test it.
      sb.append("");
      lastPos = JPQLUtils.addInClause(sb, "p", "id", lastPos, paramList.size());
      sb.append(" and g.id = ?").append(lastPos);
      lastPos++;
      break;
    case deleted:
      // ???
      break;
    case self:
      // select self
      sb.append(ActivityDb.JPQL_FINDACTIVITY);
      lastPos = JPQLUtils.addInClause(sb, "a", "userId", lastPos, paramList.size());
      break;
    default:
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Group ID not recognized");

    }
    
    // Get total results, that is count the total number of rows for this query
    Long totalResults = JPQLUtils.getTotalResults(entityManager, sb.toString(), paramList);
    
    // Execute paginated query
    if (totalResults > 0) {
      plist = JPQLUtils.getListQuery(entityManager, sb.toString(), paramList, options);
    }

    if (plist == null) {
      plist = Lists.newArrayList();
    }

    plist = JPQLUtils.getListQuery(entityManager, sb.toString(), paramList, null);

    if (plist == null) {
      plist = new ArrayList<Activity>();
    }

    // all of the above could equally have been placed into a thread to overlay the
    // db wait times.
    RestfulCollection<Activity> restCollection = new RestfulCollection<Activity>(
        plist, options.getFirst(), totalResults.intValue(), options.getMax());
    return ImmediateFuture.newInstance(restCollection);
  }
  
  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#getActivities(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, org.apache.shindig.social.opensocial.spi.CollectionOptions, java.util.Set, org.apache.shindig.auth.SecurityToken)
   */
  public Future<RestfulCollection<Activity>> getActivities(UserId userId,
      GroupId groupId, String appId, Set<String> fields,
      CollectionOptions options, Set<String> activityIds, SecurityToken token)
      throws ProtocolException {
    return ImmediateFuture.newInstance(new RestfulCollection<Activity>(getActivities(userId, activityIds, token)));
  }

  /* (non-Javadoc)
   * @see org.apache.shindig.social.opensocial.spi.ActivityService#getActivity(org.apache.shindig.social.opensocial.spi.UserId, org.apache.shindig.social.opensocial.spi.GroupId, java.lang.String, java.util.Set, java.lang.String, org.apache.shindig.auth.SecurityToken)
   */
  public Future<Activity> getActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, String activityId, SecurityToken token) throws ProtocolException {
    Activity activity = getActivities(userId, activityId,  token);
    if ( activity != null  ) {
      return ImmediateFuture.newInstance(activity);
    }
    throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST,"Cant find activity");
  }


  /**
   * Gets the activities.
   * 
   * @param userId the user id
   * @param token the token
   * @param activityId the activity id
   * 
   * @return the activities
   */
  private Activity getActivities(UserId userId, String activityId,
      SecurityToken token) {
    Query q = entityManager.createNamedQuery(ActivityDb.FINDBY_ACTIVITY_ID);
    String uid = SPIUtils.getUserList(userId, token);
    q.setParameter(ActivityDb.PARAM_USERID, uid);
    q.setParameter(ActivityDb.PARAM_ACTIVITYID, activityId);
    q.setFirstResult(0);
    q.setMaxResults(1);
    List<?> activities = q.getResultList();
    if ( activities != null && !activities.isEmpty()) {
      return (Activity) activities.get(0);
    }
    return null;
  }


  /**
   * Gets the activities.
   * 
   * @param userId the user id
   * @param token the token
   * @param activityIds the activity ids
   * 
   * @return the activities
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

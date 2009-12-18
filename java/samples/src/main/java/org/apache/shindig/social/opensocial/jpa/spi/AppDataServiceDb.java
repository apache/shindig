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
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.jpa.ApplicationDataMapDb;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;

/**
 *
 */
public class AppDataServiceDb implements AppDataService {

  private EntityManager entityManager;

  @Inject
  public AppDataServiceDb(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * {@inheritDoc}
   */
  public Future<Void> deletePersonData(UserId userId, GroupId groupId, String appId,
      Set<String> fields, SecurityToken token) throws ProtocolException {

    List<ApplicationDataMapDb> dataMaps = getDataMap(userId, groupId, appId, token);
    for (ApplicationDataMapDb adm : dataMaps) {
      for (String f : fields) {
        adm.getValues().remove(f);
      }
    }
     // TODO How should transactions be managed? Should samples be using warp-persist instead?
     if (!entityManager.getTransaction().isActive()) {
       entityManager.getTransaction().begin();
     }

    entityManager.flush();
    entityManager.getTransaction().commit();

    return ImmediateFuture.newInstance(null);
  }

  /**
   * @param userId
   * @param groupId
   * @param appId
   * @param token
   * @return
   */
  private List<ApplicationDataMapDb> getDataMap(UserId userId, GroupId groupId, String appId,
      SecurityToken token) {
    List<String> paramList = Lists.newArrayList();
    paramList.add(SPIUtils.getUserList(userId, token));
    int lastParam = 1;
    StringBuilder sb = new StringBuilder();

    switch (groupId.getType()) {
    case all:
      // userId translates into all contacts
      sb.append(ApplicationDataMapDb.FINDBY_ALL_GROUP);
      sb.append(" and am.personId = ?").append(lastParam);
      lastParam++;
      break;
    case deleted:
      // ignored
      break;
    case friends:
      sb.append(ApplicationDataMapDb.FINDBY_FRIENDS_GROUP);
      sb.append(" and am.personId = ?").append(lastParam);
      lastParam++;
      // userId translates into all friends
      break;
    case groupId:
      sb.append(ApplicationDataMapDb.FINDBY_GROUP_GROUP);
      sb.append(" and am.personId = ?").append(lastParam);
      lastParam++;
      sb.append(" and g.id = ?").append(lastParam);
      paramList.add(groupId.getGroupId());
      lastParam++;
      // userId translates into friends within a group
      break;
    default: // including self
      // userId is the user Id
      sb.append(ApplicationDataMapDb.FINDBY_SELF_GROUP);
      sb.append(" am.personId = ?").append(lastParam);
      lastParam++;
      break;

    }
    sb.append(" and am.application.id = ?").append(lastParam);
    lastParam++;
    paramList.add(appId);
    return JPQLUtils.getListQuery(entityManager, sb.toString(), paramList, null);

  }

  /**
   * {@inheritDoc}
   */
  public Future<DataCollection> getPersonData(Set<UserId> userIds, GroupId groupId, String appId,
      Set<String> fields, SecurityToken token) throws ProtocolException {
    List<String> paramList = SPIUtils.getUserList(userIds, token);
    int lastParam = 1;
    StringBuilder sb = new StringBuilder();

    switch (groupId.getType()) {
    case all:
      // userId translates into all contacts
      sb.append(ApplicationDataMapDb.FINDBY_ALL_GROUP);
      lastParam = JPQLUtils.addInClause(sb, "am", "personId", lastParam, paramList.size());
      break;
    case deleted:
      // ignored
      break;
    case friends:
      sb.append(ApplicationDataMapDb.FINDBY_FRIENDS_GROUP);
      lastParam = JPQLUtils.addInClause(sb, "p", "id", lastParam, paramList.size());
      sb.append(')');
      // userId translates into all friends
      break;
    case groupId:
      sb.append(ApplicationDataMapDb.FINDBY_GROUP_GROUP);
      lastParam = JPQLUtils.addInClause(sb, "am", "personId", lastParam, paramList.size());
      sb.append(" and g.id = ?").append(lastParam);
      paramList.add(groupId.getGroupId());
      lastParam++;
      // userId translates into friends within a group
      break;
    default: // including self
      // userId is the user Id
      sb.append(ApplicationDataMapDb.FINDBY_SELF_GROUP);
      lastParam = JPQLUtils.addInClause(sb, "am", "personId", lastParam, paramList.size());
      break;

    }
    sb.append(" and am.application.id = ?").append(lastParam);
    lastParam++;
    paramList.add(appId);

    // load the map up
    List<ApplicationDataMapDb> dataMaps = JPQLUtils.getListQuery(entityManager, sb.toString(),
        paramList, null);
    Map<String, Map<String, String>> results = new HashMap<String, Map<String, String>>();

    // only add in the fields
    if (fields == null || fields.isEmpty()) {
      for (ApplicationDataMapDb adm : dataMaps) {
        results.put(adm.getPersonId(), adm.getValues());
      }
    } else {
      for (ApplicationDataMapDb adm : dataMaps) {
        Map<String, String> m = Maps.newHashMap();
        for (String f : fields) {
          String value = adm.getValues().get(f);
          if (null != value) {
            m.put(f, value);
          }
        }
        results.put(adm.getPersonId(), m);
      }
    }
    DataCollection dc = new DataCollection(results);
    return ImmediateFuture.newInstance(dc);
  }

  /**
   * {@inheritDoc}
   */
  public Future<Void> updatePersonData(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Map<String, String> values, SecurityToken token)
      throws ProtocolException {
    List<ApplicationDataMapDb> dataMaps = getDataMap(userId, groupId, appId, token);
    for (ApplicationDataMapDb adm : dataMaps) {
      for (String f : fields) {
        adm.getValues().put(f, values.get(f));
      }
    }

    // TODO How should transactions be managed? Should samples be using warp-persist instead?
    if (!entityManager.getTransaction().isActive()) {
      entityManager.getTransaction().begin();
    }
    entityManager.flush();
    entityManager.getTransaction().commit();

    return ImmediateFuture.newInstance(null);
  }

}

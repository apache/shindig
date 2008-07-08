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
package org.apache.shindig.social.abdera.json;

import com.google.inject.Inject;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.abdera.SocialRequestContext;
import org.apache.shindig.social.abdera.SocialRouteManager;
import org.apache.shindig.social.dataservice.GroupId;
import org.apache.shindig.social.dataservice.RestfulCollection;
import org.apache.shindig.social.dataservice.UserId;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import java.util.concurrent.Future;

public class PersonJsonAdapter extends SimpleJsonAdapter<Person> {

  @Inject
  public PersonJsonAdapter(BeanJsonConverter beanJsonConverter) {
    super(beanJsonConverter);
  }

  @Override
  public Future<ResponseItem<Person>> getEntity(SocialRequestContext request, SecurityToken token) {
    String uid = request.getTarget().getParameter("uid");
    UserId userId = UserId.fromJson(uid);

    return personService.getPerson(userId, request.getFields(), token);
  }

  @Override
  public Future<ResponseItem<RestfulCollection<Person>>> getEntities(SocialRequestContext request,
      SecurityToken token) {
    String uid = request.getTarget().getParameter("uid");
    UserId userId = UserId.fromJson(uid);

    GroupId groupId;
    switch (SocialRouteManager.getUrlTemplate(request)) {
      case JSON_PROFILES_OF_CONNECTIONS_OF_USER:
        groupId = new GroupId(GroupId.Type.all, "all");
        break;
      case JSON_PROFILES_OF_FRIENDS_OF_USER:
        // TODO: Change activities service to handle the friend lookup itself
        groupId = new GroupId(GroupId.Type.friends, "friends");
        break;
      case JSON_PROFILES_IN_GROUP_OF_USER:
        groupId = new GroupId(GroupId.Type.groupId, request.getTarget().getParameter("gid"));
        break;
      default:
        // TODO: Clean this code up so we don't need this check
        throw new UnsupportedOperationException(
            "The person adpater was reached with an unsupported url");
    }

    return personService.getPeople(userId, groupId, request.getOrderBy(), request.getFilterBy(),
        request.getStartIndex(), request.getCount(), request.getFields(), token);
  }
}

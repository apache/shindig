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

import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.Set;
import java.util.concurrent.Future;


public class PersonHandler extends DataRequestHandler {

  private final PersonService personService;

  private static final String PEOPLE_PATH = "/people/{userId}+/{groupId}/{personId}+";

  @Inject
  public PersonHandler(PersonService personService) {
    this.personService = personService;
  }

  protected Future<? extends ResponseItem> handleDelete(RequestItem request) {
    return error(ResponseError.BAD_REQUEST, "You can't delete people.", null);
  }

  protected Future<? extends ResponseItem> handlePut(RequestItem request) {
    return error(ResponseError.NOT_IMPLEMENTED, "You can't update right now.", null);
  }

  protected Future<? extends ResponseItem> handlePost(RequestItem request) {
    return error(ResponseError.NOT_IMPLEMENTED, "You can't add people right now.", null);
  }

  /**
   * Allowed end-points /people/{userId}+/{groupId} /people/{userId}/{groupId}/{optionalPersonId}+
   *
   * examples: /people/john.doe/@all /people/john.doe/@friends /people/john.doe/@self
   */
  protected Future<? extends ResponseItem> handleGet(RequestItem request) {
    request.applyUrlTemplate(PEOPLE_PATH);

    GroupId groupId = request.getGroup();
    Set<String> optionalPersonId = Sets.newLinkedHashSet(request.getListParameter("personId"));
    Set<String> fields = request.getFields(Person.Field.DEFAULT_FIELDS);
    Set<UserId> userIds = request.getUsers();

    // Preconditions
    Preconditions.requireNotEmpty(userIds, "No userId specified");
    if (userIds.size() > 1 && !optionalPersonId.isEmpty()) {
      throw new IllegalArgumentException("Cannot fetch personIds for multiple userIds");
    }

    if (userIds.size() == 1) {
      if (optionalPersonId.isEmpty()) {
        if (groupId.getType() == GroupId.Type.self) {
          return personService.getPerson(userIds.iterator().next(), fields, request.getToken());
        } else {
          return personService.getPeople(userIds, groupId, request.getSortBy(),
              request.getSortOrder(), request.getFilterBy(), request.getFilterOperation(),
              request.getFilterValue(), request.getStartIndex(),
              request.getCount(), fields, request.getToken());
        }
      } else if (optionalPersonId.size() == 1) {
        // TODO: Add some crazy concept to handle the userId?
        return personService.getPerson(new UserId(UserId.Type.userId,
            optionalPersonId.iterator().next()),
            fields, request.getToken());
      } else {
        Set<UserId> personIds = Sets.newLinkedHashSet();
        for (String pid : optionalPersonId) {
          personIds.add(new UserId(UserId.Type.userId, pid));
        }
        // Every other case is a collection response of optional person ids
        return personService.getPeople(personIds, new GroupId(GroupId.Type.self, null),
            request.getSortBy(), request.getSortOrder(), request.getFilterBy(),
            request.getFilterOperation(), request.getFilterValue(), request.getStartIndex(),
            request.getCount(), fields, request.getToken());
      }
    }

    // Every other case is a collection response.
    return personService.getPeople(userIds, groupId, request.getSortBy(),
        request.getSortOrder(), request.getFilterBy(), request.getFilterOperation(),
        request.getFilterValue(), request.getStartIndex(), request.getCount(),
        fields, request.getToken());
  }
}

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

import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.common.util.ImmediateFuture;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import java.util.Set;
import java.util.concurrent.Future;


public class PersonHandler extends DataRequestHandler {
  private PersonService personService;

  private static final String PEOPLE_PATH = "/people/{userId}/{groupId}/{personId}";
  protected static final Set<String> DEFAULT_PERSON_FIELDS = Sets.newHashSet(
      Person.Field.ID.toString(),
      Person.Field.NAME.toString(),
      Person.Field.THUMBNAIL_URL.toString());

  @Inject
  public PersonHandler(PersonService personService) {
    this.personService = personService;
  }

  protected Future<? extends ResponseItem> handleDelete(RequestItem request) {
    return ImmediateFuture.newInstance(new ResponseItem<Object>(ResponseError.BAD_REQUEST,
        "You can't delete people. ", null));
  }

  protected Future<? extends ResponseItem> handlePut(RequestItem request) {
    return ImmediateFuture.newInstance(new ResponseItem<Object>(ResponseError.NOT_IMPLEMENTED,
        "You can't add people right now. ", null));
  }

  protected Future<? extends ResponseItem> handlePost(RequestItem request) {
    return ImmediateFuture.newInstance(new ResponseItem<Object>(ResponseError.NOT_IMPLEMENTED,
        "You can't add people right now. ", null));
  }

  /**
   * /people/{userId}/{groupId}/{optionalPersonId}
   *
   * examples:
   * /people/john.doe/@all
   * /people/john.doe/@friends
   * /people/john.doe/@self
   */
  protected Future<? extends ResponseItem> handleGet(RequestItem request) {
    request.parseUrlWithTemplate(PEOPLE_PATH);

    UserId userId = request.getUser();
    GroupId groupId = request.getGroup();
    String optionalPersonId = request.getParameters().get("personId");
    Set<String> fields = request.getFields(DEFAULT_PERSON_FIELDS);

    if (groupId.getType() == GroupId.Type.self) {
      return personService.getPerson(userId, fields, request.getToken());

    } else if (optionalPersonId != null) {
      // TODO: Add some crazy concept to handle the userId?
      return personService.getPerson(new UserId(UserId.Type.userId, optionalPersonId),
          fields, request.getToken());
    }

    return personService.getPeople(userId, groupId, request.getOrderBy(),
        request.getFilterBy(), request.getStartIndex(), request.getCount(),
        fields, request.getToken());
  }

}

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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;


public class PersonHandler extends DataRequestHandler {
  private PersonService personService;

  @Inject
  public PersonHandler(PersonService personService) {
    this.personService = personService;
  }

  ResponseItem handleDelete(HttpServletRequest servletRequest,
      SecurityToken token) {
    return new ResponseItem<Object>(ResponseError.BAD_REQUEST,
        "You can't delete people. ", null);
  }

  ResponseItem handlePut(HttpServletRequest servletRequest,
      SecurityToken token) {
    return new ResponseItem<Object>(ResponseError.NOT_IMPLEMENTED,
        "You can't add people right now. ", null);  }

  ResponseItem handlePost(HttpServletRequest servletRequest,
      SecurityToken token) {
    return new ResponseItem<Object>(ResponseError.NOT_IMPLEMENTED,
        "You can't add people right now. ", null);
  }

  /**
   * /people/{userId}/{groupId}/{optionalPersonId}
   *
   * examples:
   * /people/john.doe/@all
   * /people/john.doe/@friends
   * /people/john.doe/@self
   */
  ResponseItem handleGet(HttpServletRequest servletRequest,
      SecurityToken token) {
    String[] segments = getParamsFromRequest(servletRequest);

    UserId userId = UserId.fromJson(segments[0]);
    GroupId groupId = GroupId.fromJson(segments[1]);

    String optionalPersonId = null;
    if (segments.length > 2) {
      optionalPersonId = segments[2];
    }

    if (optionalPersonId != null
        || groupId.getType() == GroupId.Type.self) {
      return personService.getPerson(userId, token);
    }

    PersonService.SortOrder sort = getEnumParam(servletRequest, "orderBy",
        PersonService.SortOrder.topFriends, PersonService.SortOrder.class);
    PersonService.FilterType filter = getEnumParam(servletRequest, "filterBy",
        PersonService.FilterType.all, PersonService.FilterType.class);

    int first = getIntegerParam(servletRequest, "startIndex", 0);
    int max = getIntegerParam(servletRequest, "count", 20);

    Set<String>  profileDetails = Sets.newHashSet(
        getListParam(servletRequest, "fields",
            Lists.newArrayList(Person.Field.ID.toString(),
                Person.Field.NAME.toString(),
                Person.Field.THUMBNAIL_URL.toString())));

    return personService.getPeople(userId, groupId, sort, filter, first, max,
        profileDetails, token);
  }

}

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

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.inject.ImplementedBy;

import java.util.Set;
import java.util.concurrent.Future;

@ImplementedBy(JsonDbOpensocialService.class)

public interface PersonService {

  public static String TOP_FRIENDS_SORT = "topFriends";
  public static String TOP_FRIENDS_FILTER = "topFriends";
  public static String HAS_APP_FILTER = "hasApp";

  public enum SortOrder {
    ascending, descending
  }

  public enum FilterOperation {
    contains, equals, startsWith, present
  }

  /**
   * Returns a list of people that correspond to the passed in person ids.
   *
   * @param userIds A set of users
   * @param groupId The group
   * @param collectionOptions How to filter, sort and paginate the collection being fetched
   * @param fields The profile details to fetch. Empty set implies all
   * @param token The gadget token @return a list of people.
   */
  Future<RestfulCollection<Person>> getPeople(Set<UserId> userIds, GroupId groupId,
      CollectionOptions collectionOptions, Set<String> fields, SecurityToken token);

  /**
   * Returns a person that corresponds to the passed in person id.
   *
   * @param id The id of the person to fetch.
   * @param fields The fields to fetch.
   * @param token The gadget token
   * @return a list of people.
   */
  Future<RestfulItem<Person>> getPerson(UserId id, Set<String> fields, SecurityToken token);
}

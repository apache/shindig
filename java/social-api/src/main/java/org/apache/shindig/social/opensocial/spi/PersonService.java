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
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.inject.ImplementedBy;

import java.util.Set;
import java.util.concurrent.Future;

@ImplementedBy(JsonDbOpensocialService.class)

public interface PersonService {

  /**
   * When used will sort people by the container's definition of top friends. Note that both the
   * sort order and the filter are required to deliver a topFriends response. The PersonService
   * implementation should take this into account when delivering a topFriends response.
   */
  public static String TOP_FRIENDS_SORT = "topFriends";
  /**
   * Retrieves only the user's top friends. The meaning of top and how many top is is defined by the
   * PersonService implementation.
   */
  public static String TOP_FRIENDS_FILTER = "topFriends";
  /**
   * Retrieves all friends with any data for this application.
   * TODO: how is this application defined
   */
  public static String HAS_APP_FILTER = "hasApp";
  /**
   * Retrieves all friends. (ie no filter)
   */
  public static String ALL_FILTER = "all";
  /**
   * Will filter the people requested by checking if they are friends with the given idSpec. The
   * filter value will be set to the userId of the target friend.
   */
  public static String IS_WITH_FRIENDS_FILTER = "isFriendsWith";

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
      CollectionOptions collectionOptions, Set<String> fields, SecurityToken token)
      throws SocialSpiException;

  /**
   * Returns a person that corresponds to the passed in person id.
   *
   * @param id The id of the person to fetch.
   * @param fields The fields to fetch.
   * @param token The gadget token
   * @return a list of people.
   */
  Future<Person> getPerson(UserId id, Set<String> fields, SecurityToken token)
      throws SocialSpiException;
}

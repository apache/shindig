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
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;

import com.google.inject.ImplementedBy;

import java.util.Set;
import java.util.concurrent.Future;

@ImplementedBy(JsonDbOpensocialService.class)

public interface PersonService {

  public enum SortBy {
    topFriends, name
  }

  public enum SortOrder {
    ascending, descending
  }

  public enum FilterType {
    all, hasApp, topFriends
  }

  public enum FilterOperation {
    contains, equals, startsWith, present
  }

  /**
   * Returns a list of people that correspond to the passed in person ids.
   *
   * @param userIds A set of users
   * @param groupId The group
   * @param sortBy How to sort the people
   * @param sortOrder The direction of the sort
   * @param filter How the people should be filtered.
   * @param filterOperation The operation to filter with 
   * @param filterValue The value to filter with.
   * @param first The index of the first person to fetch.
   * @param max The max number of people to fetch.
   * @param fields The profile details to fetch. Empty set implies all
   * @param token The gadget token @return a list of people.
   * TODO(doll): This method is getting way too long. We need to pass a more complex object instead.
   */
  Future<ResponseItem<RestfulCollection<Person>>> getPeople(Set<UserId> userIds, GroupId groupId,
      SortBy sortBy, SortOrder sortOrder, FilterType filter, FilterOperation filterOperation,
      String filterValue, int first, int max,
      Set<String> fields, SecurityToken token);

  /**
   * Returns a person that corresponds to the passed in person id.
   *
   * @param id The id of the person to fetch.
   * @param fields The fields to fetch.
   * @param token The gadget token
   * @return a list of people.
   */
  Future<ResponseItem<Person>> getPerson(UserId id, Set<String> fields, SecurityToken token);
}

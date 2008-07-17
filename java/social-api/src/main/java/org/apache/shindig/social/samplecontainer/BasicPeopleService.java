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
package org.apache.shindig.social.samplecontainer;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.dataservice.GroupId;
import org.apache.shindig.social.dataservice.PersonService;
import org.apache.shindig.social.dataservice.RestfulCollection;
import org.apache.shindig.social.dataservice.UserId;
import org.apache.shindig.social.opensocial.model.Person;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class BasicPeopleService implements PersonService {
  private static final Comparator<Person> NAME_COMPARATOR
      = new Comparator<Person>() {
    public int compare(Person person, Person person1) {
      String name = person.getName().getUnstructured();
      String name1 = person1.getName().getUnstructured();
      return name.compareTo(name1);
    }
  };

  private XmlStateFileFetcher fetcher;

  @Inject
  public BasicPeopleService(XmlStateFileFetcher fetcher) {
    this.fetcher = fetcher;
    fetcher.loadDefaultStateFileIfNoneLoaded();
  }

  private List<Person> getPeople(List<String> ids, SecurityToken token) {
    Map<String, Person> allPeople = fetcher.getAllPeople();

    List<Person> people = Lists.newArrayList();
    for (String id : ids) {
      Person person = allPeople.get(id);
      if (person != null) {
        if (id.equals(token.getViewerId())) {
          person.setIsViewer(true);
        }
        if (id.equals(token.getOwnerId())) {
          person.setIsOwner(true);
        }
        people.add(person);
      }
    }
    return people;
  }

  public Future<ResponseItem<RestfulCollection<Person>>> getPeople(UserId userId,
      GroupId groupId, PersonService.SortOrder sortOrder,
      PersonService.FilterType filter, int first, int max,
      Set<String> profileDetails, SecurityToken token) {
    List<String> ids = Lists.newArrayList();
    switch (groupId.getType()) {
      case all:
      case friends:
        List<String> friendIds = fetcher.getFriendIds().get(userId.getUserId(token));
        if (friendIds != null) {
          ids.addAll(friendIds);
        }
        break;
      case self:
        ids.add(userId.getUserId(token));
    }

    List<Person> people = getPeople(ids, token);

    // We can pretend that by default the people are in top friends order
    if (sortOrder.equals(PersonService.SortOrder.name)) {
      Collections.sort(people, NAME_COMPARATOR);
    }

    // TODO: The samplecontainer doesn't really have the concept of HAS_APP so
    // we can't support any filters yet. We should fix this.

    int totalSize = people.size();
    int last = first + max;
    people = people.subList(first, Math.min(last, totalSize));

    RestfulCollection<Person> collection = new RestfulCollection<Person>(people,
        first, totalSize);
    return ImmediateFuture.newInstance(new ResponseItem<RestfulCollection<Person>>(collection));
  }

  public Future<ResponseItem<Person>> getPerson(UserId id, Set<String> fields,
      SecurityToken token) {
    List<Person> people = getPeople(Lists.newArrayList(id.getUserId(token)), token);
    if (people.size() == 1) {
      return ImmediateFuture.newInstance(new ResponseItem<Person>(people.get(0)));
    } else {
      return ImmediateFuture.newInstance(new ResponseItem<Person>(ResponseError.BAD_REQUEST,
          "Person " + id.getUserId(token) + " not found", null));
    }
  }

}

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
package org.apache.shindig.social.file;

import org.apache.shindig.social.PeopleHandler;
import org.apache.shindig.social.Person;
import org.apache.shindig.social.Name;
import org.apache.shindig.social.IdSpec;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;

public class BasicPeopleHandler implements PeopleHandler {
  public List<Person> getPeople(IdSpec idSpec) throws JSONException {
    // TODO: Actually read from file
    // TODO: Use the opensource Collections library
    ArrayList<Person> people = new ArrayList<Person>();

    switch (idSpec.getType()) {
      case VIEWER:
      case OWNER:
        people.add(new Person("john.doe", new Name("John Doe")));
        break;
      case VIEWER_FRIENDS:
      case OWNER_FRIENDS:
        people.add(new Person("jane.doe", new Name("Jane Doe")));
        people.add(new Person("jane.doe", new Name("George Doe")));
        break;
      case USER_IDS:
        List<String> userIds = idSpec.fetchUserIds();
        for (String userId : userIds) {
          people.add(new Person(userId, new Name(userId)));
        }
    }
    return people;
  }
}

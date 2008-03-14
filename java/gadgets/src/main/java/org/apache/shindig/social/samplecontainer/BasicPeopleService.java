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

import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.opensocial.PeopleService;
import org.apache.shindig.social.opensocial.model.IdSpec;
import org.apache.shindig.social.opensocial.model.Person;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BasicPeopleService implements PeopleService {

  public ResponseItem<List<Person>> getPeople(List<String> ids) {
    Map<String, Person> allPeople = XmlStateFileFetcher.get().getAllPeople();

    List<Person> people = new ArrayList<Person>();
    for (String id : ids) {
      people.add(allPeople.get(id));
    }
    return new ResponseItem<List<Person>>(people);
  }

  public List<String> getIds(IdSpec idSpec) throws JSONException {
    Map<IdSpec.Type, List<String>> idMap
        = XmlStateFileFetcher.get().getIdMap();

    if (idSpec.getType() == IdSpec.Type.USER_IDS) {
      return idSpec.fetchUserIds();
    } else {
      return idMap.get(idSpec.getType());
    }
  }
}

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

import org.apache.shindig.social.IdSpec;
import org.apache.shindig.social.Name;
import org.apache.shindig.social.PeopleHandler;
import org.apache.shindig.social.Person;
import org.apache.shindig.social.Phone;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicPeopleHandler implements PeopleHandler {
  private Map<IdSpec.Type, List<String>> idMap
      = new HashMap<IdSpec.Type, List<String>>();
  private Map<String, Person> allPeople
      = new HashMap<String, Person>();

  public BasicPeopleHandler() {
    // TODO: Get file from user in web ui
    String stateFile = "http://localhost:8080/gadgets/files/samplecontainer/state-basicfriendlist.xml";
    XmlStateFileFetcher fetcher = new XmlStateFileFetcher(stateFile);
    Document doc = fetcher.fetchStateDocument(true);
    setupData(doc);
  }

  private void setupData(Document stateDocument) {
    Element root = stateDocument.getDocumentElement();

    // TODO: Eventually the viewer and owner shouldn't be hardcoded. You should
    // be able to visit other allPeople's "profile" pages in the sample container
    setupPeopleInXmlTag(root, "viewer", IdSpec.Type.VIEWER);
    setupPeopleInXmlTag(root, "owner", IdSpec.Type.OWNER);
    setupPeopleInXmlTag(root, "viewerFriends", IdSpec.Type.VIEWER_FRIENDS);
    setupPeopleInXmlTag(root, "ownerFriends", IdSpec.Type.OWNER_FRIENDS);

    // Handle empty people
    if (idMap.get(IdSpec.Type.OWNER).isEmpty()) {
      idMap.put(IdSpec.Type.OWNER, idMap.get(IdSpec.Type.VIEWER));
    }

    if (idMap.get(IdSpec.Type.OWNER_FRIENDS).isEmpty()) {
      idMap.put(IdSpec.Type.OWNER_FRIENDS,
          idMap.get(IdSpec.Type.VIEWER_FRIENDS));
    }
  }

  // Adds all people in the xml tag to the allPeople map.
  // Also returns the relevant ids
  private void setupPeopleInXmlTag(Element root, String tagName,
      IdSpec.Type idType) {
    // TODO: Use the opensource Collections library
    List<String> ids = new ArrayList<String>();

    NodeList elements = root.getElementsByTagName(tagName);
    if (elements == null || elements.item(0) == null) {
      idMap.put(idType, ids);
      return;
    }

    NodeList personNodes = elements.item(0).getChildNodes();

    for (int i = 0; i < personNodes.getLength(); i++) {
      NamedNodeMap attributes = personNodes.item(i).getAttributes();
      if (attributes == null) {
        continue;
      }

      String name = attributes.getNamedItem("name").getNodeValue();
      String id = attributes.getNamedItem("id").getNodeValue();
      Person person = new Person(id, new Name(name));

      Node phoneItem = attributes.getNamedItem("phone");
      if (phoneItem != null) {
        String phone = phoneItem.getNodeValue();
        Phone[] phones = {new Phone(phone, null)};
        person.setPhoneNumbers(phones);
      }

      allPeople.put(id, person);
      ids.add(id);
    }

    idMap.put(idType, ids);
  }

  public List<Person> getPeople(List<String> ids) {
    List<Person> people = new ArrayList<Person>();
    for (String id : ids) {
      people.add(allPeople.get(id));
    }
    return people;
  }

  public List<String> getIds(IdSpec idSpec) throws JSONException {
    if (idSpec.getType() == IdSpec.Type.USER_IDS) {
      return idSpec.fetchUserIds();
    } else {
      return idMap.get(idSpec.getType());
    }
  }
}

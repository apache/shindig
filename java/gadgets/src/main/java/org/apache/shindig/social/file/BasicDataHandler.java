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

import org.apache.shindig.social.DataHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicDataHandler implements DataHandler {
  // TODO: This obviously won't work on multiple servers
  // If we care then we should do something about it
  private static Map<String, Map<String, String>> allData;

  public BasicDataHandler() {
    if (allData != null) {
      return;
    }

    allData = new HashMap<String, Map<String, String>>();

    // TODO: Should use guice here for one global thingy and get url from web ui
    String stateFile = "http://localhost:8080/gadgets/files/samplecontainer/state-basicfriendlist.xml";
    XmlStateFileFetcher fetcher = new XmlStateFileFetcher(stateFile);
    Document doc = fetcher.fetchStateDocument(true);
    setupData(doc);
  }

  private void setupData(Document stateDocument) {
    Element root = stateDocument.getDocumentElement();

    NodeList elements = root.getElementsByTagName("personAppData");
    NodeList personDataNodes = elements.item(0).getChildNodes();

    for (int i = 0; i < personDataNodes.getLength(); i++) {
      Node personDataNode = personDataNodes.item(i);
      NamedNodeMap attributes = personDataNode.getAttributes();
      if (attributes == null) {
        continue;
      }

      String id = attributes.getNamedItem("person").getNodeValue();
      String field = attributes.getNamedItem("field").getNodeValue();
      String value = personDataNode.getTextContent();

      Map<String, String> currentData = allData.get(id);
      if (currentData == null) {
        currentData = new HashMap<String, String>();
        allData.put(id, currentData);
      }
      currentData.put(field, value);
    }
  }

  public Map<String, Map<String, String>> getPersonData(List<String> ids) {
    // TODO: Use the opensource Collections library
    Map<String, Map<String, String>> data =
        new HashMap<String, Map<String, String>>();

    for (String id : ids) {
      data.put(id, allData.get(id));
    }

    return data;
  }

  public boolean updatePersonData(String id, String key, String value) {
    if (!isValidKey(key)) {
      return false;
    }

    Map<String, String> personData = allData.get(id);
    if (personData == null) {
      personData = new HashMap<String, String>();
      allData.put(id, personData);
    }

    personData.put(key, value);
    return true;
  }

  /**
   * Determines whether the input is a valid key. Valid keys match the regular
   * expression [\w\-\.]+. The logic is not done using java.util.regex.* as
   * that is 20X slower.
   *
   * @param key the key to validate.
   * @return true if the key is a valid appdata key, false otherwise.
   */
  public static boolean isValidKey(String key) {
    if (key == null || key.length() == 0) {
      return false;
    }
    for (int i = 0; i < key.length(); ++i) {
      char c = key.charAt(i);
      if ((c >= 'a' && c <= 'z') ||
          (c >= 'A' && c <= 'Z') ||
          (c >= '0' && c <= '9') ||
          (c == '-') ||
          (c == '_') ||
          (c == '.')) {
        continue;
      }
      return false;
    }
    return true;
  }

}

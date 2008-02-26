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
import org.apache.shindig.social.IdSpec;
import org.json.JSONException;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class BasicDataHandler implements DataHandler {
  public Map<String, Map<String, String>> getPersonData(IdSpec idSpec)
      throws JSONException {
    // TODO: Actually read from file
    // TODO: Use the opensource Collections library
    Map<String, Map<String, String>> map =
        new HashMap<String, Map<String, String>>();

    switch (idSpec.getType()) {
      case VIEWER:
      case OWNER:
        Map<String, String> data = new HashMap<String, String>();
        data.put("count", "3");

        map.put("john.doe", data);
        break;
      case VIEWER_FRIENDS:
      case OWNER_FRIENDS:
        Map<String, String> janeData = new HashMap<String, String>();
        janeData.put("count", "7");
        Map<String, String> georgeData = new HashMap<String, String>();
        georgeData.put("count", "2");

        map.put("jane.doe", janeData);
        map.put("george.doe", georgeData);
        break;
      case USER_IDS:
        List<String> userIds = idSpec.fetchUserIds();
        for (String userId : userIds) {
          Map<String, String> userData = new HashMap<String, String>();
          userData.put("count", "1");
          map.put(userId, userData);
        }
    }
    return map;
  }
}

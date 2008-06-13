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
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.dataservice.AppDataService;
import org.apache.shindig.social.dataservice.DataCollection;
import org.apache.shindig.social.dataservice.GroupId;
import org.apache.shindig.social.dataservice.UserId;
import org.apache.shindig.social.opensocial.DataService;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class BasicDataService implements DataService, AppDataService {

  private XmlStateFileFetcher fetcher;

  @Inject
  public BasicDataService(XmlStateFileFetcher fetcher) {
    this.fetcher = fetcher;
    fetcher.loadDefaultStateFileIfNoneLoaded();
  }

  public ResponseItem<Map<String, Map<String, String>>> getPersonData(
      List<String> ids, List<String> keys, SecurityToken token) {

    Map<String, Map<String, String>> allData = fetcher.getAppData();

    Map<String, Map<String, String>> data
        = Maps.newHashMapWithExpectedSize(ids.size());

    for (String id : ids) {
      Map<String, String> allPersonData = allData.get(id);
      if (allPersonData != null) {
        if (keys == null || keys.isEmpty()) {
          data.put(id, allPersonData);
        } else {
          Map<String, String> personData = Maps.newHashMap();
          for (String key : keys) {
            String value = allPersonData.get(key);
            if (value != null) {
              personData.put(key, value);
            }
          }
          data.put(id, personData);
        }

      }
    }

    return new ResponseItem<Map<String, Map<String, String>>>(data);
  }

  public ResponseItem updatePersonData(String id, String key, String value,
      SecurityToken token) {
    if (!isValidKey(key)) {
      return new ResponseItem<Object>(ResponseError.BAD_REQUEST,
          "The person data key had invalid characters",
          new JSONObject());
    }

    fetcher.setAppData(id, key, value);
    return new ResponseItem<JSONObject>(new JSONObject());
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


  // New interface methods

  public ResponseItem<DataCollection> getPersonData(
      UserId userId, GroupId groupId, List<String> fields,
      String appId, SecurityToken token) {
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

    // TODO: Respect appId
    Map<String, Map<String, String>> data
        = getPersonData(ids, fields, token).getResponse();
    return new ResponseItem<DataCollection>(new DataCollection(data));
  }

  public ResponseItem updatePersonData(UserId userId,
      GroupId groupId, List<String> fields,
      Map<String, String> values, String appId, SecurityToken token) {
    for (String field : fields) {
      if (!isValidKey(field)) {
        return new ResponseItem<Object>(ResponseError.BAD_REQUEST,
            "The person data key had invalid characters",
            new JSONObject());
      }
    }

    switch(groupId.getType()) {
      case self:
        for (String field : fields) {
          String value = values.get(field);
          // TODO: Respect appId
          fetcher.setAppData(userId.getUserId(token), field, value);
        }
        break;
      default:
        return new ResponseItem<Object>(ResponseError.NOT_IMPLEMENTED,
            "We don't support updating data in batches yet", new Object());
    }

    return new ResponseItem<JSONObject>(new JSONObject());
  }

  public ResponseItem deletePersonData(UserId userId,
      GroupId groupId, List<String> fields, String appId,
      SecurityToken token) {
    switch(groupId.getType()) {
      case self:
        for (String field : fields) {
          // TODO: Respect appId
          fetcher.setAppData(userId.getUserId(token), field, null);
        }
        break;
      default:
        return new ResponseItem<Object>(ResponseError.NOT_IMPLEMENTED,
            "We don't support deleting data in batches yet", new Object());
    }

    return new ResponseItem<JSONObject>(new JSONObject());
  }
}

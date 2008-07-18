/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shindig.social.canonical;

import org.apache.shindig.common.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.social.ResponseError;
import org.apache.shindig.social.ResponseItem;
import org.apache.shindig.social.dataservice.ActivityService;
import org.apache.shindig.social.dataservice.AppDataService;
import org.apache.shindig.social.dataservice.DataCollection;
import org.apache.shindig.social.dataservice.GroupId;
import org.apache.shindig.social.dataservice.PersonService;
import org.apache.shindig.social.dataservice.RestfulCollection;
import org.apache.shindig.social.dataservice.UserId;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.util.BeanJsonConverter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Future;

/**
 * Implementation of supported services backed by a JSON DB
 */
@Singleton
public class JsonDbOpensocialService implements ActivityService, PersonService, AppDataService {

  private static final Comparator<Person> NAME_COMPARATOR = new Comparator<Person>() {
    public int compare(Person person, Person person1) {
      String name = person.getName().getUnstructured();
      String name1 = person1.getName().getUnstructured();
      return name.compareTo(name1);
    }
  };

  /**
   * The DB
   */
  private JSONObject db;

  /**
   * The JSON<->Bean converter
   */
  private BeanJsonConverter converter;

  /**
   * db["activities"] -> Array<Person>
   */
  private static final String PEOPLE_TABLE = "people";

  /**
   * db["people"] -> Map<Person.Id, Array<Activity>>
   */
  private static final String ACTIVITIES_TABLE = "activities";

  /**
   * db["data"] -> Map<Person.Id, Map<String, String>>
   */
  private static final String DATA_TABLE = "data";

  /**
   * db["friendLinks"] -> Map<Person.Id, Array<Person.Id>>
   */
  private static final String FRIEND_LINK_TABLE = "friendLinks";

  @Inject
  public JsonDbOpensocialService(@Named("canonical.json.db")String jsonLocation,
      BeanJsonConverter converter) throws Exception {
    String content = IOUtils.toString(ResourceLoader.openResource(jsonLocation), "UTF-8");
    this.db = new JSONObject(content);
    this.converter = converter;
  }

  public JSONObject getDb() {
    return db;
  }

  public void setDb(JSONObject db) {
    this.db = db;
  }

  public Future<ResponseItem<RestfulCollection<Activity>>> getActivities(UserId userId,
      GroupId groupId, String appId, Set<String> fields, SecurityToken token) {
    List<Activity> result = Lists.newArrayList();
    try {
      Set<String> idSet = getIdSet(userId, groupId, token);
      for (String id : idSet) {
        if (db.getJSONObject(ACTIVITIES_TABLE).has(id)) {
          JSONArray activities = db.getJSONObject(ACTIVITIES_TABLE).getJSONArray(id);
          for (int i = 0; i < activities.length(); i++) {
            JSONObject activity = activities.getJSONObject(i);
            if (appId != null && activity.get(Activity.Field.APP_ID.toString()).equals(appId)) {
              result.add(convertToActivity(activity, fields));
            } else if (appId == null) {
              result.add(convertToActivity(activity, fields));
            }
          }
        }
      }
      return ImmediateFuture.newInstance(new ResponseItem<RestfulCollection<Activity>>(
          new RestfulCollection<Activity>(result)));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<RestfulCollection<Activity>>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
  }

  public Future<ResponseItem<Activity>> getActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, String activityId, SecurityToken token) {
    try {
      String user = userId.getUserId(token);
      if (db.getJSONObject(ACTIVITIES_TABLE).has(user)) {
        JSONArray activities = db.getJSONObject(ACTIVITIES_TABLE).getJSONArray(user);
        for (int i = 0; i < activities.length(); i++) {
          JSONObject activity = activities.getJSONObject(i);
          if (activity.get(Activity.Field.USER_ID.toString()).equals(user) &&
              activity.get(Activity.Field.ID.toString()).equals(activityId)) {
            return ImmediateFuture.newInstance(new ResponseItem<Activity>(
                convertToActivity(activity, fields)));
          }
        }
      }
      return ImmediateFuture.newInstance(new ResponseItem<Activity>(ResponseError.BAD_REQUEST,
          "Activity not found", null));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<Activity>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
  }

  public Future<ResponseItem<Object>> deleteActivity(UserId userId, GroupId groupId, String appId,
      String activityId, SecurityToken token) {
    try {
      String user = userId.getUserId(token);
      if (db.getJSONObject(ACTIVITIES_TABLE).has(user)) {
        JSONArray activities = db.getJSONObject(ACTIVITIES_TABLE).getJSONArray(user);
        if (activities != null) {
          JSONArray newList = new JSONArray();
          for (int i = 0; i < activities.length(); i++) {
            JSONObject activity = activities.getJSONObject(i);
            if (!activity.get(Activity.Field.ID.toString()).equals(activityId)) {
              newList.put(activity);
            }
          }
          db.getJSONObject(ACTIVITIES_TABLE).put(user, newList);
          // TODO. This seems very odd that we return no useful response in this case
          // There is no way to represent not-found
          // if (found) { ??
          //}
        }
      }
      // What is the appropriate response here??
      return ImmediateFuture.newInstance(new ResponseItem<Object>(""));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<Object>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
  }

  public Future<ResponseItem<Object>> createActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Activity activity, SecurityToken token) {
    // Are fields really needed here?
    try {
      JSONObject jsonObject = convertFromActivity(activity, fields);
      if (!jsonObject.has(Activity.Field.ID.toString())) {
        jsonObject.put(Activity.Field.ID.toString(), System.currentTimeMillis());
      }
      JSONArray jsonArray = db.getJSONObject(ACTIVITIES_TABLE).getJSONArray(userId.getUserId(token));
      if (jsonArray == null) {
        jsonArray = new JSONArray();
        db.getJSONObject(ACTIVITIES_TABLE).put(userId.getUserId(token), jsonArray);
      }
      jsonArray.put(jsonObject);
      return ImmediateFuture.newInstance(new ResponseItem<Object>(""));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<Object>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
  }

  public Future<ResponseItem<RestfulCollection<Person>>> getPeople(UserId userId, GroupId groupId,
      SortOrder sortOrder, FilterType filter, int first, int max,
      Set<String> fields, SecurityToken token) {
    List<Person> result = Lists.newArrayList();
    try {
      JSONArray people = db.getJSONArray(PEOPLE_TABLE);

      Set<String> idSet = getIdSet(userId, groupId, token);

      for (int i = 0; i < people.length(); i++) {
        JSONObject person = people.getJSONObject(i);
        if (!idSet.contains(person.get(Person.Field.ID.toString()))) {
          continue;
        }
        // Add group support later
        result.add(convertToPerson(person, fields));
      }

      // We can pretend that by default the people are in top friends order
      if (sortOrder.equals(PersonService.SortOrder.name)) {
        Collections.sort(result, NAME_COMPARATOR);
      }

      // TODO: The samplecontainer doesn't really have the concept of HAS_APP so
      // we can't support any filters yet. We should fix this.

      int totalSize = result.size();
      int last = first + max;
      result = result.subList(first, Math.min(last, totalSize));

      RestfulCollection<Person> collection = new RestfulCollection<Person>(result,
          first, totalSize);

      return ImmediateFuture.newInstance(new ResponseItem<RestfulCollection<Person>>(collection));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<RestfulCollection<Person>>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
  }

  public Future<ResponseItem<Person>> getPerson(UserId id, Set<String> fields,
      SecurityToken token) {
    try {
      JSONArray people = db.getJSONArray(PEOPLE_TABLE);

      for (int i = 0; i < people.length(); i++) {
        JSONObject person = people.getJSONObject(i);
        if (id != null && person.get(Person.Field.ID.toString())
            .equals(id.getUserId(token))) {
          return ImmediateFuture.newInstance(new ResponseItem<Person>(
              convertToPerson(person, fields)));
        }
      }
      return ImmediateFuture.newInstance(new ResponseItem<Person>(ResponseError.BAD_REQUEST,
          "Person not found", null));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<Person>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
  }

  public Future<ResponseItem<DataCollection>> getPersonData(UserId userId, GroupId groupId,
      String appId, Set<String> fields, SecurityToken token) {
    // TODO. Does fields==null imply all?
    try {
      Map<String, Map<String, String>> idToData = Maps.newHashMap();
      Set<String> idSet = getIdSet(userId, groupId, token);
      for (String id : idSet) {
        JSONObject personData;
        if (!db.getJSONObject(DATA_TABLE).has(id)) {
          personData = new JSONObject();
        } else {
          if (fields != null && !fields.isEmpty()) {
            personData = new JSONObject(
                db.getJSONObject(DATA_TABLE).getJSONObject(id),
                fields.toArray(new String[fields.size()]));
          } else {
            personData = db.getJSONObject(DATA_TABLE).getJSONObject(id);
          }
        }

        // TODO: We can use the converter here to do this for us
        Iterator keys = personData.keys();
        Map<String, String> data = Maps.newHashMap();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          data.put(key, personData.getString(key));
        }
        idToData.put(id, data);
      }
      return ImmediateFuture.newInstance(new ResponseItem<DataCollection>(
          new DataCollection(idToData)));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<DataCollection>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
  }

  public Future<ResponseItem<Object>> deletePersonData(UserId userId, GroupId groupId, String appId,
      Set<String> fields, SecurityToken token) {
    try {
      String user = userId.getUserId(token);
      if (!db.getJSONObject(DATA_TABLE).has(user)) {
        return null;
      }
      JSONObject newPersonData = new JSONObject();
      JSONObject oldPersonData = db.getJSONObject(DATA_TABLE).getJSONObject(user);
      Iterator keys = oldPersonData.keys();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        if (fields != null && !fields.contains(key)) {
          newPersonData.put(key, oldPersonData.getString(key));
        }
      }
      db.getJSONObject(DATA_TABLE).put(user, newPersonData);
      return ImmediateFuture.newInstance(new ResponseItem<Object>(""));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<Object>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
  }

  public Future<ResponseItem<Object>> updatePersonData(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Map<String, String> values, SecurityToken token) {
    // TODO: this seems redundant. No need to pass both fields and a map of field->value
    // TODO: According to rest, yes there is. If a field is in the param list but not in the map
    // that means it is a delete

    for (String key : values.keySet()) {
      if (!isValidKey(key)) {
        return ImmediateFuture.newInstance(new ResponseItem<Object>(ResponseError.BAD_REQUEST,
            "The person data key had invalid characters", null));
      }
    }

    try {
      JSONObject personData = db.getJSONObject(DATA_TABLE).getJSONObject(userId.getUserId(token));
      if (personData == null) {
        personData = new JSONObject();
        db.getJSONObject(DATA_TABLE).put(userId.getUserId(token), personData);
      }

      for (Map.Entry<String, String> entry : values.entrySet()) {
        personData.put(entry.getKey(), entry.getValue());
      }
      return ImmediateFuture.newInstance(new ResponseItem<Object>(""));
    } catch (JSONException je) {
      return ImmediateFuture.newInstance(new ResponseItem<Object>(
          ResponseError.INTERNAL_ERROR, je.getMessage(), null));
    }
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

  /**
   * Get the set of user id's from a user and group
   */
  private Set<String> getIdSet(UserId user, GroupId group, SecurityToken token)
      throws JSONException {
    String userId = user.getUserId(token);

    if (group == null) {
      return Sets.newLinkedHashSet(userId);
    }

    Set<String> returnVal = Sets.newLinkedHashSet();
    switch (group.getType()) {
      case all:
      case friends:
      case groupId:
        if (db.getJSONObject(FRIEND_LINK_TABLE).has(userId)) {
          JSONArray friends = db.getJSONObject(FRIEND_LINK_TABLE).getJSONArray(userId);
          for (int i = 0; i < friends.length(); i++) {
            returnVal.add(friends.getString(i));
          }
        }
        break;
      case self:
        returnVal.add(userId);
        break;
    }
    return returnVal;
  }

  private Activity convertToActivity(JSONObject object, Set<String> fields) throws JSONException {
    if (fields != null && !fields.isEmpty()) {
      // Create a copy with just the specified fields
      object = new JSONObject(object, fields.toArray(new String[fields.size()]));
    }
    return converter.convertToObject(object.toString(), Activity.class);
  }

  private JSONObject convertFromActivity(Activity activity, Set<String> fields)
      throws JSONException {
    // TODO Not using fields yet
    return new JSONObject(converter.convertToString(activity));
  }

  private Person convertToPerson(JSONObject object, Set<String> fields) throws JSONException {
    if (fields != null && !fields.isEmpty()) {
      // Create a copy with just the specified fields
      object = new JSONObject(object, fields.toArray(new String[fields.size()]));
    }
    return converter.convertToObject(object.toString(), Person.class);
  }
}
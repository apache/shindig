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
package org.apache.shindig.extras.as.sample;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.extras.as.opensocial.model.ActivityEntry;
import org.apache.shindig.extras.as.opensocial.model.ActivityObject;
import org.apache.shindig.extras.as.opensocial.spi.ActivityStreamService;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Sample Implementation of the ActivityStreams Service using a JSON 
 * database.
 */
public class ActivityStreamsJsonDbService implements ActivityStreamService {
  JsonDbOpensocialService jsonDb;
  JSONObject db;
  BeanConverter converter;

  /**
   * Create a new ActivityStreamsJsonDbService instance via Guice injection
   *
   * @param jsonDb a {@link org.apache.shindig.social.sample.spi.JsonDbOpensocialService} object.
   * @param converter a {@link org.apache.shindig.protocol.conversion.BeanConverter} object
   *     annotated with "shindig.bean.converter.json".
   */
  @Inject
  public ActivityStreamsJsonDbService(JsonDbOpensocialService jsonDb,
                                      @Named("shindig.bean.converter.json")
                                      BeanConverter converter) {
    this.jsonDb = jsonDb;
    this.db = jsonDb.getDb();
    this.converter = converter;
  }


  /**
   * db["people"] -> Map<Person.Id, Array<ActivityEntry>>
   */
  private static final String ACTIVITYSTREAMS_TABLE = "activityEntries";


  // Are fields really needed here?
  /** {@inheritDoc} */
  public Future<Void> createActivityEntry(UserId userId, GroupId groupId, String appId,
        Set<String> fields, ActivityEntry activityEntry, SecurityToken token) throws ProtocolException {
    try {
      JSONObject jsonEntry = convertFromActivityEntry(activityEntry, fields);
      JSONObject jsonEntryObject = jsonEntry.getJSONObject(ActivityEntry.Field.OBJECT.toString());
      if (!jsonEntryObject.has(ActivityObject.Field.ID.toString())) {
        jsonEntryObject.put(ActivityObject.Field.ID.toString(), System.currentTimeMillis());
        jsonEntry.put(ActivityEntry.Field.OBJECT.toString(), jsonEntryObject);
      }

      // TODO: bug fixed: jsonArray will not be null; will throw exception!
      // Fix in createActivity()
      JSONArray jsonArray;
      if (db.getJSONObject(ACTIVITYSTREAMS_TABLE).has(userId.getUserId(token))) {
        jsonArray = db.getJSONObject(ACTIVITYSTREAMS_TABLE).getJSONArray(userId.getUserId(token));
      } else {
        jsonArray = new JSONArray();
        db.getJSONObject(ACTIVITYSTREAMS_TABLE).put(userId.getUserId(token), jsonArray);
      }
      jsonArray.put(jsonEntry);
      return ImmediateFuture.newInstance(null);
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }
  }

  /** {@inheritDoc} */
  public Future<Void> deleteActivityEntries(UserId userId, GroupId groupId,
      String appId, Set<String> activityIds, SecurityToken token) throws ProtocolException {
    try {
      String user = userId.getUserId(token);

      if (db.getJSONObject(ACTIVITYSTREAMS_TABLE).has(user)) {
        JSONArray activityEntries = db.getJSONObject(ACTIVITYSTREAMS_TABLE).getJSONArray(user);

        if (activityEntries != null) {
          JSONArray newList = new JSONArray();
          for (int i = 0; i < activityEntries.length(); i++) {
            JSONObject activityEntry = activityEntries.getJSONObject(i);
            if (!activityIds.contains(activityEntry.getJSONObject(ActivityEntry.Field.OBJECT.toString()).getString(ActivityObject.Field.ID.toString()))) {
              newList.put(activityEntry);
            }
          }
          db.getJSONObject(ACTIVITYSTREAMS_TABLE).put(user, newList);
          // TODO: This seems very odd that we return no useful response in this
          // case
          // There is no way to represent not-found
          // if (found) { ??
          // }
        }
      }
      // What is the appropriate response here??
      return ImmediateFuture.newInstance(null);
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }
  }

  /** {@inheritDoc} */
  public Future<ActivityEntry> getActivityEntry(UserId userId, GroupId groupId,
      String appId, Set<String> fields, String activityId, SecurityToken token)
      throws ProtocolException {
    try {       
      String user = userId.getUserId(token);
      if (db.getJSONObject(ACTIVITYSTREAMS_TABLE).has(user)) {
        JSONArray activityEntries = db.getJSONObject(ACTIVITYSTREAMS_TABLE).getJSONArray(user);
        for (int i = 0; i < activityEntries.length(); i++) {
          JSONObject activityEntry = activityEntries.getJSONObject(i);
          JSONObject actor = activityEntry.getJSONObject(ActivityEntry.Field.ACTOR.toString());
          String actorId = actor.get(ActivityObject.Field.ID.toString()).toString();
          if (actorId.equals(user)
              && activityEntry.getJSONObject(ActivityEntry.Field.OBJECT.toString()).get(ActivityObject.Field.ID.toString()).toString().equals(activityId)) {
            return ImmediateFuture.newInstance(jsonDb.filterFields(activityEntry, fields, ActivityEntry.class));
          }
        }
      }

      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "ActivityEntry not found");
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }
  }
  
  /** {@inheritDoc} */
  public Future<RestfulCollection<ActivityEntry>> getActivityEntries(
      Set<UserId> userIds, GroupId groupId, String appId, Set<String> fields,
      CollectionOptions options, SecurityToken token)
      throws ProtocolException {
      List<ActivityEntry> result = Lists.newArrayList();
      
    try {
      Set<String> idSet = jsonDb.getIdSet(userIds, groupId, token);
      for (String id : idSet) {
        if (db.getJSONObject(ACTIVITYSTREAMS_TABLE).has(id)) {
          JSONArray activityEntries = db.getJSONObject(ACTIVITYSTREAMS_TABLE).getJSONArray(id);
          for (int i = 0; i < activityEntries.length(); i++) {
            JSONObject activityEntry = activityEntries.getJSONObject(i);
            result.add(jsonDb.filterFields(activityEntry, fields, ActivityEntry.class));
            // TODO: ActivityStreams don't have appIds
          }
        }
      }
      return ImmediateFuture.newInstance(new RestfulCollection<ActivityEntry>(result));
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }
  }
  
  /** {@inheritDoc} */
  public Future<RestfulCollection<ActivityEntry>> getActivityEntries(
      UserId userId, GroupId groupId, String appId, Set<String> fields,
      CollectionOptions options, Set<String> activityIds, SecurityToken token)
      throws ProtocolException {
    List<ActivityEntry> result = Lists.newArrayList();
    try {
      String user = userId.getUserId(token);
      if (db.getJSONObject(ACTIVITYSTREAMS_TABLE).has(user)) {
        JSONArray activityEntries = db.getJSONObject(ACTIVITYSTREAMS_TABLE).getJSONArray(user);
        for (int i = 0; i < activityEntries.length(); i++) {
          JSONObject activityEntry = activityEntries.getJSONObject(i);
          JSONObject actor = new JSONObject(activityEntry.get(ActivityEntry.Field.ACTOR.toString()));
          String actorId = actor.get(ActivityObject.Field.ID.toString()).toString();
          if (actorId.equals(user)
            && activityIds.contains(activityEntry.getJSONObject(ActivityEntry.Field.OBJECT.toString()).getString(ActivityObject.Field.ID.toString()).toString())) {
            result.add(jsonDb.filterFields(activityEntry, fields, ActivityEntry.class));
          }
        }
      }
      return ImmediateFuture.newInstance(new RestfulCollection<ActivityEntry>(result));
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }
  }

  private JSONObject convertFromActivityEntry(ActivityEntry activityEntry, Set<String> fields)
    throws JSONException {
    // TODO Not using fields yet
    return new JSONObject(converter.convertToString(activityEntry));
  }
}

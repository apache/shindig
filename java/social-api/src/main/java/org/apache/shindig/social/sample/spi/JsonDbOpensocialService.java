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
package org.apache.shindig.social.sample.spi;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.protocol.model.SortOrder;
import org.apache.shindig.social.core.model.NameImpl;
import org.apache.shindig.social.core.model.PersonImpl;
import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.ActivityEntry;
import org.apache.shindig.social.opensocial.model.Album;
import org.apache.shindig.social.opensocial.model.Group;
import org.apache.shindig.social.opensocial.model.MediaItem;
import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.MessageCollection;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.social.opensocial.spi.ActivityService;
import org.apache.shindig.social.opensocial.spi.ActivityStreamService;
import org.apache.shindig.social.opensocial.spi.AlbumService;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.GroupService;
import org.apache.shindig.social.opensocial.spi.MediaItemService;
import org.apache.shindig.social.opensocial.spi.MessageService;
import org.apache.shindig.social.opensocial.spi.PersonService;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Implementation of supported services backed by a JSON DB.
 */
@Singleton
public class JsonDbOpensocialService implements ActivityService, PersonService, AppDataService,
    MessageService, AlbumService, MediaItemService, ActivityStreamService, GroupService {

  private static final Comparator<Person> NAME_COMPARATOR = new Comparator<Person>() {
    public int compare(Person person, Person person1) {
      String name = person.getName().getFormatted();
      String name1 = person1.getName().getFormatted();
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
  private BeanConverter converter;

  /**
   * db["people"] -> Array<Person>
   */
  private static final String PEOPLE_TABLE = "people";

  /**
   * db["groups"] -> Array<Group>
   */
  private static final String GROUPS_TABLE = "groups";

  /**
   * db["groupMembers"] -> Array<Person>
   */
  private static final String GROUP_MEMBERS_TABLE = "groupMembers";

  /**
   * db["activities"] -> Map<Person.Id, Array<Activity>>
   */
  private static final String ACTIVITIES_TABLE = "activities";

  /**
   * db["albums"] -> Map<Person.Id, Array<Album>>
   */
  private static final String ALBUMS_TABLE = "albums";

  /**
   * db["mediaItems"] -> Map<Person.Id, Array<MediaItem>>
   */
  private static final String MEDIAITEMS_TABLE = "mediaItems";

  /**
   * db["data"] -> Map<Person.Id, Map<String, String>>
   */
  private static final String DATA_TABLE = "data";

  /**
   * db["friendLinks"] -> Map<Person.Id, Array<Person.Id>>
   */
  private static final String FRIEND_LINK_TABLE = "friendLinks";

  /**
   * db["messages"] -> Map<Person.Id, Array<Message>>
   */
  private static final String MESSAGE_TABLE = "messages";

  /**
   * db["passwords"] -> Map<Person.Id, String>
   */
  private static final String PASSWORDS_TABLE = "passwords";

  /**
   * db["activityEntries"] -> Map<Person.Id, Array<ActivityEntry>>
   */
  private static final String ACTIVITYSTREAMS_TABLE = "activityEntries";

  /**
   * Anonymous name.
   */
  private static final String ANONYMOUS_NAME = "Anonymous";

  private Authority authority;

  /**
   * Initializes the JsonDbOpensocialService using Guice
   *
   * @param jsonLocation location of the json data provided by the shindig.canonical.json.db parameter
   * @param converter an injected BeanConverter
   * @throws java.lang.Exception if any
   */
  @Inject
  public JsonDbOpensocialService(@Named("shindig.canonical.json.db")
  String jsonLocation, @Named("shindig.bean.converter.json")
  BeanConverter converter,
  @Named("shindig.contextroot") String contextroot) throws Exception {
    String content = IOUtils.toString(ResourceLoader.openResource(jsonLocation), "UTF-8");
    this.db = new JSONObject(content.replace("%contextroot%", contextroot));
    this.converter = converter;
  }

  /**
   * Allows access to the underlying json db.
   *
   * @return a reference to the json db
   */
  public JSONObject getDb() {
    return db;
  }

   /**
   * override the json database
   * @param db a {@link org.json.JSONObject}.
   */
  public void setDb(JSONObject db) {
    this.db = db;
  }

  @Inject(optional = true)
  public void setAuthority(Authority authority) {
    this.authority = authority;
  }

  /** {@inheritDoc} */
  public Future<RestfulCollection<Activity>> getActivities(Set<UserId> userIds, GroupId groupId,
      String appId, Set<String> fields, CollectionOptions options, SecurityToken token)
      throws ProtocolException {
    List<Activity> result = Lists.newArrayList();
    try {
      Set<String> idSet = getIdSet(userIds, groupId, token);
      for (String id : idSet) {
        if (db.getJSONObject(ACTIVITIES_TABLE).has(id)) {
          JSONArray activities = db.getJSONObject(ACTIVITIES_TABLE).getJSONArray(id);
          for (int i = 0; i < activities.length(); i++) {
            JSONObject activity = activities.getJSONObject(i);
            if (appId == null || !activity.has(Activity.Field.APP_ID.toString())) {
              result.add(filterFields(activity, fields, Activity.class));
            } else if (activity.get(Activity.Field.APP_ID.toString()).equals(appId)) {
              result.add(filterFields(activity, fields, Activity.class));
            }
          }
        }
      }
      return Futures.immediateFuture(new RestfulCollection<Activity>(result));
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<RestfulCollection<Activity>> getActivities(UserId userId, GroupId groupId,
      String appId, Set<String> fields, CollectionOptions options, Set<String> activityIds,
      SecurityToken token) throws ProtocolException {
    List<Activity> result = Lists.newArrayList();
    try {
      String user = userId.getUserId(token);
      if (db.getJSONObject(ACTIVITIES_TABLE).has(user)) {
        JSONArray activities = db.getJSONObject(ACTIVITIES_TABLE).getJSONArray(user);
        for (int i = 0; i < activities.length(); i++) {
          JSONObject activity = activities.getJSONObject(i);
          if (activity.get(Activity.Field.USER_ID.toString()).equals(user)
              && activityIds.contains(activity.getString(Activity.Field.ID.toString()))) {
            result.add(filterFields(activity, fields, Activity.class));
          }
        }
      }
      return Futures.immediateFuture(new RestfulCollection<Activity>(result));
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<Activity> getActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, String activityId, SecurityToken token) throws ProtocolException {
    try {
      String user = userId.getUserId(token);
      if (db.getJSONObject(ACTIVITIES_TABLE).has(user)) {
        JSONArray activities = db.getJSONObject(ACTIVITIES_TABLE).getJSONArray(user);
        for (int i = 0; i < activities.length(); i++) {
          JSONObject activity = activities.getJSONObject(i);
          if (activity.get(Activity.Field.USER_ID.toString()).equals(user)
              && activity.get(Activity.Field.ID.toString()).equals(activityId)) {
            return Futures.immediateFuture(filterFields(activity, fields, Activity.class));
          }
        }
      }

      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Activity not found");
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<Void> deleteActivities(UserId userId, GroupId groupId, String appId,
      Set<String> activityIds, SecurityToken token) throws ProtocolException {
    try {
      String user = userId.getUserId(token);
      if (db.getJSONObject(ACTIVITIES_TABLE).has(user)) {
        JSONArray activities = db.getJSONObject(ACTIVITIES_TABLE).getJSONArray(user);
        if (activities != null) {
          JSONArray newList = new JSONArray();
          for (int i = 0; i < activities.length(); i++) {
            JSONObject activity = activities.getJSONObject(i);
            if (!activityIds.contains(activity.getString(Activity.Field.ID.toString()))) {
              newList.put(activity);
            }
          }
          db.getJSONObject(ACTIVITIES_TABLE).put(user, newList);
          // TODO. This seems very odd that we return no useful response in this
          // case
          // There is no way to represent not-found
          // if (found) { ??
          // }
        }
      }
      // What is the appropriate response here??
      return Futures.immediateFuture(null);
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<Void> createActivity(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Activity activity, SecurityToken token) throws ProtocolException {
    // Are fields really needed here?
    try {
      JSONObject jsonObject = convertFromActivity(activity, fields);
      if (!jsonObject.has(Activity.Field.ID.toString())) {
        jsonObject.put(Activity.Field.ID.toString(), System.currentTimeMillis());
      }
      JSONArray jsonArray = db.getJSONObject(ACTIVITIES_TABLE)
          .getJSONArray(userId.getUserId(token));
      if (jsonArray == null) {
        jsonArray = new JSONArray();
        db.getJSONObject(ACTIVITIES_TABLE).put(userId.getUserId(token), jsonArray);
      }
      // TODO (woodser): if used with PUT, duplicate activity would be created?
      jsonArray.put(jsonObject);
      return Futures.immediateFuture(null);
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<RestfulCollection<Person>> getPeople(Set<UserId> userIds, GroupId groupId,
      CollectionOptions options, Set<String> fields, SecurityToken token) throws ProtocolException {
    List<Person> result = Lists.newArrayList();
    try {
      JSONArray people = db.getJSONArray(PEOPLE_TABLE);

      Set<String> idSet = getIdSet(userIds, groupId, token);

      for (int i = 0; i < people.length(); i++) {
        JSONObject person = people.getJSONObject(i);
        if (!idSet.contains(person.get(Person.Field.ID.toString()))) {
          continue;
        }

        // Add group support later
        Person personObj = filterFields(person, fields, Person.class);
        Map<String, Object> appData = getPersonAppData(
            person.getString(Person.Field.ID.toString()), fields);
        personObj.setAppData(appData);

        result.add(personObj);
      }

      if (GroupId.Type.self == groupId.getType() && result.isEmpty()) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "People '" + idSet + "' not found");
      }

      // We can pretend that by default the people are in top friends order
      if (options.getSortBy().equals(Person.Field.NAME.toString())) {
        Collections.sort(result, NAME_COMPARATOR);

        if (options.getSortOrder() == SortOrder.descending) {
          Collections.reverse(result);
        }
      }

      // TODO: The samplecontainer doesn't really have the concept of HAS_APP so
      // we can't support any filters yet. We should fix this.

      int totalSize = result.size();
      int last = options.getFirst() + options.getMax();
      result = result.subList(options.getFirst(), Math.min(last, totalSize));

      return Futures.immediateFuture(new RestfulCollection<Person>(result, options.getFirst(), totalSize, options.getMax()));
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<Person> getPerson(UserId id, Set<String> fields, SecurityToken token)
      throws ProtocolException {
    if (id != null && AnonymousSecurityToken.ANONYMOUS_ID.equals(id.getUserId())) {
      Person anonymous = new PersonImpl();
      anonymous.setId(AnonymousSecurityToken.ANONYMOUS_ID);
      anonymous.setName(new NameImpl(ANONYMOUS_NAME));
      anonymous.setNickname(ANONYMOUS_NAME);
      return Futures.immediateFuture(anonymous);
    }
    try {
      JSONArray people = db.getJSONArray(PEOPLE_TABLE);

      for (int i = 0; i < people.length(); i++) {
        JSONObject person = people.getJSONObject(i);
        if (id != null && person.get(Person.Field.ID.toString()).equals(id.getUserId(token))) {
          Person personObj = filterFields(person, fields, Person.class);
          Map<String, Object> appData = getPersonAppData(person.getString(Person.Field.ID
              .toString()), fields);
          personObj.setAppData(appData);

          return Futures.immediateFuture(personObj);
        }
      }
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Person '" + id.getUserId(token) + "' not found");
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<Person> updatePerson(UserId id, Person person, SecurityToken token)
      throws ProtocolException {
    try {
      String viewer = token.getViewerId(); // viewer
      String user = id.getUserId(token); // person to update

      if (!viewerCanUpdatePerson(viewer,user)) {
        throw new ProtocolException(HttpServletResponse.SC_FORBIDDEN, "User '" + viewer + "' does not have enough privileges to update person '"+user+"'");
      }

      JSONArray people = db.getJSONArray(PEOPLE_TABLE);

      for (int i = 0; i < people.length(); i++) {
        JSONObject curPerson = people.getJSONObject(i);

        if (user != null && curPerson.getString(Person.Field.ID.toString()).equals(user)) {
          // Convert user to JSON and set ID
          JSONObject jsonPerson = convertToJson(person);
          // go through all properties to update in the submitted person object
          // and change them in the current person object
          for (String key : JSONObject.getNames(jsonPerson)) {
            curPerson.put(key,jsonPerson.get(key));
          }

          people.put(i,curPerson);
          return Futures.immediateFuture(converter.convertToObject(curPerson.toString(), Person.class));
        }
      }

      // Error - no album found to update with given ID
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "User ID " + user + " does not exist");
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }

  }

  /** Check if a viewer is allowed to update the given person record. **/
  protected boolean viewerCanUpdatePerson(String viewer, String person) {
    // A person can only update his own personal data (by default)
    // if you wish to allow other people to update the personal data of the user
    // you should change the current function
    return viewer.equals(person) ? true : false;
  }

  private Map<String, Object> getPersonAppData(String id, Set<String> fields) {
    try {
      Map<String, Object> appData = null;
      JSONObject personData = db.getJSONObject(DATA_TABLE).optJSONObject(id);
      if (personData != null) {
        if (fields.contains(Person.Field.APP_DATA.toString())) {
          appData = Maps.newHashMap();
          @SuppressWarnings("unchecked")
          Iterator<String> keys = personData.keys();
          while (keys.hasNext()) {
            String key = keys.next();
            appData.put(key, personData.get(key));
          }
        } else {
          String appDataPrefix = Person.Field.APP_DATA.toString() + '.';
          for (String field : fields) {
            if (field.startsWith(appDataPrefix)) {
              if (appData == null) {
                appData = Maps.newHashMap();
              }

              String appDataField = field.substring(appDataPrefix.length());
              if (personData.has(appDataField)) {
                appData.put(appDataField, personData.get(appDataField));
              }
            }
          }
        }
      }

      return appData;
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<DataCollection> getPersonData(Set<UserId> userIds, GroupId groupId, String appId,
      Set<String> fields, SecurityToken token) throws ProtocolException {
    try {
      Map<String, Map<String, Object>> idToData = Maps.newHashMap();
      Set<String> idSet = getIdSet(userIds, groupId, token);
      for (String id : idSet) {
        JSONObject personData;
        if (!db.getJSONObject(DATA_TABLE).has(id)) {
          personData = new JSONObject();
        } else {
          if (!fields.isEmpty()) {
            personData = new JSONObject(db.getJSONObject(DATA_TABLE).getJSONObject(id), fields
                .toArray(new String[fields.size()]));
          } else {
            personData = db.getJSONObject(DATA_TABLE).getJSONObject(id);
          }
        }

        // TODO: We can use the converter here to do this for us

        // JSONObject keys are always strings
        @SuppressWarnings("unchecked")
        Iterator<String> keys = personData.keys();
        Map<String, Object> data = Maps.newHashMap();
        while (keys.hasNext()) {
          String key = keys.next();
          data.put(key, personData.getString(key));
        }
        idToData.put(id, data);
      }
      return Futures.immediateFuture(new DataCollection(idToData));
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<Void> deletePersonData(UserId userId, GroupId groupId, String appId,
      Set<String> fields, SecurityToken token) throws ProtocolException {
    try {
      String user = userId.getUserId(token);
      if (!db.getJSONObject(DATA_TABLE).has(user)) {
        return null;
      }
      JSONObject newPersonData = new JSONObject();
      JSONObject oldPersonData = db.getJSONObject(DATA_TABLE).getJSONObject(user);

      // JSONObject keys are always strings
      @SuppressWarnings("unchecked")
      Iterator<String> keys = oldPersonData.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        if (!fields.contains(key)) {
          newPersonData.put(key, oldPersonData.getString(key));
        }
      }
      db.getJSONObject(DATA_TABLE).put(user, newPersonData);
      return Futures.immediateFuture(null);
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<Void> updatePersonData(UserId userId, GroupId groupId, String appId,
      Set<String> fields, Map<String, Object> values, SecurityToken token)
      throws ProtocolException {
    // TODO: this seems redundant. No need to pass both fields and a map of
    // field->value
    // TODO: According to rest, yes there is. If a field is in the param list
    // but not in the map
    // that means it is a delete

    try {
      JSONObject personData = db.getJSONObject(DATA_TABLE).getJSONObject(userId.getUserId(token));
      if (personData == null) {
        personData = new JSONObject();
        db.getJSONObject(DATA_TABLE).put(userId.getUserId(token), personData);
      }

      for (Map.Entry<String, Object> entry : values.entrySet()) {
        personData.put(entry.getKey(), entry.getValue());
      }
      return Futures.immediateFuture(null);
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<RestfulCollection<Group>> getGroups(UserId userId,
		CollectionOptions options, Set<String> fields, SecurityToken token)
		throws ProtocolException {
    List<Group> result = Lists.newArrayList();
    String user = userId.getUserId(token);
    try {
      JSONArray groups = db.getJSONObject(GROUPS_TABLE).getJSONArray(user);

      for (int i = 0; i < groups.length(); i++) {
        JSONObject group = groups.getJSONObject(i);

        Group groupObj = filterFields(group, fields, Group.class);
        result.add(groupObj);
      }
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }

    return Futures.immediateFuture(new RestfulCollection<Group>(result));
  }

  /**
   * {@inheritDoc}
   *
   * Post a message for a set of users.
   */
  public Future<Void> createMessage(UserId userId, String appId, String msgCollId, Message message,
      SecurityToken token) throws ProtocolException {
    for (String recipient : message.getRecipients()) {
      try {
        JSONArray outbox = db.getJSONObject(MESSAGE_TABLE).getJSONArray(recipient);
        if (outbox == null) {
          outbox = new JSONArray();
          db.getJSONObject(MESSAGE_TABLE).put(recipient, outbox);
        }

        outbox.put(message);
      } catch (JSONException je) {
        throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
            je);
      }
    }

    return Futures.immediateFuture(null);
  }

  /** {@inheritDoc} */
  public Future<RestfulCollection<MessageCollection>> getMessageCollections(UserId userId,
      Set<String> fields, CollectionOptions options, SecurityToken token) throws ProtocolException {
    try {
      List<MessageCollection> result = Lists.newArrayList();
      JSONObject messageCollections = db.getJSONObject(MESSAGE_TABLE).getJSONObject(
          userId.getUserId(token));
      for (String msgCollId : JSONObject.getNames(messageCollections)) {
        JSONObject msgColl = messageCollections.getJSONObject(msgCollId);
        msgColl.put("id", msgCollId);
        JSONArray messages = msgColl.getJSONArray("messages");
        int numMessages = (messages == null) ? 0 : messages.length();
        msgColl.put("total", String.valueOf(numMessages));
        msgColl.put("unread", String.valueOf(numMessages));

        result.add(filterFields(msgColl, fields, MessageCollection.class));
      }
      return Futures.immediateFuture(new RestfulCollection<MessageCollection>(result));
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<Void> deleteMessages(UserId userId, String msgCollId, List<String> ids,
      SecurityToken token) throws ProtocolException {
    throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED,
        "this functionality is not yet available");
  }

  /**
   * {@inheritDoc}
   *
   * Gets the messsages in an user's queue.
   */
  public Future<RestfulCollection<Message>> getMessages(UserId userId, String msgCollId,
      Set<String> fields, List<String> msgIds, CollectionOptions options, SecurityToken token)
      throws ProtocolException {
    try {
      List<Message> result = Lists.newArrayList();
      JSONArray messages = db.getJSONObject(MESSAGE_TABLE).getJSONObject(userId.getUserId(token))
          .getJSONObject(msgCollId).getJSONArray("messages");

      // TODO: special case @all

      if (messages == null) {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "message collection"
            + msgCollId + " not found");
      }

      // TODO: filter and sort outbox.
      for (int i = 0; i < messages.length(); i++) {
        JSONObject msg = messages.getJSONObject(i);
        result.add(filterFields(msg, fields, Message.class));
      }

      return Futures.immediateFuture(new RestfulCollection<Message>(result));

    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(),
          je);
    }
  }

  /** {@inheritDoc} */
  public Future<MessageCollection> createMessageCollection(UserId userId,
      MessageCollection msgCollection, SecurityToken token) throws ProtocolException {
    throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED,
        "this functionality is not yet available");
  }

  /** {@inheritDoc} */
  public Future<Void> modifyMessage(UserId userId, String msgCollId, String messageId,
      Message message, SecurityToken token) throws ProtocolException {
    throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED,
        "this functionality is not yet available");
  }

  /** {@inheritDoc} */
  public Future<Void> modifyMessageCollection(UserId userId, MessageCollection msgCollection,
      SecurityToken token) throws ProtocolException {
    throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED,
        "this functionality is not yet available");
  }

  /** {@inheritDoc} */
  public Future<Void> deleteMessageCollection(UserId userId, String msgCollId, SecurityToken token)
      throws ProtocolException {
    throw new ProtocolException(HttpServletResponse.SC_NOT_IMPLEMENTED,
        "this functionality is not yet available");
  }

  /**
   * Public methods for use with Authentication Classes
   *
   * @param username a {@link java.lang.String} object.
   * @return a {@link java.lang.String} object.
   */
  public String getPassword(String username) {
    try {
      return db.getJSONObject(PASSWORDS_TABLE).getString(username);
    } catch (JSONException e) {
      return null;
    }
  }

  private Set<String> getIdSet(UserId user, GroupId group, SecurityToken token)
      throws JSONException {
    String userId = user.getUserId(token);

    if (group == null) {
      return ImmutableSortedSet.of(userId);
    }

    Set<String> returnVal = Sets.newLinkedHashSet();
    switch (group.getType()) {
    case all:
    case friends:
      if (db.getJSONObject(FRIEND_LINK_TABLE).has(userId)) {
        JSONArray friends = db.getJSONObject(FRIEND_LINK_TABLE).getJSONArray(userId);
        for (int i = 0; i < friends.length(); i++) {
          returnVal.add(friends.getString(i));
        }
      }
      break;
    case objectId:
      if (db.getJSONObject(GROUP_MEMBERS_TABLE).has(group.toString())) {
        JSONArray groupMembers = db.getJSONObject(GROUP_MEMBERS_TABLE).getJSONArray(group.toString());
        for (int i = 0; i < groupMembers.length(); i++) {
          returnVal.add(groupMembers.getString(i));
        }
      }
      break;
    case self:
      returnVal.add(userId);
      break;
    }
    return returnVal;
  }

  /**
   * Get the set of user id's for a set of users and a group
   *
   * @param users set of UserIds
   * @param group the group
   * @param token a token
   * @return set of Id strings
   * @throws org.json.JSONException if errors in Json
   */
  public Set<String> getIdSet(Set<UserId> users, GroupId group, SecurityToken token)
      throws JSONException {
    Set<String> ids = Sets.newLinkedHashSet();
    for (UserId user : users) {
      ids.addAll(getIdSet(user, group, token));
    }
    return ids;
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<Album> getAlbum(UserId userId, String appId, Set<String> fields,
                                String albumId, SecurityToken token) throws ProtocolException {
    try {
      // First ensure user has a table
      String user = userId.getUserId(token);
      if (db.getJSONObject(ALBUMS_TABLE).has(user)) {
        // Retrieve user's albums
        JSONArray userAlbums = db.getJSONObject(ALBUMS_TABLE).getJSONArray(user);

        // Search albums for given ID and owner
        JSONObject album;
        for (int i = 0; i < userAlbums.length(); i++) {
          album = userAlbums.getJSONObject(i);
          if (album.getString(Album.Field.ID.toString()).equals(albumId) &&
              album.getString(Album.Field.OWNER_ID.toString()).equals(user)) {
            return Futures.immediateFuture(filterFields(album, fields, Album.class));
          }
        }
      }

      // Album wasn't found
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Album ID " + albumId + " does not exist");
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<RestfulCollection<Album>> getAlbums(UserId userId, String appId,
                                                    Set<String> fields, CollectionOptions options, Set<String> albumIds,
                                                    SecurityToken token) throws ProtocolException {
    try {
      // Ensure user has a table
      String user = userId.getUserId(token);
      if (db.getJSONObject(ALBUMS_TABLE).has(user)) {
        // Get user's albums
        JSONArray userAlbums = db.getJSONObject(ALBUMS_TABLE).getJSONArray(user);

        // Stores target albums
        List<Album> result = Lists.newArrayList();

        // Search for every albumId
        boolean found;
        JSONObject curAlbum;
        for (String albumId : albumIds) {
          // Search albums for this albumId
          found = false;
          for (int i = 0; i < userAlbums.length(); i++) {
            curAlbum = userAlbums.getJSONObject(i);
            if (curAlbum.getString(Album.Field.ID.toString()).equals(albumId) &&
                curAlbum.getString(Album.Field.OWNER_ID.toString()).equals(user)) {
              result.add(filterFields(curAlbum, fields, Album.class));
              found = true;
              break;
            }
          }

          // Error - albumId not found
          if (!found) {
            throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Album ID " + albumId + " does not exist");
          }
        }

        // Return found albums
        return Futures.immediateFuture(new RestfulCollection<Album>(result));
      }

      // Album table doesn't exist for user
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "User '" + user + "' has no albums");
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<RestfulCollection<Album>> getAlbums(Set<UserId> userIds,
                                                    GroupId groupId, String appId, Set<String> fields,
                                                    CollectionOptions options, SecurityToken token)
      throws ProtocolException {
    try {
      List<Album> result = Lists.newArrayList();
      Set<String> idSet = getIdSet(userIds, groupId, token);

      // Gather albums for all user IDs
      for (String id : idSet) {
        if (db.getJSONObject(ALBUMS_TABLE).has(id)) {
          JSONArray userAlbums = db.getJSONObject(ALBUMS_TABLE).getJSONArray(id);
          for (int i = 0; i < userAlbums.length(); i++) {
            JSONObject album = userAlbums.getJSONObject(i);
            if (album.getString(Album.Field.OWNER_ID.toString()).equals(id)) {
              result.add(filterFields(album, fields, Album.class));
            }
          }
        }
      }
      return Futures.immediateFuture(new RestfulCollection<Album>(result));
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<Void> deleteAlbum(UserId userId, String appId, String albumId,
                                  SecurityToken token) throws ProtocolException {
    try {
      boolean targetFound = false;      // indicates if target album is found
      JSONArray newAlbums = new JSONArray();  // list of albums minus target
      String user = userId.getUserId(token);  // retrieve user id

      // First ensure user has a table
      if (db.getJSONObject(ALBUMS_TABLE).has(user)) {
        // Get user's albums
        JSONArray userAlbums = db.getJSONObject(ALBUMS_TABLE).getJSONArray(user);

        // Compose new list of albums excluding album to be deleted
        JSONObject curAlbum;
        for (int i = 0; i < userAlbums.length(); i++) {
          curAlbum = userAlbums.getJSONObject(i);
          if (curAlbum.getString(Album.Field.ID.toString()).equals(albumId)) {
            targetFound = true;
          } else {
            newAlbums.put(curAlbum);
          }
        }
      }

      // Overwrite user's albums with updated list if album found
      if (targetFound) {
        db.getJSONObject(ALBUMS_TABLE).put(user, newAlbums);
        return Futures.immediateFuture(null);
      } else {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Album ID " + albumId + " does not exist");
      }
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: userId and album's ownerId don't have to match - potential problem
  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<Void> createAlbum(UserId userId, String appId, Album album,
                                  SecurityToken token) throws ProtocolException {
    try {
      // Get table of user's albums
      String user = userId.getUserId(token);
      JSONArray userAlbums = db.getJSONObject(ALBUMS_TABLE).getJSONArray(user);
      if (userAlbums == null) {
        userAlbums = new JSONArray();
        db.getJSONObject(ALBUMS_TABLE).put(user, userAlbums);
      }

      // Convert album to JSON and set ID & owner
      JSONObject jsonAlbum = convertToJson(album);
      if (!jsonAlbum.has(Album.Field.ID.toString())) {
        jsonAlbum.put(Album.Field.ID.toString(), System.currentTimeMillis());
      }
      if (!jsonAlbum.has(Album.Field.OWNER_ID.toString())) {
        jsonAlbum.put(Album.Field.OWNER_ID.toString(), user);
      }

      // Insert new album into table
      userAlbums.put(jsonAlbum);
      return Futures.immediateFuture(null);
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<Void> updateAlbum(UserId userId, String appId, Album album,
                                  String albumId, SecurityToken token) throws ProtocolException {
    try {
      // First ensure user has a table
      String user = userId.getUserId(token);
      if (db.getJSONObject(ALBUMS_TABLE).has(user)) {
        // Retrieve user's albums
        JSONArray userAlbums = db.getJSONObject(ALBUMS_TABLE).getJSONArray(user);

        // Convert album to JSON and set ID
        JSONObject jsonAlbum = convertToJson(album);
        jsonAlbum.put(Album.Field.ID.toString(), albumId);

        // Iterate through albums to identify album to update
        for (int i = 0; i < userAlbums.length(); i++) {
          JSONObject curAlbum = userAlbums.getJSONObject(i);
          if (curAlbum.getString(Album.Field.ID.toString()).equals(albumId)) {
            userAlbums.put(i, jsonAlbum);
            return Futures.immediateFuture(null);
          }
        }
      }

      // Error - no album found to update with given ID
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Album ID " + albumId + " does not exist");
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<MediaItem> getMediaItem(UserId userId, String appId,
                                        String albumId, String mediaItemId, Set<String> fields,
                                        SecurityToken token) throws ProtocolException {
    try {
      // First ensure user has a table
      String user = userId.getUserId(token);
      if (db.getJSONObject(MEDIAITEMS_TABLE).has(user)) {
        // Retrieve user's MediaItems
        JSONArray userMediaItems = db.getJSONObject(MEDIAITEMS_TABLE).getJSONArray(user);

        // Search user's MediaItems for given ID and album
        JSONObject mediaItem;
        for (int i = 0; i < userMediaItems.length(); i++) {
          mediaItem = userMediaItems.getJSONObject(i);
          if (mediaItem.getString(MediaItem.Field.ID.toString()).equals(mediaItemId) &&
              mediaItem.getString(MediaItem.Field.ALBUM_ID.toString()).equals(albumId)) {
            return Futures.immediateFuture(filterFields(mediaItem, fields, MediaItem.class));
          }
        }
      }

      // MediaItem wasn't found
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "MediaItem ID '" + mediaItemId + "' does not exist within Album '" + albumId + '\'');
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<RestfulCollection<MediaItem>> getMediaItems(UserId userId,
                                                            String appId, String albumId, Set<String> mediaItemIds,
                                                            Set<String> fields, CollectionOptions options, SecurityToken token)
      throws ProtocolException {
    try {
      // Ensure user has a table
      String user = userId.getUserId(token);
      if (db.getJSONObject(MEDIAITEMS_TABLE).has(user)) {
        // Get user's MediaItems
        JSONArray userMediaItems = db.getJSONObject(MEDIAITEMS_TABLE).getJSONArray(user);

        // Stores found MediaItems
        List<MediaItem> result = Lists.newArrayList();

        // Search for every MediaItem ID target
        boolean found;
        JSONObject curMediaItem;
        for (String mediaItemId : mediaItemIds) {
          // Search existing MediaItems for this MediaItem ID
          found = false;
          for (int i = 0; i < userMediaItems.length(); i++) {
            curMediaItem = userMediaItems.getJSONObject(i);
            if (curMediaItem.getString(MediaItem.Field.ID.toString()).equals(albumId) &&
                curMediaItem.getString(MediaItem.Field.ALBUM_ID.toString()).equals(albumId)) {
              result.add(filterFields(curMediaItem, fields, MediaItem.class));
              found = true;
              break;
            }
          }

          // Error - MediaItem ID not found
          if (!found) {
            throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "MediaItem ID " + mediaItemId + " does not exist within Album " + albumId);
          }
        }

        // Return found MediaItems
        return Futures.immediateFuture(new RestfulCollection<MediaItem>(result));
      }

      // Table doesn't exist for user
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "MediaItem table not found for user " + user);
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<RestfulCollection<MediaItem>> getMediaItems(UserId userId,
                                                            String appId, String albumId, Set<String> fields,
                                                            CollectionOptions options, SecurityToken token)
      throws ProtocolException {
    try {
      // First ensure user has a table
      String user = userId.getUserId(token);
      if (db.getJSONObject(MEDIAITEMS_TABLE).has(user)) {
        // Retrieve user's MediaItems
        JSONArray userMediaItems = db.getJSONObject(MEDIAITEMS_TABLE).getJSONArray(user);

        // Stores target MediaItems
        List<MediaItem> result = Lists.newArrayList();

        // Search user's MediaItems for given album
        JSONObject curMediaItem;
        for (int i = 0; i < userMediaItems.length(); i++) {
          curMediaItem = userMediaItems.getJSONObject(i);
          if (curMediaItem.getString(MediaItem.Field.ALBUM_ID.toString()).equals(albumId)) {
            result.add(filterFields(curMediaItem, fields, MediaItem.class));
          }
        }

        // Return found MediaItems
        return Futures.immediateFuture(new RestfulCollection<MediaItem>(result));
      }

      // Album wasn't found
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Album ID " + albumId + " does not exist");
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<RestfulCollection<MediaItem>> getMediaItems(
      Set<UserId> userIds, GroupId groupId, String appId,
      Set<String> fields, CollectionOptions options, SecurityToken token)
      throws ProtocolException {
    try {
      List<MediaItem> result = Lists.newArrayList();
      Set<String> idSet = getIdSet(userIds, groupId, token);

      // Gather MediaItems for all user IDs
      for (String id : idSet) {
        if (db.getJSONObject(MEDIAITEMS_TABLE).has(id)) {
          JSONArray userMediaItems = db.getJSONObject(MEDIAITEMS_TABLE).getJSONArray(id);
          for (int i = 0; i < userMediaItems.length(); i++) {
            result.add(filterFields(userMediaItems.getJSONObject(i), fields, MediaItem.class));
          }
        }
      }
      return Futures.immediateFuture(new RestfulCollection<MediaItem>(result));
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<Void> deleteMediaItem(UserId userId, String appId,
                                      String albumId, String mediaItemId, SecurityToken token)
      throws ProtocolException {
    try {
      boolean targetFound = false;        // indicates if target MediaItem is found
      JSONArray newMediaItems = new JSONArray();  // list of MediaItems minus target
      String user = userId.getUserId(token);    // retrieve user id

      // First ensure user has a table
      if (db.getJSONObject(MEDIAITEMS_TABLE).has(user)) {
        // Get user's MediaItems
        JSONArray userMediaItems = db.getJSONObject(MEDIAITEMS_TABLE).getJSONArray(user);

        // Compose new list of MediaItems excluding item to be deleted
        JSONObject curMediaItem;
        for (int i = 0; i < userMediaItems.length(); i++) {
          curMediaItem = userMediaItems.getJSONObject(i);
          if (curMediaItem.getString(MediaItem.Field.ID.toString()).equals(mediaItemId) &&
              curMediaItem.getString(MediaItem.Field.ALBUM_ID.toString()).equals(albumId)) {
            targetFound = true;
          } else {
            newMediaItems.put(curMediaItem);
          }
        }
      }

      // Overwrite user's MediaItems with updated list if target found
      if (targetFound) {
        db.getJSONObject(MEDIAITEMS_TABLE).put(user, newMediaItems);
        return Futures.immediateFuture(null);
      } else {
        throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "MediaItem ID " + mediaItemId + " does not exist existin within Album " + albumId);
      }
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<Void> createMediaItem(UserId userId, String appId,
                                      String albumId, MediaItem mediaItem, SecurityToken token)
      throws ProtocolException {
    try {
      // Get table of user's MediaItems
      JSONArray userMediaItems = db.getJSONObject(MEDIAITEMS_TABLE).getJSONArray(userId.getUserId(token));
      if (userMediaItems == null) {
        userMediaItems = new JSONArray();
        db.getJSONObject(MEDIAITEMS_TABLE).put(userId.getUserId(token), userMediaItems);
      }

      // Convert MediaItem to JSON and set ID & Album ID
      JSONObject jsonMediaItem = convertToJson(mediaItem);
      jsonMediaItem.put(MediaItem.Field.ALBUM_ID.toString(), albumId);
      if (!jsonMediaItem.has(MediaItem.Field.ID.toString())) {
        jsonMediaItem.put(MediaItem.Field.ID.toString(), System.currentTimeMillis());
      }

      // Insert new MediaItem into table
      userMediaItems.put(jsonMediaItem);
      return Futures.immediateFuture(null);
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // TODO: not using appId

  /** {@inheritDoc} */
  public Future<Void> updateMediaItem(UserId userId, String appId,
                                      String albumId, String mediaItemId, MediaItem mediaItem,
                                      SecurityToken token) throws ProtocolException {
    try {
      // First ensure user has a table
      String user = userId.getUserId(token);
      if (db.getJSONObject(MEDIAITEMS_TABLE).has(user)) {
        // Retrieve user's MediaItems
        JSONArray userMediaItems = db.getJSONObject(MEDIAITEMS_TABLE).getJSONArray(user);

        // Convert MediaItem to JSON and set ID & Album ID
        JSONObject jsonMediaItem = convertToJson(mediaItem);
        jsonMediaItem.put(MediaItem.Field.ID.toString(), mediaItemId);
        jsonMediaItem.put(MediaItem.Field.ALBUM_ID.toString(), albumId);

        // Iterate through MediaItems to identify item to update
        for (int i = 0; i < userMediaItems.length(); i++) {
          JSONObject curMediaItem = userMediaItems.getJSONObject(i);
          if (curMediaItem.getString(MediaItem.Field.ID.toString()).equals(mediaItemId) &&
              curMediaItem.getString(MediaItem.Field.ALBUM_ID.toString()).equals(albumId)) {
            userMediaItems.put(i, jsonMediaItem);
            return Futures.immediateFuture(null);
          }
        }
      }

      // Error - no MediaItem found with given ID and Album ID
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "MediaItem ID " + mediaItemId + " does not exist existin within Album " + albumId);
    } catch (JSONException je) {
      throw new ProtocolException(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          je.getMessage(), je);
    }
  }

  // Are fields really needed here?
  /** {@inheritDoc} */
  public Future<ActivityEntry> updateActivityEntry(UserId userId, GroupId groupId, String appId,
        Set<String> fields, ActivityEntry activityEntry, String activityId, SecurityToken token) throws ProtocolException {
    try {
      JSONObject jsonEntry = convertFromActivityEntry(activityEntry, fields);
      if (!jsonEntry.has(ActivityEntry.Field.ID.toString())) {
        if (activityId != null) {
          jsonEntry.put(ActivityEntry.Field.ID.toString(), activityId);
        } else {
          jsonEntry.put(ActivityEntry.Field.ID.toString(), System.currentTimeMillis());
        }
      }
      activityId = jsonEntry.getString(ActivityEntry.Field.ID.toString());

      JSONArray jsonArray;
      if (db.getJSONObject(ACTIVITYSTREAMS_TABLE).has(userId.getUserId(token))) {
        jsonArray = db.getJSONObject(ACTIVITYSTREAMS_TABLE).getJSONArray(userId.getUserId(token));
      } else {
        jsonArray = new JSONArray();
        db.getJSONObject(ACTIVITYSTREAMS_TABLE).put(userId.getUserId(token), jsonArray);
      }

      // Find & replace activity
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject entry = jsonArray.getJSONObject(i);
        if (entry.getString(ActivityEntry.Field.ID.toString()).equals(activityId)) {
          jsonArray.put(i, jsonEntry);
          return Futures.immediateFuture(filterFields(jsonEntry, fields, ActivityEntry.class));
        }
      }
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Activity not found: " + activityId);
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }
  }

  // Are fields really needed here?
  /** {@inheritDoc} */
  public Future<ActivityEntry> createActivityEntry(UserId userId, GroupId groupId, String appId,
        Set<String> fields, ActivityEntry activityEntry, SecurityToken token) throws ProtocolException {
    try {
      JSONObject jsonEntry = convertFromActivityEntry(activityEntry, fields);
      if (!jsonEntry.has(ActivityEntry.Field.ID.toString())) {
        jsonEntry.put(ActivityEntry.Field.ID.toString(), System.currentTimeMillis());
      }
      String activityId = jsonEntry.getString(ActivityEntry.Field.ID.toString());

      JSONArray jsonArray;
      if (db.getJSONObject(ACTIVITYSTREAMS_TABLE).has(userId.getUserId(token))) {
        jsonArray = db.getJSONObject(ACTIVITYSTREAMS_TABLE).getJSONArray(userId.getUserId(token));
      } else {
        jsonArray = new JSONArray();
        db.getJSONObject(ACTIVITYSTREAMS_TABLE).put(userId.getUserId(token), jsonArray);
      }

      // Ensure activity does not already exist
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject entry = jsonArray.getJSONObject(i);
        if (entry.getString(ActivityEntry.Field.ID.toString()).equals(activityId)) {
          throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Activity already exists: " + activityId);
        }
      }
      jsonArray.put(jsonEntry);
      return Futures.immediateFuture(filterFields(jsonEntry, fields, ActivityEntry.class));
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
            if (!activityIds.contains(activityEntry.getString(ActivityEntry.Field.ID.toString()))) {
              newList.put(activityEntry);
            }
          }
          db.getJSONObject(ACTIVITYSTREAMS_TABLE).put(user, newList);
        }
      }
      return Futures.immediateFuture(null);
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
          if (activityEntry.getString(ActivityEntry.Field.ID.toString()).equals(activityId)) {
            return Futures.immediateFuture(filterFields(activityEntry, fields, ActivityEntry.class));
          }
        }
      }
      throw new ProtocolException(HttpServletResponse.SC_BAD_REQUEST, "Activity not found: " + activityId);
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
      Set<String> idSet = getIdSet(userIds, groupId, token);
      for (String id : idSet) {
        if (db.getJSONObject(ACTIVITYSTREAMS_TABLE).has(id)) {
          JSONArray activityEntries = db.getJSONObject(ACTIVITYSTREAMS_TABLE).getJSONArray(id);
          for (int i = 0; i < activityEntries.length(); i++) {
            JSONObject activityEntry = activityEntries.getJSONObject(i);
            result.add(filterFields(activityEntry, fields, ActivityEntry.class));
            // TODO: ActivityStreams don't have appIds
          }
        }
      }
      Collections.sort(result, Collections.reverseOrder());
      return Futures.immediateFuture(new RestfulCollection<ActivityEntry>(result));
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
        for(String activityId : activityIds) {
          boolean found = false;
          for (int i = 0; i < activityEntries.length(); i++) {
            JSONObject activityEntry = activityEntries.getJSONObject(i);
            if (activityEntry.getString(ActivityEntry.Field.ID.toString()).equals(activityId)) {
              result.add(filterFields(activityEntry, fields, ActivityEntry.class));
              found = true;
              break;
            }
          }
          if (!found) {
            throw new ProtocolException(HttpServletResponse.SC_NOT_FOUND, "Activity not found: " + activityId);
          }
        }
      }
      Collections.sort(result, Collections.reverseOrder());
      return Futures.immediateFuture(new RestfulCollection<ActivityEntry>(result));
    } catch (JSONException je) {
      throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
    }
  }

  // TODO Why specifically handle Activity instead of generic POJO (below)?

  private JSONObject convertFromActivity(Activity activity, Set<String> fields)
      throws JSONException {
    // TODO Not using fields yet
    return new JSONObject(converter.convertToString(activity));
  }

  private JSONObject convertFromActivityEntry(ActivityEntry activityEntry, Set<String> fields) throws JSONException {
    // TODO Not using fields yet
    return new JSONObject(converter.convertToString(activityEntry));
  }

  private JSONObject convertToJson(Object object) throws JSONException {
    // TODO not using fields yet
    return new JSONObject(converter.convertToString(object));
  }

  public <T> T filterFields(JSONObject object, Set<String> fields,
                            Class<T> clz) throws JSONException {
    if (!fields.isEmpty()) {
      // Create a copy with just the specified fields
      object = new JSONObject(object, fields.toArray(new String[fields
          .size()]));
    }
    String objectVal = object.toString();
    if (authority != null) {
      objectVal = objectVal.replace("%origin%", authority.getOrigin());
    } else {
      //provide default for junit tests
      objectVal = objectVal.replace("%origin%", "http://localhost:8080");
    }
    return converter.convertToObject(objectVal, clz);
  }
}

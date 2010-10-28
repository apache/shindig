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

package org.apache.shindig.extras.as.opensocial.model;

import java.util.List;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.extras.as.core.model.ActivityObjectImpl;

import com.google.inject.ImplementedBy;

/**
 * A representation of an ActivityStream object, a thing which participates in an Activity.
 */
@ImplementedBy(ActivityObjectImpl.class)
@Exportablebean
public interface ActivityObject {
  
  /*
   * Fields that represent JSON elements for an activity entry.
   */
  public static enum Field {
    ID("id"),
    DISPLAY_NAME("displayName"),
    SUMMARY("summary"),
    MEDIA("media"),
    LINK("link"),
    OBJECT_TYPE("objectType"),
    IN_REPLY_TO("inReplyTo"),
    ATTACHED_OBJECTS("attachedObjects"),
    REPLIES("replies"),
    REACTIONS("reactions"),
    ACTION_LINKS("actionLinks"),
    UPSTREAM_DUPLICATES("upstreamDuplicates"),
    DOWNSTREAM_DUPLICATES("downstreamDuplicates"),
    STANDARD_LINKS("standardLinks");
    
    /*
     * The name of the JSON element.
     */
    private final String jsonString;
    
    /**
     * Constructs the field base for the JSON element.
     * 
     * @param jsonString the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }
    
    /**
     * Returns the name of the JSON element.
     * 
     * @return String the name of the JSON element
     */
    public String toString() {
      return jsonString;
    }
  }
  
  /**
   * Gets the absolute URI that uniquely identifies the object
   *
   * @return a non-null string
   */
  String getId();

  /**
   * Set the absolute URI that uniquely identifies the object
   *
   * @param id a non-null string
   */
  void setId(String id);

  /**
   * @return the human-readable name fo the object
   */
  String getDisplayName();

  /**
   * Sets the name
   * @param name a human-readable name
   */
  void setDisplayName(String displayName);

  /**
   * Gets the human-readable summary for this object.
   * @return the summary
   */
  String getSummary();

  /**
   * Sets the human-readable summary for this object.
   *
   * @param summary a summary
   */
  void setSummary(String summary);

  /**
   * Get the link to a media item
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.MediaLink} object.
   */
  MediaLink getMedia();

  /**
   * Set the link to a media item
   *
   * @param media a {@link org.apache.shindig.extras.as.opensocial.model.MediaLink} object.
   */
  void setMedia(MediaLink media);

  /**
   * Get the permanent link
   *
   * @return a permalink string, possibly null
   */
  String getLink();

  /**
   * Set the permanent link
   *
   * @param permalink a permalink string, possibly null
   */
  void setLink(String link);

  /**
   * Returns the ActivityObject's object type.
   *
   * @return String representing the object type
   */
  String getObjectType();

  /**
   * Set's the ActivityObject's object type.
   *
   * @param objectType is the object type
   */
  void setObjectType(String objectType);

  /**
   * Get the Activity this item is a response to
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} 
   */
  ActivityObject getInReplyTo();

  /**
   * Set the Activity this item is a response to
   *
   * @param inReplyTo a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  void setInReplyTo(ActivityObject inReplyTo);

  /**
   * Gets the list of Attached Activities for this entry
   *
   * @return a list of ActivityObjects
   */
  List<ActivityObject> getAttachedObjects();

  /**
   * Sets the list of Attached Activities for this entry
   *
   * @param attachedObjects a list of ActivityObjects
   */
  void setAttachedObjects(List<ActivityObject> attachedObjects);

  /**
   * Gets the list of reply Activities for this entry
   *
   * @return a list of ActivityObjects
   */
  List<ActivityObject> getReplies();

  /**
   * Sets the list of reply Activities for this entry
   *
   * @param replies a list of ActivityObjects
   */
  void setReplies(List<ActivityObject> replies);

  /**
   * Gets the list of reaction Activities for this entry
   *
   * @return a list of ActivityObjects
   */
  List<ActivityObject> getReactions();

  /**
   * Sets the list of reaction Activities for this entry
   *
   * @param reactions a list of ActivityObjects
   */
  void setReactions(List<ActivityObject> reactions);

  /**
   * Returns the ActionLinks for this object
   *
   * @return List<ActionLink> is the list of ActionLink objects
   */
  List<ActionLink> getActionLinks();

  /**
   * Sets the ActionLinks for this object
   *
   * @param actionLinks is the list of ActionLinks
   */
  void setActionLinks(List<ActionLink> actionLinks);

  /**
   * Get the list of upstream duplicates.
   *
   * @return a list of strings
   */
  List<String> getUpstreamDuplicates();

  /**
   * Set the list of upstream duplicates.
   *
   * @param upstreamDuplicates a list of strings containing duplicate IDs
   */
  void setUpstreamDuplicates(List<String> upstreamDuplicates);

  /**
   * Get the list of downstream duplicates.
   *
   * @return a list of strings
   */
  List<String> getDownstreamDuplicates();

  /**
   * Set the list of downstream duplicates
   *
   * @param downstreamDuplicates a list of strings containing duplicate IDs
   */
  void setDownstreamDuplicates(List<String> downstreamDuplicates);

  /**
   * Return the Object's StandardLinks
   *
   * @return List<StandardLink> is the list of StandardLinks
   */
  List<StandardLink> getStandardLinks();

  /**
   * Set the standard link string
   *
   * @param standardLinks the standard link
   */
  void setStandardLinks(List<StandardLink> standardLinks);
}

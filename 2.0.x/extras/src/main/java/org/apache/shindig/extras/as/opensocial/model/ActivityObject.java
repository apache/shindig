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
    NAME("name"),
    SUMMARY("summary"),
    MEDIA("media"),
    PERMALINK("permalink"),
    TYPE("type"),
    IN_REPLY_TO("inReplyTo"),
    ATTACHED("attached"),
    REPLY("reply"),
    REACTION("reaction"),
    ACTION("action"),
    UPSTREAM_DUPLICATE_ID("upstreamDuplicateId"),
    DOWNSTREAM_DUPLICATE_ID("downstreamDuplicateId"),
    STANDARD_LINK("standardLink");
    
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
  String getName();

  /**
   * Sets the name
   * @param name a human-readable name
   */
  void setName(String name);

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
  String getPermalink();

  /**
   * Set the permanent link
   *
   * @param permalink a permalink string, possibly null
   */
  void setPermalink(String permalink);

  /**
   * Returns a list of Type strings
   *
   * @return a list of Type strings
   */
  List<String> getType();

  /**
   * set the list of Type strings
   *
   * @param type a list of Type strings
   */
  void setType(List<String> type);

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
  List<ActivityObject> getAttached();

  /**
   * Sets the list of Attached Activities for this entry
   *
   * @param attached a list of ActivityObjects
   */
  void setAttached(List<ActivityObject> attached);

  /**
   * Gets the list of reply Activities for this entry
   *
   * @return a list of ActivityObjects
   */
  List<ActivityObject> getReply();

  /**
   * Sets the list of reply Activities for this entry
   *
   * @param reply a list of ActivityObjects
   */
  void setReply(List<ActivityObject> reply);

  /**
   * Gets the list of reaction Activities for this entry
   *
   * @return a list of ActivityObjects
   */
  List<ActivityObject> getReaction();

  /**
   * Sets the list of reaction Activities for this entry
   *
   * @param reaction a list of ActivityObjects
   */
  void setReaction(List<ActivityObject> reaction);

  /**
   * Returns an ActionLink for this object
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.ActionLink} object.
   */
  ActionLink getAction();

  /**
   * Sets the ActionLink for this object
   *
   * @param action a {@link org.apache.shindig.extras.as.opensocial.model.ActionLink} object.
   */
  void setAction(ActionLink action);

  /**
   * Get the list of upstream duplicate Ids
   *
   * @return a list of strings
   */
  List<String> getUpstreamDuplicateId();

  /**
   * Set the list of upstream duplicate Ids
   *
   * @param upstreamDuplicateId a list of strings containing duplicate Ids
   */
  void setUpstreamDuplicateId(List<String> upstreamDuplicateId);

  /**
   * Get the list of downstream duplicate Ids
   *
   * @return a list of strings
   */
  List<String> getDownstreamDuplicateId();

  /**
   * Set the list of downstream duplicate Ids
   *
   * @param downstreamDuplicateId a list of strings containing duplicate Ids
   */
  void setDownstreamDuplicateId(List<String> downstreamDuplicateId);

  /**
   * Return a standard link string
   *
   * @return the standard link
   */
  String getStandardLink();

  /**
   * Set the standard link string
   *
   * @param standardLink the standard link
   */
  void setStandardLink(String standardLink);
}

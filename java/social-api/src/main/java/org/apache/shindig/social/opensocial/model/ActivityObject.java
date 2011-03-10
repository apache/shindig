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

package org.apache.shindig.social.opensocial.model;

import java.util.List;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.ActivityObjectImpl;

import com.google.inject.ImplementedBy;

/**
 * A representation of an Activity's object.
 */
@ImplementedBy(ActivityObjectImpl.class)
@Exportablebean
public interface ActivityObject {
  
  /*
   * Fields that represent JSON elements for an activity entry.
   */
  public static enum Field {
    ATTACHED_OBJECTS("attachedObjects"),
    DISPLAY_NAME("displayName"),
    DOWNSTREAM_DUPLICATES("downstreamDuplicates"),
    EMBED_CODE("embedCode"),
    ID("id"),
    IMAGE("image"),
    OBJECT_TYPE("objectType"),
    SUMMARY("summary"),
    UPSTREAM_DUPLICATES("upstreamDuplicates"),
    URL("url");
    
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
   * @return the human-readable name of the object
   */
  String getDisplayName();

  /**
   * Sets the name
   * @param name a human-readable name
   */
  void setDisplayName(String displayName);

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
   * Returns the object's embedCode property.
   * 
   * @return String is the object's embedCode property.
   */
  String getEmbedCode();
  
  /**
   * Sets the objet's embedCode property.
   * 
   * @param embedCode is the value to set embedCode to
   */
  void setEmbedCode(String embedCode);

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
   * Get the link to a representative image.
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.MediaLink} object.
   */
  MediaLink getImage();

  /**
   * Set the link to a representative image.
   *
   * @param image a {@link org.apache.shindig.extras.as.opensocial.model.MediaLink} object.
   */
  void setImage(MediaLink image);

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
   * Returns the object's URL.
   * 
   * @return String is the object's URL
   */
  String getUrl();
  
  /**
   * Sets the object's URL.
   * 
   * @param url is the value to set the object's URL
   */
  void setUrl(String url);
}

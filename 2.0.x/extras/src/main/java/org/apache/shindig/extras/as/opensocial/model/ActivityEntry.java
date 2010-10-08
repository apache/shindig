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
import org.apache.shindig.extras.as.core.model.ActivityEntryImpl;

import com.google.inject.ImplementedBy;

/**
 * <p>ActivityEntry interface.</p>
 * TODO: comment a description for this class
 * TODO: ensure verbs are up to date
 * TODO: comment all classes
 */

@ImplementedBy(ActivityEntryImpl.class)
@Exportablebean
public interface ActivityEntry {
  
  /**
   * Fields that represent JSON elements for an activity entry.
   */
  public static enum Field {
    ICON("icon"),
    TIME("time"),
    ACTOR("actor"),
    VERB("verb"),
    OBJECT("object"),
    TARGET("target"),
    GENERATOR("generator"),
    SERVICE_PROVIDER("serviceProvider"),
    TITLE("title"),
    BODY("body"),
    STANDARD_LINK("standardLink");
    
    /**
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
   * Possible verbs for an activity stream entry.
   */
  public static enum Verb {
    MARK_AS_FAVORITE("markAsFavorite"),
    START_FOLLOWING("startFollowing"),
    MARK_AS_LIKED("markAsLiked"),
    MAKE_FRIEND("makeFriend"),
    JOIN("join"),
    PLAY("play"),
    POST("post"),
    SAVE("save"),
    SHARE("share"),
    TAG("tag"),
    UPDATE("update");
    
    /**
     * The name of the JSON element.
     */
    private final String jsonString;
    
    /**
     * Constructs the field base for the JSON element.
     * 
     * @param jsonString the name of the element
     */
    private Verb(String jsonString) {
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
   * <p>getIcon</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getIcon();

  /**
   * <p>setIcon</p>
   *
   * @param icon a {@link java.lang.String} object.
   */
  void setIcon(String icon);

  /**
   * <p>getTime</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getTime();

  /**
   * <p>setTime</p>
   *
   * @param time a {@link java.lang.String} object.
   */
  void setTime(String time);

  /**
   * <p>getActor</p>
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  ActivityObject getActor();

  /**
   * <p>setActor</p>
   *
   * @param actor a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  void setActor(ActivityObject actor);

  /**
   * <p>getVerb</p>
   *
   * @return a {@link java.util.List} object.
   */
  List<String> getVerb();

  /**
   * <p>setVerb</p>
   *
   * @param verb a {@link java.util.List} object.
   */
  void setVerb(List<String> verb);

  /**
   * <p>getObject</p>
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  ActivityObject getObject();

  /**
   * <p>setObject</p>
   *
   * @param object a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  void setObject(ActivityObject object);
  
  /**
   * <p>getTarget</p>
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  ActivityObject getTarget();
  
  /**
   * <p>setTarget</p>
   *
   * @param target a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  void setTarget(ActivityObject target);
  
  /**
   * <p>getGenerator</p>
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  ActivityObject getGenerator();

  /**
   * <p>setGenerator</p>
   *
   * @param generator a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  void setGenerator(ActivityObject generator);

  /**
   * <p>getServiceProvider</p>
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  ActivityObject getServiceProvider();

  /**
   * <p>setServiceProvider</p>
   *
   * @param serviceProvider a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  void setServiceProvider(ActivityObject serviceProvider);

  /**
   * <p>getTitle</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getTitle();

  /**
   * <p>setTitle</p>
   *
   * @param title a {@link java.lang.String} object.
   */
  void setTitle(String title);

  /**
   * <p>getBody</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getBody();

  /**
   * <p>setBody</p>
   *
   * @param body a {@link java.lang.String} object.
   */
  void setBody(String body);

  /**
   * <p>getStandardLink</p>
   *
   * @return a {@link java.util.List} object.
   */
  List<String> getStandardLink();

  /**
   * <p>setStandardLink</p>
   *
   * @param standardLink a {@link java.util.List} object.
   */
  void setStandardLink(List<String> standardLink);
}

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
import java.util.Map;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.ActivityEntryImpl;

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
    POSTED_TIME("postedTime"),
    ACTOR("actor"),
    VERB("verb"),
    OBJECT("object"),
    TARGET("target"),
    GENERATOR("generator"),
    PROVIDER("provider"),
    TITLE("title"),
    BODY("body"),
    LINKS("links"),
    TO("to"),
    CC("cc"),
    BCC("bcc");
    
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
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.MediaLink} object.
   */
  MediaLink getIcon();

  /**
   * <p>setIcon</p>
   *
   * @param icon a {@link org.apache.shindig.extras.as.opensocial.model.MediaLink} object.
   */
  void setIcon(MediaLink icon);

  /**
   * <p>getPostedTime</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getPostedTime();

  /**
   * <p>setPostedTime</p>
   *
   * @param postedTime a {@link java.lang.String} object.
   */
  void setPostedTime(String postedTime);

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
  String getVerb();

  /**
   * <p>setVerb</p>
   *
   * @param verb a {@link java.util.List} object.
   */
  void setVerb(String verb);

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
   * <p>getProvider</p>
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  ActivityObject getProvider();

  /**
   * <p>setServiceProvider</p>
   *
   * @param provider a {@link org.apache.shindig.extras.as.opensocial.model.ActivityObject} object.
   */
  void setProvider(ActivityObject provider);

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
   * <p>links</p>
   *
   * @return a {@link java.util.Map} object.
   */
  Map<String, List<StandardLink>> getLinks();

  /**
   * <p>setLinks</p>
   *
   * @param links a {@link java.util.Map} object.
   */
  void setLinks(Map<String, List<StandardLink>> links);
  
  /**
   * <p>getTo</p>
   *
   * @return a List<String> of target recipients
   */
  List<String> getTo();
  
  /**
   * <p>setTo</p>
   *
   * @param to is the list of target recipients
   */
  void setTo(List<String> to);
  
  /**
   * <p>getCC</p>
   *
   * @return a List<String> of carbon-copy recipients
   */
  List<String> getCc();
  
  /**
   * <p>setCC</p>
   *
   * @param cc is the list of carbon-copy recipients
   */
  void setCc(List<String> cc);
  
  /**
   * <p>getBCC</p>
   *
   * @return a List<String> of BCC recipients
   */
  List<String> getBcc();
  
  /**
   * <p>setBCC</p>
   *
   * @param bcc is the list of BCC recipients
   */
  void setBcc(List<String> bcc);
}

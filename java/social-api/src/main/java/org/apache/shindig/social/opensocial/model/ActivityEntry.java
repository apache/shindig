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

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.ActivityEntryImpl;

import com.google.inject.ImplementedBy;

/**
 * <p>ActivityEntry interface.</p>
 */
@ImplementedBy(ActivityEntryImpl.class)
@Exportablebean
public interface ActivityEntry {
  
  /**
   * Fields that represent JSON elements for an activity entry.
   */
  public static enum Field {
    ACTOR("actor"),
    BODY("body"),
    GENERATOR("generator"),
    ICON("icon"),
    OBJECT("object"),
    POSTED_TIME("postedTime"),
    PROVIDER("provider"),
    TARGET("target"),
    TITLE("title"),
    UPDATED_TIME("updatedTime"),
    VERB("verb"),
    OPENSOCIAL("openSocial");
    
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
   * <p>getActor</p>
   *
   * @return a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  ActivityObject getActor();

  /**
   * <p>setActor</p>
   *
   * @param actor a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  void setActor(ActivityObject actor);

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
   * <p>getGenerator</p>
   *
   * @return a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  ActivityObject getGenerator();

  /**
   * <p>setGenerator</p>
   *
   * @param generator a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  void setGenerator(ActivityObject generator);
  
  /**
   * <p>getIcon</p>
   *
   * @return a {@link org.apache.shindig.social.opensocial.model.MediaLink} object.
   */
  MediaLink getIcon();

  /**
   * <p>setIcon</p>
   *
   * @param icon a {@link org.apache.shindig.social.opensocial.model.MediaLink} object.
   */
  void setIcon(MediaLink icon);

  /**
   * <p>getObject</p>
   *
   * @return a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  ActivityObject getObject();

  /**
   * <p>setObject</p>
   *
   * @param object a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  void setObject(ActivityObject object);

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
   * <p>getProvider</p>
   *
   * @return a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  ActivityObject getProvider();

  /**
   * <p>setServiceProvider</p>
   *
   * @param provider a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  void setProvider(ActivityObject provider);
  
  /**
   * <p>getTarget</p>
   *
   * @return a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  ActivityObject getTarget();
  
  /**
   * <p>setTarget</p>
   *
   * @param target a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object.
   */
  void setTarget(ActivityObject target);

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
   * <p>getUpdatedTime</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getUpdatedTime();

  /**
   * <p>setUpdatedTime</p>
   *
   * @param postedTime a {@link java.lang.String} object.
   */
  void setUpdatedTime(String updatedTime);

  /**
   * <p>getVerb</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getVerb();

  /**
   * <p>setVerb</p>
   *
   * @param verb a {@link java.lang.String} object.
   */
  void setVerb(String verb);
  
  /**
   * <p>getOpenSocial</p>
   * 
   * @return a {@link org.apache.shindig.social.opensocial.model.OpenSocial} object
   */
  OpenSocial getOpenSocial();
  
  /**
   * <p>setOpenSocial</p>
   * 
   * @return a {@link org.apache.shindig.social.opensocial.model.OpenSocial} object
   */
  void setOpenSocial(OpenSocial extensions);
}

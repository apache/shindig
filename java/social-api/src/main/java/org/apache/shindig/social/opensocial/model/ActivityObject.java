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
package org.apache.shindig.social.opensocial.model;

import java.util.List;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.protocol.model.ExtendableBean;
import org.apache.shindig.social.core.model.ActivityObjectImpl;

import com.google.inject.ImplementedBy;

/**
 * A representation of an Activity's object.
 *
 * Note that an Activity's object may contain fields from an Activity when
 * the objectType is of type 'activity'.  As such, ActivityObject becomes
 * a superset of Activity.  Refer to the Activity Streams spec.
 */
@ImplementedBy(ActivityObjectImpl.class)
@Exportablebean
public interface ActivityObject extends ExtendableBean {

  /**
   * Fields that represent the JSON elements.
   */
  public static enum Field {
    // Activity's object fields
    ATTACHMENTS("attachments"),
    AUTHOR("author"),
    CONTENT("content"),
    DISPLAY_NAME("displayName"),
    DOWNSTREAM_DUPLICATES("downstreamDuplicates"),
    ID("id"),
    IMAGE("image"),
    OBJECT_TYPE("objectType"),
    PUBLISHED("published"),
    SUMMARY("summary"),
    UPDATED("updated"),
    UPSTREAM_DUPLICATES("upstreamDuplicates"),
    URL("url"),
    OPENSOCIAL("openSocial");

    // The name of the JSON element
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
   * <p>getAttachments</p>
   *
   * @return a list of {@link org.apache.shindig.social.opensocial.model.ActivityObject} object
   */
  List<ActivityObject> getAttachments();

  /**
   * <p>setAttachments</p>
   *
   * @param attachments a list of {@link org.apache.shindig.social.opensocial.model.ActivityObject} objects
   */
  void setAttachments(List<ActivityObject> attachments);

  /**
   * <p>getAuthor</p>
   *
   * @return a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object
   */
  ActivityObject getAuthor();

  /**
   * <p>setAuthor</p>
   *
   * @param author a {@link org.apache.shindig.social.opensocial.model.ActivityObject} object
   */
  void setAuthor(ActivityObject author);

  /**
   * <p>getContent</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getContent();

  /**
   * <p>setContent</p>
   *
   * @param content a {@link java.lang.String} object.
   */
  void setContent(String content);

  /**
   * <p>getDisplayName</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getDisplayName();

  /**
   * <p>setDisplayName</p>
   *
   * @param displayName a {@link java.lang.String} object
   */
  void setDisplayName(String displayName);

  /**
   * <p>getDownstreamDuplicates</p>
   *
   * @return a list of {@link java.lang.String} objects
   */
  List<String> getDownstreamDuplicates();

  /**
   * <p>setDownstreamDuplicates</p>
   *
   * @param downstreamDuplicates a list of {@link java.lang.String} objects
   */
  void setDownstreamDuplicates(List<String> downstreamDuplicates);

  /**
   * <p>getId</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getId();

  /**
   * <p>setId</p>
   *
   * @param id a {@link java.lang.String} object.
   */
  void setId(String id);

  /**
   * <p>getImage</p>
   *
   * @return a {@link org.apache.shindig.extras.as.opensocial.model.MediaLink} object
   */
  MediaLink getImage();

  /**
   * <p>setImage</p>
   *
   * @param image a {@link org.apache.shindig.extras.as.opensocial.model.MediaLink} object
   */
  void setImage(MediaLink image);

  /**
   * <p>getObjectType</p>
   *
   * @return a {@link java.lang.String} object
   */
  String getObjectType();

  /**
   * <p>setObjectType</p>
   *
   * @param objectType a {@link java.lang.String} object
   */
  void setObjectType(String objectType);

  /**
   * <p>getPublished</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getPublished();

  /**
   * <p>setPublished</p>
   *
   * @param published a {@link java.lang.String} object.
   */
  void setPublished(String published);

  /**
   * <p>getSummary</p>
   *
   * @return a {@link java.lang.String} object
   */
  String getSummary();

  /**
   * <p>setSummary</p>
   *
   * @param summary a {@link java.lang.String} object
   */
  void setSummary(String summary);

  /**
   * <p>getUpdated</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getUpdated();

  /**
   * <p>setUpdated</p>
   *
   * @param updated a {@link java.lang.String} object.
   */
  void setUpdated(String updated);

  /**
   * <p>getUpstreamDuplicates</p>
   *
   * @return a list of {@link java.lang.String} objects
   */
  List<String> getUpstreamDuplicates();

  /**
   * <p>setUpstreamDuplicates</p>
   *
   * @param upstreamDuplicates a list of {@link java.lang.String} objects
   */
  void setUpstreamDuplicates(List<String> upstreamDuplicates);

  /**
   * <p>getUrl</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getUrl();

  /**
   * <p>setUrl</p>
   *
   * @param url a {@link java.lang.String} object.
   */
  void setUrl(String url);

  /**
   * <p>getOpenSocial</p>
   *
   * @return a {@link org.apache.shindig.protocol.model.ExtendableBean} object
   */
  ExtendableBean getOpenSocial();

  /**
   * <p>setOpenSocial</p>
   *
   * @return a {@link org.apache.shindig.protocol.model.ExtendableBean} object
   */
  void setOpenSocial(ExtendableBean opensocial);
}

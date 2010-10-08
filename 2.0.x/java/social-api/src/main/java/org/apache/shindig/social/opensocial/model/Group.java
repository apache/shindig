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

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.GroupImpl;
import org.apache.shindig.social.opensocial.spi.GroupId;

import com.google.inject.ImplementedBy;

/**
 * <p>
 * OpenSocial Groups are owned by people, and are used to tag or categorize people and their relationships.
 * Each group has a display name, an identifier which is unique within the groups owned by that person, and a URI link.
 * A group may be a private, invitation-only, public or a personal group used to organize friends.
 * </p>
 * <p>
 * From http://opensocial-resources.googlecode.com/svn/spec/1.0/Social-Data.xml#Group
 * </p>
 *
 * @since 2.0.0
 */
@ImplementedBy(GroupImpl.class)
@Exportablebean
public interface Group {

  public static enum Field {
    /**
     * Unique ID for this group Required.
     */
    ID("Id"),
    /**
     * Title of group Required.
     */
    TITLE("title"),
    /**
     * Description of group Optional.
     */
    DESCRIPTION("description");

    /**
     * The json field that the instance represents.
     */
    private final String jsonString;

    /**
     * create a field base on the a json element.
     *
     * @param jsonString the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * emit the field as a json element.
     *
     * @return the field name
     */
    @Override
    public String toString() {
      return jsonString;
    }
  }

  /**
   * @return a groupId for this group
   */
  GroupId getId();

  /**
   * Set the default group id
   *
   * @param id a valid GroupId
   */
  void setId(GroupId id);

  /**
   * @return the title of the group
   */
  String getTitle();

  /**
   * Sets the title of this group
   *
   * @param title a valid title
   */
  void setTitle(String title);

  /**
   * @return the description of this group
   */
  String getDescription();

  /**
   * Sets the description of this group
   *
   * @param description a valid description
   */
  void setDescription(String description);
}

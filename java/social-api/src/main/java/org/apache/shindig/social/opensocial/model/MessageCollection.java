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

import org.apache.shindig.social.core.model.MessageCollectionImpl;

import com.google.inject.ImplementedBy;
import java.util.List;
import java.util.Set;
import java.util.Date;

/**
 * Base interface for all message collection objects.
 *
 * see
 * http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.MessageCollection.html
 */

@ImplementedBy(MessageCollectionImpl.class)
public interface MessageCollection {

  public String OUTBOX = "@outbox";
  public String ALL = "@all";

  /**
   * An enumeration of field names in a message.
   */
  public static enum Field {
    ID("id"),
    /** the field name for the title of this message collection. */
    TITLE("title"),
    /** the field name for total number of messages. */
    TOTAL("total"),
    /** the field name for the total number of unread messages */
    UNREAD("unread"),
    /** The field name for the updated time stamp */
    UPDATED("updated");

    /**
     * the name of the field.
     */
    private final String jsonString;

    public static final Set<String> ALL_FIELDS = EnumUtil.getEnumStrings(Field.values());

    /**
     * Create a field based on a name.
     * @param jsonString the name of the field
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * @return a string representation of the enum.
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }


  /**
   * Gets the unique ID of the message collection.
   * @return the ID of the message
   */
  String getId();

  /**
   * Sets the unique ID of the message collection.
   * @param id the ID value to set
   */
  void setId(String id);

  /**
   * Gets the title of the message collection.
   * @return the title of the message
   */
  String getTitle();

  /**
   * Sets the title of the message message collection.
   * @param newTitle the title of the message
   */
  void setTitle(String newTitle);

  /**
   * Gets the total number of messages for this collection.
   * @return the total number of messages
   */
  Integer getTotal();

  /**
   * Sets the total number of messages for this collection
   *
   * @param total the total number of messages
   */
  void setTotal(Integer total);

  /**
   * Gets the total number of unread messages.
   * @return the total number of unread messages
   */
  Integer getUnread();

  /**
   * Sets the total number of unread messages.
   * @param unread the number of unread messages
   */
  void setUnread(Integer unread);

  /**
   * Returns the last time this message collection was modified.
   * @return the updated time
   */
  Date getUpdated();

  /**
   * Sets the updated time for this message collection.
   * @param updated
   */
  void setUpdated(Date updated);

  /**
   * Get the URLs related to the message collection.
   *
   * @return the URLs related to the message collection
   */
  List<Url> getUrls();

  /**
   * Set the URLs related to the message collection
   *
   * @param urls the URLs related to the message collection
   */
  void setUrls(List<Url> urls);


}

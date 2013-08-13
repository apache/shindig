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
import org.apache.shindig.social.core.model.MessageImpl;

import com.google.inject.ImplementedBy;
import java.util.List;
import java.util.Set;
import java.util.Date;

/**
 *
 * Base interface for all message objects.
 *
 * see
 * <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Message">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Message</a>
 * <br/>
 * <a href="http://wiki.opensocial.org/index.php?title=Messaging_API_Changes">
 * http://wiki.opensocial.org/index.php?title=Messaging_API_Changes</a>
 */

@ImplementedBy(MessageImpl.class)
@Exportablebean
public interface Message {

  /**
   * An enumeration of field names in a message.
   */
  public static enum Field {
    APP_URL("appUrl"),
    /** the field name for body. */
    BODY("body"),
    /** the field name for body id. */
    BODY_ID("bodyId"),
    /** the field name for the collection IDs */
    COLLECTION_IDS("collectionIds"),
    /** the field name for the unique ID of this message. */
    ID("id"),
    /** the field name for the Parent Message Id for this message. */
    IN_REPLY_TO("inReplyTo"),
    /** the field name for replies to this message */
    REPLIES("replies"),
    /** the field name for recipient list. */
    RECIPIENTS("recipients"),
    /** the field name for the ID of the person who sent this message. */
    SENDER_ID("senderId"),
    /** the field name for the time this message was sent. */
    TIME_SENT("timeSent"),
    /** the field name for title. */
    TITLE("title"),
    /** the field name for title id. */
    TITLE_ID("titleId"),
    /** the field name for type. */
    TYPE("type"),
    /** the field name for status. */
    STATUS("status"),
    /** the field name for updated time stamp. */
    UPDATED("updated"),
    /** the field name for urls. */
    URLS("urls");

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
   * The type of a message.
   */
  public enum Type {
    /** An email. */
    EMAIL("email"),
    /** A short private message. */
    NOTIFICATION("notification"),
    /** A message to a specific user that can be seen only by that user. */
    PRIVATE_MESSAGE("privateMessage"),
    /** A message to a specific user that can be seen by more than that user. */
    PUBLIC_MESSAGE("publicMessage");


    /**
     * The type of message.
     */
    private final String jsonString;

    /**
     * Create a message type based on a string token.
     * @param jsonString the type of message
     */
    private Type(String jsonString) {
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
   * The Status of a message.
   */
  public enum Status {
    NEW("new"), DELETED("deleted"), FLAGGED("read");
    /**
     * The type of message.
     */
    private final String jsonString;

    /**
     * Create a message type based on a string token.
     * @param jsonString the type of message
     */
    private Status(String jsonString) {
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
   * Gets the App URL for a message.
   *
   * Used if an App generated the message.
   *
   * @return the Application URL
   */
  String getAppUrl();

  /**
   * Set the App URL for a message.
   * @param url the URL to set.
   */
  void setAppUrl(String url);

  /**
   * Gets the main text of the message.
   * @return the main text of the message
   */
  String getBody();

  /**
   * Sets the main text of the message.
   * HTML attributes are allowed and are sanitized by the container
   * @param newBody the main text of the message
   */
  void setBody(String newBody);

  /**
   * Gets the body id.
   * Used for message submission
   * @return the body ID
   */
  String getBodyId();

  /**
   * Sets the body id.
   * @param bodyId A valid body id defined in the gadget XML.
   */
  void setBodyId(String bodyId);


  /**
   * Gets the collection Ids for this message.
   */
  List<String> getCollectionIds();

  /**
   * Sets the collection Ids for this message.
   */
  void setCollectionIds(List<String> collectionIds);

  /**
   * Gets the unique ID of the message
   * @return the ID of the message
   */
  String getId();

  /**
   * Sets the unique ID of the message.
   * @param id the ID value to set
   */
  void setId(String id);

  /**
   * Gets the parent message ID.
   * @return message id
   */
  String getInReplyTo();
  /**
   * Sets the parent message ID
   * @param parentId the parentId to set
   */
  void setInReplyTo(String parentId);

  /**
   * Gets the recipient list of the message.
   * @return the recipients of the message
   */
  List<String> getRecipients();

  /**
   * Gets the list of Replies to this message
   * @return
   */
  List<String> getReplies();

  /**
   * Gets the Status of the message.
   * @return the status of the message
   */
  Status getStatus();

  /**
   * Sets the Status of the message.
   * @param status the status to set
   */
  void setStatus(Status status);

  /**
   * Sets the recipients of the message.
   * HTML attributes are allowed and are sanitized by the container
   * @param recipients the recipients text of the message
   */
  void setRecipients(List<String> recipients);

  /**
   * Gets the sender ID value.
   * @return sender person id
   */
  String getSenderId();

  /**
   * sets the sender ID.
   * @param senderId the sender id to set
   */
  void setSenderId(String senderId);

  /**
   * Gets the time the message was sent.
   * @return the message sent time
   */
  Date getTimeSent();

  /**
   * Sets the time the message was sent.
   * @param timeSent the time the message was sent
   */
  void setTimeSent(Date timeSent);

  /**
   * Gets the title of the message.
   * @return the title of the message
   */
  String getTitle();

  /**
   * Sets the title of the message.
   * HTML attributes are allowed and are sanitized by the container.
   * @param newTitle the title of the message
   */
  void setTitle(String newTitle);

  /**
   * Gets the title ID for this message.
   * Used for message submission.
   * @return the title Id
   */
  String getTitleId();

  /**
   * Sets the title ID for this message.
   * Used for message submission.
   *
   * @param titleId the title ID as defined in the gadget XML
   */
  void setTitleId(String titleId);

  /**
   * Gets the type of the message, as specified by opensocial.Message.Type.
   * @return the type of message (enum Message.Type)
   */
  Type getType();

  /**
   * Sets the type of the message, as specified by opensocial.Message.Type.
   * @param newType the type of message (enum Message.Type)
   */
  void setType(Type newType);

  /**
   * Gets the updated timestamp for the message.
   * @return the updated date of the message
   */
  Date getUpdated();

  /**
   * Sets the updated timestamp for the message.
   */
  void setUpdated(Date updated);

  /**
   * Get the URLs related to the message
   *
   * @return the URLs related to the person, their webpages, or feeds
   */
  List<Url> getUrls();

  /**
   * Set the URLs related to the message
   *
   * @param urls the URLs related to the person, their webpages, or feeds
   */
  void setUrls(List<Url> urls);


  /**
   * TODO implement either a standard 'sanitizing' facility or
   * define an interface that can be set on this class so
   * others can plug in their own.
   * @param htmlStr String to be sanitized.
   * @return the sanitized HTML String
   */
  String sanitizeHTML(String htmlStr);
}

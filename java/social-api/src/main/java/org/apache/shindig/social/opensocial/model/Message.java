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
import org.apache.shindig.social.core.model.MessageImpl;

import com.google.inject.ImplementedBy;

/**
 *
 * Base interface for all message objects.
 *
 * see
 * <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Message">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Message</a>
 *
 */

@ImplementedBy(MessageImpl.class)
@Exportablebean
public interface Message {

  /**
   * An enumeration of field names in a message.
   */
  public static enum Field {
    /** the field name for body. */
    BODY("body"),
    /** the field name for title. */
    TITLE("title"),
    /** the field name for type. */
    TYPE("type");

    /**
     * the name of the field.
     */
    private final String jsonString;

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
    EMAIL("EMAIL"),
    /** A short private message. */
    NOTIFICATION("NOTIFICATION"),
    /** A message to a specific user that can be seen only by that user. */
    PRIVATE_MESSAGE("PRIVATE_MESSAGE"),
    /** A message to a specific user that can be seen by more than that user. */
    PUBLIC_MESSAGE("PUBLIC_MESSAGE");

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
   * TODO implement either a standard 'sanitizing' facility or
   * define an interface that can be set on this class so
   * others can plug in their own.
   * @param htmlStr String to be sanitized.
   * @return the sanitized HTML String
   */
  String sanitizeHTML(String htmlStr);
}

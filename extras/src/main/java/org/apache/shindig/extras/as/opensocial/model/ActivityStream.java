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

import org.apache.shindig.extras.as.core.model.ActivityStreamImpl;

import org.apache.shindig.protocol.model.Exportablebean;

import com.google.inject.ImplementedBy;

/**
 * Interface for an Activity Stream.
 * <p>
 * See the Activity Streams specification for more detail:
 * http://activitystrea.ms/
 */
@ImplementedBy(ActivityStreamImpl.class)
@Exportablebean
public interface ActivityStream {
  
  /*
   * Fields that represent JSON elements for an activity entry.
   */
  public static enum Field {
    DISPLAY_NAME("displayName"),
    LANGUAGE("language"),
    ENTRIES("entries"),
    ID("id"),
    SUBJECT("subject");
    
    /*
     * The name of the JSON element.
     */
    private final String jsonString;
    
    /*
     * Constructs the field base for the JSON element.
     * 
     * @param jsonString the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }
    
    /*
     * Returns the name of the JSON element.
     * 
     * @return String the name of the JSON element
     */
    public String toString() {
      return jsonString;
    }
  }
  
  /**
   * Sets the Id for this stream
   * @param id a string
   */
  void setId(String id);
  
  /**
         * Return the id string
   *
   * @return a string
   */
  String getId();
  
  /**
   * Set the list of ActivityEntry objects
   * @param entries a list of ActivityEntry
   */
  void setEntries(List<ActivityEntry> entries);
  
  /**
   * Get the list of ActivityEntry objects
   *
   * @return a list of ActivityEntry
   */
  List<ActivityEntry> getEntries();
  
  /**
   * Set the language for this stream
   *
   * @param language a language string
   */
  void setLanguage(String language);
  
  /**
   * Get the language for this stream
   *
   * @return a language string
   */
  String getLanguage();
  
  /**
   * Set the subject for this stream
   *
   * @param subject a subject string
   */
  void setSubject(String subject);
  
  /**
   * Get the subject for this stream
   *
   * @return a subject string
   */
  String getSubject();
  
  /**
   * Get the display name for this stream
   *
   * @param displayName a display name
   */
  void setDisplayName(String displayName);
  
  /**
   * Get the human readable name for this stream
   *
   * @return a display name
   */
  String getDisplayName();
}

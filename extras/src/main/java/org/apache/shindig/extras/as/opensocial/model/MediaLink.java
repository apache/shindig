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

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.extras.as.core.model.MediaLinkImpl;
import com.google.inject.ImplementedBy;

/*
 * TODO: comment this class.
 */
/**
 * <p>MediaLink interface.</p>
 *
 */
@ImplementedBy(MediaLinkImpl.class)
@Exportablebean
public interface MediaLink {

  /**
   * Fields that represent the JSON elements.
   */
  public static enum Field {
    TARGET("target"),
    TYPE("type"),
    WIDTH("width"),
    HEIGHT("height"),
    DURATION("duration");
    
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
   * Returns the target of this MediaLink
   *
   * @return a target
   */
  String getTarget();

  /**
   * Sets the target for this MediaLink
   *
   * @param target a target linke
   */
  void setTarget(String target);

  /**
   * Returns the type of the MediaLink
   *
   * @return a type
   */
  String getType();

  /**
   * Sets the type of the MediaLink
   *
   * @param type a type
   */
  void setType(String type);

  /**
   * <p>getWidth</p>
   *
   * @return a {@link java.lang.String} object.
   */
  String getWidth();

  /**
   * Sets the Width of this mediaLink
   *
   * @param width a width
   */
  void setWidth(String width);

  /**
   * Sets the Height of this mediaLink
   *
   * @return a height
   */
  String getHeight();

  /**
   * Sets the Height of this mediaLink
   *
   * @param height a height
   */
  void setHeight(String height);

  /**
   * Returns the duration of this mediaLink
   *
   * @return a duration
   */
  String getDuration();

  /**
   * Sets the duration of this mediaLink
   *
   * @param duration a duration
   */
  void setDuration(String duration);
}

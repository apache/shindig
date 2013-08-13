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
import org.apache.shindig.protocol.model.ExtendableBean;
import org.apache.shindig.social.core.model.MediaLinkImpl;

import com.google.inject.ImplementedBy;

/**
 * <p>MediaLink interface.</p>
 */
@ImplementedBy(MediaLinkImpl.class)
@Exportablebean
public interface MediaLink extends ExtendableBean {

  /**
   * Fields that represent the JSON elements.
   */
  public static enum Field {
    DURATION("duration"),
    HEIGHT("height"),
    URL("url"),
    WIDTH("width"),
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
   * Returns the duration of this media.
   *
   * @return Integer is the target's duration
   */
  Integer getDuration();

  /**
   * Sets the duration of this media.
   *
   * @param duration is the target's duration
   */
  void setDuration(Integer duration);

  /**
   * Sets the height of this media.
   *
   * @return Integer the target's height
   */
  Integer getHeight();

  /**
   * Sets the height of this media.
   *
   * @param height is the target's height
   */
  void setHeight(Integer height);

  /**
   * Returns the target URL of this MediaLink.
   *
   * @return a target
   */
  String getUrl();

  /**
   * Sets the target URL for this MediaLink.
   *
   * @param target a target link
   */
  void setUrl(String url);

  /**
   * <p>Returns the width of this media.</p>
   *
   * @return Integer the target's width
   */
  Integer getWidth();

  /**
   * Sets the width of this media.
   *
   * @param width is the target's width
   */
  void setWidth(Integer width);

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

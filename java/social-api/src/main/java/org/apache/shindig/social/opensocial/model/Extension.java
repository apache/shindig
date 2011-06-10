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
import org.apache.shindig.social.core.model.ExtensionImpl;

import com.google.inject.ImplementedBy;

/**
 * A generic class to represent extensions to data models.
 */
@ImplementedBy(ExtensionImpl.class)
@Exportablebean
public interface Extension {

  public static enum Field {
    EMBED("embed"); // Embedded Experiences

    /**
     * The name of the JSON element.
     */
    private final String jsonString;

    /**
     * Constructs the field base for the JSON element.
     * 
     * @param jsonString
     *          the name of the element
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
   * Gets the embedded experience for this activity.
   * 
   * @return the embedded experience for this activity
   */
  EmbeddedExperience getEmbed();

  /**
   * Sets the emnbedded experience for this activity.
   * 
   * @param embed
   *          the embedded experience to set
   */
  void setEmbed(EmbeddedExperience embed);
}

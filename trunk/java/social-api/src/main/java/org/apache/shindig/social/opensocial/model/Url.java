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

import org.apache.shindig.social.core.model.UrlImpl;

import com.google.inject.ImplementedBy;


/**
 * The base interface of all Url objects.
 */
@ImplementedBy(UrlImpl.class)
public interface Url extends ListField {

  /**
   * An enumeration of the field names used in Url objects.
   */
  public static enum Field {
    /** the name of the value field. */
    VALUE("value"),
    /** the name of the linkText field. */
    LINK_TEXT("linkText"),
    /** the name of the type field. */
    TYPE("type");

    /**
     * The name of this field.
     */
    private final String jsonString;

    /**
     * Construct a new field based on a name.
     *
     * @param jsonString the name of the field
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * The string representation of the enum.
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * Get the text associated with the link.
   *
   * @return the link text
   */
  String getLinkText();

  /**
   * Set the Link text associated with the link.
   *
   * @param linkText the link text
   */
  void setLinkText(String linkText);
}

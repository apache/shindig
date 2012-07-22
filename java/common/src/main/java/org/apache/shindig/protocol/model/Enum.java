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
package org.apache.shindig.protocol.model;

/**
 * see <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Enum">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Enum</a>
 *
 * Base class for all Enum objects. This class allows containers to use constants for fields that
 * have a common set of values.
 */
@Exportablebean
public interface Enum<E extends Enum.EnumKey> {

  /**
   * Set of fields associated with an Enum object.
   */
  public static enum Field {
    /**
     * The value of the field.
     */
    VALUE("value"),
    /**
     * The display value of the field.
     */
    DISPLAY_VALUE("displayValue");

    /**
     * The json representation of the feild enum.
     */
    private final String jsonString;

    /**
     * Create a field enum.
     * @param jsonString the json value of the enum.
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * Gets the value of this Enum. This is the string displayed to the user. If the container
   * supports localization, the string should be localized.
   *
   * @return the Enum's user visible value
   */
  String getDisplayValue();

  /**
   * Sets the value of this Enum. This is the string displayed to the user. If the container
   * supports localization, the string should be localized.
   *
   * @param displayValue The value to set.
   */
  void setDisplayValue(String displayValue);

  /**
   * Gets the key for this Enum. Use this for logic within your gadget.
   *
   * @return java.lang.Enum key object for this Enum.
   */
  E getValue();

  /**
   * Sets the key for this Enum. Use this for logic within your gadget.
   *
   * @param value The value to set.
   */
  void setValue(E value);

  /**
 * base interface for keyed Enumerators.
   */
  public static interface EnumKey {
    String getDisplayValue();
  }
}

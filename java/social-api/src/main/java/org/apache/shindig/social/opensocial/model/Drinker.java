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

/**
 * public java.lang.Enum for opensocial.Enum.Drinker.
 */
public enum Drinker implements org.apache.shindig.protocol.model.Enum.EnumKey {
  /** Heavy drinker. */
  HEAVILY("HEAVILY", "Heavily"),
  /** non drinker. */
  NO("NO", "No"),
  /** occasional drinker. */
  OCCASIONALLY("OCCASIONALLY", "Occasionally"),
  /** has quit drinking. */
  QUIT("QUIT", "Quit"),
  /** in the process of quitting. */
  QUITTING("QUITTING", "Quitting"),
  /** regular drinker. */
  REGULARLY("REGULARLY", "Regularly"),
  /** drinks socially. */
  SOCIALLY("SOCIALLY", "Socially"),
  /** yes, a drinker of alchhol. */
  YES("YES", "Yes");

  /**
   * the Json representation.
   */
  private final String jsonString;

  /**
   * the value used for display purposes.
   */
  private final String displayValue;

  /**
   * private internal constructor for the enum.
   * @param jsonString the json representation.
   * @param displayValue the display value.
   */
  private Drinker(String jsonString, String displayValue) {
    this.jsonString = jsonString;
    this.displayValue = displayValue;
  }

  /**
   * {@inheritDoc}
   * @see java.lang.Enum#toString()
   */
  @Override
  public String toString() {
    return this.jsonString;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.shindig.protocol.model.Enum.EnumKey#getDisplayValue()
   */
  public String getDisplayValue() {
    return displayValue;
  }
}

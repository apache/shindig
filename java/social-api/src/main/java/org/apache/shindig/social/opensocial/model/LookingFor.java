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
 * public java.lang.Enum for opensocial.Enum.LookingFor.
 */
public enum LookingFor implements org.apache.shindig.protocol.model.Enum.EnumKey {
  /** Interested in dating. */
  DATING("DATING", "Dating"),
  /** Looking for friends. */
  FRIENDS("FRIENDS", "Friends"),
  /** Looking for a relationship. */
  RELATIONSHIP("RELATIONSHIP", "Relationship"),
  /** Just want to network. */
  NETWORKING("NETWORKING", "Networking"),
  /** */
  ACTIVITY_PARTNERS("ACTIVITY_PARTNERS", "Activity partners"),
  /** */
  RANDOM("RANDOM", "Random");

  /**
   * The Json representation of the value.
   */
  private final String jsonString;

  /**
   * The value used for display purposes.
   */
  private final String displayValue;

  /**
   * Construct a looking for enum.
   * @param jsonString the json representation of the enum.
   * @param displayValue the value used for display purposes.
   */
  private LookingFor(String jsonString, String displayValue) {
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

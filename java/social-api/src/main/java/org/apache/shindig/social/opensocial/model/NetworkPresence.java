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
 * public java.lang.Enum for opensocial.Enum.NetworkPresence.
 */
public enum NetworkPresence implements org.apache.shindig.protocol.model.Enum.EnumKey {
  /** Currently Online. */
  ONLINE("ONLINE", "Online"),
  /** Currently Offline. */
  OFFLINE("OFFLINE", "Offline"),
  /** Currently online but away. */
  AWAY("AWAY", "Away"),
  /** In a chat or available to chat. */
  CHAT("CHAT", "Chat"),
  /** Online, but don't disturb. */
  DND("DND", "Do Not Disturb"),
  /** Gone away for a longer period of time. */
  XA("XA", "Extended Away");

  /**
   * The Json representation of the value.
   */
  private final String jsonString;

  /**
   * The value used for display purposes.
   */
  private final String displayValue;

  /**
   * Create a network presence enum.
   * @param jsonString the json value.
   * @param displayValue the display value.
   */
  private NetworkPresence(String jsonString, String displayValue) {
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

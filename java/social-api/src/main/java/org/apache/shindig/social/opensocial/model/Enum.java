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

/**
 * see http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Enum.html
 *
 * Base class for all Enum objects. This class allows containers to use constants for fields that
 * have a common set of values.
 */
public interface Enum<E extends Enum.EnumKey> {

  /**
   * Set of fields associated with an Enum object
   */
  public static enum Field {

    KEY("key"),
    // TODO Shouldnt this be 'displayValue'
    DISPLAY_VALUE("displayValue");

    private final String jsonString;

    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  public interface EnumKey {

    String getDisplayValue();
  }

  /**
   * public java.lang.Enum for opensocial.Enum.Drinker.
   */
  public enum Drinker implements EnumKey {

    HEAVILY("HEAVILY", "Heavily"),
    NO("NO", "No"),
    OCCASIONALLY("OCCASIONALLY", "Occasionally"),
    QUIT("QUIT", "Quit"),
    QUITTING("QUITTING", "Quitting"),
    REGULARLY("REGULARLY", "Regularly"),
    SOCIALLY("SOCIALLY", "Socially"),
    YES("YES", "Yes");

    private final String jsonString;

    private final String displayValue;

    private Drinker(String jsonString, String displayValue) {
      this.jsonString = jsonString;
      this.displayValue = displayValue;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }

    public String getDisplayValue() {
      return displayValue;
    }
  }

  /**
   * public java.lang.Enum for opensocial.Enum.Gender.
   */
  public enum Gender implements EnumKey {

    FEMALE("FEMALE", "Female"),
    MALE("MALE", "Male");

    private final String jsonString;

    private final String displayValue;

    private Gender(String jsonString, String displayValue) {
      this.jsonString = jsonString;
      this.displayValue = displayValue;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }

    public String getDisplayValue() {
      return displayValue;
    }
  }

  /**
   * public java.lang.Enum for opensocial.Enum.Smoker.
   */
  public enum Smoker implements EnumKey {

    HEAVILY("HEAVILY", "Heavily"),
    NO("NO", "No"),
    OCCASIONALLY("OCCASIONALLY", "Ocasionally"),
    QUIT("QUIT", "Quit"),
    QUITTING("QUITTING", "Quitting"),
    REGULARLY("REGULARLY", "Regularly"),
    SOCIALLY("SOCIALLY", "Socially"),
    YES("YES", "Yes");

    private final String jsonString;

    private final String displayValue;

    private Smoker(String jsonString, String displayValue) {
      this.jsonString = jsonString;
      this.displayValue = displayValue;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }

    public String getDisplayValue() {
      return displayValue;
    }
  }

  /**
   * public java.lang.Enum for opensocial.Enum.NetworkPresence.
   */
  public enum NetworkPresence implements EnumKey {

    ONLINE("ONLINE", "Online"),
    OFFLINE("OFFLINE", "Offline"),
    AWAY("AWAY", "Away"),
    CHAT("CHAT", "Chat"),
    DND("DND", "Do Not Disturb"),
    XA("XA", "Extended Away");

    private final String jsonString;

    private final String displayValue;

    private NetworkPresence(String jsonString, String displayValue) {
      this.jsonString = jsonString;
      this.displayValue = displayValue;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }

    public String getDisplayValue() {
      return displayValue;
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
  E getKey();

  /**
   * Sets the key for this Enum. Use this for logic within your gadget.
   *
   * @param key The value to set.
   */
  void setKey(E key);
}

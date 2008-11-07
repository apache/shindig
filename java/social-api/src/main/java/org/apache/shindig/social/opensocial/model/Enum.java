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
   * base interface for keyed Enumerators.
   */
  public interface EnumKey {
    String getDisplayValue();
  }

  /**
   * public java.lang.Enum for opensocial.Enum.Drinker.
   */
  public enum Drinker implements EnumKey {
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
     * @see org.apache.shindig.social.opensocial.model.Enum.EnumKey#getDisplayValue()
     */
    public String getDisplayValue() {
      return displayValue;
    }
  }

  /**
   * public java.lang.Enum for opensocial.Enum.Smoker.
   */
  public enum Smoker implements EnumKey {
    /**  A heavy smoker. */
    HEAVILY("HEAVILY", "Heavily"),
    /** Non smoker. */
    NO("NO", "No"),
    /** Smokes occasionally. */
    OCCASIONALLY("OCCASIONALLY", "Ocasionally"),
    /** Has quit smoking. */
    QUIT("QUIT", "Quit"),
    /** in the process of quitting smoking. */
    QUITTING("QUITTING", "Quitting"),
    /** regular smoker, but not a heavy smoker. */
    REGULARLY("REGULARLY", "Regularly"),
    /** smokes socially. */
    SOCIALLY("SOCIALLY", "Socially"),
    /** yes, a smoker. */
    YES("YES", "Yes");

    /**
     * The Json representation of the value.
     */
    private final String jsonString;

    /**
     * The value used for display purposes.
     */
    private final String displayValue;

    /**
     * Create a Smoker enumeration.
     * @param jsonString the json representation of the value.
     * @param displayValue the value used for display purposes.
     */
    private Smoker(String jsonString, String displayValue) {
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
     * @see org.apache.shindig.social.opensocial.model.Enum.EnumKey#getDisplayValue()
     */
    public String getDisplayValue() {
      return displayValue;
    }
  }

  /**
   * public java.lang.Enum for opensocial.Enum.NetworkPresence.
   */
  public enum NetworkPresence implements EnumKey {
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
     * @see org.apache.shindig.social.opensocial.model.Enum.EnumKey#getDisplayValue()
     */
    public String getDisplayValue() {
      return displayValue;
    }
  }

  /**
   * public java.lang.Enum for opensocial.Enum.LookingFor.
   */
  public enum LookingFor implements EnumKey {
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
     * @see org.apache.shindig.social.opensocial.model.Enum.EnumKey#getDisplayValue()
     */
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
  E getValue();

  /**
   * Sets the key for this Enum. Use this for logic within your gadget.
   *
   * @param value The value to set.
   */
  void setValue(E value);
}

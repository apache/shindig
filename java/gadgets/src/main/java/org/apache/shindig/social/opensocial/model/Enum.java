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

import org.apache.shindig.social.AbstractGadgetData;
import org.apache.shindig.social.Mandatory;


/**
 * see
 * http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Enum.html
 * 
 * Base class for all Enum objects. This class allows containers to use constants
 * for fields that have a common set of values.
 *
 */
public final class Enum<E extends java.lang.Enum<E>> extends AbstractGadgetData {
  @Mandatory private String displayValue = null;
  private java.lang.Enum<E> key = null;

  /**
   * public java.lang.Enum for opensocial.Enum.Drinker
   */
  public enum Drinker {
    HEAVILY("HEAVILY"),
    NO("NO"),
    OCCASIONALLY("OCCASIONALLY"),
    QUIT("QUIT"),
    QUITTING("QUITTING"),
    REGULARLY("REGULARLY"),
    SOCIALLY("SOCIALLY"),
    YES("YES");

    private final String jsonString;

    private Drinker(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * public java.lang.Enum for opensocial.Enum.Gender
   */
  public enum Gender {
    FEMALE("FEMALE"),
    MALE("MALE");

    private final String jsonString;

    private Gender(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * public java.lang.Enum for opensocial.Enum.Smoker
   */
  public enum Smoker {
    HEAVILY("HEAVILY"),
    NO("NO"),
    OCCASIONALLY("OCCASIONALLY"),
    QUIT("QUIT"),
    QUITTING("QUITTING"),
    REGULARLY("REGULARLY"),
    SOCIALLY("SOCIALLY"),
    YES("YES");

    private final String jsonString;

    private Smoker(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * Constructor
   * @param initKey java.lang.Enum initial key
   * @param initValue String initial display value
   * @throws NullPointerException if initValue is null
   */
  public Enum(java.lang.Enum<E> initKey, String initValue) throws NullPointerException {
    
    if (null != initValue) {
      this.displayValue = initValue;
      this.key = initKey;
    }
    else {
      throw new NullPointerException("Enum.setDisplayValue cannot be set to null");
    }
  }

  /**
   * Gets the value of this Enum. This is the string displayed to the user.
   * If the container supports localization, the string will be localized.
   * @return the Enum's user visible value
   */
  public String getDisplayValue() {
    return this.displayValue;
  }

  /**
   * Sets the value of this Enum. This is the string displayed to the user.
   * If the container supports localization, the string will be localized.
   * @throws NullPointerException if the String passed is null.
   */
  public void setDisplayValue(String newStr) throws NullPointerException {
    if (null != newStr) {
      // TODO: Is this string localized??
//      this.key = SmokerKey.get(newStr.trim().toUpperCase());
      this.displayValue = newStr;
    }
    else {
      throw new NullPointerException("Enum.setDisplayValue cannot be set to null");
    }
  }

  /**
   * Gets the key for this Enum.
   * Use this for logic within your gadget.
   * @return java.lang.Enum key object for this Enum.
   */
  public java.lang.Enum<E> getKey() {
    return this.key;
  }

  /**
   * Sets the key for this Enum.
   * Use this for logic within your gadget.
   */
  public void setKey(java.lang.Enum<E> newKey) {
    this.key = newKey;
  }


  /**
   * Helper method that converts a String to an equivalent key value (if any)
   * @param value the String value to be matched with appropriate keys.
   * @return the matching java.lang.Enum key matching the string, if any
   */
  public java.lang.Enum<E> stringToKey(String value) {
    if ((null != value) && (null != key)) {
      return java.lang.Enum.valueOf(key.getDeclaringClass(), value);
    }
    return null;
  }
}

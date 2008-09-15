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
package org.apache.shindig.social.opensocial.jpa;

import org.apache.shindig.social.opensocial.model.Enum;

/**
 * This a utility holder class for Enums to assist in database storage. It does not have any
 * database tables or persistence associated with it.
 * 
 * @param <E> The Enum type.
 */
public final class EnumDb<E extends Enum.EnumKey> implements Enum<E> {
  private String displayValue;
  private E value = null;

  /**
   * Constructs a Enum object.
   * 
   * @param value EnumKey The key to use
   * @param displayValue String The display value
   */
  public EnumDb(E value, String displayValue) {
    this.value = value;
    this.displayValue = displayValue;
  }

  /**
   * Constructs a Enum object.
   * 
   * @param value The key to use. Will use the value from getDisplayValue() as the display value.
   */
  public EnumDb(E value) {
    this(value, value.getDisplayValue());
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Enum#getDisplayValue()
   */
  public String getDisplayValue() {
    return displayValue;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Enum#setDisplayValue(java.lang.String)
   */
  public void setDisplayValue(String displayValue) {
    this.displayValue = displayValue;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Enum#getValue()
   */
  public E getValue() {
    return value;
  }

  /** 
   * {@inheritDoc}
   * @see org.apache.shindig.social.opensocial.model.Enum#setValue(org.apache.shindig.social.opensocial.model.Enum.EnumKey)
   */
  public void setValue(E value) {
    this.value = value;
  }

}

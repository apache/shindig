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
 * Implementation of the Enum interface
 */
public final class EnumImpl<E extends Enum.EnumKey> implements Enum<E> {
  private String displayValue;
  private E value = null;

  /**
   * Constructs a Enum object.
   * @param value EnumKey The key to use
   * @param displayValue String The display value
   */
  public EnumImpl(E value, String displayValue) {
    this.value = value;
    this.displayValue = displayValue;
  }

  /**
   * Constructs a Enum object.
   * @param value The key to use. Will use the value from getDisplayValue() as
   *     the display value.
   */
  public EnumImpl(E value) {
    this(value, value.getDisplayValue());
  }

  public String getDisplayValue() {
    return displayValue;
  }

  public void setDisplayValue(String displayValue) {
    this.displayValue = displayValue;
  }

  public E getValue() {
    return value;
  }

  public void setValue(E value) {
    this.value = value;
  }
}

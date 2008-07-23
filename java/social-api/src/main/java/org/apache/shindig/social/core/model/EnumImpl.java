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
package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.Enum;

public final class EnumImpl<E extends Enum.EnumKey> implements Enum<E> {
  private String displayValue;
  private E key = null;

  /**
   * Constructs a Enum object.
   * @param key EnumKey The key to use
   * @param displayValue String The display value
   */
  public EnumImpl(E key, String displayValue) {
    this.key = key;
    this.displayValue = displayValue;
  }

  /**
   * Constructs a Enum object.
   * @param key The key to use. Will use the value from getDisplayValue() as
   *     the display value.
   */
  public EnumImpl(E key) {
    this(key, key.getDisplayValue());
  }

  public String getDisplayValue() {
    return this.displayValue;
  }

  public void setDisplayValue(String displayValue) {
    this.displayValue = displayValue;
  }

  public E getKey() {
    return this.key;
  }

  public void setKey(E key) {
    this.key = key;
  }

}

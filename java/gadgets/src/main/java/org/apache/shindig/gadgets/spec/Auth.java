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
package org.apache.shindig.gadgets.spec;


/**
   * The supported auth modes for Preload
 */
public enum Auth {
  NONE, SIGNED, AUTHENTICATED;

  /**
   * @param value
   * @return The parsed value (defaults to NONE)
   */
  public static Auth parse(String value) {
    if (value != null) {
      value = value.trim();
      if (value.length() == 0) return Auth.NONE;
      try {
        return Auth.valueOf(value.toUpperCase());
      } catch (IllegalArgumentException iae) {
        return Auth.NONE;
      }
    } else {
      return Auth.NONE;
    }
  }

  /**
   * Use lowercase as toString form
   * @return string value of Auth type
   */
  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }
}

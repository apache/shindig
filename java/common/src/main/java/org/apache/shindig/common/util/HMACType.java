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
package org.apache.shindig.common.util;

/**
 *  HMACSHA algorithm is a family of Keyed-Hashing for Message Authentication as defined in RFC 2104 and FIPS 198-1
 *
 */
public enum HMACType {
  HMACSHA1(20, "HMACSHA1"), //$NON-NLS-1$
  HMACSHA256(32, "HMACSHA256"), //$NON-NLS-1$
  HMACSHA384(48, "HMACSHA384"), //$NON-NLS-1$
  HMACSHA512(64, "HMACSHA512"); //$NON-NLS-1$

  private final int length;
  private final String name;

  private HMACType(int length, String name) {
    this.length = length;
    this.name = name;
  }

  public String toString() {
    return this.name;
  }

  public String getName() {
    return this.name;
  }

  public int getLength() {
    return this.length;
  }

}

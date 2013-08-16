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
 * The Secure Hash Algorithm is a family of cryptographic hash functions published by the National Institute of Standards and Technology (NIST)
 */
public enum DigestType {
  SHA("SHA"), //$NON-NLS-1$
  SHA256("SHA-256"), //$NON-NLS-1$
  SHA384("SHA-384"), //$NON-NLS-1$
  SHA512("SHA-512"); //$NON-NLS-1$
  private final String name;

  private DigestType(String name) {
    this.name = name;
  }

  public String toString() {
    return this.name;
  }

  public String getName() {
    return this.name;
  }
}

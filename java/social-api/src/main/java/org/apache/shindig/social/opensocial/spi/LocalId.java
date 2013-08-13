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
package org.apache.shindig.social.opensocial.spi;

import java.util.regex.Pattern;

import com.google.common.base.Objects;

/**
 * LocalId as defined by the OpenSocial 2.0.1 Spec
 * @see http://opensocial-resources.googlecode.com/svn/spec/2.0.1/Core-Data.xml#Local-Id
 */
public class LocalId {

  private String localId;
  private static final Pattern localIdPattern = Pattern.compile("[\\w.-]*");

  /**
   * Constructor for LocalId.
   *
   * @param localId String to try and create LocalId from
   * @throws IllegalArgumentException
   */
  public LocalId(String localId) throws IllegalArgumentException {
    if(localId != null) {
      setLocalId(localId);
    } else {
      setLocalId("");
    }
  }

  /**
   * Validate localId is of the from defined in spec.
   *
   * @param localId String
   * @return boolean If validation passes
   */
  private boolean validate(String localId) {
    return localIdPattern.matcher(localId).matches();
  }

  /**
   * Get the stored localId.
   *
   * @return localId String
   */
  public String getLocalId() {
    return this.localId;
  }

  /**
   * Sets the localId after validating its format
   *
   * @param localId String
   * @return boolean If succeeded
   * @throws IllegalArgumentException
   */
  public boolean setLocalId(String localId) throws IllegalArgumentException {
    if(validate(localId)) {
      this.localId = localId;
      return true;
    } else {
      throw new IllegalArgumentException("The provided LocalId is not valid");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof LocalId)) {
      return false;
    }
    LocalId actual = (LocalId) o;
    return this.getLocalId().equals(actual.getLocalId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.localId);
  }

  @Override
  public String toString() {
    return this.localId;
  }
}

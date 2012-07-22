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

import com.google.common.base.Objects;

/**
 * GlobalId as defined by the OpenSocial 2.0.1 Spec
 * @see http://opensocial-resources.googlecode.com/svn/spec/2.0.1/Core-Data.xml#Global-Id
 */
public class GlobalId {

  private DomainName domainName;
  private LocalId localId;

  /**
   * Try to construct a GlobalId with a string that contains a valid
   * DomainName and valid LocalId separated by a colon (:).
   *
   * @param globalId String to try and create GlobalId from
   * @throws IllegalArgumentException when the globalId provided is not valid and
   *   cannot be parsed into a valid DomainName and/or LocalId
   */
  public GlobalId(String globalId) throws IllegalArgumentException {
    try {
      String[] gid = globalId.split(":");
      if(gid.length != 2) {
        throw new IllegalArgumentException("The provided GlobalId is not valid");
      }
      this.domainName = new DomainName(gid[0]);
      this.localId = new LocalId(gid[1]);
    } catch(IllegalArgumentException e) {
      throw new IllegalArgumentException("The provided GlobalId is not valid");
    }
  }

  /**
   * Construct a GlobalId with the provided a valid DomainName and LocalId
   *
   * @param domainName DomainName object
   * @param localId LocalId object
   */
  public GlobalId(DomainName domainName, LocalId localId) {
    this.domainName = domainName;
    this.localId = localId;
  }

  /**
   * Try and construct a GlobalId given a string for a DomainName and a string for a LocalId
   *
   * @param domainName String to try and create DomainName from
   * @param localId String to try and create LocalId from
   * @throws IllegalArgumentException
   */
  public GlobalId(String domainName, String localId) throws IllegalArgumentException {
    this.domainName = new DomainName(domainName);
    this.localId = new LocalId(localId);
  }

  /**
   * Get the domainName
   *
   * @return domainName DomainName
   */
  public DomainName getDomainName() {
    return this.domainName;
  }

  /**
   * Set the domainName with a DomainName
   *
   * @param domainName DomainName
   */
  public void setDomainName(DomainName domainName) {
    this.domainName = domainName;
  }

  /**
   * Set the domainName with a String
   *
   * @param domainName String
   * @throws IllegalArgumentException
   */
  public void setDomainName(String domainName) throws IllegalArgumentException {
    this.domainName = new DomainName(domainName);
  }

  /**
   * Get the localId
   *
   * @return localId LocalId
   */
  public LocalId getLocalId() {
    return this.localId;
  }

  /**
   * Set the localId with a LocalId
   *
   * @param localId LocalId
   */
  public void setLocalId(LocalId localId) {
    this.localId = localId;
  }

  /**
   * Set the localId with a String
   *
   * @param localId String
   */
  public void setLocalId(String localId) {
    this.localId = new LocalId(localId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof GlobalId)) {
      return false;
    }
    GlobalId actual = (GlobalId) o;
    return this.getDomainName().equals(actual.getDomainName())
        && this.getLocalId().equals(actual.getLocalId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.domainName, this.localId);
  }

  @Override
  public String toString() {
    return domainName + ":" + localId.toString();
  }
}

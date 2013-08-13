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
 * Domain-Name as defined by the OpenSocial 2.0.1 Spec
 * @see http://opensocial-resources.googlecode.com/svn/spec/2.0.1/Core-Data.xml#Domain-Name
 */
public class DomainName {

  private String domainName;
  private static final Pattern domainNamePattern = Pattern.compile("[\\w.-]*");

  /**
   * Constructor for DomainName
   *
   * @param domainName String to try and create DomainName from
   * @throws IllegalArgumentException
   */
  public DomainName(String domainName) throws IllegalArgumentException {
    setDomainName(domainName);
  }

  /**
   * Validates the domain name meets the spec definition.
   *
   * @return boolean Is a valid domain name by spec definition?
   */
  private boolean validate(String domainName) {
	  return domainNamePattern.matcher(domainName).matches();
  }

  /**
   * Get the domainName.
   *
   * @return domainName String
   */
  public String getDomainName() {
    return this.domainName;
  }

  /**
   * Set the domainName after validating its format.
   *
   * @param domainName String
   * @return boolean If succeeded
   * @throws IllegalArgumentException
   */
  public boolean setDomainName(String domainName) throws IllegalArgumentException {
    if(validate(domainName)) {
      this.domainName = domainName;
      return true;
    } else {
      throw new IllegalArgumentException("The provided DomainName is not valid");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof DomainName)) {
      return false;
    }
    DomainName actual = (DomainName) o;
    return this.getDomainName().equals(actual.getDomainName());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.domainName);
  }

  @Override
  public String toString() {
    return this.domainName;
  }
}

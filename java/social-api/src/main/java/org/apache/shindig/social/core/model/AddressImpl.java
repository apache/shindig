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
package org.apache.shindig.social.core.model;

import org.apache.shindig.social.opensocial.model.Address;

/**
 * Default representation of an {@link org.apache.shindig.social.opensocial.model.Address}
 */
public class AddressImpl implements Address {
  private String country;
  private Float latitude;
  private Float longitude;
  private String locality;
  private String postalCode;
  private String region;
  private String streetAddress;
  private String type;
  private String formatted;
  private Boolean primary;

  public AddressImpl() { }

  public AddressImpl(String formatted) {
    this.formatted = formatted;
  }

  public String getCountry() {
    return country;
  }

  /** {@inheritDoc} */
  public void setCountry(String country) {
    this.country = country;
  }

  public Float getLatitude() {
    return latitude;
  }

  /** {@inheritDoc} */
  public void setLatitude(Float latitude) {
    this.latitude = latitude;
  }

  public String getLocality() {
    return locality;
  }

  /** {@inheritDoc} */
  public void setLocality(String locality) {
    this.locality = locality;
  }

  public Float getLongitude() {
    return longitude;
  }

  /** {@inheritDoc} */
  public void setLongitude(Float longitude) {
    this.longitude = longitude;
  }

  public String getPostalCode() {
    return postalCode;
  }

  /** {@inheritDoc} */
  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getRegion() {
    return region;
  }

  /** {@inheritDoc} */
  public void setRegion(String region) {
    this.region = region;
  }

  public String getStreetAddress() {
    return streetAddress;
  }

  /** {@inheritDoc} */
  public void setStreetAddress(String streetAddress) {
    this.streetAddress = streetAddress;
  }

  public String getType() {
    return type;
  }

  /** {@inheritDoc} */
  public void setType(String type) {
    this.type = type;
  }

  public String getFormatted() {
    return formatted;
  }

  /** {@inheritDoc} */
  public void setFormatted(String formatted) {
    this.formatted = formatted;
  }

  public Boolean getPrimary() {
    return primary;
  }

  /** {@inheritDoc} */
  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }
}

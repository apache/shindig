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

import static javax.persistence.GenerationType.IDENTITY;

import org.apache.shindig.social.opensocial.model.Address;
import org.apache.shindig.social.opensocial.model.Person;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import java.util.List;

@Entity
@Table(name = "address")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "address_usage")
@DiscriminatorValue(value = "sharedaddress")
@NamedQuery(name = AddressDb.FINDBY_POSTCODE, query = "select a from AddressDb a where a.postalCode = :postalcode ")
public class AddressDb implements Address, DbObject {
  public static final String FINDBY_POSTCODE = "q.address.findbypostcode";

  public static final String PARAM_POSTCODE = "postalcode";

  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "oid")
  private long objectId;

  @Version
  @Column(name = "version")
  protected long version;

  @OneToMany(targetEntity = PersonDb.class, mappedBy = "currentLocation")
  private List<Person> atLocation;

  @Basic
  @Column(name = "country", length = 255)
  private String country;

  @Basic
  @Column(name = "latitude")
  private Float latitude;

  @Basic
  @Column(name = "longitude")
  private Float longitude;

  @Basic
  @Column(name = "locality", length = 255)
  private String locality;

  @Basic
  @Column(name = "postal_code", length = 255)
  private String postalCode;

  @Basic
  @Column(name = "region", length = 255)
  private String region;

  @Basic
  @Column(name = "street_address", length = 255)
  private String streetAddress;

  @Basic
  @Column(name = "type", length = 255)
  private String type;

  @Basic
  @Column(name = "formatted", length = 255)
  private String formatted;

  @Basic
  @Column(name = "primary_address")
  private Boolean primary;

  public AddressDb() {
  }

  public AddressDb(String formatted) {
    this.formatted = formatted;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Float getLatitude() {
    return latitude;
  }

  public void setLatitude(Float latitude) {
    this.latitude = latitude;
  }

  public String getLocality() {
    return locality;
  }

  public void setLocality(String locality) {
    this.locality = locality;
  }

  public Float getLongitude() {
    return longitude;
  }

  public void setLongitude(Float longitude) {
    this.longitude = longitude;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getStreetAddress() {
    return streetAddress;
  }

  public void setStreetAddress(String streetAddress) {
    this.streetAddress = streetAddress;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getFormatted() {
    return formatted;
  }

  public void setFormatted(String formatted) {
    this.formatted = formatted;
  }

  public Boolean getPrimary() {
    return primary;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  /**
   * @return the objectId
   */
  public long getObjectId() {
    return objectId;
  }

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId(long objectId) {
    this.objectId = objectId;
  }

  /**
   * @return the atLocation
   */
  public List<Person> getAtLocation() {
    return atLocation;
  }

  /**
   * @param atLocation the atLocation to set
   */
  public void setAtLocation(List<Person> atLocation) {
    this.atLocation = atLocation;
  }
}

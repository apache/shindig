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
package org.apache.shindig.social.opensocial.model;

import org.apache.shindig.social.core.model.AddressImpl;

import com.google.inject.ImplementedBy;

/**
 * Base interface for all address objects
 * see http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Address.Field.html.
 *
 */
@ImplementedBy(AddressImpl.class)
public interface Address {

  /**
   * The fields that represent the address object ion json form.
   */
  public static enum Field {
    COUNTRY("country"),
    LATITUDE("latitude"),
    LOCALITY("locality"),
    LONGITUDE("longitude"),
    POSTAL_CODE("postalCode"),
    REGION("region"),
    /** this field may be multiple lines */
    STREET_ADDRESS("streetAddress"),
    TYPE("type"),
    FORMATTED("formatted"),
    PRIMARY("primary");

    /**
     * The json field that the instance represents.
     */
    private final String jsonString;

    /**
     * create a field base on the a json element.
     *
     * @param jsonString the name of the element
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * emit the field as a json element.
     *
     * @return the field name
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * Get the country.
   *
   * @return the country
   */
  String getCountry();

  /**
   * Set the country.
   *
   * @param country the country
   */
  void setCountry(String country);

  /**
   * Get the latitude.
   *
   * @return latitude
   */
  Float getLatitude();

  /**
   * Set the latitude.
   *
   * @param latitude latitude
   */
  void setLatitude(Float latitude);

  /**
   * Get the locality.
   *
   * @return the locality
   */
  String getLocality();

  /**
   * Set the locality.
   *
   * @param locality the locality
   */
  void setLocality(String locality);

  /**
   * Get the longitude of the address in degrees.
   *
   * @return the longitude of the address in degrees
   */
  Float getLongitude();

  /**
   * Set the longitude of the address in degrees.
   *
   * @param longitude the longitude of the address in degrees.
   */
  void setLongitude(Float longitude);

  /**
   * Get the Postal code for the address.
   *
   * @return the postal code for the address
   */
  String getPostalCode();

  /**
   * Set the postal code for the address.
   *
   * @param postalCode the postal code
   */
  void setPostalCode(String postalCode);

  /**
   * Get the region.
   *
   * @return the region
   */
  String getRegion();

  /**
   * Set the region.
   *
   * @param region the region
   */
  void setRegion(String region);

  /**
   * Get the street address.
   *
   * @return the street address
   */
  String getStreetAddress();

  /**
   * Set the street address.
   *
   * @param streetAddress the street address
   */
  void setStreetAddress(String streetAddress);

  /**
   * Get the type of label of the address.
   *
   * @return the type or label of the address
   */
  String getType();

  /**
   * Get the type of label of the address.
   *
   * @param type the type of label of the address.
   */
  void setType(String type);

  /**
   * Get the formatted address.
   *
   * @return the formatted address
   */
  String getFormatted();

  /**
   * Set the formatted address.
   *
   * @param formatted the formatted address
   */
  void setFormatted(String formatted);

  Boolean getPrimary();

  void setPrimary(Boolean primary);
}

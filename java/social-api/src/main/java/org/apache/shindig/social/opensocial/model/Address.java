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
    /** the json field for country. */
    COUNTRY("country"),
    /** the json field for extendedAddress. */
    EXTENDED_ADDRESS("extendedAddress"),
    /** the json field for latitude. */
    LATITUDE("latitude"),
    /** the json field for locality. */
    LOCALITY("locality"),
    /** the json field for longitude. */
    LONGITUDE("longitude"),
    /** the json field for poBox. */
    PO_BOX("poBox"),
    /** the json field for postalCode. */
    POSTAL_CODE("postalCode"),
    /** the json field for region. */
    REGION("region"),
    /** the json field for streetAddress. */
    STREET_ADDRESS("streetAddress"),
    /** the json field for type. */
    TYPE("type"),
    /** the json field for unstructuredAddress. */
    UNSTRUCTURED_ADDRESS("unstructuredAddress");

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
   * Get the country. Container support for this field is REQUIRED.
   *
   * @return the country
   */
  String getCountry();

  /**
   * Set the country. Container support for this field is REQUIRED.
   *
   * @param country the country
   */
  void setCountry(String country);

  /**
   * Get the extended street address. Container support for this field is REQUIRED.
   *
   * @return the extended street address
   */
  String getExtendedAddress();

  /**
   * Set the extended street address. Container support for this field is REQUIRED.
   *
   * @param extendedAddress the extended street address
   */
  void setExtendedAddress(String extendedAddress);

  /**
   * Get the latitude. Container support for this field is REQUIRED.
   *
   * @return latitude
   */
  Float getLatitude();

  /**
   * Set the latitude. Container support for this field is REQUIRED.
   *
   * @param latitude latitude
   */
  void setLatitude(Float latitude);

  /**
   * Get the locality. Container support for this field is REQUIRED.
   *
   * @return the locality
   */
  String getLocality();

  /**
   * Set the locality. Container support for this field is REQUIRED.
   *
   * @param locality the locality
   */
  void setLocality(String locality);

  /**
   * Get the longitude of the address in degrees. Container support for this field is REQUIRED.
   *
   * @return the longitude of the address in degrees
   */
  Float getLongitude();

  /**
   * Set the longitude of the address in degrees. Container support for this field is REQUIRED.
   *
   * @param longitude the longitude of the address in degrees.
   */
  void setLongitude(Float longitude);

  /**
   * Get the P O box. Container support for this field is REQUIRED.
   *
   * @return the PO box
   */
  String getPoBox();

  /**
   * Set the PO box. Container support for this field is REQUIRED.
   *
   * @param poBox the PO box
   */
  void setPoBox(String poBox);

  /**
   * Get the Postal code for the address. Container support for this field is REQUIRED.
   *
   * @return the postal code for the address
   */
  String getPostalCode();

  /**
   * Set the postal code for the address. Container support for this field is REQUIRED.
   *
   * @param postalCode the postal code
   */
  void setPostalCode(String postalCode);

  /**
   * Get the region. Container support for this field is REQUIRED.
   *
   * @return the region
   */
  String getRegion();

  /**
   * Set the region. Container support for this field is REQUIRED.
   *
   * @param region the region
   */
  void setRegion(String region);

  /**
   * Get the street address. Container support for this field is REQUIRED.
   *
   * @return the street address
   */
  String getStreetAddress();

  /**
   * Set the street address. Container support for this field is REQUIRED.
   *
   * @param streetAddress the street address
   */
  void setStreetAddress(String streetAddress);

  /**
   * Get the type of label of the address. Container support for this field is REQUIRED.
   *
   * @return the type or label of the address
   */
  String getType();

  /**
   * Get the type of label of the address. Container support for this field is REQUIRED.
   *
   * @param type the type of label of the address.
   */
  void setType(String type);

  /**
   * Get the unstructured address. If the container does not have structured addresses in its data
   * store, this field contains the unstructured address that the user entered, specified as a
   * string. Container support for this field is REQUIRED.
   *
   * @return
   */
  String getUnstructuredAddress();

  /**
   * Set the unstructured address. If the container does not have structured addresses in its data
   * store, this field contains the unstructured address that the user entered, specified as a
   * string. Container support for this field is REQUIRED.
   *
   * @param unstructuredAddress the unstructured address
   */
  void setUnstructuredAddress(String unstructuredAddress);
}

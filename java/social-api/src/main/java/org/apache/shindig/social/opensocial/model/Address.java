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
package org.apache.shindig.social.opensocial.model;

import java.util.EnumSet;
import java.util.Map;

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.AddressImpl;

import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.google.inject.ImplementedBy;

/**
 * Base interface for all address objects
 * see <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Address">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Address</a>.
 */

@ImplementedBy(AddressImpl.class)
@Exportablebean
public interface Address {

  /**
   * The fields that represent the address object in json form.
   */
  public static enum Field {
    /** the field name for country. */
    COUNTRY("country"),
    /** the field name for latitude. */
    LATITUDE("latitude"),
    /** the field name for locality. */
    LOCALITY("locality"),
    /** the field name for longitude. */
    LONGITUDE("longitude"),
    /** the field name for postalCode. */
    POSTAL_CODE("postalCode"),
    /** the field name for region. */
    REGION("region"),
    /** the feild name for streetAddress this field may be multiple lines. */
    STREET_ADDRESS("streetAddress"),
    /** the field name for type. */
    TYPE("type"),
    /** the field name for formatted. */
    FORMATTED("formatted"),
    /** the field name for primary. */
    PRIMARY("primary");

    private static final Map<String, Field> LOOKUP = Maps.uniqueIndex(EnumSet.allOf(Field.class),
        Functions.toStringFunction());

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

    public static Field getField(String jsonString) {
      return LOOKUP.get(jsonString);
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

  /**
   * <p>
   * Get a Boolean value indicating whether this instance of the Plural Field is the primary or
   * preferred value of for this field, e.g. the preferred mailing address. Service Providers MUST
   * NOT mark more than one instance of the same Plural Field as primary="true", and MAY choose not
   * to mark any fields as primary, if this information is not available. Introduced in v0.8.1
   * </p><p>
   * The service provider may wish to share the address instance between items and primary related
   * to the address from which this came, so if the address came from an Organization, primary
   * relates to the primary address of the organization, and not necessary the primary address of
   * all addresses.
   * </p><p>
   * If the address is not part of a list (eg Person.location ) primary has no meaning.
   * <p>
   * @return true if the instance if the primary instance.
   */
  Boolean getPrimary();

  /**
   * @see  org.apache.shindig.social.opensocial.model.Address#getPrimary()
   * @param primary set the Primary status of this Address.
   */
  void setPrimary(Boolean primary);
}

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

import org.apache.shindig.protocol.model.Exportablebean;
import org.apache.shindig.social.core.model.NameImpl;

import com.google.inject.ImplementedBy;

/**
 * Base interface for all name objects.
 * see
 * <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Name">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Name</a>
 */

@ImplementedBy(NameImpl.class)
@Exportablebean
public interface Name {

  /**
   * An enumeration of fields in the json name object.
   */
  public static enum Field {
    /**
     * The additional name.
     */
    ADDITIONAL_NAME("additionalName"),
    /**
     * The family name.
     */
    FAMILY_NAME("familyName"),
    /**
     * The given name.
     */
    GIVEN_NAME("givenName"),
    /**
     * The honorific prefix.
     */
    HONORIFIC_PREFIX("honorificPrefix"),
    /**
     * The honorific suffix.
     */
    HONORIFIC_SUFFIX("honorificSuffix"),
    /**
     * The formatted name.
     */
    FORMATTED("formatted");

    /**
     * the json key for this field.
     */
    private final String jsonString;

    /**
     * Construct the a field enum.
     * @param jsonString the json key for the field.
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * @return the name, formatted.
   */
  String getFormatted();

  /**
   * set the name formatted.
   * @param formatted the name, formatted.
   */
  void setFormatted(String formatted);

  /**
   * @return get the additional name.
   */
  String getAdditionalName();

  /**
   * @param additionalName set the additional name.
   */
  void setAdditionalName(String additionalName);

  /**
   * @return the family name.
   */
  String getFamilyName();

  /**
   * @param familyName the family name being set.
   */
  void setFamilyName(String familyName);

  /**
   * @return the given name.
   */
  String getGivenName();

  /**
   * @param givenName the given name to be set.
   */
  void setGivenName(String givenName);

  /**
   * @return the honorific prefix.
   */
  String getHonorificPrefix();

  /**
   * @param honorificPrefix the honorific prefix to be set.
   */
  void setHonorificPrefix(String honorificPrefix);

  /**
   * @return the honorific suffix.
   */
  String getHonorificSuffix();

  /**
   * @param honorificSuffix the honorific suffix to set.
   */
  void setHonorificSuffix(String honorificSuffix);
}

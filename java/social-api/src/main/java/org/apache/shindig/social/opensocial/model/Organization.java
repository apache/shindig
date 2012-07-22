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
import org.apache.shindig.social.core.model.OrganizationImpl;

import com.google.inject.ImplementedBy;

import java.util.Date;

/**
 * Describes a current or past organizational affiliation of this contact. Service Providers that
 * support only a single Company Name and Job Title field should represent them with a single
 * organization element with name and title properties, respectively.
 *
 * see <a href="http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Organization">
 * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/opensocial-reference#opensocial.Organization</a>
 */

@ImplementedBy(OrganizationImpl.class)
@Exportablebean
public interface Organization {

  /**
   * An Enumeration of field names for Organization.
   */
  public static enum Field {
    /** the name of the address field. */
    ADDRESS("address"),
    /** the name of the description field. */
    DESCRIPTION("description"),
    /** the name of the endDate field. */
    END_DATE("endDate"),
    /** the name of the field field. */
    FIELD("field"),
    /** the name of the name field. */
    NAME("name"),
    /** the name of the salary field. */
    SALARY("salary"),
    /** the name of the startDate field. */
    START_DATE("startDate"),
    /** the name of the subField field. */
    SUB_FIELD("subField"),
    /** the name of the title field. */
    TITLE("title"),
    /** the name of the webpage field. */
    WEBPAGE("webpage"),
    /**
     * the name of the type field, Should have the value of "job" or "school" to be put in the right
     * js fields.
     */
    TYPE("type"),
    /** the name of the primary field. */
    PRIMARY("primary");

    /**
     * the name of this field.
     */
    private final String jsonString;

    /**
     * Construct a field based on the name of the field.
     *
     * @param jsonString the name of the field
     */
    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    /**
     * @return a string representation of the enum.
     */
    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  /**
   * Get the address of the organization, specified as an Address. Container support for this field
   * is OPTIONAL.
   *
   * @return the Address of the organization
   */
  Address getAddress();

  /**
   * Set the address of the organization, specified as an Address. Container support for this field
   * is OPTIONAL.
   *
   * @param address the address of the organization
   */
  void setAddress(Address address);

  /**
   * Get a description or notes about the person's work in the organization, specified as a string.
   * This could be the courses taken by a student, or a more detailed description about a
   * Organization role. Container support for this field is OPTIONAL.
   *
   * @return a description about the persons work in the organization
   */
  String getDescription();

  /**
   * Set a description or notes about the person's work in the organization, specified as a string.
   * This could be the courses taken by a student, or a more detailed description about a
   * Organization role. Container support for this field is OPTIONAL.
   *
   * @param description a description about the persons work in the organization
   */
  void setDescription(String description);

  /**
   * Get the date the person stopped at the organization, specified as a Date. A null date indicates
   * that the person is still involved with the organization. Container support for this field is
   * OPTIONAL.
   *
   * @return the date the person stopped at the organization
   */
  Date getEndDate();

  /**
   * Set the date the person stopped at the organization, specified as a Date. A null date indicates
   * that the person is still involved with the organization. Container support for this field is
   * OPTIONAL.
   *
   * @param endDate the date the person stopped at the organization
   */
  void setEndDate(Date endDate);

  /**
   * Get the field the organization is in, specified as a string. This could be the degree pursued
   * if the organization is a school. Container support for this field is OPTIONAL.
   *
   * @return the field the organization is in
   */
  String getField();

  /**
   * Set the field the organization is in, specified as a string. This could be the degree pursued
   * if the organization is a school. Container support for this field is OPTIONAL.
   *
   * @param field the field the organization is in
   */
  void setField(String field);

  /**
   * Get the name of the organization, specified as a string. For example, could be a school name or
   * a job company. Container support for this field is OPTIONAL.
   *
   * @return the name of the organization
   */
  String getName();

  /**
   * Set the name of the organization, specified as a string. For example, could be a school name or
   * a job company. Container support for this field is OPTIONAL.
   *
   * @param name the name of the organization
   */
  void setName(String name);

  /**
   * Get the salary the person receives from the organization, specified as a string. Container
   * support for this field is OPTIONAL.
   *
   * @return the salary the person receives
   */
  String getSalary();

  /**
   * Set the salary the person receives from the organization, specified as a string. Container
   * support for this field is OPTIONAL.
   *
   * @param salary the salary the person receives
   */
  void setSalary(String salary);

  /**
   * Get the date the person started at the organization, specified as a Date. Container support for
   * this field is OPTIONAL.
   *
   * @return the start date at the organization
   */
  Date getStartDate();

  /**
   * Set the date the person started at the organization, specified as a Date. Container support for
   * this field is OPTIONAL.
   *
   * @param startDate the start date at the organization
   */
  void setStartDate(Date startDate);

  /**
   * Get the subfield the Organization is in, specified as a string. Container support for this
   * field is OPTIONAL.
   *
   * @return the subfield the Organization is in
   */
  String getSubField();

  /**
   * Set the subfield the Organization is in, specified as a string. Container support for this
   * field is OPTIONAL.
   *
   * @param subField the subfield the Organization is in
   */
  void setSubField(String subField);

  /**
   * Get the title or role the person has in the organization, specified as a string. This could be
   * graduate student, or software engineer. Container support for this field is OPTIONAL.
   *
   * @return the title or role the person has in the organization
   */
  String getTitle();

  /**
   * Set the title or role the person has in the organization, specified as a string. This could be
   * graduate student, or software engineer. Container support for this field is OPTIONAL.
   *
   * @param title the title or role the person has in the organization
   */
  void setTitle(String title);

  /**
   * Get a webpage related to the organization, specified as a string. Container support for this
   * field is OPTIONAL.
   *
   * @return the URL of a webpage related to the organization
   */
  String getWebpage();

  /**
   * Get a webpage related to the organization, specified as a string. Container support for this
   * field is OPTIONAL.
   *
   * @param webpage the URL of a webpage related to the organization
   */
  void setWebpage(String webpage);

  /**
   * Get the type of field for this instance, usually used to label the preferred function of the
   * given contact information. The type of organization, with Canonical Values <em>job</em> and
   * <em>school</em>.
   *
   * @return the type of the field
   */
  String getType();

  /**
   * Set the type of field for this instance, usually used to label the preferred function of the
   * given contact information. The type of organization, with Canonical Values <em>job</em> and
   * <em>school</em>.
   *
   * @param type the type of the field
   */
  void setType(String type);

  /**
   * Get Boolean value indicating whether this instance of the Plural Field is the primary or
   * preferred Organization.
   *
   * @return true if this is a primary or preferred value
   */
  Boolean getPrimary();

  /**
   * Set Boolean value indicating whether this instance of the Plural Field is the primary or
   * preferred Organization.
   *
   * @param primary true if this is a primary or preferred value
   */
  void setPrimary(Boolean primary);

}

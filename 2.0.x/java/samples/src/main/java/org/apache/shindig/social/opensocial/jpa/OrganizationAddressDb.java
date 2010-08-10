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

import org.apache.shindig.social.opensocial.model.Organization;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * This object connects to a single Address, and to a single organization, defining the
 * organizations relationship with the address. The class extends AddressDb, which stores itself in
 * the address table. The specialization of this class is stored in organization_address and joined
 * on the objectId property (oid column). Records are discriminated using the address_usage column
 * in the address table and set to the value 'sharedaddress' (i.e. no discrimination) indicating
 * that the address is shared.
 */
// TODO, uncertain about the mapping of this, oid <-> oid means one to one, and this is only
// associated with a single
// Organization. IMHO, we should be mapping organizational_address.address_id to address.oid, but
// need to think about this.
@Entity
@Table(name = "organizational_address")
@DiscriminatorValue(value = "sharedaddress")
// this is the same as others since we want to share the data.
public class OrganizationAddressDb extends AddressDb {
  /**
   * Indicates this address is the primary address for the organization.
   */
  @Basic
  @Column(name = "primary_organization")
  private Boolean primary;

  /**
   * This address is associated with a single organization in this form.
   *
   */
  @OneToOne(targetEntity = OrganizationDb.class)
  @JoinColumn(name = "organization_id", referencedColumnName = "oid")
  private Organization organization;

  /**
   * the type of the address for the organization.
   *
   * @see org.apache.shindig.social.opensocial.model.Address
   */
  @Basic
  @Column(name = "type", length = 255)
  private String type;

  /**
   * Create an organizational address.
   */
  public OrganizationAddressDb() {
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.shindig.social.opensocial.jpa.AddressDb#getType()
   */
  public String getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.shindig.social.opensocial.jpa.AddressDb#setType(java.lang.String)
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.shindig.social.opensocial.jpa.AddressDb#getPrimary()
   */
  public Boolean getPrimary() {
    return primary;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.shindig.social.opensocial.jpa.AddressDb#setPrimary(java.lang.Boolean)
   */
  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  /**
   * The organization this address address relates to.
   *
   * @return the organization
   */
  public Organization getOrganization() {
    return organization;
  }

  /**
   * Set the organization this address relates to.
   *
   * @param organization the organization to set
   */
  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

}

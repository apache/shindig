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

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * This is a property of an application, extending the listfield type, and using the type property
 * to perform the mapping. Main storage is in the listfield table, but application property stores
 * the details of the properties of the application.
 */
@Entity
@Table(name = "application_property")
@PrimaryKeyJoinColumn(name = "oid")
public class ApplicationPropertyDb extends ListFieldDb {
  /**
   * The application  relationship connected with this property.
   */
  @ManyToOne(targetEntity = ApplicationDb.class)
  @JoinColumn(name = "application_id", referencedColumnName = "oid")
  protected ApplicationDb application;

  /**
   * @return the application
   */
  public ApplicationDb getApplication() {
    return application;
  }

  /**
   * @param application the application to set
   */
  public void setApplication(ApplicationDb application) {
    this.application = application;
  }

}

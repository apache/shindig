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

import org.apache.shindig.social.core.model.OrganizationImpl;

import com.google.inject.ImplementedBy;
import java.util.Date;

/**
 * see
 * http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Organization.Field.html
 *
 */

@ImplementedBy(OrganizationImpl.class)

public interface Organization {

  public static enum Field {
    ADDRESS("address"),
    DESCRIPTION("description"),
    END_DATE("endDate"),
    FIELD("field"),
    NAME("name"),
    SALARY("salary"),
    START_DATE("startDate"),
    SUB_FIELD("subField"),
    TITLE("title"),
    WEBPAGE("webpage"),
    /** Should have the value of "job" or "school" to be put in the right js fields */
    TYPE("type"),
    PRIMARY("primary");

    private final String jsonString;

    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  Address getAddress();

  void setAddress(Address address);

  String getDescription();

  void setDescription(String description);

  Date getEndDate();

  void setEndDate(Date endDate);

  String getField();

  void setField(String field);

  String getName();

  void setName(String name);

  String getSalary();

  void setSalary(String salary);

  Date getStartDate();

  void setStartDate(Date startDate);

  String getSubField();

  void setSubField(String subField);

  String getTitle();

  void setTitle(String title);

  String getWebpage();

  void setWebpage(String webpage);

  String getType();

  void setType(String type);

  Boolean getPrimary();

  void setPrimary(Boolean primary);

}

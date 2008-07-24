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

import org.apache.shindig.social.core.model.EmailImpl;

import com.google.inject.ImplementedBy;

/**
 * see http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Email.Field.html
 *
 */

@ImplementedBy(EmailImpl.class)
public interface Email {

  /**
   * The fields that represent the email object ion json form.
   */
  public static enum Field {
    /** the json field for address. */
    ADDRESS("address"),
    /** the json field for type. */
    TYPE("type");

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
   * The email address, specified as a string.
   *
   * @return email address
   */
  String getAddress();

  /**
   * The email address, specified as a string.
   *
   * @param address email address
   */
  void setAddress(String address);

  /**
   * The email type or label, specified as a string. Examples: work, my favorite store, my house,
   * etc.
   *
   * @return email type or label
   */
  String getType();

  /**
   * The email type or label, specified as a string. Examples: work, my favorite store, my house,
   * etc.
   *
   * @param type email type or label
   */
  void setType(String type);
}

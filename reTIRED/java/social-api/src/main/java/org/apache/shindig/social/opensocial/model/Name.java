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

import com.google.inject.ImplementedBy;

/**
 * see
 * http://code.google.com/apis/opensocial/docs/0.7/reference/opensocial.Name.Field.html
 *
 */

@ImplementedBy(NameImpl.class)

public interface Name {

  public static enum Field {
    ADDITIONAL_NAME("additionalName"),
    FAMILY_NAME("familyName"),
    GIVEN_NAME("givenName"),
    HONORIFIC_PREFIX("honorificPrefix"),
    HONORIFIC_SUFFIX("honorificSuffix"),
    UNSTRUCTURED("unstructured");

    private final String jsonString;

    private Field(String jsonString) {
      this.jsonString = jsonString;
    }

    @Override
    public String toString() {
      return this.jsonString;
    }
  }

  String getUnstructured();

  void setUnstructured(String unstructured);

  String getAdditionalName();

  void setAdditionalName(String additionalName);

  String getFamilyName();

  void setFamilyName(String familyName);

  String getGivenName();

  void setGivenName(String givenName);

  String getHonorificPrefix();

  void setHonorificPrefix(String honorificPrefix);

  String getHonorificSuffix();

  void setHonorificSuffix(String honorificSuffix);
}

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
package org.apache.shindig.social.opensocial.jpa.api;

import org.apache.shindig.protocol.model.FilterOperation;

/**
 * A FilterSpecifiation encapsulates the Filter operation for a specific field. Name of the target
 * property based on the operation.
 */
public class FilterSpecification {

  public static final String SPECIAL_OPERATION = "special operation";
  private String finalProperty;
  private FilterOperation[] filterOptions;
  private boolean special;

  /**
   * Create a FilterSpecification with a target final property name and a set of acceptable
   * operations.
   *
   * @param finalProperty the name of the final property on the class as used by JPQL
   * @param filterOptions an array of operations that may be applied to this property
   */
  public FilterSpecification(String finalProperty, FilterOperation[] filterOptions) {
    this.finalProperty = finalProperty;
    this.filterOptions = new FilterOperation[filterOptions.length];
    System.arraycopy(filterOptions, 0, this.filterOptions, 0, filterOptions.length);
    this.special = false;
  }

  /**
   * Create a default filter operation that operates on special terms, ie that is has no filter
   * mapping and is handled as a special case in the processing. Im this case the finalProperty is
   * set to a reserved value.
   */
  public FilterSpecification() {
    this.special = true;
  }

  /**
   * Convert the property into the final property.
   *
   * @param operation the operation that is being used.
   * @return returns the final property name, or null if the operation is not applicable
   */
  public String translateProperty(FilterOperation operation) {
    if (special) {
      return SPECIAL_OPERATION;
    } else {
      for (FilterOperation fo : filterOptions) {
        if (fo.equals(operation)) {
          return finalProperty;
        }
      }
      return null;
    }
  }

  /**
   * If the final property is special, then return true.
   * @param finalProp the final property
   * @return true if special
   */
  public static boolean isSpecial(String finalProp) {
    return SPECIAL_OPERATION.equals(finalProp);
  }

  /**
   * If the final property is valid, return true.
   * @param finalProp the final property
   * @return true if valid.
   */
  public static boolean isValid(String finalProp) {
    return (finalProp != null);
  }

}

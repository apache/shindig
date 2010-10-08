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
 * Specifies the ability to filter an object
 */
public interface FilterCapability {
  /**
   * Check to see if the property is filterable on an operation. The final property that is returned
   * must not be directly based on the fieldName passed in and must be suitable for direct use
   * within a JPQL statement. (ie don't trust the passed in parameter)
   * 
   * @param fieldName the field name that is being filtered, value is not to be trusted.
   * @param filterOperation the operation being applied to the field.
   * @return the final property that is being filtered or null is the filter operation specified is
   *         not applicable
   */
  String findFilterableProperty(String fieldName, FilterOperation filterOperation);
}

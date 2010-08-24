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
package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.junit.Test;

public class GadgetHandlerServiceTest extends EasyMockTestCase {

  // Next test verify that the API data classes are configured correctly.
  // The mapping is done using reflection in runtime, so this test verify mapping is complete
  // this test will prevent from not intended change to the API.
  // DO NOT REMOVE TEST
  @Test
  public void testHandlerDataDelegation() throws Exception {
    BeanDelegator delegator = new BeanDelegator(
        GadgetsHandlerService.apiClasses, GadgetsHandlerService.enumConversionMap);
    delegator.validate();
  }
}

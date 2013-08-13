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
package org.apache.shindig.gadgets.oauth;

import junitx.extensions.EqualsHashCodeTestCase;
import static junitx.framework.Assert.assertNotEquals;

public class BasicOAuthStoreTokenIndexTest extends EqualsHashCodeTestCase {
  public BasicOAuthStoreTokenIndexTest() { super("TestHashCodeEquals");}

  protected Object createInstance() throws Exception {
    BasicOAuthStoreTokenIndex eq =  new BasicOAuthStoreTokenIndex();
    eq.setGadgetUri("http://www.example.com/foo");
    eq.setModuleId(100000000);
    eq.setServiceName("test");
    eq.setUserId("abc");
    return eq;
  }

  protected Object createNotEqualInstance() throws Exception {
    return new BasicOAuthStoreTokenIndex();
  }


  public void testHashCode() {
    BasicOAuthStoreTokenIndex eq1 = new BasicOAuthStoreTokenIndex();
    BasicOAuthStoreTokenIndex eq2 = new BasicOAuthStoreTokenIndex();

    // just be sure that our new hashcode method works
    eq1.setModuleId(100);
    eq2.setModuleId(200);
    assertNotEquals(eq1.hashCode(), eq2.hashCode());

  }
}

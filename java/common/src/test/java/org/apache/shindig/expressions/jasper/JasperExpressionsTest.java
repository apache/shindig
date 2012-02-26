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
package org.apache.shindig.expressions.jasper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.ExpressionsTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JasperExpressionsTest  extends ExpressionsTest{

  @Before
  @Override
  public void setUp() {
    super.setUp();
    expressions = new Expressions(null, null, new JasperTypeConverter(), new JasperProvider());
  }

  @Ignore
  @Test
  public void booleanCoercionOfStringsFails() throws Exception{
    // Case-sensitive coercion:  FALSE is true
    // Test fails because Jasper type conversion routines does not recognize FALSE.
    addVariable("bool", "FALSE");
    assertFalse(evaluate("${!bool}", Boolean.class));

    // Jasper cannot handle this
    addVariable("bool", "booga");
    assertFalse(evaluate("${!bool}", Boolean.class));
  }

  @Ignore
  @Test
  public void booleanCoercionOfNumbersFails() throws Exception {
    // These test cases will not pass with Jasper due to ELSupport exceptions
    // thrown when coercing Integer to Boolean
    addVariable("bool", 0);
    assertTrue(evaluate("${!bool}", Boolean.class));

    addVariable("bool", 1);
    assertFalse(evaluate("${!bool}", Boolean.class));

    evaluate("${true && 5}", String.class);
  }

}

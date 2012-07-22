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
package org.apache.shindig.gadgets.oauth2.persistence.sample;

import org.apache.shindig.gadgets.oauth2.MockUtils;
import org.junit.Assert;
import org.junit.Test;

public class OAuth2GadgetBindingTest extends MockUtils {
  private static final OAuth2GadgetBinding FIXTURE = new OAuth2GadgetBinding(MockUtils.GADGET_URI1,
      MockUtils.SERVICE_NAME, "xxx", true);

  @Test
  public void testOAuth2GadgetBinding_1() throws Exception {
    final OAuth2GadgetBinding result = new OAuth2GadgetBinding("xxx", "yyy", "zzz", false);

    Assert.assertNotNull(result);
    Assert.assertEquals("zzz", result.getClientName());
    Assert.assertEquals("yyy", result.getGadgetServiceName());
    Assert.assertEquals("xxx", result.getGadgetUri());
    Assert.assertEquals(false, result.isAllowOverride());
  }

  @Test
  public void testEquals_1() throws Exception {
    final Object obj = new OAuth2GadgetBinding(MockUtils.GADGET_URI1, MockUtils.SERVICE_NAME,
        "xxx", true);

    final boolean result = OAuth2GadgetBindingTest.FIXTURE.equals(obj);

    Assert.assertEquals(true, result);
  }

  @Test
  public void testEquals_2() throws Exception {
    final Object obj = new Object();

    final boolean result = OAuth2GadgetBindingTest.FIXTURE.equals(obj);

    Assert.assertEquals(false, result);
  }

  @Test
  public void testEquals_3() throws Exception {
    final boolean result = OAuth2GadgetBindingTest.FIXTURE.equals(null);

    Assert.assertEquals(false, result);
  }

  @Test
  public void testGetClientName_1() throws Exception {
    final String result = OAuth2GadgetBindingTest.FIXTURE.getClientName();

    Assert.assertEquals("xxx", result);
  }

  @Test
  public void testGetGadgetServiceName_1() throws Exception {
    final String result = OAuth2GadgetBindingTest.FIXTURE.getGadgetServiceName();

    Assert.assertEquals(MockUtils.SERVICE_NAME, result);
  }

  @Test
  public void testGetGadgetUri_1() throws Exception {
    final String result = OAuth2GadgetBindingTest.FIXTURE.getGadgetUri();

    Assert.assertEquals(MockUtils.GADGET_URI1, result);
  }

  @Test
  public void testHashCode_1() throws Exception {
    final int result = OAuth2GadgetBindingTest.FIXTURE.hashCode();

    Assert.assertEquals(-1901114596, result);
  }

  @Test
  public void testIsAllowOverride_1() throws Exception {
    final boolean result = OAuth2GadgetBindingTest.FIXTURE.isAllowOverride();

    Assert.assertEquals(true, result);
  }

  @Test
  public void testIsAllowOverride_2() throws Exception {
    final boolean result = OAuth2GadgetBindingTest.FIXTURE.isAllowOverride();

    Assert.assertEquals(true, result);
  }

  @Test
  public void testToString_1() throws Exception {
    final String result = OAuth2GadgetBindingTest.FIXTURE.toString();

    Assert.assertEquals(
        "org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2GadgetBinding: gadgetUri = "
            + MockUtils.GADGET_URI1 + " , gadgetServiceName = " + MockUtils.SERVICE_NAME
            + " , allowOverride = true", result);
  }
}

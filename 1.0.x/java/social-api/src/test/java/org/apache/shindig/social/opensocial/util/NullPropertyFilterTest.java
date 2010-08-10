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
package org.apache.shindig.social.opensocial.util;

import net.sf.json.JSONArray;
import net.sf.json.util.PropertyFilter;

import org.apache.shindig.social.core.util.NullPropertyFilter;

import com.google.common.collect.Lists;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

/**
 * Test the NullPropertyFilter.
 */
public class NullPropertyFilterTest {
  /**
   * perform the test
   */
  @Test
  public void testApply() {
    PropertyFilter pf = new NullPropertyFilter();
    Assert.assertTrue(pf.apply(null, null, null));
    JSONArray jsa = new JSONArray();
    Assert.assertTrue(pf.apply(null, null, jsa));
    jsa.add("element");
    Assert.assertFalse(pf.apply(null, null, jsa));
    Assert.assertTrue(pf.apply(null, null, Lists.newArrayList()));
    Assert.assertFalse(pf.apply(null, null, Lists.newArrayList("element", "element")));
    Assert.assertTrue(pf.apply(null, null, new Object[] {}));
    Assert.assertFalse(pf.apply(null, null, new Object[] { "element" }));
  }
}

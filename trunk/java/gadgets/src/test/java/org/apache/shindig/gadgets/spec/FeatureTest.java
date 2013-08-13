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
package org.apache.shindig.gadgets.spec;

import java.util.Set;

import org.apache.shindig.common.xml.XmlUtil;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class FeatureTest extends Assert {
  @Test
  public void testRequire() throws Exception {
    String xml = "<Require feature=\"foo\"/>";
    Feature feature = new Feature(XmlUtil.parse(xml));
    assertEquals("foo", feature.getName());
    assertTrue(feature.getRequired());
  }

  @Test
  public void testOptional() throws Exception {
    String xml = "<Optional feature=\"foo\"/>";
    Feature feature = new Feature(XmlUtil.parse(xml));
    assertEquals("foo", feature.getName());
    assertFalse(feature.getRequired());
  }

  @Test
  public void testParams() throws Exception {
    String key = "bar";
    String value = "Hello, World!";
    String xml = "<Require feature=\"foo\">" +
                 "  <Param name=\"" + key + "\">" + value + "</Param>" +
                 "</Require>";
    Feature feature = new Feature(XmlUtil.parse(xml));
    Multimap<String, String> params = feature.getParams();
    assertEquals(1, params.size());
    assertEquals(ImmutableList.of(value), params.get(key));
  }

  @Test
  public void testMultiParams() throws Exception {
    String key = "bar";
    String key2 = "bar2";
    String value = "Hello, World!";
    String value2 = "Goodbye, World!";
    // Verify that multiple parameters are supported, and are returned in-order
    String xml = "<Require feature=\"foo\">" +
                 "  <Param name=\"" + key + "\">" + value + "</Param>" +
                 "  <Param name=\"" + key + "\">" + value2 + "</Param>" +
                 "  <Param name=\"" + key2 + "\">" + value2 + "</Param>" +
                 "  <Param name=\"" + key2 + "\">" + value + "</Param>" +
                 "</Require>";
    Feature feature = new Feature(XmlUtil.parse(xml));
    Multimap<String, String> params = feature.getParams();
    assertEquals(2, params.keySet().size());
    assertEquals(ImmutableList.of(value, value2), params.get(key));
    assertEquals(value, feature.getParam(key));
    assertEquals(ImmutableList.of(value2, value), params.get(key2));
    assertEquals(value2, feature.getParam(key2));
    assertTrue(params.get("foobar").isEmpty());
    assertNull(feature.getParam("foobar"));
  }


  @Test
  public void testViews() throws Exception {
    String xml = "<Require feature=\"foo\" views=\"view1\">" +
                 "</Require>";
    Feature feature = new Feature(XmlUtil.parse(xml));
    Set<String> views = feature.getViews();
    assertTrue(views.size() == 1);
    assertTrue(views.contains("view1"));

    xml = "<Require feature=\"foo\" views=\"view1, view2\">" +
    "</Require>";
		feature = new Feature(XmlUtil.parse(xml));
		views = feature.getViews();
		assertTrue(views.size() == 2);
		assertTrue(views.contains("view1"));
		assertTrue(views.contains("view2"));

    xml = "<Require feature=\"foo\">" +
    "</Require>";
		feature = new Feature(XmlUtil.parse(xml));
		views = feature.getViews();
		assertTrue(views != null);
		assertTrue(views.size() == 0);
  }

  @Test(expected=SpecParserException.class)
  public void testDoesNotLikeUnnamedFeatures() throws Exception {
    String xml = "<Require/>";
    new Feature(XmlUtil.parse(xml));
  }

  @Test(expected=SpecParserException.class)
  public void testEnforceParamNames() throws Exception {
    String xml = "<Require feature=\"foo\"><Param>Test</Param></Require>";
    new Feature(XmlUtil.parse(xml));
  }
}

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
package org.apache.shindig.gadgets.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.shindig.gadgets.admin.FeatureAdminData.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.caja.util.Sets;
import com.google.common.base.Objects;

/**
 * Tests for feature admin data.
 *
 * @version $Id: $
 */
public class FeatureAdminDataTest {

  private static final String VIEWS = "views";
  private static final String SETPREFS = "setprefs";
  private static final String TABS = "tabs";
  private static final String EE = "embedded-experiences";
  private static final String SELECTION = "selection";
  private Set<String> blacklist;
  private Set<String> whitelist;
  private FeatureAdminData whitelistData;
  private FeatureAdminData blacklistData;
  private FeatureAdminData nullData;
  private FeatureAdminData defaultData;

  @Before
  public void setUp() throws Exception {
    whitelist = Sets.newHashSet();
    whitelist.add(VIEWS);
    whitelist.add(SETPREFS);
    whitelist.add(TABS);

    blacklist = Sets.newHashSet();
    blacklist.add(EE);
    blacklist.add(SELECTION);

    whitelistData = new FeatureAdminData(whitelist, Type.WHITELIST);
    blacklistData = new FeatureAdminData(blacklist, Type.BLACKLIST);
    nullData = new FeatureAdminData(null, null);
    defaultData = new FeatureAdminData();
  }

  @After
  public void tearDown() throws Exception {
    whitelist = null;
    blacklist = null;
    whitelistData = null;
    blacklistData = null;
    nullData = null;
    defaultData = null;
  }

  private void validateDefaultFeatures() {
    assertEquals(whitelist, whitelistData.getFeatures());
    assertEquals(blacklist, blacklistData.getFeatures());
    assertEquals(Sets.newHashSet(), nullData.getFeatures());
    assertEquals(Sets.newHashSet(), defaultData.getFeatures());
  }

  @Test
  public void testGetFeatures() {
    validateDefaultFeatures();
  }

  @Test
  public void testAddFeatures() {
    validateDefaultFeatures();

    Set<String> toAdd = Sets.newHashSet("foo", "bar", null);
    whitelistData.addFeatures(toAdd);
    blacklistData.addFeatures(toAdd);
    nullData.addFeatures(toAdd);
    defaultData.addFeatures(toAdd);

    Set<String> actuallyAdded = Sets.newHashSet("foo", "bar");
    whitelist.addAll(actuallyAdded);
    blacklist.addAll(actuallyAdded);
    assertEquals(whitelist, whitelistData.getFeatures());
    assertEquals(blacklist, blacklistData.getFeatures());
    assertEquals(actuallyAdded, nullData.getFeatures());
    assertEquals(actuallyAdded, defaultData.getFeatures());
  }

  @Test
  public void testAddFeature() {
    validateDefaultFeatures();

    whitelistData.addFeature("foo");
    blacklistData.addFeature("foo");
    nullData.addFeature("foo");
    defaultData.addFeature("foo");
    defaultData.addFeature(null);

    whitelist.add("foo");
    blacklist.add("foo");
    assertEquals(whitelist, whitelistData.getFeatures());
    assertEquals(blacklist, blacklistData.getFeatures());
    assertEquals(Sets.newHashSet("foo"), nullData.getFeatures());
    assertEquals(Sets.newHashSet("foo"), defaultData.getFeatures());
  }

  @Test
  public void testClearFeatures() {
    validateDefaultFeatures();

    whitelistData.clearFeatures();
    blacklistData.clearFeatures();
    nullData.clearFeatures();
    defaultData.clearFeatures();

    assertEquals(Sets.newHashSet(), whitelistData.getFeatures());
    assertEquals(Sets.newHashSet(), blacklistData.getFeatures());
    assertEquals(Sets.newHashSet(), nullData.getFeatures());
    assertEquals(Sets.newHashSet(), defaultData.getFeatures());
  }

  @Test
  public void testRemoveFeatures() {
    validateDefaultFeatures();

    Set<String> toRemoveWhitelist = Sets.newHashSet(TABS, VIEWS);
    Set<String> toRemoveBlacklist = Sets.newHashSet(EE);
    whitelistData.removeFeatures(toRemoveWhitelist);
    blacklistData.removeFeatures(toRemoveBlacklist);
    nullData.removeFeatures(toRemoveWhitelist);
    defaultData.removeFeatures(toRemoveWhitelist);

    assertEquals(Sets.newHashSet(SETPREFS), whitelistData.getFeatures());
    assertEquals(Sets.newHashSet(SELECTION), blacklistData.getFeatures());
    assertEquals(Sets.newHashSet(), nullData.getFeatures());
    assertEquals(Sets.newHashSet(), defaultData.getFeatures());
  }

  @Test
  public void testRemoveFeature() {
    validateDefaultFeatures();

    whitelistData.removeFeature(TABS);
    blacklistData.removeFeature(SELECTION);
    nullData.removeFeature(TABS);
    defaultData.removeFeature(TABS);

    assertEquals(Sets.newHashSet(SETPREFS, VIEWS), whitelistData.getFeatures());
    assertEquals(Sets.newHashSet(EE), blacklistData.getFeatures());
    assertEquals(Sets.newHashSet(), nullData.getFeatures());
    assertEquals(Sets.newHashSet(), defaultData.getFeatures());
  }

  @Test
  public void testGetPriority() {
    assertEquals(Type.WHITELIST, whitelistData.getType());
    assertEquals(Type.BLACKLIST, blacklistData.getType());
    assertEquals(Type.WHITELIST, nullData.getType());
    assertEquals(Type.WHITELIST, defaultData.getType());
  }

  @Test
  public void testSetPriority() {
    whitelistData.setType(Type.BLACKLIST);
    blacklistData.setType(Type.WHITELIST);
    nullData.setType(Type.BLACKLIST);
    defaultData.setType(Type.BLACKLIST);

    assertEquals(Type.BLACKLIST, whitelistData.getType());
    assertEquals(Type.BLACKLIST, nullData.getType());
    assertEquals(Type.BLACKLIST, defaultData.getType());
    assertEquals(Type.WHITELIST, blacklistData.getType());

    nullData.setType(null);
    assertEquals(Type.WHITELIST, nullData.getType());
  }

  @Test
  public void testEquals() {
    assertTrue(whitelistData.equals(new FeatureAdminData(whitelist, Type.WHITELIST)));
    assertFalse(whitelistData.equals(new FeatureAdminData(whitelist,Type.BLACKLIST)));
    assertFalse(whitelistData.equals(new FeatureAdminData(new HashSet<String>(),
            Type.WHITELIST)));
    assertFalse(whitelistData.equals(new FeatureAdminData(Sets.newHashSet(EE), Type.WHITELIST)));
    assertFalse(whitelistData.equals(null));
    assertTrue(blacklistData.equals(new FeatureAdminData(blacklist, Type.BLACKLIST)));
    assertTrue(nullData.equals(defaultData));
    assertFalse(nullData.equals(whitelistData));
  }

  @Test
  public void testHashCode() {
    assertEquals(Objects.hashCode(this.whitelist, Type.WHITELIST), whitelistData.hashCode());
    assertEquals(Objects.hashCode(this.blacklist, Type.BLACKLIST), blacklistData.hashCode());
    assertEquals(Objects.hashCode(Sets.newHashSet(), Type.WHITELIST),
            nullData.hashCode());
    assertEquals(Objects.hashCode(Sets.newHashSet(), Type.WHITELIST),
            defaultData.hashCode());
    assertFalse(Objects.hashCode(this.blacklist, Type.WHITELIST) == whitelistData
            .hashCode());
  }
}

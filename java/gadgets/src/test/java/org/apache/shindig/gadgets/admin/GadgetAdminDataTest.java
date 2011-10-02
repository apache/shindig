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

import java.util.Set;

import org.apache.shindig.gadgets.admin.FeatureAdminData.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.caja.util.Sets;
import com.google.common.base.Objects;

/**
 * @version $Id: 3.0.0
 */
public class GadgetAdminDataTest {

  private static final String VIEWS = "views";
  private static final String SETPREFS = "setprefs";
  private static final String TABS = "tabs";
  private static final String EE = "embedded-experiences";
  private static final String SELECTION = "selection";
  private Set<String> whitelist;
  private Set<String> blacklist;
  private FeatureAdminData whitelistFeatures;
  private FeatureAdminData blacklistFeatures;
  private GadgetAdminData whitelistInfo;
  private GadgetAdminData blacklistInfo;
  private GadgetAdminData nullInfo;
  private GadgetAdminData defaultInfo;

  @Before
  public void setUp() throws Exception {
    whitelist = Sets.newHashSet(VIEWS, SETPREFS, TABS);
    blacklist = Sets.newHashSet(EE, SELECTION);
    whitelistFeatures = new FeatureAdminData(whitelist, Type.WHITELIST);
    blacklistFeatures = new FeatureAdminData(blacklist, Type.BLACKLIST);
    whitelistInfo = new GadgetAdminData(whitelistFeatures);
    blacklistInfo = new GadgetAdminData(blacklistFeatures);
    nullInfo = new GadgetAdminData(null);
    defaultInfo = new GadgetAdminData();
  }

  @After
  public void tearDown() throws Exception {
    whitelist = null;
    whitelistInfo = null;
    blacklistInfo = null;
    whitelistFeatures = null;
    blacklistFeatures = null;
    nullInfo = null;
    defaultInfo = null;
  }

  @Test
  public void testGetFeatureAdminData() {
    assertEquals(whitelistFeatures, whitelistInfo.getFeatureAdminData());
    assertEquals(blacklistFeatures, blacklistInfo.getFeatureAdminData());
    assertEquals(new FeatureAdminData(), nullInfo.getFeatureAdminData());
    assertEquals(new FeatureAdminData(), defaultInfo.getFeatureAdminData());
  }

  @Test
  public void testEquals() {
    assertTrue(whitelistInfo.equals(new GadgetAdminData(whitelistFeatures)));
    assertTrue(nullInfo.equals(new GadgetAdminData(null)));
    assertTrue(defaultInfo.equals(new GadgetAdminData()));
    assertTrue(nullInfo.equals(defaultInfo));
    assertFalse(whitelistInfo.equals(null));
    assertFalse(whitelistInfo.equals(new Object()));
    assertFalse(whitelistInfo.equals(blacklistInfo));
    assertFalse(whitelistInfo.equals(defaultInfo));
    assertFalse(whitelistInfo.equals(nullInfo));
  }

  @Test
  public void testHashCode() {
    assertEquals(Objects.hashCode(whitelistFeatures), whitelistInfo.hashCode());
    assertEquals(Objects.hashCode(blacklistFeatures), blacklistInfo.hashCode());
    assertEquals(Objects.hashCode(new FeatureAdminData()), nullInfo.hashCode());
    assertEquals(Objects.hashCode(new FeatureAdminData()), defaultInfo.hashCode());
    assertEquals(nullInfo.hashCode(), defaultInfo.hashCode());
    assertFalse(blacklistInfo.hashCode() == whitelistInfo.hashCode());
  }

}

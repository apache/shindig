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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.shindig.gadgets.admin.FeatureAdminData.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.caja.util.Sets;
import com.google.common.base.Objects;
import com.google.common.collect.Maps;

/**
 * @since 2.5.0
 */
public class ContainerAdminDataTest {

  private static final String VIEWS = "views";
  private static final String SETPREFS = "setprefs";
  private static final String TABS = "tabs";
  private static final String EE = "embedded-experiences";
  private static final String SELECTION = "selection";
  private static final String GADGET_URL_1 = "http://sample.com/gadget1.xml";
  private static final String GADGET_URL_1_WITH_PORT = "http://sample.com:80/gadget1.xml";
  private static final String GADGET_URL_2 = "http://sample.com/gadget2.xml";
  private static final String GADGET_URL_3 = "http://example.com/*";
  private static final String GADGET_URL_4 = "https://sample.com/gadget1.xml";
  private static final String GADGET_URL_4_WITH_PORT = "https://sample.com:443/gadget1.xml";

  private Set<String> whitelist;
  private Set<String> blacklist;
  private FeatureAdminData whitelistFeatures;
  private FeatureAdminData blacklistFeatures;
  private GadgetAdminData whitelistData;
  private GadgetAdminData blacklistData;
  private Map<String, GadgetAdminData> gadgetMap;
  private ContainerAdminData validData;
  private ContainerAdminData emptyData;
  private ContainerAdminData nullData;
  private ContainerAdminData defaultData;
  private RpcAdminData rpcAdminData;

  @Before
  public void setUp() throws Exception {
    whitelist = Sets.newHashSet(VIEWS, SETPREFS, TABS);
    blacklist = Sets.newHashSet(EE, SELECTION);
    whitelistFeatures = new FeatureAdminData(whitelist, Type.WHITELIST);
    blacklistFeatures = new FeatureAdminData(blacklist, Type.BLACKLIST);
    rpcAdminData = new RpcAdminData(Sets.newHashSet("rpc1", "rpc2"));

    whitelistData = new GadgetAdminData(whitelistFeatures, rpcAdminData);
    blacklistData = new GadgetAdminData(blacklistFeatures, new RpcAdminData());

    gadgetMap = Maps.newHashMap();
    gadgetMap.put(GADGET_URL_1, whitelistData);
    gadgetMap.put(GADGET_URL_2, blacklistData);
    gadgetMap.put(GADGET_URL_3, new GadgetAdminData());
    gadgetMap.put("http://*", blacklistData);
    gadgetMap.put(GADGET_URL_4_WITH_PORT, whitelistData);

    validData = new ContainerAdminData(gadgetMap);
    emptyData = new ContainerAdminData(new HashMap<String, GadgetAdminData>());
    nullData = new ContainerAdminData(null);
    defaultData = new ContainerAdminData();

  }

  @After
  public void tearDown() throws Exception {
    whitelist = null;
    blacklist = null;
    whitelistFeatures = null;
    blacklistFeatures = null;
    whitelistData = null;
    blacklistData = null;
    gadgetMap = null;
    validData = null;
    emptyData = null;
    nullData = null;
    defaultData = null;
    rpcAdminData = null;
  }

  @Test
  public void testGetGadgetAdminData() {
    assertEquals(whitelistData, validData.getGadgetAdminData(GADGET_URL_1));
    assertEquals(whitelistData, validData.getGadgetAdminData(GADGET_URL_1_WITH_PORT));
    assertEquals(blacklistData, validData.getGadgetAdminData(GADGET_URL_2));
    assertEquals(new GadgetAdminData(),
            validData.getGadgetAdminData("http://example.com/gadgets/gadget.xml"));
    assertEquals(new GadgetAdminData(),
            validData.getGadgetAdminData("http://example.com/gadget.xml"));
    assertEquals(blacklistData, validData.getGadgetAdminData("http://foo.com/gadget.xml"));
    assertEquals(blacklistData, validData.getGadgetAdminData("http://foo.com:80/gadget.xml"));
    assertNull(validData.getGadgetAdminData("https://foo.com:80/gadget.xml"));
    assertEquals(whitelistData, validData.getGadgetAdminData(GADGET_URL_4));
    assertEquals(whitelistData, validData.getGadgetAdminData(GADGET_URL_4_WITH_PORT));
    assertNull(emptyData.getGadgetAdminData(GADGET_URL_1));
    assertNull(nullData.getGadgetAdminData(GADGET_URL_1));
    assertNull(defaultData.getGadgetAdminData(GADGET_URL_1));
  }

  @Test
  public void testGetGadgetAdminMap() {
    assertEquals(gadgetMap, validData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), emptyData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), nullData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), defaultData.getGadgetAdminMap());
  }

  @Test
  public void testEquals() {
    assertTrue(validData.equals(new ContainerAdminData(gadgetMap)));
    assertTrue(emptyData.equals(new ContainerAdminData(new HashMap<String, GadgetAdminData>())));
    assertTrue(defaultData.equals(new ContainerAdminData(new HashMap<String, GadgetAdminData>())));
    assertTrue(nullData.equals(new ContainerAdminData(null)));
    assertTrue(emptyData.equals(defaultData));
    assertFalse(validData.equals(null));
    assertFalse(validData.equals(new Object()));
    assertFalse(validData.equals(gadgetMap));
    assertFalse(validData.equals(emptyData));
    assertFalse(validData.equals(nullData));
  }

  @Test
  public void testAddAndRemove() {
    defaultData.addGadgetAdminData(GADGET_URL_1, whitelistData);
    assertEquals(whitelistData, defaultData.getGadgetAdminData(GADGET_URL_1));
    GadgetAdminData test = defaultData.removeGadgetAdminData(GADGET_URL_1);
    assertNull(defaultData.getGadgetAdminData(GADGET_URL_1));
    assertEquals(whitelistData, test);

    defaultData.addGadgetAdminData(null, whitelistData);
    assertNull(defaultData.getGadgetAdminData(null));

    test = defaultData.removeGadgetAdminData(null);
    assertNull(defaultData.getGadgetAdminData(null));
    assertNull(test);

    defaultData.addGadgetAdminData(GADGET_URL_1, null);
    assertNotNull(defaultData.getGadgetAdminData(GADGET_URL_1));

    validData.addGadgetAdminData(GADGET_URL_2, null);
    assertNotNull(validData.getGadgetAdminData(GADGET_URL_2));
  }

  @Test
  public void testClearGadgetAdminData() {
    assertEquals(gadgetMap, validData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), nullData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), emptyData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), defaultData.getGadgetAdminMap());

    validData.clearGadgetAdminData();
    nullData.clearGadgetAdminData();
    emptyData.clearGadgetAdminData();
    defaultData.clearGadgetAdminData();

    assertEquals(new HashMap<String, GadgetAdminData>(), validData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), nullData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), emptyData.getGadgetAdminMap());
    assertEquals(new HashMap<String, GadgetAdminData>(), defaultData.getGadgetAdminMap());
  }

  @Test
  public void testHasGadgetAdminData() {
    assertTrue(validData.hasGadgetAdminData(GADGET_URL_1));
    assertTrue(validData.hasGadgetAdminData(GADGET_URL_2));
    assertTrue(validData.hasGadgetAdminData(GADGET_URL_1_WITH_PORT));
    assertTrue(validData.hasGadgetAdminData("http://example.com/gadget3.xml"));
    assertTrue(validData.hasGadgetAdminData("http://example.com:80/gadget3.xml"));
    assertFalse(validData.hasGadgetAdminData("https://example.com/gadget3.xml"));
    assertTrue(validData.hasGadgetAdminData(GADGET_URL_4));
    assertTrue(validData.hasGadgetAdminData(GADGET_URL_4_WITH_PORT));
    assertTrue(validData.hasGadgetAdminData("http://foo.com/gadget.xml"));
    assertTrue(validData.hasGadgetAdminData("http://foo.com:80/gadget.xml"));
    assertFalse(validData.hasGadgetAdminData("https://foo.com/gadget.xml"));
    assertFalse(nullData.hasGadgetAdminData(GADGET_URL_1));
    assertFalse(emptyData.hasGadgetAdminData(GADGET_URL_2));
    assertFalse(defaultData.hasGadgetAdminData(GADGET_URL_2));
  }

  @Test
  public void testHashCode() {
    assertEquals(Objects.hashCode(this.gadgetMap), validData.hashCode());
    assertEquals(Objects.hashCode(Maps.newHashMap()), nullData.hashCode());
    assertEquals(Objects.hashCode(Maps.newHashMap()), emptyData.hashCode());
    assertEquals(Objects.hashCode(Maps.newHashMap()), defaultData.hashCode());
    assertEquals(nullData.hashCode(), emptyData.hashCode());
    assertFalse(validData.hashCode() == defaultData.hashCode());
  }
}

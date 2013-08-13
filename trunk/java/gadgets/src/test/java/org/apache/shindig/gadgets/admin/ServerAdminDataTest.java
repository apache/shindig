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
public class ServerAdminDataTest {

  private static final String VIEWS = "views";
  private static final String SETPREFS = "setprefs";
  private static final String TABS = "tabs";
  private static final String EE = "embedded-experiences";
  private static final String SELECTION = "selection";
  private static final String GADGET_URL_1 = "http://sample.com/gadget1.xml";
  private static final String GADGET_URL_2 = "http://sample.com/gadget2.xml";
  private static final String DEFAULT = "default";
  private static final String MY_CONTAINER = "my_container";

  private Set<String> whitelist;
  private Set<String> blacklist;
  private FeatureAdminData whitelistFeatures;
  private FeatureAdminData blacklistFeatures;
  private GadgetAdminData whitelistInfo;
  private GadgetAdminData blacklistInfo;
  private RpcAdminData rpcAdminData;
  private Map<String, GadgetAdminData> defaultMap;
  private Map<String, GadgetAdminData> myMap;
  private Map<String, ContainerAdminData> containerMap;
  private ContainerAdminData defaultContainerData;
  private ContainerAdminData myContainerData;
  private ServerAdminData validData;
  private ServerAdminData emptyData;
  private ServerAdminData defaultData;
  private ServerAdminData nullData;

  @Before
  public void setUp() throws Exception {
    whitelist = Sets.newHashSet(VIEWS, SETPREFS, TABS);
    blacklist = Sets.newHashSet(EE, SELECTION);
    whitelistFeatures = new FeatureAdminData(whitelist, Type.WHITELIST);
    blacklistFeatures = new FeatureAdminData(blacklist, Type.BLACKLIST);
    rpcAdminData = new RpcAdminData(Sets.newHashSet("rpc1", "rpc2"));

    whitelistInfo = new GadgetAdminData(whitelistFeatures, rpcAdminData);
    blacklistInfo = new GadgetAdminData(blacklistFeatures, new RpcAdminData());

    defaultMap = Maps.newHashMap();
    defaultMap.put(GADGET_URL_1, whitelistInfo);
    defaultMap.put(GADGET_URL_2, blacklistInfo);

    myMap = Maps.newHashMap();
    myMap.put(GADGET_URL_2, whitelistInfo);
    myMap.put(GADGET_URL_1, new GadgetAdminData());

    defaultContainerData = new ContainerAdminData(defaultMap);
    myContainerData = new ContainerAdminData(myMap);

    containerMap = Maps.newHashMap();
    containerMap.put(DEFAULT, defaultContainerData);
    containerMap.put(MY_CONTAINER, myContainerData);

    validData = new ServerAdminData(containerMap);
    emptyData = new ServerAdminData(new HashMap<String, ContainerAdminData>());
    defaultData = new ServerAdminData();
    nullData = new ServerAdminData(null);

  }

  @After
  public void tearDown() throws Exception {
    whitelist = null;
    blacklist = null;
    whitelistFeatures = null;
    blacklistFeatures = null;
    whitelistInfo = null;
    blacklistInfo = null;
    defaultMap = null;
    myMap = null;
    containerMap = null;
    defaultContainerData = null;
    myContainerData = null;
    validData = null;
    emptyData = null;
    defaultData = null;
    nullData = null;
  }

  @Test
  public void testGetContainerAdminData() {
    assertEquals(myContainerData, validData.getContainerAdminData(MY_CONTAINER));
    assertEquals(defaultContainerData, validData.getContainerAdminData(DEFAULT));
    assertNull(emptyData.getContainerAdminData(MY_CONTAINER));
    assertNull(nullData.getContainerAdminData(DEFAULT));
    assertNull(defaultData.getContainerAdminData(MY_CONTAINER));
  }

  @Test
  public void testRemoveContainerAdminData() {
    validData.removeContainerAdminData(DEFAULT);
    emptyData.removeContainerAdminData(MY_CONTAINER);
    nullData.removeContainerAdminData(DEFAULT);
    defaultData.removeContainerAdminData(MY_CONTAINER);
    Map<String, ContainerAdminData> newMap = Maps.newHashMap();
    newMap.put(MY_CONTAINER, myContainerData);
    assertEquals(newMap, validData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), emptyData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), nullData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), defaultData.getContainerAdminDataMap());
  }

  @Test
  public void testAddContainerAdminData() {
    emptyData.addContainerAdminData(MY_CONTAINER, myContainerData);
    nullData.addContainerAdminData(DEFAULT, defaultContainerData);
    defaultData.addContainerAdminData(MY_CONTAINER, myContainerData);
    defaultData.addContainerAdminData(DEFAULT, defaultContainerData);
    defaultData.addContainerAdminData(null, myContainerData);

    assertEquals(myContainerData, emptyData.getContainerAdminData(MY_CONTAINER));
    assertEquals(defaultContainerData, nullData.getContainerAdminData(DEFAULT));
    assertEquals(defaultContainerData, defaultData.getContainerAdminData(DEFAULT));
    assertEquals(myContainerData, defaultData.getContainerAdminData(MY_CONTAINER));
    assertNull(defaultData.getContainerAdminData(null));
  }

  @Test
  public void testGetContainerAdminDataMap() {
    assertEquals(containerMap, validData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), emptyData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), nullData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), defaultData.getContainerAdminDataMap());
  }

  @Test
  public void testClearContainerAdminData() {
    validData.clearContainerAdminData();
    emptyData.clearContainerAdminData();
    nullData.clearContainerAdminData();
    defaultData.clearContainerAdminData();

    assertEquals(new HashMap<String, ContainerAdminData>(), validData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), emptyData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), nullData.getContainerAdminDataMap());
    assertEquals(new HashMap<String, ContainerAdminData>(), defaultData.getContainerAdminDataMap());
  }

  @Test
  public void testEqualsObject() {
    Map<String, ContainerAdminData> testMap = Maps.newHashMap();
    testMap.put(DEFAULT, defaultContainerData);
    testMap.put(MY_CONTAINER, myContainerData);
    assertTrue(validData.equals(new ServerAdminData(testMap)));
    assertFalse(validData.equals(nullData));
    assertTrue(nullData.equals(defaultData));

    testMap = Maps.newHashMap();
    testMap.put(MY_CONTAINER, myContainerData);
    assertFalse(validData.equals(testMap));
  }

  @Test
  public void testHasContainerAdminData() {
    assertTrue(validData.hasContainerAdminData(MY_CONTAINER));
    assertTrue(validData.hasContainerAdminData(DEFAULT));
    assertFalse(validData.hasContainerAdminData("foo"));
    assertFalse(nullData.hasContainerAdminData(MY_CONTAINER));
    assertFalse(defaultData.hasContainerAdminData(DEFAULT));
    assertFalse(emptyData.hasContainerAdminData(MY_CONTAINER));
  }

  @Test
  public void testHashCode() {
    assertEquals(Objects.hashCode(this.containerMap), validData.hashCode());
    assertEquals(Objects.hashCode(Maps.newHashMap()), nullData.hashCode());
    assertEquals(Objects.hashCode(Maps.newHashMap()), emptyData.hashCode());
    assertEquals(Objects.hashCode(Maps.newHashMap()), defaultData.hashCode());
    assertEquals(nullData.hashCode(), emptyData.hashCode());
    assertFalse(validData.hashCode() == defaultData.hashCode());
  }
}

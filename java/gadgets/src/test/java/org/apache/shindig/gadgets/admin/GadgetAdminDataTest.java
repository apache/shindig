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
 * @since 2.5.0
 */
public class GadgetAdminDataTest {

  private static final String VIEWS = "views";
  private static final String SETPREFS = "setprefs";
  private static final String TABS = "tabs";
  private static final String EE = "embedded-experiences";
  private static final String SELECTION = "selection";
  private static final String RPC1 = "rcp1";
  private static final String RPC2 = "rpc2";
  private Set<String> whitelist;
  private Set<String> blacklist;
  private Set<String> rpcServiceIds;
  private FeatureAdminData whitelistFeatures;
  private FeatureAdminData blacklistFeatures;
  private RpcAdminData rpcAdminData;
  private GadgetAdminData whitelistInfo;
  private GadgetAdminData blacklistInfo;
  private GadgetAdminData nullInfo;
  private GadgetAdminData defaultInfo;

  @Before
  public void setUp() throws Exception {
    whitelist = Sets.newHashSet(VIEWS, SETPREFS, TABS);
    blacklist = Sets.newHashSet(EE, SELECTION);
    rpcServiceIds = Sets.newHashSet(RPC1, RPC2);
    whitelistFeatures = new FeatureAdminData(whitelist, Type.WHITELIST);
    blacklistFeatures = new FeatureAdminData(blacklist, Type.BLACKLIST);
    rpcAdminData = new RpcAdminData(rpcServiceIds);
    whitelistInfo = new GadgetAdminData(whitelistFeatures, rpcAdminData);
    blacklistInfo = new GadgetAdminData(blacklistFeatures, new RpcAdminData());
    nullInfo = new GadgetAdminData(null, null);
    defaultInfo = new GadgetAdminData();
  }

  @After
  public void tearDown() throws Exception {
    whitelist = null;
    whitelistInfo = null;
    blacklistInfo = null;
    rpcServiceIds = null;
    whitelistFeatures = null;
    blacklistFeatures = null;
    rpcAdminData = null;
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
  public void testSetFeatureAdminData() {
    assertEquals(whitelistFeatures, whitelistInfo.getFeatureAdminData());
    whitelistInfo.setFeatureAdminData(null);
    assertEquals(new FeatureAdminData(), whitelistInfo.getFeatureAdminData());

    assertEquals(blacklistFeatures, blacklistInfo.getFeatureAdminData());
    blacklistInfo.setFeatureAdminData(whitelistFeatures);
    assertEquals(whitelistFeatures, blacklistInfo.getFeatureAdminData());

    assertEquals(new FeatureAdminData(), nullInfo.getFeatureAdminData());
    nullInfo.setFeatureAdminData(whitelistFeatures);
    assertEquals(whitelistFeatures, nullInfo.getFeatureAdminData());

    assertEquals(new FeatureAdminData(), defaultInfo.getFeatureAdminData());
    defaultInfo.setFeatureAdminData(whitelistFeatures);
    assertEquals(whitelistFeatures, defaultInfo.getFeatureAdminData());
  }

  @Test
  public void testGetRpcAdminData() {
    assertEquals(rpcAdminData, whitelistInfo.getRpcAdminData());
    assertEquals(new RpcAdminData(), blacklistInfo.getRpcAdminData());
    assertEquals(new RpcAdminData(), nullInfo.getRpcAdminData());
    assertEquals(new RpcAdminData(), defaultInfo.getRpcAdminData());
  }

  @Test
  public void testSetRpcAdminData() {
    assertEquals(rpcAdminData, whitelistInfo.getRpcAdminData());
    whitelistInfo.setRpcAdminData(null);
    assertEquals(new RpcAdminData(), whitelistInfo.getRpcAdminData());

    assertEquals(new RpcAdminData(), blacklistInfo.getRpcAdminData());
    blacklistInfo.setRpcAdminData(rpcAdminData);
    assertEquals(rpcAdminData, blacklistInfo.getRpcAdminData());

    assertEquals(new RpcAdminData(), nullInfo.getRpcAdminData());
    nullInfo.setRpcAdminData(rpcAdminData);
    assertEquals(rpcAdminData, nullInfo.getRpcAdminData());

    assertEquals(new RpcAdminData(), defaultInfo.getRpcAdminData());
    defaultInfo.setRpcAdminData(rpcAdminData);
    assertEquals(rpcAdminData, defaultInfo.getRpcAdminData());
  }

  @Test
  public void testEquals() {
    assertTrue(whitelistInfo.equals(new GadgetAdminData(whitelistFeatures,
            rpcAdminData)));
    assertTrue(nullInfo.equals(new GadgetAdminData(null, null)));
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
    assertEquals(Objects.hashCode(whitelistFeatures, rpcAdminData),
            whitelistInfo.hashCode());
    assertEquals(Objects.hashCode(blacklistFeatures, new RpcAdminData()),
            blacklistInfo.hashCode());
    assertEquals(Objects.hashCode(new FeatureAdminData(), new RpcAdminData()),
            nullInfo.hashCode());
    assertEquals(Objects.hashCode(new FeatureAdminData(), new RpcAdminData()),
            defaultInfo.hashCode());
    assertEquals(nullInfo.hashCode(), defaultInfo.hashCode());
    assertFalse(blacklistInfo.hashCode() == whitelistInfo.hashCode());
  }

}

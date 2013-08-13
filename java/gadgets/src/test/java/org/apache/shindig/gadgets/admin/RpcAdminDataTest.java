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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.caja.util.Sets;
import com.google.common.base.Objects;

/**
 * Unit tests for RpcAdminData.
 *
 * @since 2.5.0
 */
public class RpcAdminDataTest {

  private Set<String> populatedIds;
  private RpcAdminData empty;
  private RpcAdminData populated;
  private RpcAdminData nullData;

  @Before
  public void setUp() throws Exception {
    populatedIds = Sets.newHashSet("rpc1", "rpc2");
    empty = new RpcAdminData();
    populated = new RpcAdminData(populatedIds);
    nullData = new RpcAdminData(null);
  }

  @After
  public void tearDown() throws Exception {
    empty = null;
    populated = null;
    populatedIds = null;
    nullData = null;
  }

  @Test
  public void testHashCode() {
    assertEquals(Objects.hashCode(populatedIds), populated.hashCode());
    assertEquals(Objects.hashCode(Sets.newHashSet()), empty.hashCode());
    assertEquals(Objects.hashCode(Sets.newHashSet()), nullData.hashCode());
    assertEquals(empty.hashCode(), nullData.hashCode());
    assertFalse(populated.hashCode() == empty.hashCode());
  }

  @Test
  public void testGetAdditionalRpcServiceIds() {
    assertEquals(populatedIds, populated.getAdditionalRpcServiceIds());
    assertEquals(Sets.newHashSet(), empty.getAdditionalRpcServiceIds());
    assertEquals(Sets.newHashSet(), nullData.getAdditionalRpcServiceIds());
    assertEquals(empty.getAdditionalRpcServiceIds(), nullData.getAdditionalRpcServiceIds());
    assertFalse(populated.getAdditionalRpcServiceIds().equals(empty.getAdditionalRpcServiceIds()));
  }

  @Test
  public void testSetAdditionalRpcServiceIds() {
    assertEquals(populatedIds, populated.getAdditionalRpcServiceIds());
    Set<String> emptySet = Sets.newHashSet();
    populated.setAdditionalRpcServiceIds(emptySet);
    assertEquals(Sets.newHashSet(), populated.getAdditionalRpcServiceIds());

    assertEquals(Sets.newHashSet(), empty.getAdditionalRpcServiceIds());
    empty.setAdditionalRpcServiceIds(populatedIds);
    assertEquals(populatedIds, empty.getAdditionalRpcServiceIds());

    assertEquals(Sets.newHashSet(), nullData.getAdditionalRpcServiceIds());
    nullData.setAdditionalRpcServiceIds(populatedIds);
    assertEquals(populatedIds, nullData.getAdditionalRpcServiceIds());
  }

  @Test
  public void testAddAdditionalRpcServiceId() {
    assertEquals(populatedIds, populated.getAdditionalRpcServiceIds());
    Set<String> newIds = Sets.newHashSet(populatedIds);
    populated.addAdditionalRpcServiceId("rpc3");
    populated.addAdditionalRpcServiceId(null);
    newIds.add("rpc3");
    assertEquals(newIds, populated.getAdditionalRpcServiceIds());

    Set<String> emptyRpcIds = Sets.newHashSet();
    assertEquals(emptyRpcIds, empty.getAdditionalRpcServiceIds());
    empty.addAdditionalRpcServiceId("rpc4");
    empty.addAdditionalRpcServiceId(null);
    emptyRpcIds.add("rpc4");
    assertEquals(emptyRpcIds, empty.getAdditionalRpcServiceIds());

    emptyRpcIds = Sets.newHashSet();
    assertEquals(emptyRpcIds, nullData.getAdditionalRpcServiceIds());
    nullData.addAdditionalRpcServiceId("rpc4");
    nullData.addAdditionalRpcServiceId(null);
    emptyRpcIds.add("rpc4");
    assertEquals(emptyRpcIds, nullData.getAdditionalRpcServiceIds());
  }

  @Test
  public void testRemoveAdditionalRpcServiceId() {
    assertEquals(populatedIds, populated.getAdditionalRpcServiceIds());
    populated.removeAdditionalRpcServiceId("rpc1");
    populated.removeAdditionalRpcServiceId(null);
    Set<String> newIds = Sets.newHashSet("rpc2");
    assertEquals(newIds, populated.getAdditionalRpcServiceIds());

    Set<String> emptyRpcIds = Sets.newHashSet();
    assertEquals(emptyRpcIds, empty.getAdditionalRpcServiceIds());
    empty.removeAdditionalRpcServiceId("rpc1");
    empty.removeAdditionalRpcServiceId("");
    assertEquals(emptyRpcIds, empty.getAdditionalRpcServiceIds());

    emptyRpcIds = Sets.newHashSet();
    assertEquals(emptyRpcIds, nullData.getAdditionalRpcServiceIds());
    nullData.removeAdditionalRpcServiceId("rpc1");
    nullData.removeAdditionalRpcServiceId("");
    assertEquals(emptyRpcIds, nullData.getAdditionalRpcServiceIds());
  }

  @Test
  public void testEqualsObject() {
    assertTrue(new RpcAdminData(populatedIds).equals(populated));
    assertTrue(new RpcAdminData().equals(empty));
    assertTrue(new RpcAdminData().equals(nullData));
    assertTrue(nullData.equals(empty));
    assertFalse(populated.equals(empty));
  }

}

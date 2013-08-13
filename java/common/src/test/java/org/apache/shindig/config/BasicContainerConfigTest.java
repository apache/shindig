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
package org.apache.shindig.config;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.apache.shindig.config.ContainerConfig.ConfigObserver;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests for BasicContainerConfig
 */
public class BasicContainerConfigTest {

  protected static final Map<String, Object> DEFAULT_CONTAINER =
      makeContainer("default", "inherited", "yes");
  protected static final Map<String, Object> MODIFIED_DEFAULT_CONTAINER =
      makeContainer("default", "inherited", "si");
  protected static final Map<String, Object> EXTRA_CONTAINER = makeContainer("extra");
  protected static final Map<String, Object> MODIFIED_EXTRA_CONTAINER =
      makeContainer("extra", "inherited", "no");

  protected ContainerConfig config;

  protected static Map<String, Object> makeContainer(String name, Object... values) {
    // Not using ImmutableMap to allow null values
    Map<String, Object> newCtr = Maps.newHashMap();
    newCtr.put("gadgets.container", ImmutableList.of(name));
    for (int i = 0; i < values.length / 2; ++i) {
      newCtr.put(values[i * 2].toString(), values[i * 2 + 1]);
    }
    return Collections.unmodifiableMap(newCtr);
  }

  protected static Map<String, Object> makeContainer(List<String> name, Object... values) {
    // Not using ImmutableMap to allow null values
    Map<String, Object> newCtr = Maps.newHashMap();
    newCtr.put("gadgets.container", name);
    for (int i = 0; i < values.length / 2; ++i) {
      newCtr.put(values[i * 2].toString(), values[i * 2 + 1]);
    }
    return Collections.unmodifiableMap(newCtr);
  }

  @Before
  public void setUp() throws Exception {
    config = new BasicContainerConfig();
    config.newTransaction().clearContainers().addContainer(DEFAULT_CONTAINER).commit();
  }

  @Test
  public void testGetContainers() throws Exception {
    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();
    assertEquals(ImmutableSet.of("default", "extra"), config.getContainers());
  }

  @Test
  public void testGetProperties() throws Exception {
    assertEquals(ImmutableSet.of("gadgets.container", "inherited"),
        config.getProperties("default").keySet());
  }

  @Test
  public void testPropertyTypes() throws Exception {
    String container = "misc";
    config.newTransaction().addContainer(makeContainer("misc",
        "bool", Boolean.valueOf(true),
        "bool2", "true",
        "badbool", Integer.valueOf(1234),
        "badbool2", "notabool",
        "int", Integer.valueOf(1234),
        "int2", "1234",
        "badint", "notanint",
        "string", "abcd",
        "list", ImmutableList.of("a"),
        "badlist", "notalist",
        "map", ImmutableMap.of("a", "b"),
        "badmap", "notamap")).commit();
    assertEquals(true, config.getBool(container, "bool"));
    assertEquals(true, config.getBool(container, "bool2"));
    assertEquals(false, config.getBool(container, "badbool"));
    assertEquals(false, config.getBool(container, "badbool2"));
    assertEquals(1234, config.getInt(container, "int"));
    assertEquals(1234, config.getInt(container, "int2"));
    assertEquals(0, config.getInt(container, "badint"));
    assertEquals("abcd", config.getString(container, "string"));
    assertEquals(ImmutableList.of("a"), config.getList(container, "list"));
    assertTrue(config.getList(container, "badlist").isEmpty());
    assertEquals(ImmutableMap.of("a", "b"), config.getMap(container, "map"));
    assertTrue(config.getMap(container, "badmap").isEmpty());
  }

  @Test
  public void testInheritance() throws Exception {
    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();
    assertEquals("yes", config.getString("default", "inherited"));
    assertEquals("yes", config.getString("extra", "inherited"));
    config.newTransaction().addContainer(MODIFIED_EXTRA_CONTAINER).commit();
    assertEquals("no", config.getString("extra", "inherited"));
    config.newTransaction().addContainer(MODIFIED_DEFAULT_CONTAINER).commit();
    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();
    assertEquals("si", config.getString("extra", "inherited"));
    assertEquals("si", config.getString("extra", "inherited"));
  }

  @Test
  public void testContainersAreMergedRecursively() throws Exception {
    // Data taken from the documentation for BasicContainerConfig#mergeParents
    Map<String, Object> defaultContainer = makeContainer("default",
        "base", "/gadgets/foo",
        "user", "peter",
        "map", ImmutableMap.of("latitude", 42, "longitude", -8),
        "data", ImmutableList.of("foo", "bar"));
    Map<String, Object> newContainer = makeContainer("new",
        "user", "anne",
        "colour", "green",
        "map", ImmutableMap.of("longitude", 130),
        "data", null);
    Map<String, Object> expectedContainer = makeContainer("new",
        "base", "/gadgets/foo",
        "user", "anne",
        "colour", "green",
        "map", ImmutableMap.of("latitude", 42, "longitude", 130),
        "data", null);
    config.newTransaction().addContainer(defaultContainer).addContainer(newContainer).commit();
    assertEquals(expectedContainer, config.getProperties("new"));
  }

  @Test
  public void testNulledPropertiesRemainNulledAfterSeveralTransactions() throws Exception {
    Map<String, Object> defaultContainer = makeContainer("default", "o1", "v1", "o2", "v2", "o3", "v3");
    Map<String, Object> parentContainer = makeContainer("parent", "o3", null);
    Map<String, Object> childContainer = makeContainer("child", "parent", "parent", "o2", null);
    config.newTransaction().addContainer(defaultContainer).commit();
    config.newTransaction().addContainer(parentContainer).commit();
    config.newTransaction().addContainer(childContainer).commit();
    assertNull(config.getProperty("child", "o2"));
    assertNull(config.getProperty("child", "o3"));
    assertNull(config.getProperty("parent", "o3"));
  }

  @Test
  public void testAddNewContainer() throws Exception {
    ConfigObserver observer = EasyMock.createMock(ContainerConfig.ConfigObserver.class);
    observer.containersChanged(EasyMock.isA(ContainerConfig.class),
        EasyMock.eq(ImmutableSet.of("extra")), EasyMock.eq(ImmutableSet.<String>of()));
    EasyMock.replay(observer);
    config.addConfigObserver(observer, false);

    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();
    assertTrue(config.getContainers().contains("extra"));
    assertEquals("yes", config.getString("extra", "inherited"));
    EasyMock.verify(observer);
  }

  @Test
  public void testReplaceContainer() throws Exception {
    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();

    ConfigObserver observer = EasyMock.createMock(ContainerConfig.ConfigObserver.class);
    observer.containersChanged(EasyMock.isA(ContainerConfig.class),
        EasyMock.eq(ImmutableSet.of("extra")), EasyMock.eq(ImmutableSet.<String>of()));
    EasyMock.replay(observer);
    config.addConfigObserver(observer, false);

    config.newTransaction().addContainer(MODIFIED_EXTRA_CONTAINER).commit();
    assertTrue(config.getContainers().contains("extra"));
    assertEquals("no", config.getString("extra", "inherited"));
    EasyMock.verify(observer);
  }

  @Test
  public void testReadSameContainer() throws Exception {
    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();

    ConfigObserver observer = EasyMock.createMock(ContainerConfig.ConfigObserver.class);
    observer.containersChanged(EasyMock.isA(ContainerConfig.class),
        EasyMock.eq(ImmutableSet.<String>of()), EasyMock.eq(ImmutableSet.<String>of()));
    EasyMock.replay(observer);
    config.addConfigObserver(observer, false);


    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();
    assertTrue(config.getContainers().contains("extra"));
    assertEquals("yes", config.getString("extra", "inherited"));
    EasyMock.verify(observer);
  }

  @Test
  public void testRemoveContainer() throws Exception {
    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();

    config.newTransaction().addContainer(EXTRA_CONTAINER).commit();

    ConfigObserver observer = EasyMock.createMock(ContainerConfig.ConfigObserver.class);
    observer.containersChanged(EasyMock.isA(ContainerConfig.class),
        EasyMock.eq(ImmutableSet.<String>of()), EasyMock.eq(ImmutableSet.of("extra")));
    EasyMock.replay(observer);
    config.addConfigObserver(observer, false);

    config.newTransaction().removeContainer("extra").commit();
    assertFalse(config.getContainers().contains("extra"));
    EasyMock.verify(observer);
  }

  @Test
  public void testClearContainerConfig() throws Exception {
    ConfigObserver observer = EasyMock.createMock(ContainerConfig.ConfigObserver.class);
    observer.containersChanged(EasyMock.isA(ContainerConfig.class),
        EasyMock.eq(ImmutableSet.of("additional")), EasyMock.eq(ImmutableSet.of("extra")));
    EasyMock.replay(observer);
    config = new BasicContainerConfig();
    config
        .newTransaction()
        .clearContainers()
        .addContainer(DEFAULT_CONTAINER)
        .addContainer(EXTRA_CONTAINER)
        .commit();
    config.addConfigObserver(observer, false);

    config
        .newTransaction()
        .clearContainers()
        .addContainer(DEFAULT_CONTAINER)
        .addContainer(makeContainer("additional"))
        .commit();

    assertFalse(config.getContainers().contains("extra"));
    assertTrue(config.getContainers().contains("additional"));

    EasyMock.verify(observer);
  }

  @Test
  public void testAddObserverNotifiesImmediately() throws Exception {
    ConfigObserver observer = EasyMock.createMock(ContainerConfig.ConfigObserver.class);
    observer.containersChanged(EasyMock.isA(ContainerConfig.class),
        EasyMock.eq(ImmutableSet.of("default", "extra")), EasyMock.eq(ImmutableSet.<String>of()));
    EasyMock.replay(observer);

    config = new BasicContainerConfig();
    config
        .newTransaction()
        .addContainer(DEFAULT_CONTAINER)
        .addContainer(EXTRA_CONTAINER)
        .commit();
    config.addConfigObserver(observer, true);

    EasyMock.verify(observer);
  }

  @Test
  public void testAliasesArePopulated() throws Exception {
    Map<String, Object> container =
        makeContainer(ImmutableList.of("original", "alias"), "property", "value");
    config.newTransaction().addContainer(container).commit();
    assertEquals(ImmutableSet.of("default", "original", "alias"), config.getContainers());
    assertEquals("value", config.getString("original", "property"));
    assertEquals("value", config.getString("alias", "property"));
  }
}

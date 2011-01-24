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
package org.apache.shindig.protocol.conversion;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.apache.shindig.protocol.conversion.BeanDelegatorTest.SimpleBean;
import org.apache.shindig.protocol.conversion.BeanDelegatorTest.SimpleBeanInterface;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeanFilterTest extends Assert {

  private BeanFilter beanFilter;
  private BeanDelegator beanDelegator;

  @Before
  public void setUp() {
    beanFilter = new BeanFilter();
    beanDelegator = BeanDelegatorTest.createSimpleDelegator();
  }

  @Test
  public void testNull() throws Exception {
    assertNull(beanFilter.createFilteredBean(null, ImmutableSet.<String>of("s")));
  }

  @Test
  public void testSimple() throws Exception {
    String data = "data";

    String newData = (String) beanFilter.createFilteredBean(data, null);
    assertSame(data, newData);
  }

  @Test
  public void testInt() throws Exception {
    SimpleBean data = new SimpleBean().setI(5);
    SimpleBeanInterface dataBean = (SimpleBeanInterface) beanDelegator.createDelegator(data);

    SimpleBeanInterface newData = (SimpleBeanInterface) beanFilter.createFilteredBean(
        dataBean, ImmutableSet.<String>of("i"));
    assertEquals(5, newData.getI());

    newData = (SimpleBeanInterface) beanFilter.createFilteredBean(
        dataBean, ImmutableSet.<String>of("s"));
    // Filter is ignored for primitive types:
    assertEquals(5, newData.getI());
  }

  @Test
  public void testString() throws Exception {
    SimpleBean data = new SimpleBean().setS("data");
    SimpleBeanInterface dataBean = (SimpleBeanInterface) beanDelegator.createDelegator(data);

    SimpleBeanInterface newData = (SimpleBeanInterface) beanFilter.createFilteredBean(
        dataBean, ImmutableSet.<String>of("s"));
    assertEquals("data", newData.getS());
    newData = (SimpleBeanInterface) beanFilter.createFilteredBean(
        dataBean, ImmutableSet.<String>of("i"));
    assertNull("S is filtered out", newData.getS());
    assertNotNull("Required field", newData.getRequired());
  }

  @Test
  public void testList() throws Exception {
    SimpleBean data = new SimpleBean().setList(ImmutableList.<String>of("d1", "d2"));
    SimpleBeanInterface dataBean = (SimpleBeanInterface) beanDelegator.createDelegator(data);

    SimpleBeanInterface newData = (SimpleBeanInterface) beanFilter.createFilteredBean(
      dataBean, ImmutableSet.<String>of("s"));
    assertEquals(null, newData.getList());

    newData = (SimpleBeanInterface) beanFilter.createFilteredBean(
        dataBean, ImmutableSet.<String>of("list"));
    assertArrayEquals(data.getList().toArray(), newData.getList().toArray());
  }

  @Test
  public void testMap() throws Exception {
    List<String> list = ImmutableList.of("test");
    SimpleBean data = new SimpleBean().setS("Main").setBeanMap(
        ImmutableMap.<String, SimpleBean>of( "s1", new SimpleBean().setS("sub1").setList(list),
          "s2", new SimpleBean().setS("sub2").setList(list).setBeanMap(
              ImmutableMap.of("s2s1", new SimpleBean().setS("sub2-sub1"))
        )));
    SimpleBeanInterface dataBean = (SimpleBeanInterface) beanDelegator.createDelegator(data);

    SimpleBeanInterface newData = (SimpleBeanInterface) beanFilter.createFilteredBean(dataBean,
        ImmutableSet.<String>of("beanmap"));
    assertEquals(2, newData.getBeanMap().size());
    assertEquals(null, newData.getBeanMap().get("s1").getS());

    newData = (SimpleBeanInterface) beanFilter.createFilteredBean(dataBean,
      ImmutableSet.<String>of("beanmap", "beanmap.s"));
    assertNotSame(dataBean.getBeanMap().getClass(), newData.getBeanMap().getClass());
    assertEquals(2, newData.getBeanMap().size());
    assertEquals("sub1", newData.getBeanMap().get("s1").getS());
    assertNull("List is filtered out", newData.getBeanMap().get("s1").getList());

    newData = (SimpleBeanInterface) beanFilter.createFilteredBean(dataBean,
      ImmutableSet.<String>of("beanmap", "beanmap.*"));
    // Verify filter is a simple pass through.
    // can only check class since each time different delegator is created
    assertSame(dataBean.getBeanMap().getClass(), newData.getBeanMap().getClass());

    newData = (SimpleBeanInterface) beanFilter.createFilteredBean(dataBean,
        ImmutableSet.<String>of("beanmap", "beanmap.beanmap", "beanmap.beanmap.s"));
    assertEquals(2, newData.getBeanMap().size());
    Map<String, SimpleBeanInterface> subSubMap = newData.getBeanMap().get("s2").getBeanMap();
    assertEquals(1, subSubMap.size());
    assertEquals("sub2-sub1", subSubMap.get("s2s1").getS());
    assertNull("list is filtered", subSubMap.get("s2s1").getList());

    newData = (SimpleBeanInterface) beanFilter.createFilteredBean(dataBean,
        ImmutableSet.<String>of("beanmap", "beanmap.beanmap", "beanmap.beanmap.*"));
    assertEquals(2, newData.getBeanMap().size());
    assertNotSame(dataBean.getBeanMap().getClass(), newData.getBeanMap().getClass());
    assertSame(data.getBeanMap().get("s2").getBeanMap().getClass(),
        newData.getBeanMap().get("s2").getBeanMap().getClass());
  }

  @Test
  public void testProcessFields() {
    Set<String> srcFields = ImmutableSet.of("A", "b", "c.d.e.f", "Case", "cAse", "CASE");
    Set<String> newFields = beanFilter.processBeanFields(srcFields);
    assertEquals(7, newFields.size());
    assertTrue(newFields.contains("a"));
    assertTrue(newFields.contains("b"));
    assertTrue(newFields.contains("c"));
    assertTrue(newFields.contains("c.d"));
    assertTrue(newFields.contains("c.d.e"));
    assertTrue(newFields.contains("c.d.e.f"));
    assertTrue(newFields.contains("case"));
  }

  @Test
  public void testListFields() {
    List<String> fields = beanFilter.getBeanFields(SimpleBeanInterface.class, 3);
    assertTrue(fields.contains("Map"));
    assertTrue(fields.contains("I"));
    assertTrue(fields.contains("S"));
    assertTrue(fields.contains("Style"));
    assertTrue(fields.contains("List"));
    assertTrue(fields.contains("BeanList.List"));
    assertTrue(fields.contains("Map"));
    assertTrue(fields.contains("BeanMap.List"));
    assertTrue(fields.contains("BeanMap.BeanMap.BeanMap"));
    assertFalse(fields.contains("BeanMap.BeanMap.BeanMap.BeanMap"));
    assertEquals(77, fields.size());
    // If failed use next prints to verify and fix
    // System.out.println(fields.size());
    // System.out.println(fields.toString());
  }
}

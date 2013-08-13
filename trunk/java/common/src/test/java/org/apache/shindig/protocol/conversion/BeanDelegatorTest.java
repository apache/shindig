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

import org.junit.Assert;

import org.apache.shindig.protocol.conversion.BeanFilter.Unfiltered;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class BeanDelegatorTest extends Assert {

  // Note, this classes also used by the BeanFilter tests
  public static interface SimpleBeanInterface {
    public int getI();
    public SimpleBeanInterface setI(int i);
    public String getS();
    // Test list conversions:
    public List<String> getList();
    public List<SimpleBeanInterface> getBeanList();
    // Test Map conversion
    public Map<String, String> getMap();
    public Map<String, SimpleBeanInterface> getBeanMap();
    // Test error cases
    public String getUnknown(); // delegated class doesn't have this
    public int getWrongType(); // delegated class return different type
    public String getPrivateData(); // delegated class method is private

    // Test enum
    public enum Style { A, B }
    public Style getStyle();

    // Test of required
    @Unfiltered
    public String getRequired();
  }

  public static class SimpleBean {
    private int i;
    private String s;
    private List<String> l;
    private List<SimpleBean> beanList;
    private Map<String, String> stringMap;
    private Map<String, SimpleBean> beanMap;

    public int getI() { return i; }
    public SimpleBean setI(int ni) { i = ni; return this; }

    public String getS() { return s; }
    public SimpleBean setS(String ns) { s = ns; return this; }

    public List<String> getList() { return l; }
    public SimpleBean setList(List<String> nl) { l = nl; return this; }

    public List<SimpleBean> getBeanList() { return beanList; }
    public SimpleBean setBeanList(List<SimpleBean> nl) { beanList = nl; return this; }

    public Map<String, String> getMap() { return stringMap; }
    public SimpleBean setMap(Map<String, String> nm) { stringMap = nm; return this; }

    public Map<String, SimpleBean> getBeanMap() { return beanMap; }
    public SimpleBean setBeanMap(Map<String, SimpleBean> nm) { beanMap = nm; return this; }

    public String getWrongType() { return "this is string"; }

    @SuppressWarnings("unused")
    private String getPrivateData() { return "this is private"; }

    // Enum data:
    public enum RealStyle { R_A, R_B
    }
    RealStyle style;
    public RealStyle getStyle() { return style; }
    public SimpleBean setStyle(RealStyle style) { this.style = style; return this; }

    // Test of required
    public String getRequired() { return "required"; }
  }

  private BeanDelegator beanDelegator;
  private SimpleBean source;
  private SimpleBeanInterface proxy;

  public static BeanDelegator createSimpleDelegator() {
    BeanDelegator beanDelegator = new BeanDelegator(
        ImmutableMap.<Class<?>, Class<?>>of(SimpleBean.class, SimpleBeanInterface.class,
            SimpleBean.RealStyle.class, SimpleBeanInterface.Style.class),
        ImmutableMap.<Enum<?>, Enum<?>>of(SimpleBean.RealStyle.R_A, SimpleBeanInterface.Style.A,
            SimpleBean.RealStyle.R_B, SimpleBeanInterface.Style.B));
    return beanDelegator;
  }

  @Before
  public void setUp() {
    beanDelegator = createSimpleDelegator();
    source = new SimpleBean();
    proxy = (SimpleBeanInterface) beanDelegator.createDelegator(source);
  }

  @Test
  public void testSimpleBean() {
    String s = "test";
    source.setS(s);
    assertEquals(s, proxy.getS());

    proxy.setI(5);
    assertEquals(5, proxy.getI());
    assertEquals(5, source.getI());

    source.setStyle(SimpleBean.RealStyle.R_A);
    assertEquals(SimpleBeanInterface.Style.A, proxy.getStyle());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUnimplementedFunction() {
    proxy.getUnknown();
  }

  @Test(expected = ClassCastException.class)
  public void testWrontType() {
    proxy.getWrongType();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testPrivateAccess() {
    proxy.getPrivateData();
  }

  @Test
  public void testStringList() {
    assertNull(proxy.getList());
    List<String> stringList = ImmutableList.of("item1", "item2");
    source.setList(stringList);
    assertEquals(stringList, proxy.getList());
    stringList = ImmutableList.of();
    source.setList(stringList);
    assertEquals(stringList, proxy.getList());
  }

  @Test
  public void testBeanList() {
    List<SimpleBean> beanList = ImmutableList.of();
    source.setBeanList(beanList);
    assertEquals(beanList, proxy.getBeanList());

    SimpleBean item = new SimpleBean().setS("item");
    beanList = ImmutableList.of(item);
    source.setBeanList(beanList);
    List<SimpleBeanInterface> interList = proxy.getBeanList();
    assertEquals(1, interList.size());
    assertEquals(item.getS(), interList.get(0).getS());
  }

  @Test
  public void testStringMap() {
    assertNull(proxy.getMap());
    Map<String, String> stringMap = ImmutableMap.of("item1", "v1", "item2", "v2");
    source.setMap(stringMap);
    assertEquals(stringMap, proxy.getMap());
    stringMap = ImmutableMap.of();
    source.setMap(stringMap);
    assertEquals(stringMap, proxy.getMap());
  }

  @Test
  public void testBeanMap() {
    Map<String, SimpleBean> beanMap = ImmutableMap.of();
    source.setBeanMap(beanMap);
    assertEquals(beanMap, proxy.getBeanMap());

    SimpleBean item = new SimpleBean().setS("item");
    beanMap = ImmutableMap.of("item", item);
    source.setBeanMap(beanMap);
    Map<String, SimpleBeanInterface> interMap = proxy.getBeanMap();
    assertEquals(1, interMap.size());
    assertEquals(item.getS(), interMap.get("item").getS());
  }

  class TokenData {
    public String getId() { return "id"; }
  }

  interface TokenInter {
    public String getId();
    public String getContainer();
  }

  @Test
  public void testExtraFields() {
    TokenData data = new TokenData();
    String container = "data";
    TokenInter p = beanDelegator.createDelegator(data, TokenInter.class,
        ImmutableMap.<String, Object>of("container", container));

    assertSame(data.getId(), p.getId());
    assertSame(container, p.getContainer());
  }

  @Test
  public void testExtraFieldsBadCase() {
    TokenData data = new TokenData();
    String container = "data";
    TokenInter p = beanDelegator.createDelegator(data, TokenInter.class,
        ImmutableMap.<String, Object>of("Cont_Ainer", container));

    assertSame(data.getId(), p.getId());
    assertSame(container, p.getContainer());
  }

  // Make sure validate will actually fail
  @Test(expected = NoSuchMethodException.class)
  public void tesValidate() throws Exception {
    beanDelegator.validate();
  }

  @Test
  public void testValidateGoodBean() throws Exception {
    TokenInter p = beanDelegator.createDelegator(null, TokenInter.class,
        ImmutableMap.<String, Object>of("container", "open", "id", "test"));
    BeanDelegator.validateDelegator(p);
  }

  @Test(expected = InvocationTargetException.class)
  public void testValidateWrongtype() throws Exception {
    TokenInter p = beanDelegator.createDelegator(null, TokenInter.class,
        ImmutableMap.<String, Object>of("container", "open", "id", new Integer(5)));
    BeanDelegator.validateDelegator(p);
  }

  @Test(expected = InvocationTargetException.class)
  public void testValidateMissingField() throws Exception {
    TokenInter p = beanDelegator.createDelegator(null, TokenInter.class,
        ImmutableMap.<String, Object>of("container", "open"));
    BeanDelegator.validateDelegator(p);
  }

}

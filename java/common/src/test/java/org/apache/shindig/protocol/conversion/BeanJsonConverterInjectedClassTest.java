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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * The Class BeanJsonConverterInjectedClassTest.
 */
public class BeanJsonConverterInjectedClassTest extends Assert {

  /** The bean json converter. */
  private BeanJsonConverter beanJsonConverter;

  @Before
  public void setUp() throws Exception {
    this.beanJsonConverter = new BeanJsonConverter(Guice.createInjector(new TestModule()));
  }

  /**
   * Test json conversion of a TestInterface into a TestObject
   *
   * @throws Exception the exception
   */
  @Test
  public void testJsonToObject() throws Exception {
    String json = "{x:'xValue',y:'yValue'}";
    TestObject object = (TestObject) beanJsonConverter.convertToObject(json, TestInterface.class);
    assertNotNull("expected 'x' field not set after json conversion", object.getX());
    assertNotNull("expected 'y' field not set after json conversion", object.getY());
  }

  /**
   * TestModule that binds TestObject to TestInterface
   */
  private static class TestModule extends AbstractModule {
    /* (non-Javadoc)
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
      bind(TestInterface.class).to(TestObject.class);
    }
  }

  /**
   * TestInterface.
   */
  public interface TestInterface {
    String getX();
    void setX(String x);
  }

  /**
   * TestObject.
   */
  public static class TestObject implements TestInterface {

    private String x;
    private String y;

    public String getX() {
      return x;
    }
    public void setX(String x) {
      this.x = x;
    }

    public String getY() {
      return y;
    }
    public void setY(String y) {
      this.y = y;
    }
  }

}

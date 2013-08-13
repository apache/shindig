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
package org.apache.shindig.common;

import com.google.common.collect.Lists;

import org.easymock.EasyMock;
import org.easymock.IMockBuilder;
import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.List;


public abstract class EasyMockTestCase extends Assert {
  /** Tracks all EasyMock objects created for a test. */
  private final List<Object> mocks = Lists.newArrayList();

  /**
   * Creates a strict mock object for the given class, adds it to the internal
   * list of all mocks, and returns it.
   *
   * @param clazz Class to be mocked.
   * @return A mock instance of the given type.
   **/
  protected <T> T mock(Class<T> clazz) {
    return mock(clazz, false);
  }

  /**
   * Creates a strict or nice mock object for the given class, adds it to the internal
   * list of all mocks, and returns it.
   *
   * @param clazz Class to be mocked.
   * @param strict whether or not to make a strict mock
   * @return A mock instance of the given type.
   **/
  protected <T> T mock(Class<T> clazz, boolean strict) {
    T m = strict ? EasyMock.createMock(clazz) : EasyMock.createNiceMock(clazz);
    mocks.add(m);
    return m;
  }

  /**
   * Creates a nice mock object for the given class, adds it to the internal
   * list of all mocks, and returns it.
   *
   * @param clazz Class to be mocked.
   * @return A mock instance of the given type.
   **/


  protected <T> T mock(Class<T> clazz, Method[] methods) {
    return mock(clazz, methods, false);
  }


  /**
   * Creates a strict mock object for the given class, adds it to the internal
   * list of all mocks, and returns it.
   *
   * @param clazz Class to be mocked.
   * @return A mock instance of the given type.
   **/

  protected <T> T mock(Class<T> clazz, Method[] methods, boolean strict) {
    IMockBuilder<T> builder = EasyMock.createMockBuilder(clazz).addMockedMethods(methods);

    T m = strict ? builder.createMock() : builder.createNiceMock();
    mocks.add(m);

    return m;
  }

  /**
   * Sets each mock to replay mode in the order they were created. Call this after setting
   * all of the mock expectations for a test.
   */
  protected void replay() {
    EasyMock.replay(mocks.toArray());
  }

  protected void replay(Object mock) {
    EasyMock.replay(mock);
  }

  /**
   * Verifies each mock in the order they were created. Call this at the end of each test
   * to verify the expectations were satisfied.
   */
  protected void verify() {
    EasyMock.verify(mocks.toArray());
  }

  /**
   * Resets all of the mocks.
   */
  protected void reset() {
    EasyMock.reset(mocks.toArray());
  }

  protected void reset(Object mock) {
    EasyMock.reset(mock);
  }
}

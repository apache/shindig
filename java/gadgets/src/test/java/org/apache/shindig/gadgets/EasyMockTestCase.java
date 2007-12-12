/*
 * $Id$
 *
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.ArrayList;
import java.util.List;

public abstract class EasyMockTestCase extends TestCase {
  /** Tracks all EasyMock objects created for a test. */
  private final List<Object> mocks = new ArrayList<Object>();

  public EasyMockTestCase() {
    super();
  }

  public EasyMockTestCase(String name) {
    super(name);
  }

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
   * @param nice whether or not to make a nice mock (non-strict).
   * @return A mock instance of the given type.
   **/
  protected <T> T mock(Class<T> clazz, boolean nice) {
    T m = nice ? EasyMock.createNiceMock(clazz) : EasyMock.createMock(clazz);
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

  /**
   * Verifies each mock in the order they were created. Call this at the end of each test
   * to verify the expectations were satisfied.
   */
  protected void verify() {
    EasyMock.verify(mocks.toArray());
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.common.util;

import junit.framework.TestCase;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Tests ImmediateFuture.
 */
public class ImmediateFutureTest extends TestCase {
  public void testGet() throws Exception {
    assertEquals("foo", ImmediateFuture.newInstance("foo").get());
  }

  public void testGetNull() throws Exception {
    assertNull(ImmediateFuture.newInstance(null).get());
  }

  public void testGetWithTimeout() throws Exception {
    assertEquals("foo", ImmediateFuture.newInstance("foo").get(1L, TimeUnit.MILLISECONDS));
  }

  public void testCancel() {
    Future<String> stringFuture = ImmediateFuture.newInstance("foo");
    assertFalse(stringFuture.cancel(true));
    assertFalse(stringFuture.cancel(false));
    assertFalse(stringFuture.isCancelled());
  }

  public void testIsDone() {
    assertTrue(ImmediateFuture.newInstance("foo").isDone());
  }
}

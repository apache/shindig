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

import org.apache.shindig.protocol.RestfulCollection;

import java.util.List;
import java.util.concurrent.Future;

import com.google.common.collect.Lists;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the FutureUtils utility methods
 */
public class FutureUtilTest extends Assert {
  String firstWord = "hello";

  @Test
  public void testGetFirstOfMany() throws Exception {
    List<String> words = Lists.newArrayList(firstWord, "goodbye", "blah");
    Future<RestfulCollection<String>> collection = ImmediateFuture.newInstance(
        new RestfulCollection<String>(words));

    Future<String> futureWord = FutureUtil.getFirstFromCollection(collection);
    assertEquals(firstWord, futureWord.get());
  }

  @Test
  public void testGetFirstOfSingle() throws Exception {
    List<String> words = Lists.newArrayList(firstWord);
    Future<RestfulCollection<String>> collection = ImmediateFuture.newInstance(
        new RestfulCollection<String>(words));

    Future<String> futureWord = FutureUtil.getFirstFromCollection(collection);
    assertEquals(firstWord, futureWord.get());
  }

  @Test
  public void testGetFirstOfNone() throws Exception {
    List<String> words = Lists.newArrayList(new String[]{});
    Future<RestfulCollection<String>> collection = ImmediateFuture.newInstance(
        new RestfulCollection<String>(words));

    Future<String> futureWord = FutureUtil.getFirstFromCollection(collection);
    assertNull(futureWord.get());
  }
}

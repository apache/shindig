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
package org.apache.shindig.social.opensocial.spi;

import org.junit.Assert;
import org.junit.Test;

public class LocalIdTest extends Assert {

  @Test
  public void testLocalId() throws Exception {
    LocalId l1 = new LocalId("");
    assertTrue(l1 instanceof LocalId);

    LocalId l2 = new LocalId("195mg90a39v");
    assertTrue(l2 instanceof LocalId);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testLocalIdException() {
    new LocalId("195mg90a39v/937194");
  }
}

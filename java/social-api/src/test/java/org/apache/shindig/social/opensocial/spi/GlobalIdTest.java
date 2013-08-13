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

public class GlobalIdTest extends Assert {

  @Test
  public void testGlobalId() throws Exception {
    DomainName dn = new DomainName("example.com");
    LocalId lid = new LocalId("195mg90a39v");

    GlobalId g1 = new GlobalId(dn, lid);
    assertTrue(g1 instanceof GlobalId);

    GlobalId g2 = new GlobalId("example.com:195mg90a39v");
    assertTrue(g2 instanceof GlobalId);

    GlobalId g3 = new GlobalId("example.com", "195mg90a39v");
    assertTrue(g3 instanceof GlobalId);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testGlobalIdException() {
    new GlobalId("example.com/test:195mg90a39v");
  }
}

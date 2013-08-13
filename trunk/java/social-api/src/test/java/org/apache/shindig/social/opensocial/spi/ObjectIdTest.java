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

public class ObjectIdTest extends Assert {

  @Test
  public void testObjectId() throws Exception {
    LocalId lid = new LocalId("195mg90a39v");
    GlobalId gid = new GlobalId("example.com:195mg90a39v");

    ObjectId o1 = new ObjectId(lid);
    assertTrue(o1 instanceof ObjectId);

    ObjectId o2 = new ObjectId(gid);
    assertTrue(o2 instanceof ObjectId);

    ObjectId o3 = new ObjectId("195mg90a39v");
    assertTrue(o3 instanceof ObjectId);
    assertTrue(o3.getObjectId() instanceof LocalId);

    ObjectId o4 = new ObjectId("example.com:195mg90a39v");
    assertTrue(o4 instanceof ObjectId);
    assertTrue(o4.getObjectId() instanceof GlobalId);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testObjectIdException() {
    new ObjectId("195mg90a39v/937194");
  }
}

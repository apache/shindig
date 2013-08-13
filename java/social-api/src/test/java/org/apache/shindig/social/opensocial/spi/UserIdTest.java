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

import org.apache.shindig.common.testing.FakeGadgetToken;

import org.junit.Assert;
import org.junit.Test;

public class UserIdTest extends Assert {
  @Test
  public void testGetUserId() throws Exception {
    UserId owner = new UserId(UserId.Type.owner, "hello");
    assertEquals("owner", owner.getUserId(new FakeGadgetToken().setOwnerId("owner")));

    UserId viewer = new UserId(UserId.Type.viewer, "hello");
    assertEquals("viewer", viewer.getUserId(new FakeGadgetToken().setViewerId("viewer")));

    UserId me = new UserId(UserId.Type.me, "hello");
    assertEquals("viewer", me.getUserId(new FakeGadgetToken().setViewerId("viewer")));

    UserId user = new UserId(UserId.Type.userId, "hello");
    assertEquals("hello", user.getUserId(new FakeGadgetToken()));
  }

  @Test
  public void testFromJson() throws Exception {
    UserId owner = UserId.fromJson("@owner");
    assertEquals(UserId.Type.owner, owner.getType());

    UserId viewer = UserId.fromJson("@viewer");
    assertEquals(UserId.Type.viewer, viewer.getType());

    UserId me = UserId.fromJson("@me");
    assertEquals(UserId.Type.me, me.getType());

    UserId user = UserId.fromJson("john.doe");
    assertEquals(UserId.Type.userId, user.getType());
    assertEquals("john.doe", user.getUserId());
  }
}

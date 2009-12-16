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
package org.apache.shindig.social.opensocial.spi;

import org.apache.shindig.social.opensocial.spi.GroupId;

import org.junit.Assert;
import org.junit.Test;

public class GroupIdTest extends Assert {

  @Test
  public void testFromJson() throws Exception {
    GroupId all = GroupId.fromJson("@all");
    assertEquals(GroupId.Type.all, all.getType());

    GroupId friends = GroupId.fromJson("@friends");
    assertEquals(GroupId.Type.friends, friends.getType());

    GroupId self = GroupId.fromJson("@self");
    assertEquals(GroupId.Type.self, self.getType());

    GroupId group = GroupId.fromJson("superbia");
    assertEquals(GroupId.Type.groupId, group.getType());
    assertEquals("superbia", group.getGroupId());
  }
}
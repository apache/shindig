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

import org.apache.commons.lang.StringUtils;

public class GroupId {
  public enum Type {
    all, friends, self, deleted, groupId
  }

  private Type type;
  private String groupId;

  public GroupId(Type type, String groupId) {
    this.groupId = groupId;
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public String getGroupId() {
    // Only valid for objects with type=groupId
    return groupId;
  }

  public static GroupId fromJson(String jsonId) {
    try {
      Type idSpecEnum = Type.valueOf(jsonId.substring(1));
      return new GroupId(idSpecEnum, null);
    } catch (IllegalArgumentException e) {
      return new GroupId(Type.groupId, jsonId);
    }
  }

  // These are overriden so that EasyMock doesn't throw a fit
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof GroupId)) {
      return false;
    }

    GroupId actual = (GroupId) o;
    return this.type == actual.type
        && StringUtils.equals(this.groupId, actual.groupId);
  }

  @Override
  public int hashCode() {
    int groupHashCode = 0;
    if (this.groupId != null) {
      groupHashCode = this.groupId.hashCode();
    }
    return this.type.hashCode() + groupHashCode;
  }
}

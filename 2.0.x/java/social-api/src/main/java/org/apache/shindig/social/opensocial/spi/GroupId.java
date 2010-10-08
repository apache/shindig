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

import com.google.common.collect.ImmutableMap;
import com.google.common.base.Objects;

import java.util.Map;

/**
 * A group id used for grouping of people resources (as opposed to groups used by the GroupsHandler)
 */
public class GroupId {
  public enum Type {
    all, friends, self, deleted, groupId;

    /** A map of JSON strings to Type objects */
    private static final Map<String, Type> jsonTypeMap;

    static {
      ImmutableMap.Builder<String,Type> builder = ImmutableMap.builder();
      for (Type type : Type.values()) {
        builder.put('@' + type.name(), type);
      }
      jsonTypeMap = builder.build();
    }
    /**
     * Return the Type enum value given a specific jsonType such as @all, @friends, etc.
     *
     * @param jsonType the type string to convert
     * @return A Type Enum value or null if no value exists
     **/
    public static Type jsonValueOf(String jsonType) {
       return jsonTypeMap.get(jsonType);
    }
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
    Type idSpecEnum = Type.jsonValueOf(jsonId);
    if (idSpecEnum != null) {
      return new GroupId(idSpecEnum, null);
    }

    return new GroupId(Type.groupId, jsonId);
  }

  // These are overriden so that EasyMock doesn't throw a fit
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    
    if (!(o instanceof GroupId)) {
      return false;
    }

    GroupId actual = (GroupId) o;
    return this.type == actual.type && Objects.equal(this.groupId, actual.groupId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.groupId, this.type);
  }

  @Override
  public String toString() {
      switch (type) {
          case all:
              return "ALL";
          case deleted:
              return "DELETE";
          case friends:
              return "FRIENDS";
          case self:
              return "SELF";
          case groupId:
              return "GROUPID(" + groupId + ')';
          default:
              return "UNKNOWN";
      }
  }
}

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

import com.google.common.collect.Maps;

import java.util.Map;

public class GroupId {
  public enum Type {
    all, friends, self, deleted, groupId;

    /** A map of JSON strings to Type objects */
    private static final Map<String, Type> jsonTypeMap = Maps.newHashMap();

    static {
      for (Type type : Type.values()) {
        jsonTypeMap.put('@' + type.name(), type);
      }
    }
    /** Return the Type enum value given a specific jsonType **/
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

  @Override public String toString() {
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

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

import org.apache.shindig.auth.SecurityToken;

import com.google.common.collect.ImmutableMap;
import com.google.common.base.Objects;

import java.util.Map;

/**
 * Data structure representing a userid
 */
public class UserId {
  public enum Type {
    me, viewer, owner, userId;

    /** A map of JSON strings to Type objects */
    private static final Map<String, Type> jsonTypeMap;

    static {
      ImmutableMap.Builder<String,Type> builder = ImmutableMap.builder();
      for (Type type : Type.values()) {
        builder.put('@' + type.name(), type);
      }
      jsonTypeMap = builder.build();
    }
    /** Return the Type enum value given a specific jsonType **/
    public static Type jsonValueOf(String jsonType) {
       return jsonTypeMap.get(jsonType);
    }
  }

  private Type type;
  private String userId;

  public UserId(Type type, String userId) {
    this.type = type;
    this.userId = userId;
  }

  public Type getType() {
    return type;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserId(SecurityToken token) {
    switch(type) {
      case owner:
        return token.getOwnerId();
      case viewer:
      case me:
        return token.getViewerId();
      case userId:
        return userId;
      default:
        throw new IllegalStateException("The type field is not a valid enum: " + type);
    }
  }

  public static UserId fromJson(String jsonId) {
    Type idSpecEnum = Type.jsonValueOf(jsonId);
    if (idSpecEnum != null) {
      return new UserId(idSpecEnum, null);
    }

    return new UserId(Type.userId, jsonId);
  }

  // These are overriden so that EasyMock doesn't throw a fit
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof UserId)) {
      return false;
    }

    UserId actual = (UserId) o;
    return this.type == actual.type
        && Objects.equal(this.userId, actual.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.userId,  this.type);
  }

  @Override
  public String toString() {
      switch(type) {
          case owner:
            return "OWNER";
          case viewer:
          case me:
            return "VIEWER";
          case userId:
            return "USER(" + userId + ')';
          default:
              return "UNKNOWN";
        }

  }
}

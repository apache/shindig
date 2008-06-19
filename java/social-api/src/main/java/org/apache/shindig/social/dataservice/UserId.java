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
package org.apache.shindig.social.dataservice;

import org.apache.shindig.common.SecurityToken;

import org.apache.commons.lang.StringUtils;

public class UserId {
  public enum Type {
    viewer, owner, userId
  }

  Type type;
  String userId;

  public UserId(Type type, String userId) {
    this.type = type;
    this.userId = userId;
  }

  public String getUserId(SecurityToken token) {
    switch(type) {
      case owner:
        return token.getOwnerId();
      case viewer:
        return token.getViewerId();
      case userId:
        return userId;
      default:
        throw new IllegalStateException("The type field is not a valid enum: " + type);
    }
  }

  public static UserId fromJson(String jsonId) {
    try {
      Type idSpecEnum = Type.valueOf(jsonId.substring(1));
      return new UserId(idSpecEnum, null);
    } catch (IllegalArgumentException e) {
      return new UserId(Type.userId, jsonId);
    }
  }

  // These are overriden so that EasyMock doesn't throw a fit
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof UserId)) {
      return false;
    }

    UserId actual = (UserId) o;
    return this.type == actual.type
        && StringUtils.equals(this.userId, actual.userId);
  }

  @Override
  public int hashCode() {
    int userHashCode = this.userId == null ? 0 : this.userId.hashCode();
    return this.type.hashCode() + userHashCode;
  }
}

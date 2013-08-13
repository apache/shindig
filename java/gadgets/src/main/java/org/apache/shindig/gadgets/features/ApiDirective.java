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
package org.apache.shindig.gadgets.features;

/**
 * Represents a single &lt;exports&gt; or &lt;uses&gt; tag in a
 * feature manifest. These in turn provide context to compiler/optimizer
 * code and container code (for gadgets.rpc service IDs).
 */
public class ApiDirective {
  public enum Type {
    JS("js"),
    RPC("rpc");

    private final String code;

    private Type(String code) {
      this.code = code;
    }

    public static Type fromCode(String code) {
      for (Type value : Type.values()) {
        if (value.code.equals(code)) {
          return value;
        }
      }
      return null;
    }
  }

  private final Type type;
  private final String value;
  private final boolean isUses;

  ApiDirective(String type, String value, boolean isUses) {
    this.type = Type.fromCode(type);
    this.value = value;
    this.isUses = isUses;
  }

  public Type getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public boolean isUses() {
    return isUses;
  }

  public boolean isExports() {
    return !isUses;
  }
}

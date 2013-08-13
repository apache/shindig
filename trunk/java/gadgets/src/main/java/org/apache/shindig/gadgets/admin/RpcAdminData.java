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
package org.apache.shindig.gadgets.admin;

import java.util.Set;

import com.google.caja.util.Sets;
import com.google.common.base.Objects;

/**
 * Represents RPC administration data.
 *
 * @since 2.5.0
 */
public class RpcAdminData {

  private Set<String> additionalRpcServiceIds;

  public RpcAdminData() {
    this.additionalRpcServiceIds = Sets.newHashSet();
  }

  /**
   * Constructor.
   *
   * @param additionalRpcServiceIds
   *          Additional RPC service IDs to allow for the container.
   */
  public RpcAdminData(Set<String> additionalRpcServiceIds) {
    if (additionalRpcServiceIds == null) {
      additionalRpcServiceIds = Sets.newHashSet();
    }
    this.additionalRpcServiceIds = additionalRpcServiceIds;
  }

  /**
   * Gets the additional RPC service IDs allowed for the container.
   *
   * @return The additional RPC service IDs allowed for the container.
   */
  public Set<String> getAdditionalRpcServiceIds() {
    return additionalRpcServiceIds;
  }

  /**
   * Sets the additional RPC service IDs allowed for the container.
   *
   * @param ids
   *          The additional RPC service IDs to allow for the container.
   */
  public void setAdditionalRpcServiceIds(Set<String> ids) {
    if(ids == null) {
      ids = Sets.newHashSet();
    }
    this.additionalRpcServiceIds = ids;
  }

  /**
   * Adds an additional RPC service ID for the container.
   *
   * @param id
   *          The additional RPC service ID to allow for this container.
   */
  public void addAdditionalRpcServiceId(String id) {
    if (id != null && id.length() > 0) {
      this.additionalRpcServiceIds.add(id);
    }
  }

  /**
   * Removes a RPC service ID for the container.
   *
   * @param id
   *          The RPC service ID to remove for this container.
   */
  public void removeAdditionalRpcServiceId(String id) {
    this.additionalRpcServiceIds.remove(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RpcAdminData) {
      RpcAdminData test = (RpcAdminData) obj;
      return test.getAdditionalRpcServiceIds().equals(this.getAdditionalRpcServiceIds());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.additionalRpcServiceIds);
  }
}

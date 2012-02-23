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

import com.google.common.base.Objects;

/**
 * Information about the container's administration data.
 *
 * @since 2.5.0
 */
public class GadgetAdminData {
  // In the future as more gadget admin data is created we
  // should add it here.
  private FeatureAdminData featureAdminData;
  private RpcAdminData rpcAdminData;

  /**
   * Constructor
   */
  public GadgetAdminData() {
    this.featureAdminData = new FeatureAdminData();
    this.rpcAdminData = new RpcAdminData();
  }

  /**
   * Constructor
   *
   * @param featureAdminData
   *          Feature administration data for this gadget
   * @param rpcAdminData
   *          RPC administration data for this gadget
   */
  public GadgetAdminData(FeatureAdminData featureAdminData,
          RpcAdminData rpcAdminData) {
    if (featureAdminData == null) {
      featureAdminData = new FeatureAdminData();
    }
    if (rpcAdminData == null) {
      rpcAdminData = new RpcAdminData();
    }
    this.featureAdminData = featureAdminData;
    this.rpcAdminData = rpcAdminData;
  }

  /**
   * Gets the feature administration data for this gadget.
   *
   * @return
   */
  public FeatureAdminData getFeatureAdminData() {
    return this.featureAdminData;
  }

  /**
   * Sets the feature admin data.
   *
   * @param featureAdminData
   *          the feature admin data to set.
   */
  public void setFeatureAdminData(FeatureAdminData featureAdminData) {
    if(featureAdminData == null) {
      featureAdminData = new FeatureAdminData();
    }
    this.featureAdminData = featureAdminData;
  }

  /**
   * Gets the RPC administration data.
   *
   * @return
   */
  public RpcAdminData getRpcAdminData() {
    return this.rpcAdminData;
  }

  /**
   * Sets the RPC administration data.
   *
   * @param rpcAdminData
   *          The RPC administration data to set.
   */
  public void setRpcAdminData(RpcAdminData rpcAdminData) {
    if(rpcAdminData == null) {
      rpcAdminData = new RpcAdminData();
    }
    this.rpcAdminData = rpcAdminData;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof GadgetAdminData) {
      GadgetAdminData test = (GadgetAdminData) obj;
      return this.getFeatureAdminData().equals(test.getFeatureAdminData()) &&
      this.getRpcAdminData().equals(test.getRpcAdminData());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.featureAdminData, this.rpcAdminData);
  }
}

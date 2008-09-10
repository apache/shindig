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

package org.apache.shindig.gadgets.oauth;

/**
 * Index into the token store by  
 */
public class BasicOAuthStoreConsumerIndex {
  private String gadgetUri;
  private String serviceName;

  public String getGadgetUri() {
    return gadgetUri;
  }
  public void setGadgetUri(String gadgetUri) {
    this.gadgetUri = gadgetUri;
  }
  public String getServiceName() {
    return serviceName;
  }
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((gadgetUri == null) ? 0 : gadgetUri.hashCode());
    result =
        prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final BasicOAuthStoreConsumerIndex other = (BasicOAuthStoreConsumerIndex) obj;
    if (gadgetUri == null) {
      if (other.gadgetUri != null) return false;
    } else if (!gadgetUri.equals(other.gadgetUri)) return false;
    if (serviceName == null) {
      if (other.serviceName != null) return false;
    } else if (!serviceName.equals(other.serviceName)) return false;
    return true;
  }
}

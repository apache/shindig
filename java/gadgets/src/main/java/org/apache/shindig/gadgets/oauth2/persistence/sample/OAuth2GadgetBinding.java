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
package org.apache.shindig.gadgets.oauth2.persistence.sample;

/**
 * Binds a gadget to a client.
 *
 */
public class OAuth2GadgetBinding {
  private final boolean allowOverride;
  private final String clientName;
  private final String gadgetServiceName;
  private final String gadgetUri;

  public OAuth2GadgetBinding(final String gadgetUri, final String gadgetServiceName,
      final String clientName, final boolean allowOverride) {
    this.gadgetUri = gadgetUri;
    this.gadgetServiceName = gadgetServiceName;
    this.clientName = clientName;
    this.allowOverride = allowOverride;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof OAuth2GadgetBinding)) {
      return false;
    }
    final OAuth2GadgetBinding other = (OAuth2GadgetBinding) obj;
    if (this.gadgetUri == null) {
      if (other.gadgetUri != null) {
        return false;
      }
    } else if (!this.gadgetUri.equals(other.gadgetUri)) {
      return false;
    }
    if (this.gadgetServiceName == null) {
      if (other.gadgetServiceName != null) {
        return false;
      }
    } else if (!this.gadgetServiceName.equals(other.gadgetServiceName)) {
      return false;
    }
    return true;
  }

  public String getClientName() {
    return this.clientName;
  }

  public String getGadgetServiceName() {
    return this.gadgetServiceName;
  }

  public String getGadgetUri() {
    return this.gadgetUri;
  }

  @Override
  public int hashCode() {
    if ((this.gadgetUri != null) && (this.gadgetServiceName != null)) {
      return (this.gadgetUri + ':' + this.gadgetServiceName).hashCode();
    }

    return 0;
  }

  public boolean isAllowOverride() {
    return this.allowOverride;
  }

  @Override
  public String toString() {
    return "org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2GadgetBinding: gadgetUri = "
        + this.gadgetUri + " , gadgetServiceName = " + this.gadgetServiceName
        + " , allowOverride = " + this.allowOverride;
  }
}

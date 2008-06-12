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
package org.apache.shindig.common.testing;

import org.apache.shindig.common.SecurityToken;

/**
 * A fake SecurityToken implementation to help testing.
 */
public class FakeGadgetToken implements SecurityToken {

  private String updatedToken;
  private String trustedJson;

  public FakeGadgetToken() {
    this(null, null);
  }

  public FakeGadgetToken(String updatedToken) {
    this(updatedToken, null);
  }

  public FakeGadgetToken(String updatedToken, String trustedJson) {
    this.updatedToken = updatedToken;
    this.trustedJson = trustedJson;
  }

  public String getOwnerId() {
    return "owner";
  }

  public String getViewerId() {
    return "viewer";
  }

  public String getAppId() {
    return "app1234";
  }

  public String getDomain() {
    return "domain";
  }

  public String toSerialForm() {
    return "";
  }

  public String getAppUrl() {
    return "http://www.example.com/app.xml";
  }

  public long getModuleId() {
    return 0;
  }

  public String getUpdatedToken() {
    return updatedToken;
  }

  public String getTrustedJson() {
    return trustedJson;
  }
}

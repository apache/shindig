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
package org.apache.shindig.social.opensocial.oauth;

import org.apache.shindig.social.core.oauth.DelegatedPrincipal;

import net.oauth.OAuthMessage;

import java.io.IOException;
import java.util.logging.Logger;

public class OAuthPrincipal extends DelegatedPrincipal {

  private static Logger log = Logger.getLogger(OAuthPrincipal.class.getName());

  private OAuthMessage message;
  private String delegator;

  public OAuthPrincipal(OAuthMessage message, String delegator) {
    this.message = message;
    this.delegator = delegator;
  }

  @Override
  public String getDelegatee() {
    try {
      return message.getConsumerKey();
    } catch (IOException e) {
      log.warning("no consumer key in OAuth message - returning null");
      return null;
    }
  }

  @Override
  public String getDelegator() {
    return delegator;
  }
}

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
package org.apache.shindig.social.oauth;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthProblemException;

/**
 * Interface describing a service that can be used to look up keying material
 * needed to verify incoming "two-legged" OAuth requests.
 */
public interface OAuthConsumerStore {

  /**
   * Returns an OAuthAccessor (which includes the consumer secret needed for
   * request verification) for the given consumer key.
   *
   * @param consumerKey the consumer key (oauth_consumer_key) from the HTTP
   *        request.
   * @return an OAuthAccessor that can be passed, along with an OAuthMessage,
   *         to an OAuthValidator to validate the message. The token secret
   *         in the OAuthAccessor will be set to the empty string.
   * @throws OAuthProblemException if the information associated with the
   *         consumer. The reasons may include unknown or rejected consumer
   *         keys, etc.
   */
  OAuthAccessor getAccessor(String consumerKey) throws OAuthProblemException;
}

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

import org.apache.shindig.auth.OAuthUtil;
import org.apache.shindig.gadgets.http.HttpResponse;

import com.google.common.collect.ImmutableSet;

import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

import java.util.Set;

/**
 * Implements the
 * <a href="http://wiki.oauth.net/ProblemReporting">
 * OAuth problem reporting extension</a>
 *
 * We divide problems into three categories:
 * - problems that cause us to abort the protocol.  For example, if we don't
 *   have a consumer key that the service provider accepts, we give up.
 *
 * - problems that cause us to ask for the user's permission again.  For
 *   example, if the service provider reports that an access token has been
 *   revoked, we throw away the token and start over.
 *
 * - problems that require us to refresh our access token per the OAuth
 *   session extension protocol
 *
 * By default we assume most service provider errors fall into the second
 * category: we should ask for the user's permission again.
 */
class OAuthProtocolException extends Exception {

  /**
   * Problems that should force us to abort the protocol right away,
   * and next time the user visits ask them for permission again.
   */
  private static Set<String> fatalProblems =
      ImmutableSet.of("version_rejected",
                      "signature_method_rejected",
                      "consumer_key_unknown",
                      "consumer_key_rejected",
                      "timestamp_refused");

  /**
   * Problems that should force us to abort the protocol right away,
   * but we can still try to use the access token again later.
   */
  private static Set<String> temporaryProblems =
      ImmutableSet.of("consumer_key_refused");

  /**
   * Problems that should have us try to refresh the access token.
   */
  private static Set<String> extensionProblems =
      ImmutableSet.of("access_token_expired");

  private final boolean canRetry;

  private final boolean startFromScratch;

  private final boolean canExtend;

  private final String problemCode;

  public OAuthProtocolException(int status, OAuthMessage reply) {
    String problem = OAuthUtil.getParameter(reply, OAuthProblemException.OAUTH_PROBLEM);
    if (problem == null) {
      throw new IllegalArgumentException("No problem reported for OAuthProtocolException");
    }
    this.problemCode = problem;
    if (fatalProblems.contains(problem)) {
      startFromScratch = true;
      canRetry = false;
      canExtend = false;
    } else if (temporaryProblems.contains(problem)) {
      startFromScratch = false;
      canRetry = false;
      canExtend = false;
    } else if (extensionProblems.contains(problem)) {
      startFromScratch = false;
      canRetry = true;
      canExtend = true;
    } else {
      // fallback to status to figure out behavior
      if (status == HttpResponse.SC_UNAUTHORIZED) {
        startFromScratch = true;
        canRetry = true;
      } else {
        startFromScratch = false;
        canRetry = false;
      }
      canExtend = false;
    }
  }

  /**
   * Handle OAuth protocol errors for SPs that don't support the problem
   * reporting extension
   * @param status HTTP status code, assumed to be between 400 and 499 inclusive
   */
  public OAuthProtocolException(int status) {
    if (status == HttpResponse.SC_UNAUTHORIZED) {
      startFromScratch = true;
      canRetry = true;
    } else {
      startFromScratch = false;
      canRetry = false;
    }
    canExtend = false;
    problemCode = null;
  }

  /**
   * @return true if we've gotten confused to the point where we should give
   * up and ask the user for approval again.
   */
  public boolean startFromScratch() {
    return startFromScratch;
  }

  /**
   * @return true if we think we can make progress by attempting the protocol
   * flow again (which may require starting from scratch).
   */
  public boolean canRetry() {
    return canRetry;
  }

  /**
   * @return true if we think we can make progress by attempting to extend the lifetime of the
   * access token.
   */
  public boolean canExtend() {
    return canExtend;
  }

  /**
   * @return the OAuth problem code (from the problem reporting extension).
   */
  public String getProblemCode() {
    return problemCode;
  }
}

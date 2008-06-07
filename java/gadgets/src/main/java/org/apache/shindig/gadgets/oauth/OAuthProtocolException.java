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
package org.apache.shindig.gadgets.oauth;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.shindig.gadgets.http.HttpResponse;

import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;

/**
 * Implements the
 * <a href="http://wiki.oauth.net/ProblemReporting">
 * OAuth problem reporting extension</a>
 * 
 * We divide problems into two categories:
 * - problems that cause us to abort the protocol.  For example, if we don't
 *   have a consumer key that the service provider accepts, we give up.
 *   
 * - problems that cause us to ask for the user's permission again.  For
 *   example, if the service provider reports that an access token has been
 *   revoked, we throw away the token and start over.
 *   
 * By default we assume most service provider errors fall into the second
 * category: we should ask for the user's permission again.
 *   
 * TODO: add a third category to cope with reauthorization per the ScalableOAuth
 * extension.
 * 
 * TODO(beaton) test case
 */
class OAuthProtocolException extends Exception {

  /**
   * Problems that should force us to abort the protocol right away,
   * and next time the user visits ask them for permission again.
   */
  private static Set<String> fatalProblems;
  
  /**
   * Problems that should force us to abort the protocol right away,
   * but we can still try to use the access token again later.
   */
  private static Set<String> temporaryProblems;
  
  static {
    fatalProblems = new HashSet<String>();
    fatalProblems.add("version_rejected");
    fatalProblems.add("signature_method_rejected");
    fatalProblems.add("consumer_key_unknown");
    fatalProblems.add("consumer_key_rejected");
    fatalProblems.add("timestamp_refused");
    
    temporaryProblems = new HashSet<String>();
    temporaryProblems.add("consumer_key_refused");
  }
  
  private final String problemCode;
  private final String problemText;
  
  private final boolean canRetry;

  private final boolean startFromScratch;
  
  OAuthProtocolException(boolean canRetry) {
    this.problemCode = null;
    this.problemText = null;
    this.canRetry = canRetry;
    this.startFromScratch = false;
  }
  
  public OAuthProtocolException(OAuthMessage reply) throws IOException {
    String problem = reply.getParameter(OAuthProblemException.OAUTH_PROBLEM);
    if (problem == null) {
      throw new IllegalArgumentException(
          "No problem reported for OAuthProtocolException");
    }
    this.problemCode = problem;
    this.problemText = reply.getParameter("oauth_problem_advice");
    if (fatalProblems.contains(problem)) {
      startFromScratch = true;
      canRetry = false;
    } else if (temporaryProblems.contains(problem)) {
      startFromScratch = false;
      canRetry = false;
    } else {
      startFromScratch = true;
      canRetry = true;
    }
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

  public HttpResponse getResponseForGadget() {
    HttpResponse response = new HttpResponse(0, null, null);
    // Inch towards opensocial-0.8: this is very much an experiment, don't
    // hesitate to change it if you've got something better.
    response.getMetadata().put("oauthError", problemCode);
    response.getMetadata().put("oauthErrorText", problemText);
    return response;
  }

}

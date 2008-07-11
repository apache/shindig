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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import net.oauth.OAuthMessage;

import org.apache.shindig.gadgets.http.HttpResponse;
import org.junit.Test;

/**
 * Test cases for OAuthProtocolException
 */
public class OAuthProtocolExceptionTest {

  @Test
  public void testProblemReportingExtension() throws Exception {
    OAuthMessage m = new OAuthMessage(null, null, null);
    m.addParameter("oauth_problem", "consumer_key_refused");
    m.addParameter("oauth_problem_advice", "stuff it");
    OAuthProtocolException e = new OAuthProtocolException(m);
    assertFalse(e.canRetry());
    HttpResponse r = e.getResponseForGadget();
    Map<String, String> metadata = r.getMetadata();
    assertEquals("stuff it", metadata.get("oauthErrorText"));
    assertEquals("consumer_key_refused", metadata.get("oauthError"));
  }
}

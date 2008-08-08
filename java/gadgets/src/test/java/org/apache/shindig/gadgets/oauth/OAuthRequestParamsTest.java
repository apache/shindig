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
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.spec.Preload;
import org.junit.Test;

/**
 * Tests parameter parsing
 */
public class OAuthRequestParamsTest {
 
  @Test
  public void testInitFromPreload() throws Exception {
    String xml = "<Preload href='http://www.example.com' " +
    		"oauth_service_name='service' " +
    		"OAUTH_TOKEN_NAME='token' " +
    		"OAUTH_REQuest_token='requesttoken' " +
    		"oauth_request_token_secret='tokensecret' " +
    		"/>";

    Preload preload = new Preload(XmlUtil.parse(xml));
    OAuthRequestParams params = new OAuthRequestParams(preload);
    assertEquals("service", params.getServiceName());
    assertEquals("token", params.getTokenName());
    assertEquals("requesttoken", params.getRequestToken());
    assertEquals("tokensecret", params.getRequestTokenSecret());
    assertNull(params.getOrigClientState());
    assertFalse(params.getBypassSpecCache());
  }
}

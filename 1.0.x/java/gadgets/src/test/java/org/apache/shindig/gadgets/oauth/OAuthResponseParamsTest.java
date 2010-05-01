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

import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class OAuthResponseParamsTest {

  private BlobCrypter crypter;
  
  @Before
  public void setUp() {
    crypter = new BasicBlobCrypter("abcdefafadfaxxxx".getBytes());
  }
  
  @Test
  public void testSetAndGet() {
    OAuthResponseParams params = new OAuthResponseParams(crypter);
    params.getNewClientState().setAccessToken("access");
    params.setAznUrl("aznurl");
    params.setError(OAuthError.BAD_OAUTH_CONFIGURATION);
    params.setErrorText("errortext");
    assertEquals("access", params.getNewClientState().getAccessToken());
    assertEquals("aznurl", params.getAznUrl());
    assertEquals(OAuthError.BAD_OAUTH_CONFIGURATION, params.getError());
    assertEquals("errortext", params.getErrorText());    
  }
  
  @Test
  public void testAddParams() {
    OAuthResponseParams params = new OAuthResponseParams(crypter);
    params.getNewClientState().setAccessToken("access");
    params.setAznUrl("aznurl");
    params.setError(OAuthError.BAD_OAUTH_CONFIGURATION);
    params.setErrorText("errortext");
    HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    params.addToResponse(responseBuilder);
    HttpResponse response = responseBuilder.create();
    assertEquals("BAD_OAUTH_CONFIGURATION", response.getMetadata().get("oauthError"));
    assertEquals("errortext", response.getMetadata().get("oauthErrorText"));
    assertEquals("aznurl", response.getMetadata().get("oauthApprovalUrl"));
    assertNotNull(response.getMetadata().get("oauthState"));
    assertTrue(response.getMetadata().get("oauthState").length() > 10);
  }
    
  @Test
  public void testAddEmptyParams() {
    OAuthResponseParams params = new OAuthResponseParams(crypter);
    HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    params.addToResponse(responseBuilder);
    HttpResponse response = responseBuilder.create();
    assertTrue(response.getMetadata().isEmpty());
  }

}

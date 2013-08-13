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
package org.apache.shindig.gadgets.oauth2;

import java.net.URI;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.junit.Assert;
import org.junit.Test;

public class OAuth2ResponseParamsTest {
  @Test
  public void testOAuth2ResponseParams_1() throws Exception {

    final OAuth2ResponseParams result = new OAuth2ResponseParams();
    Assert.assertNotNull(result);
    Assert.assertEquals(null, result.getAuthorizationUrl());
  }

  @Test
  public void testAddDebug_1() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final String message = "";

    fixture.addDebug(message);
  }

  @Test
  public void testAddRequestTrace_1() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder().setStrictNoCache();
    final HttpRequest request = new HttpRequest(Uri.fromJavaUri(new URI("")));
    final HttpResponse response = responseBuilder.create();

    fixture.addRequestTrace(request, response);
  }

  @Test
  public void testAddToResponse_1() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = "";
    final String errorDescription = null;
    final String errorUri = null;
    final String errorExplanation = null;

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_2() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = "";
    final String errorDescription = "";
    final String errorUri = "";
    final String errorExplanation = null;

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_3() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = "";
    final String errorDescription = null;
    final String errorUri = "";
    final String errorExplanation = "";

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_4() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = "";
    final String errorDescription = "";
    final String errorUri = null;
    final String errorExplanation = "";

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_5() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = null;
    final String errorDescription = "";
    final String errorUri = "";
    final String errorExplanation = "";

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_6() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = "";
    final String errorDescription = "";
    final String errorUri = "";
    final String errorExplanation = "";

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_7() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug((String) null);
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = null;
    final String errorDescription = null;
    final String errorUri = null;
    final String errorExplanation = null;

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_8() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug((String) null);
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = "";
    final String errorDescription = "";
    final String errorUri = "";
    final String errorExplanation = "";

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_9() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = null;
    final String errorDescription = null;
    final String errorUri = null;
    final String errorExplanation = null;

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_10() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = "";
    final String errorDescription = "";
    final String errorUri = "";
    final String errorExplanation = "";

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_11() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug((String) null);
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = null;
    final String errorDescription = null;
    final String errorUri = null;
    final String errorExplanation = null;

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_12() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug((String) null);
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = "";
    final String errorDescription = null;
    final String errorUri = null;
    final String errorExplanation = null;

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_13() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug((String) null);
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = null;
    final String errorDescription = null;
    final String errorUri = "";
    final String errorExplanation = null;

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_14() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug((String) null);
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = null;
    final String errorDescription = "";
    final String errorUri = null;
    final String errorExplanation = null;

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_15() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug((String) null);
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = null;
    final String errorDescription = null;
    final String errorUri = null;
    final String errorExplanation = "";

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testAddToResponse_16() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug((String) null);
    fixture.setAuthorizationUrl("");
    final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
    final String errorCode = null;
    final String errorDescription = "";
    final String errorUri = "";
    final String errorExplanation = "";

    fixture.addToResponse(responseBuilder, errorCode, errorDescription, errorUri, errorExplanation);
  }

  @Test
  public void testGetAuthorizationUrl_1() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");

    final String result = fixture.getAuthorizationUrl();

    Assert.assertEquals("", result);
  }

  @Test
  public void testSetAuthorizationUrl_1() throws Exception {
    final OAuth2ResponseParams fixture = new OAuth2ResponseParams();
    fixture.addDebug("");
    fixture.setAuthorizationUrl("");
    final String authorizationUrl = "";

    fixture.setAuthorizationUrl(authorizationUrl);
  }
}

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
package org.apache.shindig.gadgets.servlet;

import com.google.common.collect.ImmutableList;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.process.ProcessingException;
import org.apache.shindig.protocol.conversion.BeanDelegator;
import org.apache.shindig.protocol.conversion.BeanFilter;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class GadgetHandlerServiceTest extends EasyMockTestCase {

  private static final String TOKEN = "<token data>";
  private static final String OWNER = "<owner>";
  private static final String VIEWER = "<viewer>";
  private static final String CONTAINER = "container";

  private final BeanDelegator delegator = new BeanDelegator(
    GadgetsHandlerService.apiClasses, GadgetsHandlerService.enumConversionMap);

  private final FakeProcessor processor = new FakeProcessor();
  private final FakeIframeUriManager urlGenerator = new FakeIframeUriManager();

  private FakeSecurityTokenCodec tokenCodec;
  private GadgetsHandlerService gadgetHandler;

  @Before
  public void setUp() {
    tokenCodec = new FakeSecurityTokenCodec();
    gadgetHandler = new GadgetsHandlerService(processor, urlGenerator,
        tokenCodec, new BeanFilter());
  }

  // Next test verify that the API data classes are configured correctly.
  // The mapping is done using reflection in runtime, so this test verify mapping is complete
  // this test will prevent from not intended change to the API.
  // DO NOT REMOVE TEST
  @Test
  public void testHandlerDataDelegation() throws Exception {
    delegator.validate();
  }


  @Test
  public void testGetMetadata() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, CONTAINER, "view",
        createTokenData(null, null), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
    assertEquals(FakeIframeUriManager.DEFAULT_IFRAME_URI.toString(), response.getIframeUrl());
    assertEquals(1, response.getViews().size());
    assertTrue(response.getViews().get("default").getContent().contains("Hello, world" ));
    assertEquals(FakeProcessor.SPEC_TITLE, response.getModulePrefs().getTitle());
    assertEquals(FakeProcessor.LINK_HREF,
        response.getModulePrefs().getLinks().get(FakeProcessor.LINK_REL).getHref().toString());
    assertEquals(FakeProcessor.LINK_REL,
        response.getModulePrefs().getLinks().get(FakeProcessor.LINK_REL).getRel());
    assertEquals(1, response.getUserPrefs().size());
    assertEquals("up_one", response.getUserPrefs().get("up_one").getDisplayName());
    assertEquals(4, response.getUserPrefs().get("up_one").getEnumValues().size());
    verify();
  }

  @Test
  public void testGetMetadataOnlyView() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, CONTAINER, null,
        createTokenData(null, null), ImmutableList.of("views.*"));
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
    assertNull(response.getIframeUrl());
    assertNull(response.getUserPrefs());
    assertNull(response.getModulePrefs());
    assertEquals(FakeProcessor.SPEC_URL, response.getUrl());
    assertEquals(1, response.getViews().size());
    assertTrue(response.getViews().get("default").getContent().contains("Hello, world" ));
    verify();
  }


  @Test(expected = ProcessingException.class)
  public void testGetMetadataNoContainer() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, null, null,
        createTokenData(null, null), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
  }


  @Test(expected = ProcessingException.class)
  public void testGetMetadataNoUrl() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        null, CONTAINER, null,
        createTokenData(null, null), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetMetadataNoFields() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, CONTAINER, null,
        createTokenData(null, null), null);
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetMetadataBadGadget() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        Uri.parse("unknown"), CONTAINER, null,
        createTokenData(null, null), null);
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
  }

  @Test
  public void testGetMetadataNoToken() throws Exception {
    GadgetsHandlerApi.MetadataRequest request = createMetadataRequest(
        FakeProcessor.SPEC_URL, CONTAINER, "view", null, ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.MetadataResponse response = gadgetHandler.getMetadata(request);
    assertEquals(FakeIframeUriManager.DEFAULT_IFRAME_URI.toString(), response.getIframeUrl());
    verify();
  }

  @Test
  public void testGetToken() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, CONTAINER,
        createTokenData(OWNER, VIEWER), ImmutableList.of("*"));
    replay();
    tokenCodec.encodedToken = TOKEN;
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
    assertEquals(TOKEN, response.getToken());
    assertEquals(OWNER, tokenCodec.tokenData.getOwnerId());
    assertEquals(VIEWER, tokenCodec.tokenData.getViewerId());
    assertEquals(CONTAINER, tokenCodec.tokenData.getContainer());
    verify();
  }

  @Test(expected = ProcessingException.class)
  public void testGetTokenNoContainer() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, null,
        createTokenData(OWNER, VIEWER), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetTokenNoUrl() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        null, CONTAINER,
        createTokenData(OWNER, VIEWER), ImmutableList.of("*"));
    replay();
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
  }

  @Test(expected = ProcessingException.class)
  public void testGetTokenNoFields() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, CONTAINER,
        createTokenData(OWNER, VIEWER), null);
    replay();
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
  }

  @Test(expected = SecurityTokenException.class)
  public void testGetTokenException() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, CONTAINER,
        createTokenData(OWNER, VIEWER), ImmutableList.of("*"));
    replay();
    tokenCodec.exc = new SecurityTokenException("bad data");
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
  }

  @Test
  public void testGetTokenNoToken() throws Exception {
    GadgetsHandlerApi.TokenRequest request = createTokenRequest(
        FakeProcessor.SPEC_URL, CONTAINER,
        null, ImmutableList.of("*"));
    replay();
    tokenCodec.encodedToken = TOKEN;
    GadgetsHandlerApi.TokenResponse response = gadgetHandler.getToken(request);
    assertEquals(TOKEN, response.getToken());
    assertNull(CONTAINER, tokenCodec.tokenData);
    verify();
  }

  private GadgetsHandlerApi.TokenData createTokenData(String ownerId, String viewerId) {
    GadgetsHandlerApi.TokenData token = mock(GadgetsHandlerApi.TokenData.class);
    if (ownerId != null) {
      EasyMock.expect(token.getOwnerId()).andReturn(ownerId).once();
    }
    if (viewerId != null) {
      EasyMock.expect(token.getViewerId()).andReturn(viewerId).once();
    }
    return token;
  }

  private GadgetsHandlerApi.MetadataRequest createMetadataRequest(Uri url, String container,
      String view, GadgetsHandlerApi.TokenData token, List<String> fields) {
    GadgetsHandlerApi.MetadataRequest request = mock(GadgetsHandlerApi.MetadataRequest.class);
    EasyMock.expect(request.getFields()).andReturn(fields).anyTimes();
    EasyMock.expect(request.getView()).andReturn(view).once();
    EasyMock.expect(request.getUrl()).andReturn(url).anyTimes();
    EasyMock.expect(request.getContainer()).andReturn(container).anyTimes();
    EasyMock.expect(request.getToken()).andReturn(token).once();
    return request;
  }

  private GadgetsHandlerApi.TokenRequest createTokenRequest(Uri url, String container,
      GadgetsHandlerApi.TokenData token, List<String> fields) {
    GadgetsHandlerApi.TokenRequest request = mock(GadgetsHandlerApi.TokenRequest.class);
    EasyMock.expect(request.getFields()).andReturn(fields).anyTimes();
    EasyMock.expect(request.getUrl()).andReturn(url).anyTimes();
    EasyMock.expect(request.getContainer()).andReturn(container).anyTimes();
    EasyMock.expect(request.getToken()).andReturn(token).once();
    return request;
  }

  private class FakeSecurityTokenCodec implements SecurityTokenCodec {
    public SecurityTokenException exc = null;
    public SecurityToken tokenData = null;
    public String encodedToken = null;

    public String encodeToken(SecurityToken token) throws SecurityTokenException {
      tokenData = token;
      if (exc != null) {
        throw exc;
      }
      return encodedToken;
    }

    public SecurityToken createToken(Map<String, String> tokenParameters)
        throws SecurityTokenException {
      if (exc != null) {
        throw exc;
      }
      return tokenData;
    }
  }
}


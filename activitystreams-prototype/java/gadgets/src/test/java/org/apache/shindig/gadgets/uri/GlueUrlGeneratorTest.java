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
package org.apache.shindig.gadgets.uri;

import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.eq;
import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.isA;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.UrlValidationStatus;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

/**
 * Tests that GlueUrlGenerator properly delegates to XUriManager classes.
 */
public class GlueUrlGeneratorTest {
  private static final Uri URI = Uri.parse("http://apache.org/path?query=foo");
  private static final Gadget GADGET = new Gadget();
  private static final Collection<String> LIBS = Lists.newArrayList("feature", "another");
  
  private IframeUriManager iframeManager;
  private JsUriManager jsManager;
  private OAuthUriManager oauthManager;
  private GlueUrlGenerator generator;
  
  @Before
  public void setUp() {
    iframeManager = createMock(IframeUriManager.class);
    jsManager = createMock(JsUriManager.class);
    oauthManager = createMock(OAuthUriManager.class);
    generator = new GlueUrlGenerator(iframeManager, jsManager, oauthManager);
  }
  
  @Test
  public void getIframeUrl() {
    expect(iframeManager.makeRenderingUri(GADGET)).andReturn(URI).once();
    replayAll();
    String uri = generator.getIframeUrl(GADGET);
    assertEquals(URI.toString(), uri);
    verifyAll();
  }
  
  @Test
  public void validateIframeUrlVersioned() {
    expect(iframeManager.validateRenderingUri(URI)).andReturn(UriStatus.VALID_VERSIONED).once();
    replayAll();
    UrlValidationStatus status = generator.validateIframeUrl(URI.toString());
    assertEquals(status, UrlValidationStatus.VALID_VERSIONED);
    verifyAll();
  }
  
  @Test
  public void validateIframeUrlUnversioned() {
    expect(iframeManager.validateRenderingUri(URI)).andReturn(UriStatus.VALID_UNVERSIONED).once();
    replayAll();
    UrlValidationStatus status = generator.validateIframeUrl(URI.toString());
    assertEquals(status, UrlValidationStatus.VALID_UNVERSIONED);
    verifyAll();
  }
  
  @Test
  public void validateIframeUrlInvalid() {
    expect(iframeManager.validateRenderingUri(URI)).andReturn(UriStatus.INVALID_VERSION).once();
    replayAll();
    UrlValidationStatus status = generator.validateIframeUrl(URI.toString());
    assertEquals(status, UrlValidationStatus.INVALID);
    verifyAll();
  }
  
  @Test
  public void validateIframeUrlInvalidOther() {
    expect(iframeManager.validateRenderingUri(URI)).andReturn(UriStatus.INVALID_DOMAIN).once();
    replayAll();
    UrlValidationStatus status = generator.validateIframeUrl(URI.toString());
    assertEquals(status, UrlValidationStatus.INVALID);
    verifyAll();
  }
  
  @Test
  public void getBundledJsUrl() {
    expect(jsManager.makeExternJsUri(isA(Gadget.class), eq(LIBS))).andReturn(URI).once();
    replayAll();
    String jsUri = generator.getBundledJsUrl(LIBS, GADGET.getContext());
    assertEquals(URI.toString(), jsUri);
    verifyAll();
  }
  
  @Test
  public void validateJsUrlVersioned() {
    expect(jsManager.processExternJsUri(URI)).andReturn(jsUri(UriStatus.VALID_VERSIONED)).once();
    replayAll();
    UrlValidationStatus status = generator.validateJsUrl(URI.toString());
    assertEquals(status, UrlValidationStatus.VALID_VERSIONED);
    verifyAll();
  }
  
  @Test
  public void validateJsUrlUnversioned() {
    expect(jsManager.processExternJsUri(URI)).andReturn(jsUri(UriStatus.VALID_UNVERSIONED)).once();
    replayAll();
    UrlValidationStatus status = generator.validateJsUrl(URI.toString());
    assertEquals(status, UrlValidationStatus.VALID_UNVERSIONED);
    verifyAll();
  }
  
  @Test
  public void validateJsUrlInvalid() {
    expect(jsManager.processExternJsUri(URI)).andReturn(jsUri(UriStatus.INVALID_VERSION)).once();
    replayAll();
    UrlValidationStatus status = generator.validateJsUrl(URI.toString());
    assertEquals(status, UrlValidationStatus.INVALID);
    verifyAll();
  }
  
  @Test
  public void validateJsUrlInvalidOther() {
    expect(jsManager.processExternJsUri(URI)).andReturn(jsUri(UriStatus.INVALID_DOMAIN)).once();
    replayAll();
    UrlValidationStatus status = generator.validateJsUrl(URI.toString());
    assertEquals(status, UrlValidationStatus.INVALID);
    verifyAll();
  }
  
  @Test
  public void getGadgetDomainOAuthCallback() {
    String container = "container";
    String host = "host";
    expect(oauthManager.makeOAuthCallbackUri(container, host)).andReturn(URI).once();
    replayAll();
    String uri = generator.getGadgetDomainOAuthCallback(container, host);
    assertEquals(URI.toString(), uri);
    verifyAll();
  }
  
  private void replayAll() {
    replay(iframeManager, jsManager, oauthManager);
  }
  
  private void verifyAll() {
    verify(iframeManager, jsManager, oauthManager);
  }
  
  private JsUriManager.JsUri jsUri(UriStatus status) {
    return new JsUriManager.JsUri(status, LIBS);
  }
}

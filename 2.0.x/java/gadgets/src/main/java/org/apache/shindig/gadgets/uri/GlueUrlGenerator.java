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

import java.util.Collection;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.UrlGenerator;
import org.apache.shindig.gadgets.UrlValidationStatus;

import com.google.inject.Inject;

/**
 * A bridge between the old UrlGenerator interface and the
 * new separated interfaces for Iframe, JS, and OAuth Uri generation.
 * 
 * This class makes it possible to seamlessly switch between the
 * old and the new, particularly while the new method has its
 * subtleties worked out.
 */
public final class GlueUrlGenerator implements UrlGenerator {
  private final IframeUriManager iframeManager;
  private final JsUriManager jsManager;
  private final OAuthUriManager oauthManager;
  
  @Inject
  public GlueUrlGenerator(IframeUriManager iframeManager,
                          JsUriManager jsManager,
                          OAuthUriManager oauthManager) {
    this.iframeManager = iframeManager;
    this.jsManager = jsManager;
    this.oauthManager = oauthManager;
  }

  public String getIframeUrl(Gadget gadget) {
    Uri iframeUri = iframeManager.makeRenderingUri(gadget);
    return iframeUri.toString();
  }

  public UrlValidationStatus validateIframeUrl(String url) {
    Uri iframeUri = Uri.parse(url);
    UriStatus uriStatus = iframeManager.validateRenderingUri(iframeUri);
    return translateStatus(uriStatus);
  }

  public String getBundledJsUrl(Collection<String> features, GadgetContext context) {
    Gadget gadget = new Gadget().setContext(context);
    Uri jsUri = jsManager.makeExternJsUri(gadget, features);
    return jsUri.toString();
  }

  public UrlValidationStatus validateJsUrl(String url) {
    Uri jsUri = Uri.parse(url);
    JsUriManager.JsUri uriStatus = jsManager.processExternJsUri(jsUri);
    return translateStatus(uriStatus.getStatus());
  }

  public String getGadgetDomainOAuthCallback(String container, String gadgetHost) {
    Uri oauthUri = oauthManager.makeOAuthCallbackUri(container, gadgetHost);
    return oauthUri.toString();
  }
  
  private UrlValidationStatus translateStatus(UriStatus uriStatus) {
    switch (uriStatus) {
    case VALID_UNVERSIONED:
      return UrlValidationStatus.VALID_UNVERSIONED;
    case VALID_VERSIONED:
      return UrlValidationStatus.VALID_VERSIONED;
    default:
      break;
    }
    return UrlValidationStatus.INVALID;
  }
}

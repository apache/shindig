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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.oauth2.logger.FilteredLogger;
import org.apache.shindig.gadgets.spec.BaseOAuthService.EndPoint;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.OAuth2Service;
import org.apache.shindig.gadgets.spec.OAuth2Spec;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

/**
 * Higher-level interface that allows callers to store and retrieve
 * OAuth2-related data directly from {@code GadgetSpec}s, {@code GadgetContext}
 * s, etc. See {@link OAuth2Store} for a more detailed explanation of the OAuth
 * 2.0 Data Store.
 */
public class GadgetOAuth2TokenStore {

  private final static String LOG_CLASS = GadgetOAuth2TokenStore.class.getName();
  private static final FilteredLogger LOG = FilteredLogger
      .getFilteredLogger(GadgetOAuth2TokenStore.LOG_CLASS);

  private static class OAuth2SpecInfo {
    private final String authorizationUrl;
    private final String scope;
    private final String tokenUrl;

    public OAuth2SpecInfo(final String authorizationUrl, final String tokenUrl, final String scope) {
      this.authorizationUrl = authorizationUrl;
      this.tokenUrl = tokenUrl;
      this.scope = scope;
    }

    public String getAuthorizationUrl() {
      return this.authorizationUrl;
    }

    public String getScope() {
      return this.scope;
    }

    public String getTokenUrl() {
      return this.tokenUrl;
    }
  }

  private final GadgetSpecFactory specFactory;

  private final OAuth2Store store;

  @Inject
  public GadgetOAuth2TokenStore(final OAuth2Store store, final GadgetSpecFactory specFactory) {
    this.store = store;
    this.specFactory = specFactory;
    if (GadgetOAuth2TokenStore.LOG.isLoggable()) {
      GadgetOAuth2TokenStore.LOG.log("this.store = {0}", this.store);
      GadgetOAuth2TokenStore.LOG.log("this.specFactory = {0}", this.specFactory);
    }
  }

  private GadgetSpec findSpec(final SecurityToken securityToken, final OAuth2Arguments arguments,
      final Uri gadgetUri) throws OAuth2RequestException {
    final boolean isLogging = GadgetOAuth2TokenStore.LOG.isLoggable();
    if (isLogging) {
      GadgetOAuth2TokenStore.LOG.entering(GadgetOAuth2TokenStore.LOG_CLASS, "findSpec",
          new Object[] { arguments, gadgetUri });
    }

    GadgetSpec ret;

    try {
      final GadgetContext context = new OAuth2GadgetContext(securityToken, arguments, gadgetUri);
      ret = this.specFactory.getGadgetSpec(context);
    } catch (final IllegalArgumentException e) {
      if (isLogging) {
        GadgetOAuth2TokenStore.LOG.log("Error finding GadgetContext " + gadgetUri.toString(), e);
      }
      throw new OAuth2RequestException(OAuth2Error.GADGET_SPEC_PROBLEM, gadgetUri.toString(), e);
    } catch (final GadgetException e) {
      if (isLogging) {
        GadgetOAuth2TokenStore.LOG.log("Error finding GadgetContext " + gadgetUri.toString(), e);
      }
      throw new OAuth2RequestException(OAuth2Error.GADGET_SPEC_PROBLEM, gadgetUri.toString(), e);
    }

    if (isLogging) {
      // this is cumbersome in the logs, just return whether or not it's null
      if (ret == null) {
        GadgetOAuth2TokenStore.LOG.exiting(GadgetOAuth2TokenStore.LOG_CLASS, "findSpec", null);
      } else {
        GadgetOAuth2TokenStore.LOG.exiting(GadgetOAuth2TokenStore.LOG_CLASS, "findSpec",
            "non-null spec omitted from logs");
      }
    }

    return ret;
  }

  /**
   * Retrieves and merges the data from the {@link OAuth2Store}, the gadget spec
   * and the request itself to populate the OAuth2 data for this requets.
   *
   * @param securityToken
   *          {@link SecurityToken} from the request
   * @param arguments
   *          {@link OAuth2Arguments} from the request
   * @param gadgetUri
   *          gadget uri from the request
   * @return the {@link OAuth2Accessor} for the request
   * @throws OAuth2RequestException
   */
  public OAuth2Accessor getOAuth2Accessor(final SecurityToken securityToken,
      final OAuth2Arguments arguments, final Uri gadgetUri) {

    final boolean isLogging = GadgetOAuth2TokenStore.LOG.isLoggable();
    if (isLogging) {
      GadgetOAuth2TokenStore.LOG.entering(GadgetOAuth2TokenStore.LOG_CLASS, "getOAuth2Accessor",
          new Object[] { securityToken, arguments, gadgetUri });
    }

    OAuth2Accessor ret = null;

    if ((this.store == null) || (gadgetUri == null) || (securityToken == null)) {
      ret = new BasicOAuth2Accessor(null, OAuth2Error.GET_OAUTH2_ACCESSOR_PROBLEM,
          "OAuth2Accessor missing a param --- store = " + this.store + " , gadgetUri = "
              + gadgetUri + " , securityToken = " + securityToken, "");
    } else {
      final String serviceName = arguments != null ? arguments.getServiceName() : "";

      OAuth2SpecInfo specInfo = null;
      try {
        specInfo = this.lookupSpecInfo(securityToken, arguments, gadgetUri);
      } catch (final OAuth2RequestException e1) {
        if (isLogging) {
          GadgetOAuth2TokenStore.LOG.log("No gadget spec", e1);
        }
        ret = new BasicOAuth2Accessor(e1, OAuth2Error.NO_GADGET_SPEC, "gadgetUri = " + gadgetUri
            + " , serviceName = " + serviceName, "");
      }

      if (specInfo == null) {
        ret = new BasicOAuth2Accessor(null, OAuth2Error.NO_GADGET_SPEC, "gadgetUri = " + gadgetUri
            + " , serviceName = " + serviceName, "");
      }

      if (ret == null && arguments != null) {
        String scope = arguments.getScope();
        if ((scope == null) || (scope.length() == 0)) {
          // no scope on request, default to module prefs scope
          scope = specInfo.getScope();
        }

        if ((scope == null) || (scope.length() == 0)) {
          scope = "";
        }

        OAuth2Accessor persistedAccessor;
        try {
          persistedAccessor = this.store.getOAuth2Accessor(gadgetUri.toString(), serviceName,
              securityToken.getViewerId(), scope);
        } catch (final GadgetException e) {
          if (isLogging) {
            GadgetOAuth2TokenStore.LOG.log("Exception in getOAuth2Accessor", e);
          }
          persistedAccessor = null;
        }

        if (persistedAccessor == null) {
          ret = new BasicOAuth2Accessor(null, OAuth2Error.GET_OAUTH2_ACCESSOR_PROBLEM,
              "gadgetUri = " + gadgetUri + " , serviceName = " + serviceName, "");
        } else {
          final OAuth2Accessor mergedAccessor = new BasicOAuth2Accessor(persistedAccessor);

          if (persistedAccessor.isAllowModuleOverrides()) {
            final String specAuthorizationUrl = specInfo.getAuthorizationUrl();
            final String specTokenUrl = specInfo.getTokenUrl();

            if ((specAuthorizationUrl != null) && (specAuthorizationUrl.length() > 0)) {
              mergedAccessor.setAuthorizationUrl(specAuthorizationUrl);
            }
            if ((specTokenUrl != null) && (specTokenUrl.length() > 0)) {
              mergedAccessor.setTokenUrl(specTokenUrl);
            }
          }

          this.store.storeOAuth2Accessor(mergedAccessor);

          ret = mergedAccessor;
        }
      }
    }

    if (isLogging) {
      GadgetOAuth2TokenStore.LOG
          .exiting(GadgetOAuth2TokenStore.LOG_CLASS, "getOAuth2Accessor", ret);
    }

    return ret;
  }

  /**
   *
   * @return the {@link OAuth2Store}, never <code>null</code>
   */
  public OAuth2Store getOAuth2Store() {
    return this.store;
  }

  private OAuth2SpecInfo lookupSpecInfo(final SecurityToken securityToken,
      final OAuth2Arguments arguments, final Uri gadgetUri) throws OAuth2RequestException {

    final boolean isLogging = GadgetOAuth2TokenStore.LOG.isLoggable();
    if (isLogging) {
      GadgetOAuth2TokenStore.LOG.entering(GadgetOAuth2TokenStore.LOG_CLASS, "lookupSpecInfo",
          new Object[] { securityToken, arguments, gadgetUri });
    }

    final GadgetSpec spec = this.findSpec(securityToken, arguments, gadgetUri);
    final OAuth2Spec oauthSpec = spec.getModulePrefs().getOAuth2Spec();
    if (oauthSpec == null) {
      throw new OAuth2RequestException(OAuth2Error.LOOKUP_SPEC_PROBLEM,
          "Failed to retrieve OAuth URLs, spec for gadget " + securityToken.getAppUrl()
              + " does not contain OAuth element.", null);
    }
    final OAuth2Service service = oauthSpec.getServices().get(arguments.getServiceName());
    if (service == null) {
      throw new OAuth2RequestException(OAuth2Error.LOOKUP_SPEC_PROBLEM,
          "Failed to retrieve OAuth URLs, spec for gadget does not contain OAuth service "
              + arguments.getServiceName() + ".  Known services: "
              + Joiner.on(',').join(oauthSpec.getServices().keySet()) + '.', null);
    }

    String authorizationUrl = null;
    final EndPoint authorizationUrlEndpoint = service.getAuthorizationUrl();
    if (authorizationUrlEndpoint != null) {
      authorizationUrl = authorizationUrlEndpoint.url.toString();
    }

    String tokenUrl = null;
    final EndPoint tokenUrlEndpoint = service.getTokenUrl();
    if (tokenUrlEndpoint != null) {
      tokenUrl = tokenUrlEndpoint.url.toString();
    }

    final OAuth2SpecInfo ret = new OAuth2SpecInfo(authorizationUrl, tokenUrl, service.getScope());

    if (isLogging) {
      GadgetOAuth2TokenStore.LOG.exiting(GadgetOAuth2TokenStore.LOG_CLASS, "lookupSpecInfo", ret);
    }

    return ret;
  }
}

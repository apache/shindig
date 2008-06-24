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

import net.oauth.OAuthServiceProvider;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.OAuthService;
import org.apache.shindig.gadgets.spec.OAuthSpec;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Higher-level interface that allows callers to store and retrieve
 * OAuth-related data directly from {@code GadgetSpec}s, {@code GadgetContext}s,
 * etc. See {@link OAuthStore} for a more detailed explanation of the OAuth
 * Data Store.
 */
public class GadgetOAuthTokenStore {

  /**
   * Internal class used to communicate results of parsing the gadget spec
   * between methods.
   */
  static class GadgetInfo {
    private String serviceName;
    private OAuthStore.ProviderInfo providerInfo;

    public String getServiceName() {
      return serviceName;
    }
    public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
    }
    public OAuthStore.ProviderInfo getProviderInfo() {
      return providerInfo;
    }
    public void setProviderInfo(OAuthStore.ProviderInfo providerInfo) {
      this.providerInfo = providerInfo;
    }
  }

  private static final Logger log =
      Logger.getLogger(GadgetOAuthTokenStore.class.getName());

  private final OAuthStore store;

  private final GadgetSpecFactory specFactory;

  /**
   * Public constructor.
   *
   * @param store an {@link OAuthStore} that can store and retrieve OAuth
   *              tokens, as well as information about service providers.
   */
  public GadgetOAuthTokenStore(OAuthStore store,
      GadgetSpecFactory specFactory) {
    this.store = store;
    this.specFactory = specFactory;
  }

  /**
   * Stores a negotiated consumer key and secret in the gadget store.
   * The "secret" can either be a consumer secret in the strict OAuth sense,
   * or it can be a PKCS8-then-Base64 encoded private key that we'll be using
   * with this service provider.
   *
   * @param gadgetUrl the URL of the gadget
   * @param serviceName the service provider with whom we have negotiated a
   *                    consumer key and secret.
   * @throws OAuthStoreException if there is a problem talking to the
   *                             backend store.
   * @throws OAuthNoDataException if there is no data about this service
   *                              provider stored for this gadget.
   */
  public void storeConsumerKeyAndSecret(
      URI gadgetUrl,
      String serviceName,
      OAuthStore.ConsumerKeyAndSecret keyAndSecret)
        throws OAuthStoreException, OAuthNoDataException {

    OAuthStore.ProviderKey providerKey = new OAuthStore.ProviderKey();
    providerKey.setGadgetUri(gadgetUrl.toString());
    providerKey.setServiceName(serviceName);

    store.setOAuthConsumerKeyAndSecret(providerKey, keyAndSecret);
  }

  /**
   * Stores an access token in the OAuth Data Store.
   * @param tokenKey information about the Gadget storing the token.
   * @param tokenInfo the TokenInfo to be stored in the OAuth data store.
   * @throws OAuthStoreException
   */
  public void storeTokenKeyAndSecret(OAuthStore.TokenKey tokenKey,
                                     OAuthStore.TokenInfo tokenInfo)
      throws OAuthStoreException {

    if (isEmpty(tokenKey.getGadgetUri())) {
      throw new IllegalArgumentException("found empty gadget URI in TokenKey");
    }

    if (isEmpty(tokenKey.getUserId())) {
      throw new IllegalArgumentException("found empty userId in TokenKey");
    }

    store.setTokenAndSecret(tokenKey, tokenInfo);
  }

  /**
   * Removes an access token from the OAuth data store.
   *
   * @return true if the token is removed, false if the token was not found.
   * @throws OAuthStoreException if we can't communicate with the token store.
   */
  public boolean removeToken(OAuthStore.TokenKey tokenKey)
      throws OAuthStoreException {
    try {
      store.removeToken(tokenKey);
      return true;
    } catch (OAuthNoDataException e) {
      return false;
    }
  }

  /**
   * Retrieve an OAuthAccessor that is ready to sign OAuthMessages.
   *
   * @param tokenKey information about the gadget retrieving the accessor.
   *
   * @return an OAuthAccessorInfo containing an OAuthAccessor (which can be
   *         passed to an OAuthMessage.sign method), as well as httpMethod and
   *         signatureType fields.
   * @throws OAuthNoDataException if the token couldn't be found
   * @throws OAuthStoreException if an error occurred accessing the data
   *                             store.
   */
  public OAuthStore.AccessorInfo getOAuthAccessor(OAuthStore.TokenKey tokenKey,
      boolean bypassSpecCache)
      throws GadgetException {

    if (isEmpty(tokenKey.getGadgetUri())) {
      throw new OAuthStoreException("found empty gadget URI in TokenKey");
    }

    if (isEmpty(tokenKey.getUserId())) {
      throw new OAuthStoreException("found empty userId in TokenKey");
    }

    GadgetSpec spec;
    try {
      spec = specFactory.getGadgetSpec(
          new URI(tokenKey.getGadgetUri()),
          bypassSpecCache);
    } catch (URISyntaxException e) {
      throw new OAuthStoreException("could not fetch gadget spec, gadget URI " +
          "invalid", e);
    }

    OAuthStore.ProviderInfo provInfo;
    provInfo = getProviderInfo(spec, tokenKey.getServiceName());

    return store.getOAuthAccessor(tokenKey, provInfo);
  }

  /**
   * Reads OAuth provider information out of gadget spec.
   * @param spec
   * @return a GadgetInfo
   * @throws GadgetException if some information is missing, or something else
   *                         is wrong with the spec.
   */
  public static OAuthStore.ProviderInfo getProviderInfo(
      GadgetSpec spec, String serviceName) throws GadgetException {

    OAuthSpec oauthSpec = spec.getModulePrefs().getOAuthSpec();

    if (oauthSpec == null) {
      String message = "gadget spec is missing /ModulePrefs/OAuth section";
      log.warning(message);
      throw new GadgetException(GadgetException.Code.MISSING_PARAMETER,
                                message);
    }
    
    OAuthService service = oauthSpec.getServices().get(serviceName);
    if (service == null) {
      StringBuilder message = new StringBuilder();
      message.append("Spec does not contain OAuth service '");
      message.append(serviceName);
      message.append("'.  Known services: ");
      Iterator<String> known = oauthSpec.getServices().keySet().iterator();
      while (known.hasNext()) {
        message.append("'");
        message.append(known.next());
        message.append("'");
        if (known.hasNext()) {
          message.append(", ");
        }
      }
      log.warning(message.toString());
      throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
          message.toString());
    }

    OAuthServiceProvider provider = new OAuthServiceProvider(
        service.getRequestUrl().url.toASCIIString(),
        service.getAuthorizationUrl().toASCIIString(),
        service.getAccessUrl().url.toASCIIString());

    OAuthStore.HttpMethod httpMethod;
    switch(service.getRequestUrl().method) {
      case GET:
        httpMethod = OAuthStore.HttpMethod.GET;
        break;
      case POST:
      default:
        httpMethod = OAuthStore.HttpMethod.POST;
        break;
    }
 
    OAuthStore.OAuthParamLocation paramLocation;
    switch(service.getRequestUrl().location) {
      case URL:
        paramLocation = OAuthStore.OAuthParamLocation.URI_QUERY;
        break;
      case BODY:
        paramLocation = OAuthStore.OAuthParamLocation.POST_BODY;
        break;
      case HEADER:
      default:
        paramLocation = OAuthStore.OAuthParamLocation.AUTH_HEADER;
        break;
    }

    OAuthStore.ProviderInfo provInfo = new OAuthStore.ProviderInfo();
    provInfo.setHttpMethod(httpMethod);
    provInfo.setParamLocation(paramLocation);

    // TODO: for now, we'll just set the signature type to HMAC_SHA1
    // as this will be ignored later on when retrieving consumer information.
    // There, if we find a negotiated HMAC key, we will use HMAC_SHA1. If we
    // find a negotiated RSA key, we will use RSA_SHA1. And if we find neither,
    // we may use RSA_SHA1 with a default signing key.
    provInfo.setSignatureType(OAuthStore.SignatureType.HMAC_SHA1);
    provInfo.setProvider(provider);

    return provInfo;
  }

  static boolean isEmpty(String string) {
    return (string == null) || (string.trim().length() == 0);
  }
}

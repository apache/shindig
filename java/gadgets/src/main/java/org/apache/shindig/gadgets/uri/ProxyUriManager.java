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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.gadgets.AuthType;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth2.OAuth2Arguments;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.util.List;

/**
 * Generates Uris used by the /proxy servlet
 * @since 2.0.0
 */
public interface ProxyUriManager {
  /**
   * Generate a Uri that proxies the given resource Uri.
   *
   * @param resource Resource Uri to proxy
   * @param forcedRefresh Forced expires value to use for resource
   * @return Uri of proxied resource
   */
  List<Uri> make(List<ProxyUri> resource, Integer forcedRefresh);

  public static class ProxyUri extends ProxyUriBase {
    private final Uri resource;
    private String fallbackUrl;
    private Integer resizeHeight;
    private Integer resizeWidth;
    private Integer resizeQuality;
    private boolean resizeNoExpand;
    private SecurityToken securityToken;
    private AuthType authType;
    private OAuth2Arguments oauth2Arguments;
    private OAuthArguments oauthArguments;

    // If "true" then the original content should be returned to the user
    // instead of internal server errors.
    @VisibleForTesting
    String returnOriginalContentOnError;

    // The html tag that requested this ProxyUri.
    private String htmlTagContext;

    // The User-Agent from the request
    private String userAgent;

    public ProxyUri(Gadget gadget, Uri resource) {
      super(gadget);
      this.resource = resource;
      if (AccelUriManager.CONTAINER.equals(gadget.getContext().getContainer())) {
        setReturnOriginalContentOnError(true);
      }
      if(authType == null) {
        authType = AuthType.NONE;
      }
    }

    public ProxyUri(Integer refresh, boolean debug, boolean noCache,
        String container, String gadget, Uri resource) {
      super(UriStatus.VALID_UNVERSIONED, refresh, debug, noCache, container, gadget);
      this.resource = resource;
      if (AccelUriManager.CONTAINER.equals(container)) {
        setReturnOriginalContentOnError(true);
      }
      if(authType == null) {
        authType = AuthType.NONE;
      }
    }

    public ProxyUri(UriStatus status, Uri resource, Uri base) {
      super(status, base);
      this.resource = resource;
    }

    @VisibleForTesting
    public void setReturnOriginalContentOnError(boolean returnOriginalContentOnError) {
      this.returnOriginalContentOnError = returnOriginalContentOnError ? "1" : null;
    }

    public void setUserAgent(String ua) {
      this.userAgent = ua;
    }
    public String getUserAgent() {
      return userAgent;
    }
    public void setHtmlTagContext(String htmlTagContext) {
      this.htmlTagContext = htmlTagContext;
    }
    public String getHtmlTagContext() {
      return htmlTagContext;
    }
    public SecurityToken getSecurityToken() {
      return securityToken;
    }
    public AuthType getAuthType() {
      return authType;
    }
    public OAuthArguments getOAuthArguments() {
      return oauthArguments;
    }
    public OAuth2Arguments getOAuth2Arguments() {
      return oauth2Arguments;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof ProxyUri)) {
        return false;
      }
      ProxyUri objUri = (ProxyUri) obj;
      return (super.equals(obj)
          && Objects.equal(this.resource, objUri.resource)
          && Objects.equal(this.fallbackUrl, objUri.fallbackUrl)
          && Objects.equal(this.resizeHeight, objUri.resizeHeight)
          && Objects.equal(this.resizeWidth, objUri.resizeWidth)
          && Objects.equal(this.resizeQuality, objUri.resizeQuality)
          && Objects.equal(this.resizeNoExpand, objUri.resizeNoExpand)
          && Objects.equal(this.returnOriginalContentOnError, objUri.returnOriginalContentOnError)
          && Objects.equal(this.htmlTagContext, objUri.htmlTagContext)
          && Objects.equal(this.securityToken, objUri.securityToken)
          && Objects.equal(this.authType, objUri.authType))
          && Objects.equal(this.oauthArguments, objUri.oauthArguments)
          && Objects.equal(this.oauth2Arguments, objUri.oauth2Arguments);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(super.hashCode(), resource, fallbackUrl, resizeHeight,
              resizeWidth, resizeQuality, resizeNoExpand, returnOriginalContentOnError,
              htmlTagContext, securityToken, authType, oauthArguments, oauth2Arguments);
    }

    /* (non-Javadoc)
     * @see org.apache.shindig.gadgets.uri.ProxyUriBase#setFromUri(org.apache.shindig.common.uri.Uri)
     */
    @Override
    public void setFromUri(Uri uri) {
      super.setFromUri(uri);
      if (uri != null) {
        fallbackUrl = uri.getQueryParameter(Param.FALLBACK_URL_PARAM.getKey());
        resizeHeight = getIntegerValue(uri.getQueryParameter(Param.RESIZE_HEIGHT.getKey()));
        resizeWidth = getIntegerValue(uri.getQueryParameter(Param.RESIZE_WIDTH.getKey()));
        resizeQuality = getIntegerValue(uri.getQueryParameter(Param.RESIZE_QUALITY.getKey()));
        resizeNoExpand = getBooleanValue(uri.getQueryParameter(Param.NO_EXPAND.getKey()));
        returnOriginalContentOnError = uri.getQueryParameter(
            Param.RETURN_ORIGINAL_CONTENT_ON_ERROR.getKey());
        htmlTagContext = uri.getQueryParameter(Param.HTML_TAG_CONTEXT.getKey());
        authType = AuthType.parse(uri.getQueryParameter(Param.AUTHZ.getKey()));
      }
    }

    public ProxyUri setResize(Integer w, Integer h, Integer q, boolean noExpand) {
      this.resizeHeight = h;
      this.resizeWidth = w;
      this.resizeQuality = q;
      this.resizeNoExpand = noExpand;
      return this;
    }

    public ProxyUri setFallbackUrl(String fallbackUrl) {
      this.fallbackUrl = fallbackUrl;
      return this;
    }

    public ProxyUri setSecurityToken(SecurityToken securityToken) {
      this.securityToken = securityToken;
      return this;
    }

    public ProxyUri setAuthType(AuthType authType) {
      this.authType = authType;
      return this;
    }

    public ProxyUri setOAuthArguments(OAuthArguments oauthArgments) {
      this.oauthArguments = oauthArgments;
      return this;
    }

    public ProxyUri setOAuth2Arguments(OAuth2Arguments oauth2Arguments) {
      this.oauth2Arguments = oauth2Arguments;
      return this;
    }

    public Uri getResource() {
      return resource;
    }

    public Uri getFallbackUri() throws GadgetException {
      if (fallbackUrl == null) {
        return null;
      }
      try {
        // Doing delay parsing.
        return Uri.parse(fallbackUrl);
      } catch (IllegalArgumentException e) {
        throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
            Param.FALLBACK_URL_PARAM.getKey() + " param is invalid: "
            + e, HttpResponse.SC_BAD_REQUEST);
      }
    }

    public boolean shouldReturnOrigOnErr() {
      return "1".equals(this.returnOriginalContentOnError) ||
             "true".equalsIgnoreCase(this.returnOriginalContentOnError);
    }

    @Override
    public UriBuilder makeQueryParams(Integer forcedRefresh, String version) {
      UriBuilder builder = super.makeQueryParams(forcedRefresh, version);
      if (resizeHeight != null) {
        builder.addQueryParameter(Param.RESIZE_HEIGHT.getKey(), resizeHeight.toString());
      }
      if (resizeWidth != null) {
        builder.addQueryParameter(Param.RESIZE_WIDTH.getKey(), resizeWidth.toString());
      }
      if (resizeQuality != null) {
        builder.addQueryParameter(Param.RESIZE_QUALITY.getKey(), resizeQuality.toString());
      }
      if (resizeNoExpand) {
        builder.addQueryParameter(Param.NO_EXPAND.getKey(), "1");
      }
      if (fallbackUrl != null) {
        builder.addQueryParameter(Param.FALLBACK_URL_PARAM.getKey(), fallbackUrl);
      }

      if (returnOriginalContentOnError != null) {
        builder.addQueryParameter(Param.RETURN_ORIGINAL_CONTENT_ON_ERROR.getKey(),
                                  returnOriginalContentOnError);
      }
      if (htmlTagContext != null) {
        builder.addQueryParameter(Param.HTML_TAG_CONTEXT.getKey(), htmlTagContext);
      }
      return builder;
    }

    @Override
    public HttpRequest makeHttpRequest(Uri targetUri)
        throws GadgetException {
      HttpRequest req = super.makeHttpRequest(targetUri);
      // Set image params:
      req.setParam(Param.RESIZE_HEIGHT.getKey(), resizeHeight);
      req.setParam(Param.RESIZE_WIDTH.getKey(), resizeWidth);
      req.setParam(Param.RESIZE_QUALITY.getKey(), resizeQuality);
      req.setParam(Param.NO_EXPAND.getKey(), resizeNoExpand ? "1" : "0");

      req.setParam(Param.RETURN_ORIGINAL_CONTENT_ON_ERROR.getKey(),
                   returnOriginalContentOnError);
      req.setParam(Param.HTML_TAG_CONTEXT.getKey(), htmlTagContext);
      req.setSecurityToken(securityToken);
      req.setAuthType(authType);
      if(AuthType.OAUTH.equals(authType)) {
        req.setOAuthArguments(oauthArguments);
      } else if(AuthType.OAUTH2.equals(authType)) {
        req.setOAuth2Arguments(oauth2Arguments);
      }
      return req;
    }

    // Creates new ProxyUri's for the given list of resource uri's. Note that
    // the proxy uri's will have default values for internal parameters.
    public static List<ProxyUri> fromList(Gadget gadget, List<Uri> uris) {
      List<ProxyUri> res = Lists.newArrayListWithCapacity(uris.size());
      for (Uri uri : uris) {
        res.add(new ProxyUri(gadget, uri));
      }
      return res;
    }
  }

  /**
   * Parse and validate the proxied Uri.
   *
   * @param uri A Uri presumed to be a proxied Uri generated
   *     by this class or in a compatible way
   * @return Status of the Uri passed in
   */
  ProxyUri process(Uri uri) throws GadgetException;

  public interface Versioner {
    /**
     * Generates a version for each of the provided resources.
     * @param resources Resources to version.
     * @param container Container making the request
     * @param resourceTags Index-correlated list of html tags, one per resouceUris. Any older
     * implementations can just ignore.
     * @return Index-correlated list of version strings
     */
    List<String> version(List<Uri> resources, String container, List<String> resourceTags);

    /**
     * Validate the version of the resource.
     * @param resource Uri of a proxied resource
     * @param container Container requesting the resource
     * @param value Version value to validate.
     * @return Status of the version.
     */
    UriStatus validate(Uri resource, String container, String value);
  }
}

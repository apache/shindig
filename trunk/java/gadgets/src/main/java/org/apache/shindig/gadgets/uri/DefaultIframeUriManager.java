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

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.LockedDomainService;
import org.apache.shindig.gadgets.UserPrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import com.google.caja.util.Maps;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Default implementation of an IframeUriManager which references the /ifr endpoint.
 */
public class DefaultIframeUriManager implements IframeUriManager {
  // By default, fills in values that could otherwise be templated for client population.
  private static final boolean DEFAULT_USE_TEMPLATES = false;
  static final String IFRAME_BASE_PATH_KEY = "gadgets.uri.iframe.basePath";
  static final String LOCKED_DOMAIN_REQUIRED_KEY = "gadgets.uri.iframe.lockedDomainRequired";
  public static final String LOCKED_DOMAIN_SUFFIX_KEY = "gadgets.uri.iframe.lockedDomainSuffix";
  public static final String UNLOCKED_DOMAIN_KEY = "gadgets.uri.iframe.unlockedDomain";
  public static final String SECURITY_TOKEN_ALWAYS_KEY = "gadgets.uri.iframe.alwaysAppendSecurityToken";
  public static final String LOCKED_DOMAIN_FEATURE_NAME = "locked-domain";
  public static final String SECURITY_TOKEN_FEATURE_NAME = "security-token";
  private TemplatingSignal tplSignal = null;
  private Versioner versioner = null;
  private Authority authority;

  private final ContainerConfig config;
  private final LockedDomainService ldService;
  private final SecurityTokenCodec securityTokenCodec;

  @Inject
  public DefaultIframeUriManager(ContainerConfig config,
                                 LockedDomainService ldService,
                                 SecurityTokenCodec securityTokenCodec) {
    this.config = config;
    this.ldService = ldService;
    this.securityTokenCodec = securityTokenCodec;
  }

  @Inject(optional = true)
  public void setVersioner(Versioner versioner) {
    this.versioner = versioner;
  }

  @Inject(optional = true)
  public void setTemplatingSignal(TemplatingSignal tplSignal) {
    this.tplSignal = tplSignal;
  }

  @Inject(optional = true)
  public void setAuthority(Authority authority) {
    this.authority = authority;
  }

  public Uri makeRenderingUri(Gadget gadget) {
    View view = gadget.getCurrentView();
    return buildUri(view, gadget);
  }

  // The overridable entrance method to build URI for the gadget iframe URI.
  // Implementors should not override this method if not necessary but instead override the builder
  // methods for different parts of the URI.
  protected Uri buildUri(View view, Gadget gadget) {
    UriBuilder uri;
    GadgetContext context = gadget.getContext();
    String container = context.getContainer();

    // Create UriBuilder based on different types of View
    if (View.ContentType.URL.equals(view.getType())) {
      uri = this.processUriForUrlTypeView(view, gadget);
    } else {
      uri = this.processUriForHtmlTypeView(view, gadget);
    }

    boolean useTpl = tplSignal != null ? tplSignal.useTemplates() : DEFAULT_USE_TEMPLATES;

    // Add required/default parameters for the gadget iframe uri
    this.addDefaultUriParameters(uri, gadget, view, useTpl);

    // Add all UserPrefs
    this.addAllUserPrefs(uri, gadget, view, useTpl);

    // Add the version to provide caching if needed
    if (versioner != null) {
      // Added on the query string, obviously not templated.
      addParam(uri, Param.VERSION.getKey(),
          versioner.version(gadget.getSpec().getUrl(), container), false, false);
    }

    // Handle addition of security token to the URI param
    if (wantsSecurityToken(gadget)) {
      boolean securityTokenOnQuery = isTokenNeededForRendering(gadget);

      String securityToken = generateSecurityToken(gadget);
      addParam(uri, Param.SECURITY_TOKEN.getKey(), securityToken, securityToken == null,
          !securityTokenOnQuery);
    }

    // Overridable method to allow additional parameters
    addExtras(uri, gadget);

    return uri.toUri();
  }

  // Overrideable method to add extra logic for URL type gadget view
  protected UriBuilder processUriForUrlTypeView(View view, Gadget gadget) {
    // A. type=url. Initializes all except standard parameters.
    UriBuilder uri = new UriBuilder(view.getHref());

    String container = gadget.getContext().getContainer();
    addExtrasForTypeUrl(uri, gadget, container);
    return uri;
  }

  // Overrideable method to add extra logic for HTML type gadget view
  protected UriBuilder processUriForHtmlTypeView(View view, Gadget gadget) {
    // B. Others, aka. type=html and html_sanitized.
    UriBuilder uri = new UriBuilder();

    GadgetContext context = gadget.getContext();
    String container = context.getContainer();

    // 1. Set base path.
    uri.setPath(getReqVal(container, IFRAME_BASE_PATH_KEY));

    // 2. Set host/authority.
    String ldDomain;
    try {
      ldDomain = ldService.getLockedDomainForGadget(gadget, container);
    } catch (GadgetException e) {
      throw new RuntimeException(e);
    }
    String host = "//" +
        (ldDomain == null ? getReqVal(container, UNLOCKED_DOMAIN_KEY) : ldDomain);

    Uri gadgetUri = Uri.parse(host);
    if (gadgetUri.getAuthority() == null
        && gadgetUri.getScheme() == null
        && gadgetUri.getPath().equals(host)) {
      // This is for backwards compatibility with unlocked domains like
      // "unlockeddomain.com"
      gadgetUri = Uri.parse("//" + host);
    }

    // 3. Set the scheme.
    if (StringUtils.isBlank(gadgetUri.getScheme())) {
      uri.setScheme(getScheme(gadget, container));
    } else {
      uri.setScheme(gadgetUri.getScheme());
    }

    // 4. Set the authority.
    uri.setAuthority(gadgetUri.getAuthority());

    // 5. Add the URL.
    uri.addQueryParameter(Param.URL.getKey(), context.getUrl().toString());

    return uri;
  }

  // Overrideable method to add extra logic default gadget URI parameters
  protected void addDefaultUriParameters(UriBuilder uri, Gadget gadget, View view,
      boolean useTpl) {
    GadgetContext context = gadget.getContext();
    String container = context.getContainer();

    // Add container, whose input derived other components of the URI.
    uri.addQueryParameter(Param.CONTAINER.getKey(), container);

    // Add remaining non-url standard parameters, in templated or filled form.
    addParam(uri, Param.VIEW.getKey(), view.getName(), useTpl, false);
    addParam(uri, Param.LANG.getKey(), context.getLocale().getLanguage(), useTpl, false);
    addParam(uri, Param.COUNTRY.getKey(), context.getLocale().getCountry(), useTpl, false);
    addParam(uri, Param.DEBUG.getKey(), context.getDebug() ? "1" : "0", useTpl, false);
    addParam(uri, Param.NO_CACHE.getKey(), context.getIgnoreCache() ? "1" : "0", useTpl, false);
    addParam(uri, Param.SANITIZE.getKey(), context.getSanitize() ? "1" : "0", useTpl, false);
    if (context.getCajoled()) {
      addParam(uri, Param.CAJOLE.getKey(), "1", useTpl, false);
    }
  }

  // Overrideable method to add extra logic to append user preferences. The default implementation
  // will simply read from the gadget spec for inline user prefs.
  protected void addAllUserPrefs(UriBuilder uri, Gadget gadget, View view, boolean useTpl) {
    GadgetContext context = gadget.getContext();

    UserPrefs prefs = context.getUserPrefs();
    for (UserPref up : gadget.getSpec().getUserPrefs().values()) {
      String name = up.getName();
      String data = prefs.getPref(name);
      if (data == null) {
        data = up.getDefaultValue();
      }

      boolean upInFragment = !view.needsUserPrefSubstitution();
      addParam(uri, UriCommon.USER_PREF_PREFIX + up.getName(), data, useTpl, upInFragment);
    }
  }

  // *** Start overrideable methods to handle generation of security token for the gadget URI ***

  protected String generateSecurityToken(Gadget gadget) {
    // Find a security token in the context
    try {
      SecurityToken token = gadget.getContext().getToken();

      if (securityTokenCodec != null && token != null) {
        return securityTokenCodec.encodeToken(token);
      }
    } catch (SecurityTokenException e) {
      // ignore -- no security token
    }
    return null;
  }

  protected boolean wantsSecurityToken(Gadget gadget) {
    return gadget.getAllFeatures().contains(SECURITY_TOKEN_FEATURE_NAME) ||
           config.getBool(gadget.getContext().getContainer(), SECURITY_TOKEN_ALWAYS_KEY);
  }

  // This method should be overridden to provide better caching characteristics
  // for rendering Uris. In particular, it should return true only when the gadget
  // uses server-side processing of such things as OpenSocial templates, Data pipelining,
  // and Preloads. The default implementation is naive, returning true for all URIs,
  // as this is the conservative result that will always functionally work.
  protected boolean isTokenNeededForRendering(Gadget gadget) {
    return true;
  }

  // *** End overrideable methods to handle generation of security token for the gadget URI ***

  public UriStatus validateRenderingUri(Uri inUri) {
    UriBuilder uri = new UriBuilder(inUri);

    String gadgetStr = uri.getQueryParameter(Param.URL.getKey());
    Uri gadgetUri;
    try {
      gadgetUri = Uri.parse(gadgetStr);
    } catch (Exception e) {
      // RuntimeException eg. InvalidArgumentException
      return UriStatus.BAD_URI;
    }

    String container = uri.getQueryParameter(Param.CONTAINER.getKey());
    if (container == null) {
      container = ContainerConfig.DEFAULT_CONTAINER;
    }

    String version = uri.getQueryParameter(Param.VERSION.getKey());
    if (versioner == null || version == null) {
      return UriStatus.VALID_UNVERSIONED;
    }

    return versioner.validate(gadgetUri, container, version);
  }

  public static String tplKey(String key) {
    return '%' + key + '%';
  }

  protected String getScheme(Gadget gadget, String container) {
    // Scheme-relative by default. Override for specific use cases.
    return null;
  }

  protected void addExtrasForTypeUrl(UriBuilder uri, Gadget gadget, String container) {
    Set<String> features = gadget.getViewFeatures().keySet();
    String jsHost = getReqVal(container, DefaultJsUriManager.JS_HOST_PARAM);
    String jsPathBase = getReqVal(container, DefaultJsUriManager.JS_PATH_PARAM);

    UriBuilder jsuri = null;
    if (features.size() > 0) {
      // We somewhat cheat in that jsHost may contain protocol/scheme as well.
      jsuri = new UriBuilder(Uri.parse(jsHost));

      // Add JS info to path and set it in URI.
      StringBuilder builder = new StringBuilder(jsPathBase);
      if (!jsPathBase.endsWith("/")) {
        builder.append('/');
      }
      builder.append(DefaultJsUriManager.addJsLibs(features));
      builder.append(DefaultJsUriManager.JS_SUFFIX);
      jsuri.setPath(builder.toString());
    }
    addParam(uri, Param.LIBS.getKey(), jsuri == null ? "" : jsuri.toString(), false, false);
  }

  protected void addExtras(UriBuilder uri, Gadget gadget) {
    // Add whatever custom flags are desired here.
  }

  private void addParam(UriBuilder uri, String key, String data, boolean templated,
      boolean fragment) {
    String value;
    if (templated) {
      value = tplKey(key);
    } else {
      value = data;
    }

    if (!fragment) {
      uri.addQueryParameter(key, value);
    } else {
      uri.addFragmentParameter(key, value);
    }
  }

  private String getReqVal(String container, String key) {
    String val = config.getString(container, key);

    if (val == null) {
      throw new RuntimeException("Missing required container config param, key: "
          + key + ", container: " + container);
    }
    if (authority != null) {
      val = val.replace("%authority%", authority.getAuthority());
    }

    return val;
  }

  @ImplementedBy(DefaultTemplatingSignal.class)
  public static interface TemplatingSignal {
    boolean useTemplates();
  }

  public static final class DefaultTemplatingSignal implements TemplatingSignal {
    private boolean useTemplates = true;

    @Inject(optional = true)
    public void setUseTemplates(
        @Named("shindig.urlgen.use-templates-default") Boolean useTemplates) {
      this.useTemplates = useTemplates;
    }

    public boolean useTemplates() {
      return useTemplates;
    }
  }

  /**
   * Returns a list of all URIs for rendering this gadget.  The map is
   * keyed by the view name.
   * @param gadget The gadget to generate the URIs for.
   * @return A map of URIs indexed by the view name.
   */
  public Map<String, Uri> makeAllRenderingUris(Gadget gadget) {
    Map<String, Uri> uris = Maps.newHashMap();
    Map<String, View> views = gadget.getSpec().getViews();
    for(String key : views.keySet()) {
      View view = views.get(key);
      uris.put(key, buildUri(view, gadget));
    }
    return uris;
  }
}

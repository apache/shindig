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

import com.google.common.collect.Lists;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenCodec;
import org.apache.shindig.auth.SecurityTokenException;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.UriBuilder;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.UserPrefs;
import org.apache.shindig.gadgets.spec.UserPref;
import org.apache.shindig.gadgets.spec.View;
import org.apache.shindig.gadgets.uri.UriCommon.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Default implementetion of an IframeUriManager which references the /ifr endpoint.
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
  private boolean ldEnabled = true;
  private TemplatingSignal tplSignal = null;
  private Versioner versioner = null;
  
  private final ContainerConfig config;
  private final LockedDomainPrefixGenerator ldGen;
  private final SecurityTokenCodec securityTokenCodec;

  private final List<String> ldSuffixes;

  @Inject
  public DefaultIframeUriManager(ContainerConfig config,
                                 LockedDomainPrefixGenerator ldGen,
                                 SecurityTokenCodec securityTokenCodec) {
    this.config = config;
    this.ldGen = ldGen;
    this.securityTokenCodec = securityTokenCodec;
    
    Collection<String> containers = config.getContainers();
    List<String> ldSuffixes = Lists.newArrayListWithCapacity(containers.size());
    for (String container : containers) {
      ldSuffixes.add(getReqVal(container, LOCKED_DOMAIN_SUFFIX_KEY));
    }
    this.ldSuffixes = Collections.unmodifiableList(ldSuffixes);
  }
  
  @Inject(optional = true)
  public void setLockedDomainEnabled(
      @Named("shindig.locked-domain.enabled") Boolean ldEnabled) {
    this.ldEnabled = ldEnabled;
  }
  
  @Inject(optional = true)
  public void setVersioner(Versioner versioner) {
    this.versioner = versioner;
  }
  
  @Inject(optional = true)
  public void setTemplatingSignal(TemplatingSignal tplSignal) {
    this.tplSignal = tplSignal;
  }
  
  public Uri makeRenderingUri(Gadget gadget) {
    UriBuilder uri;
    View view = gadget.getCurrentView();
    
    GadgetContext context = gadget.getContext();
    String container = context.getContainer();
    
    if (View.ContentType.URL.equals(view.getType())) {
      // A. type=url. Initializes all except standard parameters.
      uri = new UriBuilder(view.getHref());
    } else {
      // B. Others, aka. type=html and html_sanitized.
      uri = new UriBuilder();

      // 1. Set base path.
      uri.setPath(getReqVal(container, IFRAME_BASE_PATH_KEY));
    
      // 2. Set host/authority.
      String host;
      if (usingLockedDomain(gadget, container)) {
        host = ldGen.getLockedDomainPrefix(gadget.getSpec().getUrl()) +
            getReqVal(container, LOCKED_DOMAIN_SUFFIX_KEY);
      } else {
        host = getReqVal(container, UNLOCKED_DOMAIN_KEY);
      }
      uri.setAuthority(host);
    
      // 3. Set protocol/schema.
      uri.setScheme(getScheme(gadget, container));
      
      // 4. Add the URL.
      uri.addQueryParameter(Param.URL.getKey(), context.getUrl().toString());
    }
    
    // Add container, whose input derived other components of the URI.
    uri.addQueryParameter(Param.CONTAINER.getKey(), container);
    
    // Add remaining non-url standard parameters, in templated or filled form.
    boolean useTpl = tplSignal != null ? tplSignal.useTemplates() : DEFAULT_USE_TEMPLATES;
    addParam(uri, Param.VIEW.getKey(), view.getName(), useTpl, false);
    addParam(uri, Param.LANG.getKey(), context.getLocale().getLanguage(), useTpl, false);
    addParam(uri, Param.COUNTRY.getKey(), context.getLocale().getCountry(), useTpl, false);
    addParam(uri, Param.DEBUG.getKey(), context.getDebug() ? "1" : "0", useTpl, false);
    addParam(uri, Param.NO_CACHE.getKey(), context.getIgnoreCache() ? "1" : "0", useTpl, false);
    
    // Add all UserPrefs
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

    if (versioner != null) {
      // Added on the query string, obviously not templated.
      addParam(uri, Param.VERSION.getKey(),
          versioner.version(gadget.getSpec().getUrl(), container), false, false);
    }
    
    if (wantsSecurityToken(gadget)) {
      boolean securityTokenOnQuery = isTokenNeededForRendering(gadget);
      
      String securityToken = generateSecurityToken(gadget);
      addParam(uri, Param.SECURITY_TOKEN.getKey(), securityToken, securityToken == null,
          !securityTokenOnQuery);
    }
    
    addExtras(uri);
    
    return uri.toUri();
  }

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
  
  public UriStatus validateRenderingUri(Uri inUri) {
    UriBuilder uri = new UriBuilder(inUri);
    
    String gadgetStr = uri.getQueryParameter(Param.URL.getKey());
    Uri gadgetUri = null;
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
    
    // Validate domain.
    String host = uri.getAuthority().toLowerCase();
    String gadgetLdPrefix = ldGen.getLockedDomainPrefix(gadgetUri).toLowerCase();

    // If the uri starts with gadget's locked domain prefix, then the suffix
    // must be an exact match as well.
    // Lower-case to prevent casing from being relevant.
    if (ldEnabled && !lockedDomainExclusion()) {
      if (host.startsWith(gadgetLdPrefix)) {
        // Strip off prefix.
        host = host.substring(gadgetLdPrefix.length());
        String ldSuffix = getReqVal(container, LOCKED_DOMAIN_SUFFIX_KEY);
        if (!ldSuffix.equalsIgnoreCase(host)) {
          return UriStatus.INVALID_DOMAIN;
        }
      } else {
        // We need to also ensure that the URI isn't that of another valid
        // locked-domain gadget. We do this test second as it's less efficient.
        // Also, since we've already tested the "valid" locked domain case
        // we can simply say the URI is invalid if it ends with any valid
        // locked domain suffix.
        for (String ldSuffix : ldSuffixes) {
          if (host.endsWith(ldSuffix)) {
            return UriStatus.INVALID_DOMAIN;
          }
        }
      }
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
  
  /** Overridable methods for custom behavior */
  protected boolean lockedDomainExclusion() {
    // Subclass/override this to support a custom notion of dev-mode, other exclusions.
    return false;
  }
  
  protected String getScheme(Gadget gadget, String container) {
    // Scheme-relative by default. Override for specific use cases.
    return null;
  }
  
  protected void addExtras(UriBuilder uri) {
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
  
  private boolean usingLockedDomain(Gadget gadget, String container) {
    if (!ldEnabled) {
      return false;
    }
    
    if (lockedDomainExclusion()) {
      return false;
    }
    
    if (config.getBool(container, LOCKED_DOMAIN_REQUIRED_KEY)) {
      return true;
    }
    
    return gadget.getAllFeatures().contains(LOCKED_DOMAIN_FEATURE_NAME);
  }
  
  private String getReqVal(String container, String key) {
    String val = config.getString(container, key);
    if (val == null) {
      throw new RuntimeException("Missing required container config param, key: "
          + key + ", container: " + container);
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
}

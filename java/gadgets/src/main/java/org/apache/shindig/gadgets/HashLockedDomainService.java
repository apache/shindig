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
package org.apache.shindig.gadgets;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.shindig.common.logging.i18n.MessageKeys;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.uri.Uri.UriException;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.spec.Feature;
import org.apache.shindig.gadgets.uri.LockedDomainPrefixGenerator;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Locked domain implementation based on sha1.
 *
 * The generated domain takes the form:
 *
 * base32(sha1(gadget url)).
 *
 * Other domain locking schemes are possible as well.
 */
/**
 * @author <a href="mailto:dev@shindig.apache.org">Shindig Dev</a>
 * @version $Id: $
 *
 */
@Singleton
public class HashLockedDomainService implements LockedDomainService, ContainerConfig.ConfigObserver {
  //class name for logging purpose
  private static final String classname = HashLockedDomainService.class.getName();
  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);

  private final boolean enabled;
  private boolean lockSecurityTokens = false;
  private final Map<String, String> lockedSuffixes;
  private final Map<String, Boolean> required;
  private Authority authority;
  private LockedDomainPrefixGenerator ldGen;
  private final Pattern authpattern = Pattern.compile("%authority%");

  public static final String LOCKED_DOMAIN_REQUIRED_KEY = "gadgets.uri.iframe.lockedDomainRequired";
  public static final String LOCKED_DOMAIN_SUFFIX_KEY = "gadgets.uri.iframe.lockedDomainSuffix";

  /**
   * Create a LockedDomainService
   * @param config per-container configuration
   * @param enabled whether this service should do anything at all.
   */
  @Inject
  public HashLockedDomainService(ContainerConfig config,
                                 @Named("shindig.locked-domain.enabled") boolean enabled,
                                 LockedDomainPrefixGenerator ldGen) {
    this.enabled = enabled;
    this.ldGen = ldGen;
    lockedSuffixes = Maps.newHashMap();
    required = Maps.newHashMap();
    if (enabled) {
      config.addConfigObserver(this, true);
    }
  }

  /*
   * Injected methods
   */

  @Inject(optional = true)
  public void setAuthority(Authority authority) {
    this.authority = authority;
  }

  /**
   * Allows a renderer to render all gadgets that require a security token on a locked
   * domain. This is recommended security practice, as it secures the token from other
   * gadgets, but because the "security-token" dependency on "locked-domain" is
   * both implicit (added by GadgetSpec code for OAuth elements) and/or transitive
   * (included by opensocial and opensocial-templates features), turning this behavior
   * by default may take some by surprise. As such, we provide this flag. If false
   * (by default), locked-domain will apply only when the gadget's Requires/Optional
   * sections include it. Otherwise, the transitive dependency tree will be traversed
   * to make this decision.
   * @param lockSecurityTokens If true, locks domains for all gadgets requiring security-token.
   */
  @Inject(optional = true)
  public void setLockSecurityTokens(
      @Named("shindig.locked-domain.lock-security-tokens") Boolean lockSecurityTokens) {
    this.lockSecurityTokens = lockSecurityTokens;
  }


  /*
   * Public implmentation
   */

  public void containersChanged(ContainerConfig config, Collection<String> changed, Collection<String> removed) {
    for (String container : changed) {
      String suffix = config.getString(container, LOCKED_DOMAIN_SUFFIX_KEY);
      if (suffix == null) {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.logp(Level.WARNING, classname, "containersChanged", MessageKeys.NO_LOCKED_DOMAIN_CONFIG, new Object[] {container});
        }
      } else {
        lockedSuffixes.put(container, checkSuffix(suffix));
      }
      required.put(container, config.getBool(container, LOCKED_DOMAIN_REQUIRED_KEY));
    }
    for (String container : removed) {
      lockedSuffixes.remove(container);
      required.remove(container);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isSafeForOpenProxy(String host) {
    if (enabled) {
      return !isHostUsingLockedDomain(host);
    }
    return true;
  }

  public boolean isGadgetValidForHost(String host, Gadget gadget, String container) {
    container = getContainer(container);
    if (enabled) {
      if (isGadgetReqestingLocking(gadget) ||
          isHostUsingLockedDomain(host) ||
          isDomainLockingEnforced(container)) {
        String neededHost;
        try {
          neededHost = getLockedDomain(gadget, container);
        } catch (GadgetException e) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.log(Level.WARNING, "Invalid host for call.", e);
          }
          return false;
        }
        return host.equals(neededHost);
      }
    }
    return true;
  }

  public String getLockedDomainForGadget(Gadget gadget, String container) throws GadgetException {
    container = getContainer(container);
    if (enabled && !isExcludedFromLockedDomain(gadget, container)) {
      if (isGadgetReqestingLocking(gadget) || isDomainLockingEnforced(container)) {
        return getLockedDomain(gadget, container);
      }
    }
    return null;
  }

  public boolean isHostUsingLockedDomain(String host) {
    if (enabled) {
      for (String suffix : lockedSuffixes.values()) {
        if (host.endsWith(suffix)) {
          return true;
        }
      }
    }
    return false;
  }

  public String getLockedDomainPrefix(Gadget gadget) throws GadgetException {
    String ret = "";
    if (enabled) {
      ret = ldGen.getLockedDomainPrefix(getLockedDomainParticipants(gadget));
    }
    // Lower-case to prevent casing from being relevant.
    return ret.toLowerCase();
  }


  /*
   * Protected implementation
   */

  /**
   * Override methods for custom behavior
   * Allows you to override locked domain feature requests from a gadget.
   */
  protected boolean isExcludedFromLockedDomain(Gadget gadget, String container) {
    return false;
  }


  /*
   * Private implmentation
   */

  private String getLockedDomain(Gadget gadget, String container)  throws GadgetException {
    String suffix = lockedSuffixes.get(container);
    if (suffix == null) {
      return null;
    }
    return getLockedDomainPrefix(gadget) + suffix;
  }

  /**
   * @see HashLockedDomainService#setLockSecurityTokens(Boolean)
   */
  private boolean isGadgetReqestingLocking(Gadget gadget) {
    if (lockSecurityTokens) {
      return gadget.getAllFeatures().contains("locked-domain");
    }
    return gadget.getViewFeatures().keySet().contains("locked-domain");
  }

  private boolean isDomainLockingEnforced(String container) {
    return required.get(container);
  }

  private String getContainer(String container) {
    if (required.containsKey(container)) {
      return container;
    }
    return ContainerConfig.DEFAULT_CONTAINER;
  }

  private String checkSuffix(String suffix) {
    if (suffix != null) {
      Matcher m = authpattern.matcher(suffix);
      if (m.matches()) {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.warning("You should not be using %authority% replacement in a running environment!");
          LOG.warning("Check your config and specify an explicit locked domain suffix.");
          LOG.warning("Found suffix: " + suffix);
        }
        if (authority != null) {
          suffix = m.replaceAll(authority.getAuthority());
        }
      }
    }
    return suffix;
  }

  private String getLockedDomainParticipants(Gadget gadget) throws GadgetException {
    Map<String, Feature> features = gadget.getSpec().getModulePrefs().getFeatures();
    Feature ldFeature = features.get("locked-domain");

    // This gadget is always a participant.
    Set<String> filtered = new TreeSet<String>();
    filtered.add(gadget.getSpec().getUrl().toString().toLowerCase());

    if (ldFeature != null) {
      Collection<String> participants = ldFeature.getParamCollection("participant");
      for (String participant : participants) {
        // be picky, this should be a valid uri
        try {
          Uri.parse(participant);
        } catch (UriException e) {
          throw new GadgetException(GadgetException.Code.INVALID_PARAMETER,
              "Participant param must be a valid uri", e);
        }
        filtered.add(participant.toLowerCase());
      }
    }

    StringBuilder buffer = new StringBuilder();
    for (String participant : filtered) {
      buffer.append(participant);
    }
    return buffer.toString();
  }
}

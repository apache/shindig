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

import com.google.common.annotations.VisibleForTesting;
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
@Singleton
public class HashLockedDomainService extends AbstractLockedDomainService {

  /**
   * Used to observer locked domain suffixes for this class
   */
  private class HashLockedDomainObserver implements ContainerConfig.ConfigObserver {

    public void containersChanged(ContainerConfig config, Collection<String> changed,
            Collection<String> removed) {
      for (String container : changed) {
        String suffix = config.getString(container, LOCKED_DOMAIN_SUFFIX_KEY);
        if (suffix == null) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.logp(Level.WARNING, classname, "containersChanged",
                    MessageKeys.NO_LOCKED_DOMAIN_CONFIG, new Object[] { container });
          }
        } else {
          HashLockedDomainService.this.lockedSuffixes.put(container, checkSuffix(suffix));
        }
      }
      for (String container : removed) {
        HashLockedDomainService.this.lockedSuffixes.remove(container);
      }
    }
  }

  // class name for logging purpose
  private static final String classname = HashLockedDomainService.class.getName();

  private static final Logger LOG = Logger.getLogger(classname, MessageKeys.MESSAGES);
  private final Map<String, String> lockedSuffixes;
  private Authority authority;
  private LockedDomainPrefixGenerator ldGen;
  private final Pattern authpattern = Pattern.compile("%authority%");

  private HashLockedDomainObserver ldObserver;

  public static final String LOCKED_DOMAIN_SUFFIX_KEY = "gadgets.uri.iframe.lockedDomainSuffix";

  /*
   * Injected methods
   */

  /**
   * Create a LockedDomainService
   *
   * @param config
   *          per-container configuration
   * @param enabled
   *          whether this service should do anything at all.
   */
  @Inject
  public HashLockedDomainService(ContainerConfig config,
          @Named("shindig.locked-domain.enabled") boolean enabled, LockedDomainPrefixGenerator ldGen) {
    super(config, enabled);
    this.lockedSuffixes = Maps.newHashMap();
    this.ldGen = ldGen;
    if (enabled) {
      this.ldObserver = new HashLockedDomainObserver();
      config.addConfigObserver(this.ldObserver, true);
    }
  }

  @Override
  public String getLockedDomainForGadget(Gadget gadget, String container) throws GadgetException {
    container = getContainer(container);
    if (isEnabled() && !isExcludedFromLockedDomain(gadget, container)) {
      if (isGadgetReqestingLocking(gadget) || isDomainLockingEnforced(container)) {
        return getLockedDomain(gadget, container);
      }
    }
    return null;
  }

  /**
   * Generates a locked domain prefix given a gadget Uri.
   *
   * @param gadget The uri of the gadget.
   * @return A locked domain prefix for the gadgetUri.
   *         Returns empty string if locked domains are not enabled on the server.
   */
  private String getLockedDomainPrefix(Gadget gadget) throws GadgetException {
    String ret = "";
    if (isEnabled()) {
      ret = this.ldGen.getLockedDomainPrefix(getLockedDomainParticipants(gadget));
    }
    // Lower-case to prevent casing from being relevant.
    return ret.toLowerCase();
  }

  @Override
  public boolean isGadgetValidForHost(String host, Gadget gadget, String container) {
    container = getContainer(container);
    if (isEnabled()) {
      if (isGadgetReqestingLocking(gadget) || isHostUsingLockedDomain(host)
              || isDomainLockingEnforced(container)) {
        if (isRefererCheckEnabled() && !isValidReferer(gadget, container)) {
          return false;
        }
        String neededHost;
        try {
          neededHost = getLockedDomain(gadget, container);
        } catch (GadgetException e) {
          if (LOG.isLoggable(Level.WARNING)) {
            LOG.log(Level.WARNING, "Invalid host for call.", e);
          }
          return false;
        }
        return host.equalsIgnoreCase(neededHost);
      }
    }
    return true;
  }

  @Override
  public boolean isHostUsingLockedDomain(String host) {
    if (isEnabled()) {
      for (String suffix : this.lockedSuffixes.values()) {
        if (host.toLowerCase().endsWith(suffix.toLowerCase())) {
          return true;
        }
      }
    }
    return false;
  }

  @Inject(optional = true)
  public void setAuthority(Authority authority) {
    this.authority = authority;
  }

  private String checkSuffix(String suffix) {
    if (suffix != null) {
      Matcher m = this.authpattern.matcher(suffix);
      if (m.matches()) {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.warning("You should not be using %authority% replacement in a running environment!");
          LOG.warning("Check your config and specify an explicit locked domain suffix.");
          LOG.warning("Found suffix: " + suffix);
        }
        if (this.authority != null) {
          suffix = m.replaceAll(this.authority.getAuthority());
        }
      }
    }
    return suffix;
  }

  private String getContainer(String container) {
    if (this.required.containsKey(container)) {
      return container;
    }
    return ContainerConfig.DEFAULT_CONTAINER;
  }

  private String getLockedDomain(Gadget gadget, String container) throws GadgetException {
    String suffix = this.lockedSuffixes.get(container);
    if (suffix == null) {
      return null;
    }
    return getLockedDomainPrefix(gadget) + suffix;
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

  @VisibleForTesting
  ContainerConfig.ConfigObserver getConfigObserver() {
    return this.ldObserver;
  }
}

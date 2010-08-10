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

import org.apache.shindig.common.util.Base32;
import org.apache.shindig.config.ContainerConfig;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

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
public class HashLockedDomainService implements LockedDomainService {
  private static final Logger LOG = Logger.getLogger(HashLockedDomainService.class.getName());
  private final boolean enabled;
  private boolean lockSecurityTokens = false;
  private final Map<String, String> lockedSuffixes;
  private final Map<String, Boolean> required;

  public static final String LOCKED_DOMAIN_REQUIRED_KEY = "gadgets.lockedDomainRequired";
  public static final String LOCKED_DOMAIN_SUFFIX_KEY = "gadgets.lockedDomainSuffix";

  /**
   * Create a LockedDomainService
   * @param config per-container configuration
   * @param enabled whether this service should do anything at all.
   */
  @Inject
  public HashLockedDomainService(ContainerConfig config,
                                 @Named("shindig.locked-domain.enabled") boolean enabled) {
    this.enabled = enabled;
    lockedSuffixes = Maps.newHashMap();
    required = Maps.newHashMap();
    Collection<String> containers = config.getContainers();
    if (enabled) {
      for (String container : containers) {
        String suffix = config.getString(container, LOCKED_DOMAIN_SUFFIX_KEY);
        if (suffix == null) {
          LOG.warning("No locked domain configuration for " + container);
        } else {
          lockedSuffixes.put(container, suffix);
        }

        required.put(container, config.getBool(container, LOCKED_DOMAIN_REQUIRED_KEY));
      }
    }
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

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isSafeForOpenProxy(String host) {
    if (enabled) {
      return !hostRequiresLockedDomain(host);
    }
    return true;
  }

  public boolean gadgetCanRender(String host, Gadget gadget, String container) {
    container = normalizeContainer(container);
    if (enabled) {
      if (gadgetWantsLockedDomain(gadget) ||
          hostRequiresLockedDomain(host) ||
          containerRequiresLockedDomain(container)) {
        String neededHost = getLockedDomain(gadget, container);
        return host.equals(neededHost);
      }
    }
    return true;
  }

  public String getLockedDomainForGadget(Gadget gadget, String container) {
    container = normalizeContainer(container);
    if (enabled) {
      if (gadgetWantsLockedDomain(gadget) ||
          containerRequiresLockedDomain(container)) {
        return getLockedDomain(gadget, container);
      }
    }
    return null;
  }

  private String getLockedDomain(Gadget gadget, String container) {
    String suffix = lockedSuffixes.get(container);
    if (suffix == null) {
      return null;
    }
    byte[] sha1 = DigestUtils.sha(gadget.getSpec().getUrl().toString());
    String hash = new String(Base32.encodeBase32(sha1));
    return hash + suffix;
  }

  private boolean gadgetWantsLockedDomain(Gadget gadget) {
    if (lockSecurityTokens) {
      return gadget.getAllFeatures().contains("locked-domain");
    }
    return gadget.getSpec().getModulePrefs().getFeatures().keySet().contains("locked-domain");
  }

  private boolean hostRequiresLockedDomain(String host) {
    for (String suffix : lockedSuffixes.values()) {
      if (host.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  private boolean containerRequiresLockedDomain(String container) {
    return required.get(container);
  }

  private String normalizeContainer(String container) {
    if (required.containsKey(container)) {
      return container;
    }
    return ContainerConfig.DEFAULT_CONTAINER;
  }
}

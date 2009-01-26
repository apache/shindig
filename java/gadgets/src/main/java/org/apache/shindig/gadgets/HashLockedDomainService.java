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
import org.apache.shindig.gadgets.spec.GadgetSpec;

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

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isSafeForOpenProxy(String host) {
    if (enabled) {
      return !hostRequiresLockedDomain(host);
    }
    return true;
  }

  public boolean gadgetCanRender(String host, GadgetSpec gadget, String container) {
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

  public String getLockedDomainForGadget(GadgetSpec gadget, String container) {
    container = normalizeContainer(container);
    if (enabled) {
      if (gadgetWantsLockedDomain(gadget) ||
          containerRequiresLockedDomain(container)) {
        return getLockedDomain(gadget, container);
      }
    }
    return null;
  }

  private String getLockedDomain(GadgetSpec gadget, String container) {
    String suffix = lockedSuffixes.get(container);
    if (suffix == null) {
      return null;
    }
    byte[] sha1 = DigestUtils.sha(gadget.getUrl().toString());
    String hash = new String(Base32.encodeBase32(sha1));
    return hash + suffix;
  }

  private boolean gadgetWantsLockedDomain(GadgetSpec gadget) {
    return gadget.getModulePrefs().getFeatures().containsKey("locked-domain");
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

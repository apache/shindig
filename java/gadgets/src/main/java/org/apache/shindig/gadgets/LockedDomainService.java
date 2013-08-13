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

import com.google.inject.ImplementedBy;

/**
 * Interface for locked domain, a security mechanism that ensures that
 * a gadget is always registered on a fixed, unique domain. This prevents
 * attacks from other gadgets that are rendered on the same domain, since all
 * modern web browsers implement a same origin policy that prevents pages served
 * from different hosts from accessing each other's data.
 */
@ImplementedBy(HashLockedDomainService.class)
public interface LockedDomainService {
  /**
   * Check whether locked domains feature is enabled on the server.
   *
   * @return If locked domains is enabled on the server.
   */
  boolean isEnabled();

  /**
   * @return True if the host is safe for use with the open proxy.
   */
  boolean isSafeForOpenProxy(String host);

  /**
   * Check whether a gadget should be allowed to render on a particular
   * host.
   *
   * @param host host name for the content
   * @param gadget URL of the gadget
   * @param container container
   * @return true if the gadget can render
   */
  boolean isGadgetValidForHost(String host, Gadget gadget, String container);

  /**
   * Calculate the locked domain for a particular gadget on a particular
   * container.
   *
   * @param gadget URL of the gadget
   * @param container name of the container page
   * @return the host name on which the gadget should render, or null if locked domain should not
   * be used to render this gadget.
   */
  String getLockedDomainForGadget(Gadget gadget, String container) throws GadgetException;

  /**
   * Check whether a host is using a locked domain.
   *
   * @param host Host to inspect for locked domain suffix.
   * @return If the supplied host is using a locked domain.
   *         Returns false if locked domains are not enabled on the server.
   */
  boolean isHostUsingLockedDomain(String host);

  /**
   * @return If referrer check is enabled, return true. Otherwise, return false.
   */
  boolean isRefererCheckEnabled();
}

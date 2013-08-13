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
package org.apache.shindig.gadgets.servlet;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.servlet.GadgetsHandlerApi.AuthContext;

import com.google.inject.ImplementedBy;

@ImplementedBy (ModuleIdManagerImpl.class)
public interface ModuleIdManager {
  /**
   * Checks to make sure that the proposed moduleId for this gadget is valid.
   * This data is not 100% trustworthy becaue we can't extract it from a
   * token, so we validate it here, usually against the AuthContext viewerId,
   * gadgetUrl, moduleId combination.
   *
   * If the moduleId is invalid the implementation may return:
   *   null (in which case a null security token will be returned to the container)
   *   0 (Default value for non-persisted gadgets)
   *   A newly generated moduleId
   *
   * If the supplied moduleId is valid, this function is expected to return the
   * value of the moduleId param.
   *
   * @param gadgetUri The location of the gadget xml to validate the token for
   * @param containerAuthContext The Auth context.  Basically, the container security token.
   * @param moduleId The moduleId sent by the container page.
   * @return moduleId.
   */
  public Long validate(Uri gadgetUri, AuthContext containerAuthContext, Long moduleId);

  /**
   * Generate and persist a new moduleId for the given gadgetUri and container auth context.
   *
   * @param gadgetUri The location of the gadget xml to generate the token for
   * @param containerAuthContext The Auth context.  Basically, the container security token.
   * @return moduleId.
   */
  public Long generate(Uri gadgetUri, AuthContext containerAuthContext);
}

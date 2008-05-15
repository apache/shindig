/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.shindig.social.abdera;

import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.TargetType;
import org.apache.abdera.protocol.server.impl.RouteManager;


public class SocialRouteManager extends RouteManager {
  private String base;

  /**
   * @param base Should be the same as RequestContext.getContextPath()
   */
  public SocialRouteManager(String base) {
    this.base = base;
  }

  /**
   * This extension of the addRoute from the parent allows a RequestUrlTemplate
   * to be passed in instead of a name and pattern. This is just a convenience
   * method to clean up the code. The parent method maps routes to types and
   * adapters.
   *
   * @param template RequestUrlTemplate enum should contain names and patterns.
   * @param type TargetType
   * @param collectionAdapter CollectionAdapter
   * @return addRoute from the parent RouteManager
   */
  public SocialRouteManager addRoute(RequestUrlTemplate template,
      TargetType type, CollectionAdapter collectionAdapter) {
    return (SocialRouteManager) addRoute(template.toString(),
        base + template.getRoutePattern(), type, collectionAdapter);
  }
}

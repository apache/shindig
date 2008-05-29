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

import org.apache.shindig.social.abdera.json.JSONFilter;
import org.apache.shindig.social.abdera.util.ValidRequestFilter;

import com.google.inject.Inject;
import org.apache.abdera.protocol.server.impl.DefaultProvider;

public class SocialApiProvider extends DefaultProvider {
  public SocialApiProvider() {
    addFilter(new ValidRequestFilter());
    // JsonFilter should always be the last class in the chain of filters
    addFilter(new JSONFilter());
  }

  @Inject
  public void setSocialRouteManager(SocialRouteManager socialRouteManager) {
    routeManager = socialRouteManager;
    socialRouteManager.setRoutes();
    setTargetBuilder(socialRouteManager);
    setTargetResolver(socialRouteManager);
  }
}

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

import org.apache.shindig.social.EasyMockTestCase;
import org.apache.shindig.social.abdera.atom.ActivityAdapter;
import org.apache.shindig.social.abdera.atom.DataAdapter;
import org.apache.shindig.social.abdera.atom.PersonAdapter;
import org.apache.shindig.social.abdera.json.PersonJsonAdapter;

import org.apache.abdera.protocol.server.RequestContext;

public abstract class SocialApiProviderTestFixture extends EasyMockTestCase {
  public final String base = "/social/rest/";
  public final RequestContext request = mock(RequestContext.class);
  public final SocialApiProvider provider = new SocialApiProvider();

  public SocialApiProviderTestFixture() {
    provider.setSocialRouteManager(new SocialRouteManager(
        mock(PersonJsonAdapter.class),
        mock(PersonAdapter.class),
        mock(DataAdapter.class),
        mock(ActivityAdapter.class)));
  }
}

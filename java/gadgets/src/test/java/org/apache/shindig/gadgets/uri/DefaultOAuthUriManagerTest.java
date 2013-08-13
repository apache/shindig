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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.config.ContainerConfig;

import org.junit.Test;

public class DefaultOAuthUriManagerTest {
  private static final String CONTAINER = "container";
  private static final String HOST = "www.host.com";

  @Test
  public void noConfigValueConfigured() throws Exception {
    ContainerConfig config = mockConfig(null);
    DefaultOAuthUriManager manager = makeManager(config);
    assertNull(manager.makeOAuthCallbackUri(CONTAINER, HOST));
    verify(config);
  }

  @Test
  public void noHostSubstitution() throws Exception {
    String value = "http://www.apache.org/oauth/callback";
    ContainerConfig config = mockConfig(value);
    DefaultOAuthUriManager manager = makeManager(config);
    assertEquals(value, manager.makeOAuthCallbackUri(CONTAINER, HOST).toString());
    verify(config);
  }

  @Test
  public void oauthUriWithHostSubstitution() throws Exception {
    String value = "http://%host%/oauth/callback";
    ContainerConfig config = mockConfig(value);
    DefaultOAuthUriManager manager = makeManager(config);
    assertEquals("http://" + HOST + "/oauth/callback",
        manager.makeOAuthCallbackUri(CONTAINER, HOST).toString());
    verify(config);
  }

  private ContainerConfig mockConfig(String tplVal) {
    ContainerConfig config = createMock(ContainerConfig.class);
    expect(config.getString(CONTAINER, DefaultOAuthUriManager.OAUTH_GADGET_CALLBACK_URI_PARAM))
        .andReturn(tplVal).once();
    replay(config);
    return config;
  }

  private DefaultOAuthUriManager makeManager(ContainerConfig config) {
    return new DefaultOAuthUriManager(config);
  }
}

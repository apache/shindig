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
package org.apache.shindig.social.core.oauth2;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class OAuth2ServiceImplTest {
  private static final long EXPECTED_AUTHCODE_EXPIRATION = 300000;
  private static final long EXPECTED_ACCESSTOKEN_EXPIRATION = 18000000;

  private OAuth2ServiceImpl serviceImpl;

  private static class OAuth2ServiceImplTestModule extends AbstractModule {
    @Override
    protected void configure() {
      // Bind string values, to match what happens when reading the shindig.properties file.
      bindConstant().annotatedWith(Names.named("shindig.oauth2.authCodeExpiration"))
          .to(Long.toString(EXPECTED_AUTHCODE_EXPIRATION));
      bindConstant().annotatedWith(Names.named("shindig.oauth2.accessTokenExpiration"))
          .to(Long.toString(EXPECTED_ACCESSTOKEN_EXPIRATION));

      OAuth2DataService dataService = createMock(OAuth2DataService.class);
      bind(OAuth2DataService.class).toInstance(dataService);
    }
  }

  @Before
  public void setUp() {
    serviceImpl = Guice.createInjector(new OAuth2ServiceImplTestModule())
        .getInstance(OAuth2ServiceImpl.class);
  }

  @Test
  public void testAccessTokenExpirationIsConfigured() {
    assertEquals(EXPECTED_ACCESSTOKEN_EXPIRATION, serviceImpl.getAccessTokenExpires());
  }

  @Test
  public void testAuthCodeExpirationIsConfigured() {
    assertEquals(EXPECTED_AUTHCODE_EXPIRATION, serviceImpl.getAuthCodeExpires());
  }
}
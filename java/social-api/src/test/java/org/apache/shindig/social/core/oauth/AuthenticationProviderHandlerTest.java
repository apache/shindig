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
package org.apache.shindig.social.core.oauth;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.social.core.config.SocialApiGuiceModule;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;


public class AuthenticationProviderHandlerTest extends Assert {
  /**
   * Test that existing custom handlers won't be broken with the switch
   * to injecting List<ProviderHandler>.
   */
  @Test
  public void testCustomHandler() {
    Injector injector = Guice.createInjector(new SocialApiGuiceModule(),
        new CustomAuthHandlerProviderModule(), new PropertiesModule());

    AuthenticationHandlerProvider provider = injector.getInstance(
        AuthenticationHandlerProvider.class);
    assertEquals(0, provider.get().size());

    List<AuthenticationHandler> handlers = injector.getInstance(
        Key.get(new TypeLiteral<List<AuthenticationHandler>>(){}));
    assertEquals(0, handlers.size());
  }

  /**
   * AuthenticationHandlerProvider with no handlers
   */
  public static class ProvidesNoHandlers extends AuthenticationHandlerProvider {
    public ProvidesNoHandlers() {
      super(null, null, null, null);
    }

    @Override
    public List<AuthenticationHandler> get() {
      return Collections.emptyList();
    }
  }

  /**
   * Module with a custom AuthenticationHandler
   */
  public static class CustomAuthHandlerProviderModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(AuthenticationHandlerProvider.class).to(ProvidesNoHandlers.class);
    }
  }
}

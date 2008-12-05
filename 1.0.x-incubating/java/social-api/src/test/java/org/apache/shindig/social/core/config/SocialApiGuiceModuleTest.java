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
package org.apache.shindig.social.core.config;

import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.common.PropertiesModule;
import org.apache.shindig.social.core.oauth.AuthenticationHandlerProvider;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import junit.framework.TestCase;

import java.util.List;

public class SocialApiGuiceModuleTest extends TestCase {
  private Injector injector;

  @Override public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new SocialApiGuiceModule(), new PropertiesModule());
  }

  /**
   * Test default auth handler injection
   */
  public void testAuthHandler() {
    injector.getInstance(AuthenticationHandlerProvider.class).get();

    AuthenticationHandlerProvider provider =
        injector.getInstance(AuthenticationHandlerProvider.class);
    assertEquals(3, provider.get().size());

    List<AuthenticationHandler> handlers = injector.getInstance(
        Key.get(new TypeLiteral<List<AuthenticationHandler>>(){}));

    assertEquals(3, handlers.size());
  }
}

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

import org.apache.shindig.auth.AnonymousAuthenticationHandler;
import org.apache.shindig.auth.AuthenticationHandler;
import org.apache.shindig.auth.UrlParameterAuthenticationHandler;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

import java.util.List;

/**
 * Binds auth types used by gadget rendering. This should be used when running a stand-alone gadget
 * renderer.
 */
public class AuthenticationModule extends AbstractModule {

  /** {@InheritDoc} */
  @Override
  protected void configure() {
    bind(new TypeLiteral<List<AuthenticationHandler>>(){}).toProvider(AuthProvider.class);
  }

  private static class AuthProvider implements Provider<List<AuthenticationHandler>> {
    private final List<AuthenticationHandler> handlers;

    @Inject
    public AuthProvider(UrlParameterAuthenticationHandler urlParameterAuthHandler,
                        AnonymousAuthenticationHandler anonymoustAuthHandler) {
      handlers = Lists.newArrayList(urlParameterAuthHandler, anonymoustAuthHandler);
    }

    public List<AuthenticationHandler> get() {
      return handlers;
    }
  }

}

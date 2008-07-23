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

import org.apache.shindig.common.servlet.ParameterFetcher;
import org.apache.shindig.social.core.oauth.AuthenticationServletFilter;
import org.apache.shindig.social.core.oauth.BasicOAuthConsumerStore;
import org.apache.shindig.social.core.oauth.BasicOAuthTokenPrincipalMapper;
import org.apache.shindig.social.core.oauth.BasicOAuthTokenStore;
import org.apache.shindig.social.core.util.BeanConverter;
import org.apache.shindig.social.core.util.BeanJsonConverter;
import org.apache.shindig.social.core.util.BeanXmlConverter;
import org.apache.shindig.social.opensocial.oauth.OAuthConsumerStore;
import org.apache.shindig.social.opensocial.oauth.OAuthTokenPrincipalMapper;
import org.apache.shindig.social.opensocial.oauth.OAuthTokenStore;
import org.apache.shindig.social.opensocial.service.DataServiceServletFetcher;
import org.apache.shindig.social.opensocial.service.HandlerProvider;
import org.apache.shindig.social.sample.container.SampleContainerHandlerProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;

/**
 * Provides social api component injection
 */
public class SocialApiGuiceModule extends AbstractModule {

  /** {@inheritDoc} */
  @Override
  protected void configure() {
    bind(HandlerProvider.class).to(SampleContainerHandlerProvider.class);

    bind(ParameterFetcher.class).annotatedWith(Names.named("DataServiceServlet"))
        .to(DataServiceServletFetcher.class);

    bind(String.class).annotatedWith(Names.named("canonical.json.db"))
        .toInstance("sampledata/canonicaldb.json");

    // OAuth configuration

    bind(Boolean.class)
        .annotatedWith(Names.named(AuthenticationServletFilter.ALLOW_UNAUTHENTICATED))
        .toInstance(Boolean.TRUE);

    bind(OAuthValidator.class).to(SimpleOAuthValidator.class);
    bind(OAuthTokenStore.class)
        .to(BasicOAuthTokenStore.class).in(Scopes.SINGLETON);
    bind(OAuthConsumerStore.class)
        .to(BasicOAuthConsumerStore.class).in(Scopes.SINGLETON);
    bind(OAuthTokenPrincipalMapper.class)
        .to(BasicOAuthTokenPrincipalMapper.class).in(Scopes.SINGLETON);
    bind(BeanConverter.class).annotatedWith(Names.named("bean.converter.xml")).to(BeanXmlConverter.class);
    bind(BeanConverter.class).annotatedWith(Names.named("bean.converter.json")).to(BeanJsonConverter.class);
  }

}

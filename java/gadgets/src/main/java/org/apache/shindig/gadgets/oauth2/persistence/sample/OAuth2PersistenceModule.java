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
package org.apache.shindig.gadgets.oauth2.persistence.sample;

import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Cache;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Encrypter;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Persister;

import com.google.inject.AbstractModule;

/**
 * Binds default persistence classes for shindig.
 *
 */
public class OAuth2PersistenceModule extends AbstractModule {

  @Override
  protected void configure() {
    this.bind(OAuth2Persister.class).to(JSONOAuth2Persister.class);
    this.bind(OAuth2Cache.class).to(InMemoryCache.class);
    this.bind(OAuth2Encrypter.class).to(NoOpEncrypter.class);
  }
}

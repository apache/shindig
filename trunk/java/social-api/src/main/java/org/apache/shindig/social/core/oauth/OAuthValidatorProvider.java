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

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.google.inject.name.Named;
import net.oauth.OAuth;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;

/**
 * Guice Provider class for OAuthValidator.
 */
public class OAuthValidatorProvider implements Provider<OAuthValidator> {
  private final OAuthValidator validator;

  @Inject
  public OAuthValidatorProvider(@Named("shindig.oauth.validator-max-timestamp-age-ms")
                                  long maxTimestampAgeMsec) {
    validator = new SimpleOAuthValidator(maxTimestampAgeMsec, Double.parseDouble(OAuth.VERSION_1_0));
  }

  public OAuthValidator get() {
    return validator;
  }
}

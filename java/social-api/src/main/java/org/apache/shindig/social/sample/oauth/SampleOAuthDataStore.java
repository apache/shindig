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
package org.apache.shindig.social.sample.oauth;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.core.oauth.OAuthSecurityToken;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;
import org.json.JSONException;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Sample implementation for OAuth data store
public class SampleOAuthDataStore implements OAuthDataStore {
  // used to get samplecontainer data from canonicaldb.json
  private JsonDbOpensocialService service;

  @Inject
  public SampleOAuthDataStore(JsonDbOpensocialService dbService) {
    this.service = dbService;
  }

  // All valid OAuth tokens
  private static ConcurrentHashMap<String,OAuthEntry> oauthTokens = Maps.newConcurrentHashMap();

  // Get the OAuthEntry that corresponds to the oauthToken
  public OAuthEntry getEntry(String oauthToken) {
    Preconditions.checkNotNull(oauthToken);
    return oauthTokens.get(oauthToken);
  }

  // If the passed in consumerKey is valid, pass back the consumerSecret
  public String getConsumerSecret(String consumerKey) {
    try {
       return service.getDb().getJSONObject("consumerSecrets").getString(Preconditions.checkNotNull(consumerKey));
    } catch (JSONException e) {
       return null;
    }
  }

  // Generate a valid requestToken for the given consumerKey
  public OAuthEntry generateRequestToken(String consumerKey) {
    OAuthEntry entry = new OAuthEntry();
    entry.appId = consumerKey;
    entry.consumerKey = consumerKey;
    entry.consumerSecret = getConsumerSecret(consumerKey);
    entry.domain = "samplecontainer.com";
    entry.container = "default";

    entry.token = UUID.randomUUID().toString();
    entry.tokenSecret = UUID.randomUUID().toString();
      
    entry.type = OAuthEntry.Type.REQUEST;
    entry.issueTime = new Date();

    oauthTokens.put(entry.token, entry);
    return entry;
  }

  // Turns the request token into an access token
  public OAuthEntry convertToAccessToken(OAuthEntry entry) {
    Preconditions.checkNotNull(entry);
    Preconditions.checkState(entry.type == OAuthEntry.Type.REQUEST, "Token must be a request token");

    OAuthEntry accessEntry = new OAuthEntry(entry);

    accessEntry.token = UUID.randomUUID().toString();
    accessEntry.tokenSecret = UUID.randomUUID().toString();

    accessEntry.type = OAuthEntry.Type.ACCESS;
    accessEntry.issueTime = new Date();

    oauthTokens.put(entry.token, entry);

    return entry;
  }

  // Authorize the request token for the given user id
  public void authorizeToken(OAuthEntry entry, String userId) {
    Preconditions.checkNotNull(entry);
    entry.authorized = true;
    entry.userId = Preconditions.checkNotNull(userId);
  }

  // Return the proper security token for a 2 legged oauth request that has been validated
  // for the given consumerKey. App specific checks like making sure the requested user has the
  // app installed should take place in this method
  public SecurityToken getSecurityTokenForConsumerRequest(String consumerKey, String userId) {
    String domain = "samplecontainer.com";
    String container = "default";
    
    return new OAuthSecurityToken(userId, null, consumerKey, domain, container);
  }
}

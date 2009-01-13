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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.core.oauth.OAuthSecurityToken;
import org.apache.shindig.social.opensocial.oauth.OAuthLookupService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.SimpleOAuthValidator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SampleContainerOAuthLookupService implements OAuthLookupService {
  // If we were a real social network this would probably be a function
  private static Map<String, String> sampleContainerUrlToAppIdMap = ImmutableMap.of(
      "http://localhost:8080/gadgets/files/samplecontainer/examples/SocialHelloWorld.xml",
      "7810",
      "http://localhost:8080/gadgets/files/samplecontainer/examples/SocialActivitiesWorld.xml",
      "8355"
  );

  // If we were a real social network we would probably be keeping track of this in a db somewhere
  private static Map<String, ArrayList<String>> sampleContainerAppInstalls = ImmutableMap.of(
      "john.doe", Lists.newArrayList("7810", "8355")
  );

  // If we were a real social network we would establish shared secrets with each of our gadgets
  private static Map<String, String> sampleContainerSharedSecrets = ImmutableMap.of(
      "7810", "SocialHelloWorldSharedSecret",
      "8355", "SocialActivitiesWorldSharedSecret"
  );

  public boolean thirdPartyHasAccessToUser(OAuthMessage message, String appUrl, String userId) {
    String appId = getAppId(appUrl);
    return hasValidSignature(message, appUrl, appId)
        && userHasAppInstalled(userId, appId);
  }

  private boolean hasValidSignature(OAuthMessage message, String appUrl, String appId) {
    String sharedSecret = sampleContainerSharedSecrets.get(appId);
    if (sharedSecret == null) {
      return false;
    }

    OAuthServiceProvider provider = new OAuthServiceProvider(null, null, null);
    OAuthConsumer consumer = new OAuthConsumer(null, appUrl, sharedSecret, provider);
    OAuthAccessor accessor = new OAuthAccessor(consumer);

    SimpleOAuthValidator validator = new SimpleOAuthValidator();
    try {
      validator.validateMessage(message, accessor);
    } catch (OAuthException e) {
      return false;
    } catch (IOException e) {
      return false;
    } catch (URISyntaxException e) {
      return false;
    }

    return true;
  }

  private boolean userHasAppInstalled(String userId, String appId) {
    List<String> appInstalls = sampleContainerAppInstalls.get(userId);
    if (appInstalls != null) {
      for (String appInstall : appInstalls) {
        if (appInstall.equals(appId)) {
          return true;
        }
      }
    }

    return false;
  }

  public SecurityToken getSecurityToken(String appUrl, String userId) {
    return new OAuthSecurityToken(userId, appUrl, getAppId(appUrl), "samplecontainer");
  }

  private String getAppId(String appUrl) {
    return sampleContainerUrlToAppIdMap.get(appUrl);
  }

}

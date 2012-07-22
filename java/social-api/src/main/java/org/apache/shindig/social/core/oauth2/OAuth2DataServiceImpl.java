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
package org.apache.shindig.social.core.oauth2;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.shindig.common.util.ResourceLoader;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.conversion.BeanConverter;
import org.apache.shindig.social.core.oauth2.OAuth2Client.ClientType;
import org.apache.shindig.social.core.oauth2.OAuth2Types.CodeType;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class OAuth2DataServiceImpl implements OAuth2DataService {

  private JSONObject oauthDB; // the OAuth 2.0 JSON DB
  private BeanConverter converter; // the JSON<->Bean converter
  private List<OAuth2Client> clients; // list of clients
  private Map<String, List<OAuth2Code>> authCodes; // authorization codes per client
  private Map<String, List<OAuth2Code>> accessTokens; // access tokens per client

  @Inject
  public OAuth2DataServiceImpl(
      @Named("shindig.canonical.json.db") String jsonLocation,
      @Named("shindig.bean.converter.json") BeanConverter converter,
      @Named("shindig.contextroot") String contextroot) throws Exception {
    String content = IOUtils.toString(ResourceLoader.openResource(jsonLocation), "UTF-8");
    content = content.replace("%contextRoot%", contextroot);
    this.oauthDB = new JSONObject(content).getJSONObject("oauth2");
    this.converter = converter;
    this.clients = Lists.newArrayList();
    this.authCodes = Maps.newHashMap();
    this.accessTokens = Maps.newHashMap();
    loadClientsFromCanonical();
  }

  public OAuth2Client getClient(String clientId) {
    for (OAuth2Client client : clients) {
      if (client.getId().equals(clientId)) {
        return client;
      }
    }
    return null;
  }

  public OAuth2Code getAuthorizationCode(String clientId, String authCode) {
    if (authCodes.containsKey(clientId)) {
      List<OAuth2Code> codes = authCodes.get(clientId);
      for (OAuth2Code code : codes) {
        if (code.getValue().equals(authCode)) {
          return code;
        }
      }
    }
    return null;
  }

  public void registerAuthorizationCode(String clientId, OAuth2Code authCode) {
    if (authCodes.containsKey(clientId)) {
      authCodes.get(clientId).add(authCode);
    } else {
      List<OAuth2Code> list = Lists.newArrayList();
      list.add(authCode);
      authCodes.put(clientId, list);
    }
  }

  public void unregisterAuthorizationCode(String clientId, String authCode) {
    if (authCodes.containsKey(clientId)) {
      List<OAuth2Code> codes = authCodes.get(clientId);
      for (OAuth2Code code : codes) {
        if (code.getValue().equals(authCode)) {
          codes.remove(code);
          return;
        }
      }
    }
    throw new RuntimeException("signature not found"); // TODO (Eric): handle error
  }

  public OAuth2Code getAccessToken(String accessToken) {
    for (String clientId : accessTokens.keySet()) {
      List<OAuth2Code> tokens = accessTokens.get(clientId);
      for (OAuth2Code token : tokens) {
        if (token.getValue().equals(accessToken)) {
          return token;
        }
      }
    }
    return null;
  }

  public void registerAccessToken(String clientId, OAuth2Code accessToken) {
    if (accessTokens.containsKey(clientId)) {
      accessTokens.get(clientId).add(accessToken);
    } else {
      List<OAuth2Code> list = Lists.newArrayList();
      list.add(accessToken);
      accessTokens.put(clientId, list);
    }
  }

  public void unregisterAccessToken(String clientId, String accessToken) {
    if (accessTokens.containsKey(clientId)) {
      List<OAuth2Code> tokens = accessTokens.get(clientId);
      for (OAuth2Code token : tokens) {
        if (token.getValue().equals(accessToken)) {
          tokens.remove(token);
          return;
        }
      }
    }
    throw new RuntimeException("access token not found"); // TODO (Eric): handle error
  }

  public OAuth2Code getRefreshToken(String refreshToken) {
    throw new RuntimeException("not yet implemented");
  }

  public void registerRefreshToken(String clientId, OAuth2Code refreshToken) {
    throw new RuntimeException("not yet implemented");
  }

  public void unregisterRefreshToken(String clientId, String refreshToken) {
    throw new RuntimeException("not yet implemented");
  }

  private void loadClientsFromCanonical() {
    for (String clientId : JSONObject.getNames(oauthDB)) {
      JSONObject clientJson;
      try {
        clientJson = oauthDB.getJSONObject(clientId).getJSONObject("registration");
        OAuth2Client client = converter.convertToObject(clientJson.toString(), OAuth2Client.class);
        client.setType(clientJson.getString("type").equals("public") ? ClientType.PUBLIC : ClientType.CONFIDENTIAL);
        clients.add(client);
        JSONObject clientJS = oauthDB.getJSONObject(clientId);
        if (clientJS.has("authorizationCodes")) {
          JSONObject authCodes = clientJS.getJSONObject("authorizationCodes");
          for (String authCodeId : JSONObject.getNames(authCodes)) {
            OAuth2Code code = converter.convertToObject(authCodes
                .getJSONObject(authCodeId).toString(), OAuth2Code.class);
            code.setValue(authCodeId);
            code.setClient(client);
            registerAuthorizationCode(clientId, code);
          }
        }
        if (clientJS.has("accessTokens")) {
          JSONObject accessTokens = clientJS.getJSONObject("accessTokens");
          for (String accessTokenId : JSONObject.getNames(accessTokens)) {
            OAuth2Code code = converter.convertToObject(accessTokens.getJSONObject(accessTokenId).toString(), OAuth2Code.class);
            code.setValue(accessTokenId);
            code.setClient(client);
            code.setType(CodeType.ACCESS_TOKEN);
            registerAccessToken(clientId, code);
          }
        }
      } catch (JSONException je) {
        throw new ProtocolException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je.getMessage(), je);
      }
    }
  }
}

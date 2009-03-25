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
package org.apache.shindig.social.sample.oauth;

import org.jsecurity.realm.AuthorizingRealm;
import org.jsecurity.authz.AuthorizationInfo;
import org.jsecurity.authz.AuthorizationException;
import org.jsecurity.authz.SimpleAuthorizationInfo;
import org.jsecurity.subject.PrincipalCollection;
import org.jsecurity.authc.AuthenticationInfo;
import org.jsecurity.authc.AuthenticationToken;
import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AccountException;
import org.jsecurity.authc.UsernamePasswordToken;
import org.jsecurity.authc.SimpleAuthenticationInfo;
import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.apache.shindig.social.opensocial.spi.SocialSpiException;
import org.apache.shindig.social.opensocial.model.Person;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.ResponseError;
import org.json.JSONObject;
import org.json.JSONArray;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import java.util.Set;

/**
 * A Sample Jsecurity Realm that uses the JSON DB to get passwords
 *
 */
public class SampleRealm extends AuthorizingRealm {
  // HACK, jsecurity relies upon no-arg constructors..
  @Inject
  private static JsonDbOpensocialService jsonDbService;


  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken upToken = (UsernamePasswordToken) token;
    String username = upToken.getUsername();

    // Null username is invalid
    if (username == null) {
        throw new AccountException("Null usernames are not allowed by this realm.");
    }
    String password = jsonDbService.getPassword(username);

    return  new SimpleAuthenticationInfo(username, password, this.getName());
  }

  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    //null usernames are invalid
    if (principals == null) {
      throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
    }

    String username = (String) principals.fromRealm(getName()).iterator().next();


    Set<String> roleNames;

    if (username == null) {
      roleNames = ImmutableSet.of();
    } else {
      roleNames = ImmutableSet.of("foo", "goo");
    }

    return new SimpleAuthorizationInfo(roleNames);
  }

}

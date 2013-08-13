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
package org.apache.shindig.sample.shiro;

import org.apache.shindig.social.sample.spi.JsonDbOpensocialService;
import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * A Sample Shiro Realm that uses the JSON DB to get passwords
 *
 */
public class SampleShiroRealm extends AuthorizingRealm {
  // HACK, apache.shiro relies upon no-arg constructors..
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

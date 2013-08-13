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

import org.apache.shindig.gadgets.oauth2.OAuth2Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OAuth2ProviderTest {
  private OAuth2Provider PROVIDER;

  @Before
  public void setUp() {
    this.PROVIDER = new OAuth2Provider();
    this.PROVIDER.setAuthorizationHeader(true);
    this.PROVIDER.setAuthorizationUrl("xxx");
    this.PROVIDER.setClientAuthenticationType(OAuth2Message.BASIC_AUTH_TYPE);
    this.PROVIDER.setName("yyy");
    this.PROVIDER.setTokenUrl("zzz");
    this.PROVIDER.setUrlParameter(false);

  }

  @Test
  public void testOAuth2Provider_1() throws Exception {
    final OAuth2Provider result = new OAuth2Provider();
    Assert.assertNotNull(result);
  }

  @Test
  public void testEquals_1() throws Exception {
    final OAuth2Provider obj = new OAuth2Provider();
    obj.setTokenUrl("zzz");
    obj.setClientAuthenticationType(OAuth2Message.BASIC_AUTH_TYPE);
    obj.setAuthorizationUrl("xxx");
    obj.setAuthorizationHeader(true);
    obj.setName("yyy");
    obj.setUrlParameter(false);

    final boolean result = this.PROVIDER.equals(obj);

    Assert.assertEquals(true, result);
  }

  @Test
  public void testEquals_2() throws Exception {

    final Object obj = new Object();

    final boolean result = this.PROVIDER.equals(obj);

    Assert.assertEquals(false, result);
  }

  @Test
  public void testGetAuthorizationUrl_1() throws Exception {
    final String result = this.PROVIDER.getAuthorizationUrl();

    Assert.assertEquals("xxx", result);
  }

  @Test
  public void testGetClientAuthenticationType_1() throws Exception {
    final String result = this.PROVIDER.getClientAuthenticationType();

    Assert.assertEquals(OAuth2Message.BASIC_AUTH_TYPE, result);
  }

  @Test
  public void testGetName_1() throws Exception {
    final String result = this.PROVIDER.getName();

    Assert.assertEquals("yyy", result);
  }

  @Test
  public void testGetTokenUrl_1() throws Exception {
    final String result = this.PROVIDER.getTokenUrl();

    Assert.assertEquals("zzz", result);
  }

  @Test
  public void testHashCode_1() throws Exception {
    final int result = this.PROVIDER.hashCode();

    Assert.assertEquals(120153, result);
  }

  @Test
  public void testHashCode_2() throws Exception {
    final OAuth2Provider fixture = new OAuth2Provider();
    fixture.setTokenUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setAuthorizationUrl("");
    fixture.setAuthorizationHeader(true);
    fixture.setName((String) null);
    fixture.setUrlParameter(true);

    final int result = fixture.hashCode();

    Assert.assertEquals(0, result);
  }

  @Test
  public void testIsAuthorizationHeader_1() throws Exception {
    final boolean result = this.PROVIDER.isAuthorizationHeader();

    Assert.assertEquals(true, result);
  }

  @Test
  public void testIsAuthorizationHeader_2() throws Exception {
    final OAuth2Provider fixture = new OAuth2Provider();
    fixture.setTokenUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setAuthorizationUrl("");
    fixture.setAuthorizationHeader(false);
    fixture.setName("");
    fixture.setUrlParameter(true);

    final boolean result = fixture.isAuthorizationHeader();

    Assert.assertEquals(false, result);
  }

  @Test
  public void testIsUrlParameter_1() throws Exception {
    final boolean result = this.PROVIDER.isUrlParameter();

    Assert.assertEquals(false, result);
  }

  @Test
  public void testIsUrlParameter_2() throws Exception {
    final OAuth2Provider fixture = new OAuth2Provider();
    fixture.setTokenUrl("");
    fixture.setClientAuthenticationType("");
    fixture.setAuthorizationUrl("");
    fixture.setAuthorizationHeader(true);
    fixture.setName("");
    fixture.setUrlParameter(false);

    final boolean result = fixture.isUrlParameter();

    Assert.assertEquals(false, result);
  }

  @Test
  public void testToString_1() throws Exception {
    final String result = this.PROVIDER.toString();

    Assert
        .assertEquals(
            "org.apache.shindig.gadgets.oauth2.persistence.sample.OAuth2Provider: name = yyy , authorizationUrl = xxx , tokenUrl = zzz , clientAuthenticationType = Basic",
            result);
  }
}

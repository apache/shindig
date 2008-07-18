/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.social.oauth;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import junit.framework.TestCase;

public class BasicOAuthConsumerStoreTest extends TestCase {

  private BasicOAuthConsumerStore store;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    store = new BasicOAuthConsumerStore();
  }

  public void testGetAccessor() throws Exception {
    OAuthAccessor accessor = store.getAccessor("");

    assertNull(accessor.accessToken);
    assertNull(accessor.requestToken);
    assertNull(accessor.tokenSecret);

    OAuthConsumer consumer = accessor.consumer;

    assertEquals("", consumer.callbackURL);
    assertEquals("", consumer.consumerKey);
    assertEquals("", consumer.consumerSecret);

    OAuthServiceProvider provider = consumer.serviceProvider;

    assertEquals("", provider.accessTokenURL);
    assertEquals("", provider.requestTokenURL);
    assertEquals("", provider.userAuthorizationURL);
  }
}

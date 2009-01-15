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
package org.apache.shindig.gadgets.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import net.oauth.signature.RSA_SHA1;

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.apache.shindig.gadgets.oauth.OAuthStore.ConsumerInfo;
import org.apache.shindig.gadgets.oauth.OAuthStore.TokenInfo;
import org.junit.Before;
import org.junit.Test;

public class BasicOAuthStoreTest {

  private static final String SAMPLE_FILE =
      '{' +
    "'http://localhost:8080/gadgets/files/samplecontainer/examples/oauth.xml' : {" +
    "'' : {" +
    "'consumer_key' : 'gadgetConsumer'," +
    "'consumer_secret' : 'gadgetSecret'," +
    "'key_type' : 'HMAC_SYMMETRIC'" +
          '}' +
    "}," +
    "'http://rsagadget/test.xml' : {" +
    "'' : {" +
    "'consumer_key' : 'rsaconsumer'," +
    "'consumer_secret' : 'rsaprivate'," +
    "'key_type' : 'RSA_PRIVATE'" +
          '}' +
          '}' +

          '}';

  private BasicOAuthStore store;

  @Before
  public void setUp() throws Exception {
    store = new BasicOAuthStore();
    store.initFromConfigString(SAMPLE_FILE);
  }

  @Test
  public void testInit() throws Exception {
    FakeGadgetToken t = new FakeGadgetToken();
    t.setAppUrl("http://localhost:8080/gadgets/files/samplecontainer/examples/oauth.xml");
    OAuthServiceProvider provider = new OAuthServiceProvider("req", "authorize", "access");
    ConsumerInfo consumerInfo = store.getConsumerKeyAndSecret(t, "", provider);
    OAuthConsumer consumer = consumerInfo.getConsumer();
    assertEquals("gadgetConsumer", consumer.consumerKey);
    assertEquals("gadgetSecret", consumer.consumerSecret);
    assertEquals("HMAC-SHA1", consumer.getProperty("oauth_signature_method"));
    assertEquals(provider, consumer.serviceProvider);
    assertNull(consumerInfo.getKeyName());

    t.setAppUrl("http://rsagadget/test.xml");
    consumerInfo = store.getConsumerKeyAndSecret(t, "", provider);
    consumer = consumerInfo.getConsumer();
    assertEquals("rsaconsumer", consumer.consumerKey);
    assertNull(consumer.consumerSecret);
    assertEquals("RSA-SHA1", consumer.getProperty("oauth_signature_method"));
    assertEquals(provider, consumer.serviceProvider);
    assertEquals("rsaprivate", consumer.getProperty(RSA_SHA1.PRIVATE_KEY));
    assertNull(consumerInfo.getKeyName());
  }

  @Test
  public void testGetAndSetAndRemoveToken() {
    FakeGadgetToken t = new FakeGadgetToken();
    ConsumerInfo consumer = new ConsumerInfo(null, null);
    t.setAppUrl("http://localhost:8080/gadgets/files/samplecontainer/examples/oauth.xml");
    t.setViewerId("viewer-one");
    assertNull(store.getTokenInfo(t, consumer, "", ""));

    TokenInfo info = new TokenInfo("token", "secret", null, 0);
    store.setTokenInfo(t, consumer, "service", "token", info);

    info = store.getTokenInfo(t, consumer, "service", "token");
    assertEquals("token", info.getAccessToken());
    assertEquals("secret", info.getTokenSecret());

    FakeGadgetToken t2 = new FakeGadgetToken();
    t2.setAppUrl("http://localhost:8080/gadgets/files/samplecontainer/examples/oauth.xml");
    t2.setViewerId("viewer-two");
    assertNull(store.getTokenInfo(t2, consumer, "service", "token"));

    store.removeToken(t, consumer, "service", "token");
    assertNull(store.getTokenInfo(t, consumer, "service", "token"));
  }

  @Test
  public void testDefaultKey() throws Exception {
    FakeGadgetToken t = new FakeGadgetToken();
    t.setAppUrl("http://localhost:8080/not-in-store.xml");
    OAuthServiceProvider provider = new OAuthServiceProvider("req", "authorize", "access");

    try {
      store.getConsumerKeyAndSecret(t, "", provider);
      fail();
    } catch (GadgetException e) {
      // good
    }

    BasicOAuthStoreConsumerKeyAndSecret cks = new BasicOAuthStoreConsumerKeyAndSecret(
        "somekey", "default", KeyType.RSA_PRIVATE, "keyname");
    store.setDefaultKey(cks);

    ConsumerInfo consumer = store.getConsumerKeyAndSecret(t, "", provider);
    assertEquals("somekey", consumer.getConsumer().consumerKey);
    assertNull(consumer.getConsumer().consumerSecret);
    assertEquals("RSA-SHA1", consumer.getConsumer().getProperty("oauth_signature_method"));
    assertEquals("default", consumer.getConsumer().getProperty(RSA_SHA1.PRIVATE_KEY));
    assertEquals(provider, consumer.getConsumer().serviceProvider);
    assertEquals("keyname", consumer.getKeyName());
  }
}

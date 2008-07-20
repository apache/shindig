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
package org.apache.shindig.gadgets.oauth;

import org.apache.shindig.gadgets.FakeGadgetSpecFactory;
import org.apache.shindig.gadgets.GadgetTestFixture;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for BasicGadgetOAuthTokenStore
 */
public class BasicGadgetOAuthTokenStoreTest extends GadgetTestFixture {
  
  private static final String CONFIG = "" +
  		"{" +
  		" 'http://localhost:8080/gadgets/files/samplecontainer/examples/oauth.xml' : {" +
  		"   'testservice' : {" +
      "     'consumer_key' : 'noCallbackConsumer'," +
      "     'consumer_secret' : 'noCallbackSecret'," + 
      "     'key_type' : 'HMAC_SYMMETRIC'" +
      "   }" +
      " }" +
      "}";
  
  private BasicGadgetOAuthTokenStore store;
  
  @Before
  public void setUp() throws Exception {
    OAuthStore backingStore = new BasicOAuthStore();    
    store = new BasicGadgetOAuthTokenStore(backingStore, new FakeGadgetSpecFactory());
    store.initFromConfigString(CONFIG);
  }
  
  @Test
  public void testReadOk() throws Exception {
    OAuthStore.TokenKey key = new OAuthStore.TokenKey();
    key.setGadgetUri("http://localhost:8080/gadgets/files/samplecontainer/examples/oauth.xml");
    key.setServiceName("testservice");
    key.setUserId("bob");
    key.setModuleId(0);
    key.setTokenName("");
    OAuthStore.AccessorInfo accessor = store.getOAuthAccessor(key, false);
    assertEquals("noCallbackConsumer", accessor.accessor.consumer.consumerKey);
    assertEquals("noCallbackSecret", accessor.accessor.consumer.consumerSecret);
    assertEquals(OAuthStore.SignatureType.HMAC_SHA1, accessor.signatureType);
  }
}

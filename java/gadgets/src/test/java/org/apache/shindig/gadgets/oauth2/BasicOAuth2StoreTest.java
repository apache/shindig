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
package org.apache.shindig.gadgets.oauth2;

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.servlet.Authority;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Cache;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Client;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Encrypter;
import org.apache.shindig.gadgets.oauth2.persistence.OAuth2Persister;
import org.apache.shindig.gadgets.oauth2.persistence.sample.InMemoryCache;

import org.junit.Test;

/**
 * @author <a href="mailto:dev@shindig.apache.org">Shindig Dev</a>
 * @version $Id: $
 */
public class BasicOAuth2StoreTest {

  @Test
  public void testSetTokenForSharedClient() throws Exception {
    final OAuth2Cache cache = new InMemoryCache();
    final OAuth2Persister persister = MockUtils.getDummyPersister();
    final OAuth2Encrypter encrypter = MockUtils.getDummyEncrypter();
    final BlobCrypter stateCrypter = MockUtils.getDummyStateCrypter();

    OAuth2Token token = MockUtils.getAccessToken();
    OAuth2Client client = MockUtils.getClient_Code_Confidential();
    client.setSharedToken( true );

    BasicOAuth2Store mockStore = createMockBuilder( BasicOAuth2Store.class )
            .withConstructor( OAuth2Cache.class, OAuth2Persister.class, OAuth2Encrypter.class, String.class, Authority.class, String.class, BlobCrypter.class )
            .withArgs( cache, persister, encrypter, MockUtils.REDIRECT_URI, (Authority)null, (String)null, stateCrypter )
            .addMockedMethod( "getClient" )
            .addMockedMethod( "getToken" )
            .createMock();

    expect( mockStore.getClient( eq(MockUtils.GADGET_URI1), eq(MockUtils.SERVICE_NAME) ) ).andReturn( client );
    expect( mockStore.getToken( eq(token.getGadgetUri()), eq(token.getServiceName()), eq(token.getUser()), eq(token.getScope()), eq(token.getType() ) )).andReturn( token );

    replay( mockStore );

    mockStore.setToken( token );

    verify( mockStore );
  }
}


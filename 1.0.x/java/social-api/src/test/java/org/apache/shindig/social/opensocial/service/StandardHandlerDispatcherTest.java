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
package org.apache.shindig.social.opensocial.service;

import com.google.inject.Provider;

import org.apache.shindig.social.EasyMockTestCase;
import org.easymock.classextension.IMocksControl;
import org.easymock.classextension.EasyMock;

/**
 * Tests StandardHandlerDispatcher.
 */
public class StandardHandlerDispatcherTest extends EasyMockTestCase {

  private Provider<PersonHandler> personHandlerProvider;
  private Provider<AppDataHandler> appDataHandlerProvider;
  private Provider<ActivityHandler> activityHandlerProvider;

  private IMocksControl mockControl;

  private StandardHandlerDispatcher dispatcher;

  @Override
  @SuppressWarnings("unchecked")
  protected void setUp() throws Exception {
    super.setUp();

    mockControl = EasyMock.createControl();
    personHandlerProvider = (Provider<PersonHandler>)  mockControl.createMock(Provider.class);
    appDataHandlerProvider = (Provider<AppDataHandler>)  mockControl.createMock(Provider.class);
    activityHandlerProvider = (Provider<ActivityHandler>) mockControl.createMock(Provider.class);
    dispatcher = new StandardHandlerDispatcher(personHandlerProvider,
            activityHandlerProvider, appDataHandlerProvider);
  }

  public void testGetHandler() {
    PersonHandler handler = mockControl.createMock(PersonHandler.class);
    EasyMock.expect(personHandlerProvider.get()).andReturn(handler);

    mockControl.replay();

    assertSame(handler, dispatcher.getHandler(DataServiceServlet.PEOPLE_ROUTE));

    mockControl.verify();
  }

  public void testGetHandler_serviceDoesntExist() {
    mockControl.replay();

    assertNull(dispatcher.getHandler("makebelieve"));

    mockControl.verify();
  }

  public void testAddHandler() {
    DataRequestHandler mockHandler = mockControl.createMock(DataRequestHandler.class);
    @SuppressWarnings("unchecked")
    Provider<DataRequestHandler> mockProvider = mockControl.createMock(Provider.class);
    dispatcher.addHandler("mock", mockProvider);

    EasyMock.expect(mockProvider.get()).andReturn(mockHandler);

    mockControl.replay();

    assertSame(mockHandler, dispatcher.getHandler("mock"));

    mockControl.verify();
  }
}

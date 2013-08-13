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

import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Maps;
import com.google.inject.Guice;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test social specific request parameters
 */
public class SocialRequestItemTest extends Assert {

  private static final FakeGadgetToken FAKE_TOKEN = new FakeGadgetToken();

  protected SocialRequestItem request;
  protected BeanJsonConverter converter;

  @Before
  public void setUp() throws Exception {
    FAKE_TOKEN.setAppId("12345");
    FAKE_TOKEN.setOwnerId("someowner");
    FAKE_TOKEN.setViewerId("someowner");
    converter = new BeanJsonConverter(Guice.createInjector());
    request = new SocialRequestItem(
        Maps.<String, String[]>newHashMap(),
        FAKE_TOKEN, converter, converter);
  }

  @Test
  public void testGetUser() throws Exception {
    request.setParameter("userId", "@owner");
    assertEquals(UserId.Type.owner, request.getUsers().iterator().next().getType());
  }

  @Test
  public void testGetGroup() throws Exception {
    request.setParameter("groupId", "@self");
    assertEquals(GroupId.Type.self, request.getGroup().getType());
  }
}

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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.config.JsonContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.protocol.DefaultHandlerRegistry;
import org.apache.shindig.protocol.HandlerExecutionListener;
import org.apache.shindig.protocol.HandlerRegistry;
import org.apache.shindig.protocol.RestHandler;
import org.apache.shindig.protocol.conversion.BeanJsonConverter;
import org.apache.shindig.social.opensocial.spi.AlbumService;
import org.apache.shindig.social.opensocial.spi.CollectionOptionsFactory;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class AlbumHandlerTest extends EasyMockTestCase {
  private AlbumService albumService;
  private AlbumHandler handler;
  private ContainerConfig containerConfig;
  private FakeGadgetToken token;
  protected HandlerRegistry registry;
  private BeanJsonConverter converter;

  @Before
  public void setUp() throws Exception {
    token = new FakeGadgetToken();
    converter = mock(BeanJsonConverter.class);
    albumService = mock(AlbumService.class);
    JSONObject config = new JSONObject('{' + ContainerConfig.DEFAULT_CONTAINER + ':' +
        "{'gadgets.container': ['default']," +
         "'gadgets.features':{opensocial:" +
           "{supportedFields: {album: ['id', 'title', 'location']}}" +
         "}}}");

    containerConfig = new JsonContainerConfig(config, Expressions.forTesting());
    handler = new AlbumHandler(albumService, containerConfig, new CollectionOptionsFactory());

    registry = new DefaultHandlerRegistry(null, converter,
        new HandlerExecutionListener.NoOpHandler());
    registry.addHandlers(ImmutableSet.<Object>of(handler));
  }

  @Test
  public void testCreate() throws Exception {
    // TODO
  }

  @Test
  public void testGet() throws Exception {
    // TODO
  }

  @Test
  public void testUpdate() throws Exception {
    // TODO
  }

  @Test
  public void testDelete() throws Exception {
    // TODO
  }

  @Test
  public void testSupportedFields() throws Exception {
    String path = "/albums/@supportedFields";
    RestHandler operation = registry.getRestHandler(path, "GET");

    replay();
    @SuppressWarnings("unchecked")
    List<Object> received = (List<Object>) operation.execute(Maps.<String, String[]>newHashMap(),
        null, token, converter).get();
    assertEquals(3, received.size());
    assertEquals("id", received.get(0).toString());
    assertEquals("title", received.get(1).toString());
    assertEquals("location", received.get(2).toString());

    verify();
  }
}

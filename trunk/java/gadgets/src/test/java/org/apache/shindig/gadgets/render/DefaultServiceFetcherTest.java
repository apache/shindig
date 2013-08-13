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
package org.apache.shindig.gadgets.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import org.apache.shindig.auth.BasicSecurityTokenCodec;
import org.apache.shindig.common.EasyMockTestCase;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.config.JsonContainerConfig;
import org.apache.shindig.expressions.Expressions;
import org.apache.shindig.expressions.Functions;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;

import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Test fetching of osapi services from container config and endpoints.
 */
public class DefaultServiceFetcherTest extends EasyMockTestCase {
  protected DefaultServiceFetcher fetcher;
  protected HttpFetcher mockFetcher;
  protected Multimap<String, String> configuredServices;
  protected static final String endPoint1 = "http://%host%/api/rpc";
  protected static final String endPoint2 = "http://%host%/social/api/rpc";


  @Before
  public void setUp() throws Exception {
    JSONObject config = createConfig();

    JsonContainerConfig containerConfig =
        new JsonContainerConfig(config, Expressions.forTesting(new Functions()));
    mockFetcher = mock(HttpFetcher.class);
    fetcher = new DefaultServiceFetcher(containerConfig, mockFetcher);
  }

  private JSONObject createConfig() throws JSONException {
    JSONObject config = new JSONObject();
    JSONObject container = new JSONObject();
    JSONObject services = new JSONObject();
    JSONObject features = new JSONObject();

    configuredServices = ImmutableMultimap.<String, String>builder()
      .putAll("http://localhost/api/rpc", "system.listMethods", "service.get")
      .putAll("gadgets.rpc", "messages.send", "ui.resize").build();

    for (String key : configuredServices.keySet()) {
      services.put(key, configuredServices.get(key));
    }
    features.put(DefaultServiceFetcher.OSAPI_SERVICES, services);

    JSONObject endpoints = new JSONObject();

    endpoints.put(DefaultServiceFetcher.OSAPI_BASE_ENDPOINTS,
        new JSONArray(ImmutableList.of(endPoint1, endPoint2)));
    features.put(DefaultServiceFetcher.OSAPI_FEATURE_CONFIG, endpoints);
    container.put(ContainerConfig.CONTAINER_KEY, new JSONArray("['default']"));
    container.put(DefaultServiceFetcher.GADGETS_FEATURES_CONFIG, features);

    config.put("default", container);
    return config;
  }

  @Test
  public void testReadConfigNoEndpoints() throws Exception {
    JSONObject config = createConfig();
    config.getJSONObject("default").
        getJSONObject(DefaultServiceFetcher.GADGETS_FEATURES_CONFIG)
        .remove(DefaultServiceFetcher.OSAPI_FEATURE_CONFIG);
    JsonContainerConfig containerConfig =
        new JsonContainerConfig(config,
            Expressions.forTesting(new Functions()));
    fetcher = new DefaultServiceFetcher(containerConfig, mockFetcher);

    EasyMock.expect(mockFetcher.fetch(EasyMock.isA(HttpRequest.class))).andReturn(
        new HttpResponse("")).anyTimes();
    replay();
    Multimap<String, String> services = fetcher.getServicesForContainer("default", "dontcare");
    verify();
    assertEquals(configuredServices, services);
  }

  @Test
  public void testReadConfigEndpointsDown() throws Exception {
    EasyMock.expect(mockFetcher.fetch(EasyMock.isA(HttpRequest.class))).andReturn(
        new HttpResponse("")).anyTimes();
    replay();
    fetcher.setSecurityTokenCodec( new BasicSecurityTokenCodec() );
    Multimap<String, String> services = fetcher.getServicesForContainer("default", "dontcare");
    verify();
    assertEquals(configuredServices, services);
  }

  @Test
  public void testReadConfigWithValidEndpoints() throws Exception {
    List<String> endPoint1Services = ImmutableList.of("do.something", "delete.someting");
    JSONObject service1 = new JSONObject();
    service1.put("result", endPoint1Services);

    List<String> endPoint2Services = ImmutableList.of("weather.get");
    JSONObject service2 = new JSONObject();
    service2.put("result", endPoint2Services);

    EasyMock.expect(mockFetcher.fetch(EasyMock.isA(HttpRequest.class))).andReturn(
        new HttpResponse(service1.toString()));
    EasyMock.expect(mockFetcher.fetch(EasyMock.isA(HttpRequest.class))).andReturn(
        new HttpResponse(service2.toString()));

    replay();
    fetcher.setSecurityTokenCodec( new BasicSecurityTokenCodec() );
    Multimap<String, String> services = fetcher.getServicesForContainer("default", "dontcare");
    verify();
    Multimap<String, String> mergedServices = LinkedHashMultimap.create(configuredServices);
    mergedServices.putAll(endPoint1, endPoint1Services);
    mergedServices.putAll(endPoint2, endPoint2Services);
    assertEquals(mergedServices, LinkedHashMultimap.create(services));
  }

  @Test
  public void testReadConfigBadContainer() throws Exception {
    Multimap<String, String> multimap = fetcher.getServicesForContainer("badcontainer", "dontcare");
    assertEquals(0, multimap.size());
  }

  @Test
  public void testReadConfigRequestMarkedInternal() throws Exception {
    JSONObject config = createConfig();
    config.getJSONObject("default").
        getJSONObject(DefaultServiceFetcher.GADGETS_FEATURES_CONFIG)
        .getJSONObject(DefaultServiceFetcher.OSAPI_FEATURE_CONFIG)
        .put(DefaultServiceFetcher.OSAPI_BASE_ENDPOINTS, new JSONArray(ImmutableList.of(endPoint1)));

    JsonContainerConfig containerConfig =
        new JsonContainerConfig(config,
            Expressions.forTesting(new Functions()));
    CapturingHttpFetcher httpFetcher = new CapturingHttpFetcher();
    fetcher = new DefaultServiceFetcher(containerConfig, httpFetcher);
    fetcher.setSecurityTokenCodec( new BasicSecurityTokenCodec() );
    Multimap<String, String> services = fetcher.getServicesForContainer("default", "dontcare");
    assertEquals(configuredServices, services);
    assertNotNull( httpFetcher.request );
    assertTrue( httpFetcher.request.isInternalRequest() );
  }

  static class CapturingHttpFetcher implements HttpFetcher {

    public HttpRequest request;

    public CapturingHttpFetcher() {
    }

    public HttpResponse fetch(HttpRequest request) throws GadgetException {
      this.request = request;
      return new HttpResponseBuilder().setHttpStatusCode( HttpResponse.SC_OK )
                                      .setResponseString( "{\"result\":[]}" ).create();
    }
  }

}

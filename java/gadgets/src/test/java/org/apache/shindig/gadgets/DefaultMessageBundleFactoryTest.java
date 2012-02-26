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
package org.apache.shindig.gadgets;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.shindig.common.cache.Cache;
import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.LruCacheProvider;
import org.apache.shindig.common.testing.ImmediateExecutorService;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.http.RequestPipeline;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import org.easymock.EasyMock;
import org.junit.Test;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests for DefaultMessageBundleFactory
 */
public class DefaultMessageBundleFactoryTest {
  private static final Uri BUNDLE_URI = Uri.parse("http://example.org/messagex.xml");
  private static final Uri LANG_BUNDLE_URI = Uri.parse("http://example.org/messagex.xml");
  private static final Uri COUNTRY_BUNDLE_URI = Uri.parse("http://example.org/messagex.xml");
  private static final Uri ALL_BUNDLE_URI = Uri.parse("http://example.org/messagex.xml");
  private static final Uri SPEC_URI = Uri.parse("http://example.org/gadget.xml");

  private static final String MSG_0_NAME = "messageZero";
  private static final String MSG_1_NAME = "message1";
  private static final String MSG_2_NAME = "message 2";
  private static final String MSG_3_NAME = "message 3";
  private static final String MSG_0_VALUE = "Message 0 VALUE";
  private static final String MSG_0_LANG_VALUE = "Message 0 language VALUE";
  private static final String MSG_0_COUNTRY_VALUE = "Message 0 country VALUE";
  private static final String MSG_0_VIEW_VALUE = "Message 0 view VALUE";
  private static final String MSG_0_ALL_VALUE = "Message 0 a VALUE";
  private static final String MSG_1_VALUE = "msg one val";
  private static final String MSG_2_VALUE = "message two val.";
  private static final String MSG_2_VIEW_VALUE = "message two view val.";
  private static final String MSG_3_VALUE = "message three value";

  private static final Locale COUNTRY_LOCALE = new Locale("all", "US");
  private static final Locale LANG_LOCALE = new Locale("en", "ALL");
  private static final Locale LOCALE = new Locale("en", "US");

  private static final String BASIC_BUNDLE
      = "<messagebundle>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_VALUE + "</msg>" +
        "  <msg name='" + MSG_1_NAME + "'>" + MSG_1_VALUE + "</msg>" +
        "</messagebundle>";

  private static final String LANG_BUNDLE
      = "<messagebundle>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_LANG_VALUE + "</msg>" +
        "  <msg name='lang'>true</msg>" +
        "</messagebundle>";

  private static final String COUNTRY_BUNDLE
      = "<messagebundle>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_COUNTRY_VALUE + "</msg>" +
        "  <msg name='country'>true</msg>" +
        "</messagebundle>";

  private static final String ALL_ALL_BUNDLE
      = "<messagebundle>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_ALL_VALUE + "</msg>" +
        "  <msg name='all'>true</msg>" +
        "</messagebundle>";

  private static final String BASIC_SPEC
      = "<Module>" +
        "<ModulePrefs title='foo'>" +
        " <Locale>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_ALL_VALUE + "</msg>" +
        " </Locale>" +
        " <Locale country='" + LOCALE.getCountry() + "'>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_COUNTRY_VALUE + "</msg>" +
        "  <msg name='" + MSG_3_NAME + "'>" + MSG_3_VALUE + "</msg>" +
        " </Locale>" +
        " <Locale country='" + LOCALE.getCountry() + "' views='view1,view2'>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_VIEW_VALUE + "</msg>" +
        "  <msg name='" + MSG_3_NAME + "'>" + MSG_3_VALUE + "</msg>" +
        " </Locale>" +
        " <Locale views='view1'>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_ALL_VALUE + "</msg>" +
        "  <msg name='" + MSG_2_NAME + "'>" + MSG_2_VIEW_VALUE + "</msg>" +
        " </Locale>" +
        " <Locale lang='" + LOCALE.getLanguage() + "'>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_LANG_VALUE + "</msg>" +
        "  <msg name='" + MSG_1_NAME + "'>" + MSG_1_VALUE + "</msg>" +
        "  <msg name='" + MSG_2_NAME + "'>" + MSG_2_VALUE + "</msg>" +
        " </Locale>" +
        " <Locale lang='" + LOCALE.getLanguage() + "' country='" + LOCALE.getCountry() + "' " +
        "  messages='" + BUNDLE_URI + "'/>" +
        "</ModulePrefs>" +
        "<Content type='html'/>" +
        "</Module>";

  private static final String ALL_EXTERNAL_SPEC
      = "<Module>" +
        "<ModulePrefs title='foo'>" +
        " <Locale messages='" + BUNDLE_URI + "'/>" +
        " <Locale country='" + LOCALE.getCountry() + '\'' +
        "  messages='" + COUNTRY_BUNDLE_URI + "'/>" +
        " <Locale lang='" + LOCALE.getLanguage() + "' messages='" + LANG_BUNDLE_URI + "'/>" +
        " <Locale lang='" + LOCALE.getLanguage() + "' country='" + LOCALE.getCountry() + "' " +
        "  messages='" + ALL_BUNDLE_URI + "'/>" +
        "</ModulePrefs>" +
        "<Content type='html'/>" +
        "</Module>";

  private static final int MAX_AGE = 10000;

  private final RequestPipeline pipeline = EasyMock.createNiceMock(RequestPipeline.class);
  private final CacheProvider cacheProvider = new LruCacheProvider(10);
  private final Cache<String, MessageBundle> cache
      = cacheProvider.createCache(DefaultMessageBundleFactory.CACHE_NAME);
  private final DefaultMessageBundleFactory bundleFactory
      = new DefaultMessageBundleFactory(new ImmediateExecutorService(), pipeline, cacheProvider, MAX_AGE);
  private final GadgetSpec gadgetSpec;
  private final GadgetSpec externalSpec;

  public DefaultMessageBundleFactoryTest() {
    try {
      gadgetSpec = new GadgetSpec(SPEC_URI, BASIC_SPEC);
      externalSpec = new GadgetSpec(SPEC_URI, ALL_EXTERNAL_SPEC);
    } catch (GadgetException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void getExactBundle() throws Exception {
    HttpResponse response = new HttpResponse(BASIC_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(response);
    replay(pipeline);

    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, LOCALE, true, ContainerConfig.DEFAULT_CONTAINER, null);

    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertEquals(MSG_1_VALUE, bundle.getMessages().get(MSG_1_NAME));
    assertEquals(MSG_2_VALUE, bundle.getMessages().get(MSG_2_NAME));
    assertEquals(MSG_3_VALUE, bundle.getMessages().get(MSG_3_NAME));
  }

  @Test
  public void getLangBundle() throws Exception {
    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, LANG_LOCALE, true, ContainerConfig.DEFAULT_CONTAINER, null);

    assertEquals(MSG_0_LANG_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertEquals(MSG_1_VALUE, bundle.getMessages().get(MSG_1_NAME));
    assertEquals(MSG_2_VALUE, bundle.getMessages().get(MSG_2_NAME));
    assertNull(bundle.getMessages().get(MSG_3_NAME));
  }

  @Test
  public void getCountryBundle() throws Exception {
    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, COUNTRY_LOCALE, true, ContainerConfig.DEFAULT_CONTAINER, null);

    assertEquals(MSG_0_COUNTRY_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertNull(bundle.getMessages().get(MSG_1_NAME));
    assertNull(bundle.getMessages().get(MSG_2_NAME));
    assertEquals(MSG_3_VALUE, bundle.getMessages().get(MSG_3_NAME));
  }

  @Test
  public void getViewCountryBundle() throws Exception {
    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, COUNTRY_LOCALE, true, ContainerConfig.DEFAULT_CONTAINER, "view1");

    assertEquals(MSG_0_VIEW_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertNull(bundle.getMessages().get(MSG_1_NAME));
    assertEquals(MSG_2_VIEW_VALUE, bundle.getMessages().get(MSG_2_NAME));
    assertEquals(MSG_3_VALUE, bundle.getMessages().get(MSG_3_NAME));
  }


  @Test
  public void getViewAllAllBundle() throws Exception {
    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, new Locale("all", "ALL"), true, ContainerConfig.DEFAULT_CONTAINER, "view1");

    assertEquals(MSG_0_ALL_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertNull(bundle.getMessages().get(MSG_1_NAME));
    assertEquals(MSG_2_VIEW_VALUE, bundle.getMessages().get(MSG_2_NAME));
    assertNull(bundle.getMessages().get(MSG_3_NAME));
  }

  @Test
  public void getAllAllBundle() throws Exception {
    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, new Locale("all", "ALL"), true, ContainerConfig.DEFAULT_CONTAINER, null);
    assertEquals(MSG_0_ALL_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertNull(bundle.getMessages().get(MSG_1_NAME));
    assertNull(bundle.getMessages().get(MSG_2_NAME));
    assertNull(bundle.getMessages().get(MSG_3_NAME));
  }

  @Test
  public void getExactBundleAllExternal() throws Exception {
    HttpResponse response = new HttpResponse(BASIC_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(response);
    HttpResponse langResponse = new HttpResponse(LANG_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(langResponse);
    HttpResponse countryResponse = new HttpResponse(COUNTRY_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(countryResponse);
    HttpResponse allAllResponse = new HttpResponse(ALL_ALL_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(allAllResponse);

    replay(pipeline);
    MessageBundle bundle = bundleFactory.getBundle(externalSpec, LOCALE, true, ContainerConfig.DEFAULT_CONTAINER, null);
    verify(pipeline);

    assertEquals("true", bundle.getMessages().get("lang"));
    assertEquals("true", bundle.getMessages().get("country"));
    assertEquals("true", bundle.getMessages().get("all"));
    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void getLangBundleAllExternal() throws Exception {
    HttpResponse langResponse = new HttpResponse(LANG_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(langResponse);
    HttpResponse allAllResponse = new HttpResponse(ALL_ALL_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(allAllResponse);

    replay(pipeline);
    MessageBundle bundle = bundleFactory.getBundle(externalSpec, LANG_LOCALE, true, ContainerConfig.DEFAULT_CONTAINER, null);
    verify(pipeline);

    assertEquals("true", bundle.getMessages().get("lang"));
    assertEquals("true", bundle.getMessages().get("all"));
    assertEquals(MSG_0_LANG_VALUE, bundle.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void getCountryBundleAllExternal() throws Exception {
    HttpResponse countryResponse = new HttpResponse(COUNTRY_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(countryResponse);
    HttpResponse allAllResponse = new HttpResponse(ALL_ALL_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(allAllResponse);

    replay(pipeline);
    MessageBundle bundle = bundleFactory.getBundle(externalSpec, COUNTRY_LOCALE, true, ContainerConfig.DEFAULT_CONTAINER, null);
    verify(pipeline);

    assertEquals("true", bundle.getMessages().get("country"));
    assertEquals("true", bundle.getMessages().get("all"));
    assertEquals(MSG_0_COUNTRY_VALUE, bundle.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void getAllAllExternal() throws Exception {
    HttpResponse allAllResponse = new HttpResponse(ALL_ALL_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(allAllResponse);

    replay(pipeline);
    MessageBundle bundle = bundleFactory.getBundle(externalSpec, new Locale("all", "ALL"), true, ContainerConfig.DEFAULT_CONTAINER, null);
    verify(pipeline);

    assertEquals("true", bundle.getMessages().get("all"));
    assertEquals(MSG_0_ALL_VALUE, bundle.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void getBundleFromCache() throws Exception {
    HttpResponse response = new HttpResponse(BASIC_BUNDLE);
    expect(pipeline.execute(isA(HttpRequest.class))).andReturn(response).once();
    replay(pipeline);

    MessageBundle bundle0 = bundleFactory.getBundle(gadgetSpec, LOCALE, false, ContainerConfig.DEFAULT_CONTAINER, null);
    MessageBundle bundle1 = bundleFactory.getBundle(gadgetSpec, LOCALE, false, ContainerConfig.DEFAULT_CONTAINER, null);

    verify(pipeline);

    assertEquals(bundle0.getMessages().get(MSG_0_NAME), bundle1.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void ignoreCacheDoesNotStore() throws Exception {
    bundleFactory.getBundle(gadgetSpec, new Locale("all", "ALL"), true, ContainerConfig.DEFAULT_CONTAINER, null);
    assertEquals(0, cache.getSize());
  }

  @Test
  public void badResponseServedFromCache() throws Exception {
    HttpResponse expiredResponse = new HttpResponseBuilder()
        .setResponse(BASIC_BUNDLE.getBytes("UTF-8"))
        .addHeader("Pragma", "no-cache")
        .create();
    HttpResponse badResponse = HttpResponse.error();

    expect(pipeline.execute(isA(HttpRequest.class)))
        .andReturn(expiredResponse).once();
    expect(pipeline.execute(isA(HttpRequest.class)))
        .andReturn(badResponse).once();
    replay(pipeline);

    final AtomicLong time = new AtomicLong();

    bundleFactory.cache.setTimeSource(new TimeSource() {
      @Override
      public long currentTimeMillis() {
        return time.get();
      }
    });

    time.set(System.currentTimeMillis());

    MessageBundle bundle0 = bundleFactory.getBundle(gadgetSpec, LOCALE, false, ContainerConfig.DEFAULT_CONTAINER, null);

    time.set(time.get() + MAX_AGE + 1);

    MessageBundle bundle1 = bundleFactory.getBundle(gadgetSpec, LOCALE, false, ContainerConfig.DEFAULT_CONTAINER, null);

    verify(pipeline);

    assertEquals(bundle0.getMessages().get(MSG_0_NAME), bundle1.getMessages().get(MSG_0_NAME));
  }

  @Test(expected=GadgetException.class)
  public void badResponsePropagatesException() throws Exception {
    HttpResponse badResponse = HttpResponse.error();

    expect(pipeline.execute(isA(HttpRequest.class)))
        .andReturn(badResponse).once();
    replay(pipeline);

    bundleFactory.getBundle(gadgetSpec, LOCALE, false, ContainerConfig.DEFAULT_CONTAINER, null);
  }

  @Test
  public void ttlPropagatesToFetcher() throws Exception {
    CapturingFetcher capturingFetcher = new CapturingFetcher();

    MessageBundleFactory factory = new DefaultMessageBundleFactory(
        new ImmediateExecutorService(), capturingFetcher, cacheProvider, MAX_AGE);

    factory.getBundle(gadgetSpec, LOCALE, false, ContainerConfig.DEFAULT_CONTAINER, null);

    assertEquals(MAX_AGE / 1000, capturingFetcher.request.getCacheTtl());
  }

  private static class CapturingFetcher implements RequestPipeline {
    HttpRequest request;

    protected CapturingFetcher() {
    }

    public HttpResponse execute(HttpRequest request) {
      this.request = request;
      return new HttpResponse(BASIC_BUNDLE);
    }
  }
}

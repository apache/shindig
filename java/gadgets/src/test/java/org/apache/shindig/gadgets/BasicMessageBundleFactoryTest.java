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
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.cache.CacheProvider;
import org.apache.shindig.common.cache.DefaultCacheProvider;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import org.easymock.EasyMock;
import org.junit.Test;

import java.util.Locale;

/**
 * Tests for BasicMessageBundleFactory
 */
public class BasicMessageBundleFactoryTest {
  private static final Uri BUNDLE_URI = Uri.parse("http://example.org/messagex.xml");
  private static final Uri SPEC_URI = Uri.parse("http://example.org/gadget.xml");

  private static final String MSG_0_NAME = "messageZero";
  private static final String MSG_1_NAME = "message1";
  private static final String MSG_2_NAME = "message 2";
  private static final String MSG_0_VALUE = "Message 0 VALUE";
  private static final String MSG_0_ALT_VALUE = "Message 0 Alternative VALUE";
  private static final String MSG_1_VALUE = "msg one val";
  private static final String MSG_2_VALUE = "message two val.";

  private static final Locale PARENT_LOCALE = new Locale("en", "ALL");
  private static final Locale LOCALE = new Locale("en", "US");

  private static final String BASIC_BUNDLE
      = "<messagebundle>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_VALUE + "</msg>" +
        "  <msg name='" + MSG_1_NAME + "'>" + MSG_1_VALUE + "</msg>" +
        "</messagebundle>";

  private static final String BASIC_SPEC
      = "<Module>" +
        "<ModulePrefs title='foo'>" +
        " <Locale lang='all' country='ALL'>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_VALUE + "</msg>" +
        " </Locale>" +
        " <Locale lang='" + LOCALE.getLanguage() + "'>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_ALT_VALUE + "</msg>" +
        "  <msg name='" + MSG_1_NAME + "'>" + MSG_1_VALUE + "</msg>" +
        "  <msg name='" + MSG_2_NAME + "'>" + MSG_2_VALUE + "</msg>" +
        " </Locale>" +
        " <Locale lang='" + LOCALE.getLanguage() + "' country='" + LOCALE.getCountry() + "' " +
        "  messages='" + BUNDLE_URI + "'/>" +
        "</ModulePrefs>" +
        "<Content type='html'/>" +
        "</Module>";


  private final HttpFetcher fetcher = EasyMock.createNiceMock(HttpFetcher.class);
  private final MessageBundleFactory bundleFactory;
  private final CacheProvider cacheProvider;
  private final GadgetSpec gadgetSpec;

  public BasicMessageBundleFactoryTest() {
    cacheProvider = new DefaultCacheProvider();
    bundleFactory = new BasicMessageBundleFactory(fetcher, cacheProvider, 5, -1000, 1000);
    try {
      gadgetSpec = new GadgetSpec(SPEC_URI, BASIC_SPEC);
    } catch (GadgetException e) {
      throw new RuntimeException(e);
    }
  }


  @Test
  public void getBundle() throws Exception {
    HttpResponse response = new HttpResponse(BASIC_BUNDLE);
    expect(fetcher.fetch(isA(HttpRequest.class))).andReturn(response);
    replay(fetcher);

    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, LOCALE, true);

    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertEquals(MSG_1_VALUE, bundle.getMessages().get(MSG_1_NAME));
    assertEquals(MSG_2_VALUE, bundle.getMessages().get(MSG_2_NAME));
  }

  @Test
  public void getParentBundle() throws Exception {
    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, PARENT_LOCALE, true);

    assertEquals(MSG_0_ALT_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertEquals(MSG_1_VALUE, bundle.getMessages().get(MSG_1_NAME));
    assertEquals(MSG_2_VALUE, bundle.getMessages().get(MSG_2_NAME));
  }

  @Test
  public void getAllAllBundle() throws Exception {
    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, new Locale("all", "ALL"), true);
    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void badResponseServedFromCache() throws Exception {
    HttpResponse expiredResponse = new HttpResponseBuilder()
        .setResponse(BASIC_BUNDLE.getBytes("UTF-8"))
        .addHeader("Pragma", "no-cache")
        .create();
    HttpResponse badResponse = HttpResponse.error();

    expect(fetcher.fetch(isA(HttpRequest.class)))
        .andReturn(expiredResponse).once();
    expect(fetcher.fetch(isA(HttpRequest.class)))
        .andReturn(badResponse).once();
    replay(fetcher);

    MessageBundle bundle = bundleFactory.getBundle(gadgetSpec, LOCALE, true);
    bundle = bundleFactory.getBundle(gadgetSpec, LOCALE, false);

    verify(fetcher);

    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void identicalMaxTtlAndMinTtlPropagatesToFetcher() throws Exception {
    CapturingFetcher capturingFetcher = new CapturingFetcher();

    BasicMessageBundleFactory forcedBundleFactory
        = new BasicMessageBundleFactory(capturingFetcher, cacheProvider, 5, 10000, 10000);

    HttpRequest request = new HttpRequest(BUNDLE_URI)
        .setIgnoreCache(false)
        .setCacheTtl(10);

    forcedBundleFactory.getBundle(gadgetSpec, LOCALE, false);

    assertEquals(10, capturingFetcher.request.getCacheTtl());
  }

  private static class CapturingFetcher implements HttpFetcher {
    HttpRequest request;

    public HttpResponse fetch(HttpRequest request) {
      this.request = request;
      return new HttpResponse(BASIC_BUNDLE);
    }
  }
}

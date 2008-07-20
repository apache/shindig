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
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.apache.shindig.common.xml.XmlUtil;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import com.google.common.collect.Maps;

import org.easymock.EasyMock;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests for BasicMessageBundleFactory
 */
public class BasicMessageBundleFactoryTest {
  private final static URI BUNDLE_URI
      = URI.create("http://example.org/messagex.xml");
  private final static URI SPEC_URI
      = URI.create("http://example.org/gadget.xml");

  private final static String MSG_0_NAME = "messageZero";
  private final static String MSG_1_NAME = "message1";
  private final static String MSG_0_VALUE = "Message 0 VALUE";
  private final static String MSG_1_VALUE = "msg one val";
  private final static String BASIC_BUNDLE
      = "<messagebundle>" +
        "  <msg name='" + MSG_0_NAME + "'>" + MSG_0_VALUE + "</msg>" +
        "  <msg name='" + MSG_1_NAME + "'>" + MSG_1_VALUE + "</msg>" +
        "</messagebundle>";
  private final static GadgetContext NO_CACHE_CONTEXT = new GadgetContext() {
    @Override
    public boolean getIgnoreCache() {
      return true;
    }
  };

  private final HttpFetcher fetcher = EasyMock.createNiceMock(HttpFetcher.class);
  private final MessageBundleFactory bundleFactory;

  public BasicMessageBundleFactoryTest() {
    bundleFactory = new BasicMessageBundleFactory(fetcher, 5, -1000, 1000);
  }

  @Test
  public void getBundleByUri() throws Exception {
    HttpResponse response = new HttpResponse(BASIC_BUNDLE);

    expect(fetcher.fetch(isA(HttpRequest.class))).andReturn(response);
    replay(fetcher);

    MessageBundle bundle = bundleFactory.getBundle(BUNDLE_URI, true);

    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertEquals(MSG_1_VALUE, bundle.getMessages().get(MSG_1_NAME));
  }

  @Test
  public void getBundleByLocaleSpec() throws Exception {
    String localeXml = "<Locale messages='" + BUNDLE_URI + "'/>";
    LocaleSpec locale = new LocaleSpec(XmlUtil.parse(localeXml), SPEC_URI);
    HttpResponse response = new HttpResponse(BASIC_BUNDLE);

    expect(fetcher.fetch(isA(HttpRequest.class))).andReturn(response);
    replay(fetcher);

    MessageBundle bundle = bundleFactory.getBundle(locale, NO_CACHE_CONTEXT);

    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
    assertEquals(MSG_1_VALUE, bundle.getMessages().get(MSG_1_NAME));
  }

  @Test
  public void getBundleByLocaleSpecWithNestedMessages() throws Exception {
    String localeXml
        = "<Locale>" +
          "<msg name='" + MSG_0_NAME + "'>" + MSG_0_VALUE + "</msg>" +
          "</Locale>";
    LocaleSpec locale = new LocaleSpec(XmlUtil.parse(localeXml), SPEC_URI);

    MessageBundle bundle = bundleFactory.getBundle(locale, NO_CACHE_CONTEXT);

    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void badResponseServedFromCache() throws Exception {
    String localeXml
        = "<Locale>" +
          "<msg name='" + MSG_0_NAME + "'>" + MSG_0_VALUE + "</msg>" +
          "</Locale>";
    LocaleSpec locale = new LocaleSpec(XmlUtil.parse(localeXml), SPEC_URI);
    assertEquals("all", locale.getLanguage());
    Map<String, List<String>> headers = Maps.newHashMap();
    headers.put("Pragma", Arrays.asList("no-cache"));
    HttpResponse expiredResponse = new HttpResponse(
        HttpResponse.SC_OK, BASIC_BUNDLE.getBytes("UTF-8"), headers);
    HttpResponse badResponse = HttpResponse.error();

    expect(fetcher.fetch(isA(HttpRequest.class)))
        .andReturn(expiredResponse).once();
    expect(fetcher.fetch(isA(HttpRequest.class)))
        .andReturn(badResponse).once();
    replay(fetcher);

    MessageBundle bundle = bundleFactory.getBundle(BUNDLE_URI, true);
    bundle = bundleFactory.getBundle(BUNDLE_URI, false);

    verify(fetcher);

    assertEquals(MSG_0_VALUE, bundle.getMessages().get(MSG_0_NAME));
  }

  @Test
  public void badLocaleGetsEmptyBundle() throws Exception {
    assertEquals(0,
        bundleFactory.getBundle(null, NO_CACHE_CONTEXT).getMessages().size());
  }
}

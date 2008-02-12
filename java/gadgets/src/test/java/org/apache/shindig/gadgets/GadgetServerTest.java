/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets;

import static org.apache.shindig.gadgets.GadgetSpecTestFixture.DATETIME_ID;
import static org.apache.shindig.gadgets.GadgetSpecTestFixture.DATETIME_SPEC;
import static org.apache.shindig.gadgets.GadgetSpecTestFixture.DATETIME_URI;
import static org.apache.shindig.gadgets.GadgetSpecTestFixture.DATETIME_URI_STRING;
import static org.apache.shindig.gadgets.GadgetSpecTestFixture.DATETIME_XML;
import static org.apache.shindig.gadgets.GadgetSpecTestFixture.EN_US_LOCALE;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.concurrent.Executors;

public class GadgetServerTest extends EasyMockTestCase {
  GadgetServer gadgetServer;
  final RemoteContentFetcher fetcher = mock(RemoteContentFetcher.class);
  @SuppressWarnings(value="unchecked")
  final GadgetDataCache<GadgetSpec> specCache = mock(GadgetDataCache.class);
  @SuppressWarnings(value="unchecked")
  final GadgetDataCache<MessageBundle> bundleCache = mock(GadgetDataCache.class);

  void initServer(GadgetServerConfigReader customConfig) throws GadgetException {
    GadgetServerConfig config = new GadgetServerConfig();

    if (customConfig != null) {
      config.copyFrom(customConfig);
    }

    if (config.getExecutor() == null) {
      config.setExecutor(Executors.newCachedThreadPool());
    }

    if (config.getSpecCache() == null) {
      config.setSpecCache(specCache);
    }

    if (config.getMessageBundleCache() == null) {
      config.setMessageBundleCache(bundleCache);
    }

    if (config.getFeatureRegistry() == null) {
      config.setFeatureRegistry(new GadgetFeatureRegistry(null));
    }

    if (config.getContentFetcher() == null) {
      config.setContentFetcher(fetcher);
    }

    gadgetServer = new GadgetServer(config);
  }

  public void testGadgetSpecNotInCache() throws Exception {
    initServer(null);
    RemoteContent results = new RemoteContent(200, DATETIME_XML.getBytes(), null);
    ProcessingOptions options = new ProcessingOptions();

    RemoteContentRequest req = new RemoteContentRequest(DATETIME_URI);

    expect(specCache.get(eq(DATETIME_URI_STRING))).andReturn(null);
    expect(fetcher.fetch(eq(req),
                         eq(options))).andReturn(results);
    specCache.put(eq(DATETIME_URI_STRING), isA(GadgetSpec.class));
    replay();

    Gadget gadget = gadgetServer.processGadget(DATETIME_ID, UserPrefs.EMPTY, EN_US_LOCALE,
                                               RenderingContext.GADGET, options);
    verify();
  }

  public void testGadgetSpecInCache() throws Exception {
    initServer(null);
    expect(specCache.get(eq(DATETIME_URI_STRING))).andReturn(DATETIME_SPEC);
    replay();

    Gadget gadget = gadgetServer.processGadget(DATETIME_ID, UserPrefs.EMPTY, EN_US_LOCALE,
                                               RenderingContext.GADGET, null);
    assertSame(DATETIME_SPEC, gadget.getBaseSpec());
    verify();
  }

  public void testBasicGadget() throws Exception {
    initServer(null);
    RemoteContent results = new RemoteContent(200, DATETIME_XML.getBytes(), null);
    ProcessingOptions options = new ProcessingOptions();

    RemoteContentRequest req = new RemoteContentRequest(DATETIME_URI);

    expect(specCache.get(eq(DATETIME_URI_STRING))).andReturn(null);
    expect(fetcher.fetch(eq(req),
                         eq(options))).andReturn(results);
    specCache.put(eq(DATETIME_URI_STRING), isA(GadgetSpec.class));
    replay();

    Gadget gadget = gadgetServer.processGadget(DATETIME_ID, UserPrefs.EMPTY, EN_US_LOCALE,
                                               RenderingContext.GADGET, options);
    assertEquals("Hello, World!", gadget.getTitle());
    assertEquals("Goodbye, World!", gadget.getContentData());
    verify();
  }

  public void testBlacklistedGadget() throws Exception {
    GadgetBlacklist blacklist = mock(GadgetBlacklist.class);
    initServer(new GadgetServerConfig().setGadgetBlacklist(blacklist));
    expect(specCache.get(eq(DATETIME_URI_STRING))).andReturn(null);
    expect(blacklist.isBlacklisted(eq(DATETIME_URI))).andReturn(true);
    replay();

    try {
      gadgetServer.processGadget(DATETIME_ID, UserPrefs.EMPTY, EN_US_LOCALE,
                                 RenderingContext.GADGET, null);
      fail();
    } catch (GadgetServer.GadgetProcessException ex) {
      assertEquals(1, ex.getComponents().size());
      assertEquals(GadgetException.Code.BLACKLISTED_GADGET,
                   ex.getComponents().get(0).getCode());
    }
    verify();
  }
}

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


import org.apache.shindig.gadgets.http.CrossServletState;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.MessageBundle;

import java.util.Set;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GadgetTestFixture extends EasyMockTestCase {
  public final HttpServletRequest request = mock(HttpServletRequest.class, true);
  public final HttpServletResponse response = mock(HttpServletResponse.class, true);
  public final GadgetServer gadgetServer;
  public final RemoteContentFetcher fetcher = mock(RemoteContentFetcher.class, true);
  @SuppressWarnings(value="unchecked")
  public final DataFetcher<GadgetSpec> specFetcher = mock(DataFetcher.class, true);
  @SuppressWarnings(value="unchecked")
  public final DataFetcher<MessageBundle> bundleFetcher
      = mock(DataFetcher.class);
  public final GadgetBlacklist blacklist = mock(GadgetBlacklist.class, true);
  public final GadgetFeatureRegistry registry;
  public final CrossServletState state = new CrossServletState() {
    @Override
    public GadgetServer getGadgetServer() {
      return gadgetServer;
    }

    @Override
    public GadgetSigner getGadgetSigner() {
      return null;
    }

    @Override
    public String getJsUrl(Set<String> libs, GadgetContext context) {
      StringBuilder bs = new StringBuilder();
      boolean first = false;
      for (String lib : libs) {
        if (!first) {
          first = true;
        } else {
          bs.append(":");
        }
        bs.append(lib);
      }
      return bs.toString();
    }

    @Override
    public String getIframeUrl(Gadget gadget) {
      return "";
    }

    @Override
    public void init(ServletContext config) {

    }
  };

  public GadgetTestFixture() {
    GadgetServerConfig config = new GadgetServerConfig();
    config.setExecutor(Executors.newSingleThreadExecutor());
    config.setGadgetSpecFetcher(specFetcher);
    config.setMessageBundleFetcher(bundleFetcher);
    config.setContentFetcher(fetcher);
    GadgetFeatureRegistry temp = null;
    try {
      temp = new GadgetFeatureRegistry(null, fetcher);
    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed to create feature registry");
    }
    registry = temp;
    config.setFeatureRegistry(registry);
    config.setGadgetBlacklist(blacklist);
    gadgetServer = new GadgetServer(config);
  }
}

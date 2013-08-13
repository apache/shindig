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
package org.apache.shindig.gadgets.preload;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.testing.FakeGadgetToken;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetContext;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Base code for the preloader tests.
 */
public class PreloaderTestFixture {
  protected static final Uri GADGET_URL = Uri.parse("http://example.org/gadget.xml");
  protected static final String CONTAINER = "some-container";
  protected static final String HOST = "example.org";
  protected String view = "default";
  protected boolean ignoreCache = false;
  public Map<String, String> contextParams = Maps.newHashMap();

  public final GadgetContext context = new GadgetContext() {
    @Override
    public SecurityToken getToken() {
      return new FakeGadgetToken();
    }

    @Override
    public String getView() {
      return view;
    }

    @Override
    public String getContainer() {
      return CONTAINER;
    }

    @Override
    public Uri getUrl() {
      return GADGET_URL;
    }

    @Override
    public String getHost() {
      return HOST;
    }

    @Override
    public String getParameter(String name) {
      return contextParams.get(name);
    }

    @Override
    public boolean getIgnoreCache() {
      return ignoreCache;
    }
  };
}

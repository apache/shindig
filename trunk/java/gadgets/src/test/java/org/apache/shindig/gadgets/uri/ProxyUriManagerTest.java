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
package org.apache.shindig.gadgets.uri;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for ProxyUriManager.
 */
public class ProxyUriManagerTest {
  final static Uri URI = Uri.parse("http://www.example.org");

  private Gadget getGadget(final String container) throws SpecParserException {
    GadgetSpec spec = new GadgetSpec(URI,
        "<Module><ModulePrefs author=\"a\" title=\"t\"></ModulePrefs>" +
        "<Content></Content></Module>");
    return new Gadget().setContext(new GadgetContext() {
      @Override
      public String getParameter(String name) {
        return "0";
      }

      @Override
      public String getContainer() {
        return container;
      }
    }).setSpec(spec);
  }

  @Test
  public void testReturnOrigContentOnErrorNullWhenNullContainer() throws Exception {
    ProxyUriManager.ProxyUri proxyUri = new ProxyUriManager.ProxyUri(
        0, true, false, null, "test", URI);
    assertNull(proxyUri.returnOriginalContentOnError);

    proxyUri = new ProxyUriManager.ProxyUri(getGadget(null), URI);
    assertNull(proxyUri.returnOriginalContentOnError);
  }

  @Test
  public void testReturnOrigContentOnErrorNullWhenNonAccelContainer() throws Exception {
    ProxyUriManager.ProxyUri proxyUri = new ProxyUriManager.ProxyUri(
        0, true, false, "dummy", "test", Uri.parse("http://www.example.org"));
    assertNull(proxyUri.returnOriginalContentOnError);

    proxyUri = new ProxyUriManager.ProxyUri(getGadget("dummy"), URI);
    assertNull(proxyUri.returnOriginalContentOnError);
  }

  @Test
  public void testReturnOrigContentOnErrorTrueWhenAccelContainer() throws Exception {
    ProxyUriManager.ProxyUri proxyUri = new ProxyUriManager.ProxyUri(
        0, true, false, "accel", "test", Uri.parse("http://www.example.org"));
    assertEquals("1", proxyUri.returnOriginalContentOnError);

    proxyUri = new ProxyUriManager.ProxyUri(getGadget("accel"), URI);
    assertEquals("1", proxyUri.returnOriginalContentOnError);
  }
}

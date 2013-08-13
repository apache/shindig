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

import static org.junit.Assert.assertEquals;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.GadgetContext;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.SpecParserException;

import org.junit.Test;

public class ProxyUriBaseTest {
  final static Uri URI = Uri.parse("http://www.example.org/foo.html");

  private Gadget createGadget(final String container) throws SpecParserException {
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
  public void testWithSetAuthorityAsGadgetParam() throws Exception {
    injectAuthorityAsGadgetParam(true);
    ProxyUriBase proxyUriBase = new ProxyUriBase(createGadget(null));
    assertEquals(URI.getAuthority(), proxyUriBase.getGadget());

    injectAuthorityAsGadgetParam(false);
    proxyUriBase = new ProxyUriBase(createGadget(null));
    assertEquals(URI.toString(), proxyUriBase.getGadget());
  }

  private void injectAuthorityAsGadgetParam(final Boolean val) {
    Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(Boolean.class).annotatedWith(
            Names.named("org.apache.shindig.gadgets.uri.setAuthorityAsGadgetParam"))
            .toInstance(val);
        requestStaticInjection(ProxyUriBase.class);
      }
    });
  }
}

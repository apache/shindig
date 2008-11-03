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
package org.apache.shindig.gadgets.parse;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * Provide parse bindings
 */
public class ParseModule extends AbstractModule {

  /**
   * {@InheritDoc}
   */
  @Override
  protected void configure() {
    //bind(GadgetHtmlParser.class).to(NekoHtmlParser.class);
    bind(GadgetHtmlParser.class).to(CajaHtmlParser.class);
    bind(DOMImplementation.class).toProvider(DOMImplementationProvider.class);
  }

  /**
   * Provider of new HTMLDocument implementations. Used to hide XML parser weirdness
   */
  public static class DOMImplementationProvider implements Provider<DOMImplementation> {

    DOMImplementation domImpl;

    public DOMImplementationProvider() {
      try {
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        // Require the traversal API
        domImpl = registry.getDOMImplementation("XML 1.0 Traversal 2.0");
      } catch (Exception e) {
        // Try another
      }
      // This is ugly but effective
      try {
        if (domImpl == null) {
          domImpl = (DOMImplementation)
              Class.forName("org.apache.xerces.internal.dom.DOMImplementationImpl").
                  getMethod("getDOMImplementation").invoke(null);
        }
      } catch (Exception ex) {
        //try another
      }
      try {
        if (domImpl == null) {
        domImpl = (DOMImplementation)
          Class.forName("com.sun.org.apache.xerces.internal.dom.DOMImplementationImpl").
              getMethod("getDOMImplementation").invoke(null);
        }
      } catch (Exception ex) {
        throw new RuntimeException("Could not find HTML DOM implementation", ex);
      }
    }

    public DOMImplementation get() {
      return domImpl;
    }
  }
}

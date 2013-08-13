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
package org.apache.shindig.gadgets.parse;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import org.apache.shindig.gadgets.parse.nekohtml.NekoSimplifiedHtmlParser;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import java.lang.reflect.InvocationTargetException;

/**
 * Provide parse bindings
 */
public class ParseModule extends AbstractModule {

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configure() {
    bind(GadgetHtmlParser.class).to(NekoSimplifiedHtmlParser.class);
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
      } catch (ClassNotFoundException e) {
        // Try another
      } catch (InstantiationException e) {
        // Try another
      } catch (IllegalAccessException e) {
        // Try another
      }
      // This is ugly but effective
      try {
        if (domImpl == null) {
          domImpl = (DOMImplementation)
              Class.forName("org.apache.xerces.dom.DOMImplementationImpl").
                  getMethod("getDOMImplementation").invoke(null);
        }
      } catch (ClassNotFoundException ex) {
        // ignore, try another
      } catch (IllegalAccessException ex) {
        // ignore, try another
      } catch (InvocationTargetException ex) {
        // ignore, try another
      } catch (NoSuchMethodException ex) {
        // ignore, try another
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

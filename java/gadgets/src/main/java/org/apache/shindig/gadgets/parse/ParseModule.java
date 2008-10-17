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

import org.apache.shindig.gadgets.parse.caja.CajaHtmlParser;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;

import org.w3c.dom.html.HTMLDocument;

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
    bind(HTMLDocument.class).toProvider(HTMLDocumentProvider.class);
  }

  /**
   * Provider of new HTMLDocument implementations. Used to hide XML parser weirdness
   */
  public static class HTMLDocumentProvider implements Provider<HTMLDocument> {

    Class htmlDocImpl;

    public HTMLDocumentProvider() {
      // This is ugly but effective
      try {
        htmlDocImpl = Class.forName("org.apache.html.dom.HTMLDocumentImpl");
      } catch (ClassNotFoundException cnfe) {
        try {
          htmlDocImpl = Class.forName("com.sun.org.apache.html.internal.dom.HTMLDocumentImpl");
        } catch (ClassNotFoundException cnfe2) {
          throw new RuntimeException("Could not find HTML DOM implementation", cnfe2);
        }
      }
    }

    public HTMLDocument get() {
      try {
        return (HTMLDocument) htmlDocImpl.newInstance();
      } catch (Exception e) {
        throw new RuntimeException("Could not create HTML DOM from class "
            + htmlDocImpl.getName(), e);
      }
    }
  }
}

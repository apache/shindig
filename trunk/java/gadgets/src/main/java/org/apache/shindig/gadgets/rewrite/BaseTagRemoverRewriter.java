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
package org.apache.shindig.gadgets.rewrite;

import org.apache.shindig.gadgets.Gadget;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponseBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple rewriter that deletes the base tag from the html document.
 *
 * @since 2.0.0
 */
public class BaseTagRemoverRewriter implements GadgetRewriter, ResponseRewriter {
  private static final Logger logger = Logger.getLogger(BaseTagRemoverRewriter.class.getName());

  public void rewrite(Gadget gadget, MutableContent mc) {
    Document doc = mc.getDocument();

    NodeList list = doc.getElementsByTagName("base");
    for (int i = 0; i < list.getLength(); i++) {
      Element baseElement = (Element) list.item(i);
      baseElement.getParentNode().removeChild(baseElement);

      if (baseElement.hasAttribute("href") && logger.isLoggable(Level.FINE)) {
        logger.fine("Removing base tag pointing to: "
                    + baseElement.getAttribute("href") + " for gadget: "
                    + gadget.getContext().getUrl().toString());
      }
    }

    mc.documentChanged();
  }

  public void rewrite(HttpRequest request, HttpResponseBuilder response, Gadget gadget)
          throws RewritingException {
    if (RewriterUtils.isHtml(request, response)) {
      if(gadget == null) {
        gadget = DomWalker.makeGadget(request);
      }
      rewrite(gadget, response);
    }
  }
}

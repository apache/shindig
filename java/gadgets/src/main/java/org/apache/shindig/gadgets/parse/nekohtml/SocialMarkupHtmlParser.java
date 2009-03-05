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
package org.apache.shindig.gadgets.parse.nekohtml;

import java.io.StringReader;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.HTMLScanner;
import org.w3c.dom.DOMImplementation;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Supports parsing of social markup blocks inside gadget content.
 * &lt;script&gt; elements with types of either "text/os-template"
 * or "text/os-data" are parsed inline into contained DOM hierarchies
 * for subsequent processing by the pipeline and template rewriters.
 */
@Singleton
public class SocialMarkupHtmlParser extends NekoSimplifiedHtmlParser {
  @Inject
  public SocialMarkupHtmlParser(DOMImplementation documentProvider) {
    super(documentProvider);
  }

  @Override
  protected boolean isElementImportant(QName name) {
    // For now, just include everything
    return true;
  }

  @Override
  protected HTMLConfiguration newConfiguration() {
    HTMLConfiguration config = super.newConfiguration();
    config.setFeature("http://xml.org/sax/features/namespaces", true);
    return config;
  }

  @Override
  protected DocumentHandler newDocumentHandler(String source, HTMLScanner scanner) {
    return new SocialMarkupDocumentHandler(source, scanner);
  }

  private class SocialMarkupDocumentHandler extends DocumentHandler {

    private StringBuilder scriptContent;
    private boolean inScript = false;
    private final HTMLScanner scanner;

    public SocialMarkupDocumentHandler(String content, HTMLScanner scanner) {
      super(content);
      this.scanner = scanner;
    }

    @Override
    public void characters(XMLString text, Augmentations augs) throws XNIException {
      if (inScript) {
        scriptContent.append(text.ch, text.offset, text.length);
      } else {
        super.characters(text, augs);
      }
    }

    @Override
    public void endElement(QName name, Augmentations augs) throws XNIException {
      if (inScript) {
        XMLInputSource scriptSource = new XMLInputSource(null, null, null);
        scriptSource.setCharacterStream(new StringReader(scriptContent.toString()));
        scriptContent.setLength(0);
        inScript = false;
        
        // Evaluate the content of the script block immediately
        scanner.evaluateInputSource(scriptSource);
      }
      
      super.endElement(name, augs);
    }

    @Override
    public void startElement(QName name, XMLAttributes xmlAttributes, Augmentations augs)
        throws XNIException {
      // Start gathering the content of any os-data or os-template elements
      if (name.rawname.toLowerCase().equals("script")) {
        String type = xmlAttributes.getValue("type");
        if ("text/os-data".equals(type) ||
            "text/os-template".equals(type)) {
          if (inScript) {
            throw new XNIException("Nested OpenSocial script elements");
          }
          inScript = true;
          if (scriptContent == null) {
            scriptContent = new StringBuilder();
          }
        }
      }
      
      super.startElement(name, xmlAttributes, augs);
    }
  }
}

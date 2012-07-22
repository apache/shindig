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
package org.apache.shindig.gadgets.parse.caja;

import com.google.caja.parser.html.Nodes;
import com.google.caja.render.Concatenator;
import com.google.caja.reporting.MarkupRenderMode;
import com.google.caja.reporting.RenderContext;
import org.apache.shindig.gadgets.parse.HtmlSerialization;
import org.apache.shindig.gadgets.parse.HtmlSerializer;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Serializer for VanillaCajaHtmlParser.
 */
public class VanillaCajaHtmlSerializer implements HtmlSerializer {
  public String serialize(Document doc) {
    try {
      StringWriter sw = HtmlSerialization.createWriter(doc);
      if (doc.getDoctype() != null) {
        HtmlSerialization.outputDocType(doc.getDoctype(), sw);
      }
      RenderContext renderContext =
          new RenderContext(new Concatenator(sw, null))
              // More compact but needs charset set correctly.
              .withAsciiOnly(false)
              .withMarkupRenderMode(MarkupRenderMode.HTML);

      // Use render unsafe in order to retain comments in the serialized HTML.
      // TODO: This function is deprecated. Use a non-deprecated function.
      Nodes.renderUnsafe(doc, renderContext);
      return sw.toString();
    } catch (IOException e) {
      return null;
    }
  }
}

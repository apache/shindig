/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shindig.gadgets;

import junit.framework.TestCase;

import java.net.URI;

public class GadgetSpecParserTest extends TestCase {

  private static final GadgetSpecParser parser = new GadgetSpecParser();

  private static class BasicGadgetId implements GadgetView.ID {
    URI uri;
    public URI getURI() {
      return uri;
    }
    int moduleId;
    public int getModuleId() {
      return moduleId;
    }
    public String getKey() {
      return uri.toString();
    }
  }

  public void testBasicGadget() throws Exception {
    BasicGadgetId id = new BasicGadgetId();
    id.uri = new URI("http://example.org/text.xml");
    id.moduleId = 1;
    byte[] xml = ("<?xml version=\"1.0\"?>" +
                 "<Module>" +
                 "<ModulePrefs title=\"Hello, world!\"/>" +
                 "<Content type=\"html\">Hello!</Content>" +
                 "</Module>").getBytes();
    GadgetSpec spec = parser.parse(id, xml);

    assertEquals("Hello!", spec.getContentData());
    assertEquals("Hello, world!", spec.getTitle());
  }
}

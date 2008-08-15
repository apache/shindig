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

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import org.easymock.classextension.EasyMock;

import junit.framework.TestCase;

public class GadgetCssDeclarationTest extends TestCase {
  static ParsedCssDeclaration makeParsedDecl(String name, String value) {
    ParsedCssDeclaration parsed =
        EasyMock.createNiceMock(ParsedCssDeclaration.class);
    expect(parsed.getName()).andReturn(name).anyTimes();
    expect(parsed.getValue()).andReturn(value).anyTimes();
    replay(parsed);
    return parsed;
  }
  
  public void testDeclarationCreatedFromParsed() {
    ParsedCssDeclaration parsed = makeParsedDecl("foo", "bar");
    GadgetCssDeclaration decl = new GadgetCssDeclaration(parsed);
    assertEquals("foo", decl.getName());
    assertEquals("bar", decl.getValue());
    decl.setValue("baz");
    assertEquals("baz", decl.getValue());
  }
  
  public void testDeclarationCreatedFromRaw() {
    GadgetHtmlAttribute attrib = new GadgetHtmlAttribute("foo", "bar");
    assertEquals("foo", attrib.getName());
    assertEquals("bar", attrib.getValue());
    attrib.setValue("baz");
    assertEquals("baz", attrib.getValue());
  }
}

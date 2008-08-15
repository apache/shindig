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

public class GadgetHtmlAttributeTest extends TestCase {
  static ParsedHtmlAttribute makeParsedAttribute(String name, String value) {
    ParsedHtmlAttribute parsed = EasyMock.createNiceMock(ParsedHtmlAttribute.class);
    expect(parsed.getName()).andReturn(name).anyTimes();
    expect(parsed.getValue()).andReturn(value).anyTimes();
    replay(parsed);
    return parsed;
  }
  
  public void testAttributeCreatedFromParsed() {
    ParsedHtmlAttribute parsed = makeParsedAttribute("foo", "bar");
    GadgetHtmlAttribute attrib = new GadgetHtmlAttribute(parsed);
    assertEquals("foo", attrib.getName());
    assertEquals("bar", attrib.getValue());
    attrib.setValue("baz");
    assertEquals("baz", attrib.getValue());
  }
  
  public void testAttributeCreatedFromRaw() {
    GadgetHtmlAttribute attrib = new GadgetHtmlAttribute("foo", "bar");
    assertEquals("foo", attrib.getName());
    assertEquals("bar", attrib.getValue());
    attrib.setValue("baz");
    assertEquals("baz", attrib.getValue());
  }
}

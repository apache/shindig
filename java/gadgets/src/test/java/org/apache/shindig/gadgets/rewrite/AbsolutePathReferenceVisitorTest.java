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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor.VisitStatus;

import org.junit.Test;

import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class AbsolutePathReferenceVisitorTest extends DomWalkerTestBase {
  private static final Uri ABSOLUTE_URI = Uri.parse("http://host.com/path");
  private static final String JS_URI_STR = "javascript:foo();";
  private static final Uri RELATIVE_URI = Uri.parse("/host/relative");
  private static final Uri RELATIVE_RESOLVED_URI = GADGET_URI.resolve(RELATIVE_URI);
  private static final Uri PATH_RELATIVE_URI = Uri.parse("path/relative");
  private static final Uri PATH_RELATIVE_RESOLVED_URI = GADGET_URI.resolve(PATH_RELATIVE_URI);
  private static final String INVALID_URI_STRING = "!^|BAD URI|^!";
  
  @Test
  public void bypassComment() throws Exception {
    Comment comment = doc.createComment("howdy pardner");
    assertEquals(VisitStatus.BYPASS, getVisitStatus(comment));
  }
  
  @Test
  public void bypassText() throws Exception {
    Text text = doc.createTextNode("back scratchah! get ya back scratcha he'yah!");
    assertEquals(VisitStatus.BYPASS, getVisitStatus(text));
  }
  
  @Test
  public void bypassNonSupportedTag() throws Exception {
    Element div = elem("div", "src", RELATIVE_URI.toString(), "href", RELATIVE_URI.toString());
    assertEquals(VisitStatus.BYPASS, getVisitStatus(div));
  }
  
  @Test
  public void bypassTagWithoutAttrib() throws Exception {
    Element a = elem("a");
    assertEquals(VisitStatus.BYPASS, getVisitStatus(a));
  }
  
  @Test
  public void absolutifyTagA() throws Exception {
    checkAbsolutifyStates("a");
  }
  
  @Test
  public void absolutifyTagImg() throws Exception {
    checkAbsolutifyStates("img");
  }
  
  @Test
  public void absolutifyTagLink() throws Exception {
    checkAbsolutifyStates("link");
  }
  
  @Test
  public void absolutifyTagScript() throws Exception {
    checkAbsolutifyStates("script");
  }
  
  @Test
  public void absolutifyTagObject() throws Exception {
    checkAbsolutifyStates("object");
  }

  @Test
  public void revisitDoesNothing() throws Exception {
    assertFalse(new AbsolutePathReferenceVisitor().revisit(gadget(), null));
  }
  
  private void checkAbsolutifyStates(String tagName) throws Exception {
    String lcTag = tagName.toLowerCase();
    String ucTag = tagName.toUpperCase();
    String validAttr = AbsolutePathReferenceVisitor.RESOURCE_TAGS.get(lcTag);
    String invalidAttr = validAttr + "whoknows";
    
    // lowercase, correct attrib, relative-possible URL
    Element lcValidRelative = elem(lcTag, validAttr, RELATIVE_URI.toString());
    assertEquals(VisitStatus.MODIFY, getVisitStatus(lcValidRelative));
    assertEquals(RELATIVE_RESOLVED_URI.toString(), lcValidRelative.getAttribute(validAttr));
    
    Element lcValidPathRelative = elem(lcTag, validAttr, PATH_RELATIVE_URI.toString());
    assertEquals(VisitStatus.MODIFY, getVisitStatus(lcValidPathRelative));
    assertEquals(PATH_RELATIVE_RESOLVED_URI.toString(),
        lcValidPathRelative.getAttribute(validAttr));
    
    // uppercase, same
    Element ucValidRelative = elem(ucTag, validAttr, RELATIVE_URI.toString());
    assertEquals(VisitStatus.MODIFY, getVisitStatus(ucValidRelative));
    assertEquals(RELATIVE_RESOLVED_URI.toString(), ucValidRelative.getAttribute(validAttr));

    Element ucValidPathRelative = elem(ucTag, validAttr, PATH_RELATIVE_URI.toString());
    assertEquals(VisitStatus.MODIFY, getVisitStatus(ucValidPathRelative));
    assertEquals(PATH_RELATIVE_RESOLVED_URI.toString(),
        ucValidPathRelative.getAttribute(validAttr));

    // lowercase, correct attrib, invalid URL
    Element lcValidInvalid = elem(lcTag, validAttr, INVALID_URI_STRING);
    assertEquals(VisitStatus.BYPASS, getVisitStatus(lcValidRelative));
    assertEquals(INVALID_URI_STRING, lcValidInvalid.getAttribute(validAttr));
    
    // lowercase, correct attrib, absolute URL
    Element lcValidAbsolute = elem(lcTag, validAttr, ABSOLUTE_URI.toString());
    assertEquals(VisitStatus.BYPASS, getVisitStatus(lcValidAbsolute));
    assertEquals(ABSOLUTE_URI.toString(), lcValidAbsolute.getAttribute(validAttr));
    
    // lowercase, invalid attrib, relative-possible URL
    Element lcInvalidRelative = elem(lcTag, invalidAttr, RELATIVE_URI.toString());
    assertEquals(VisitStatus.BYPASS, getVisitStatus(lcInvalidRelative));
    assertEquals(RELATIVE_URI.toString(), lcInvalidRelative.getAttribute(invalidAttr));
      
    // lowercase, valid attrib, absolute (JS) URL
    Element lcValidJs = elem(lcTag, validAttr, JS_URI_STR);
    assertEquals(VisitStatus.BYPASS, getVisitStatus(lcValidJs));
    assertEquals(JS_URI_STR, lcValidJs.getAttribute(validAttr));
  }
  
  private VisitStatus getVisitStatus(Node node) throws Exception {
    return new AbsolutePathReferenceVisitor().visit(gadget(), node);
  }
}

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

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.rewrite.DomWalker.Visitor.VisitStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

public class AbsolutePathReferenceVisitorTest extends DomWalkerTestBase {
  private static final Uri ABSOLUTE_URI = Uri.parse("http://host.com/path");
  private static final String JS_URI_STR = "javascript:foo();";
  private static final Uri RELATIVE_URI = Uri.parse("/host/relative");
  private static final Uri RELATIVE_RESOLVED_URI = GADGET_URI.resolve(RELATIVE_URI);
  private static final Uri PATH_RELATIVE_URI = Uri.parse("path/relative");
  private static final Uri PATH_RELATIVE_RESOLVED_URI = GADGET_URI.resolve(PATH_RELATIVE_URI);
  private static final String INVALID_URI_STRING = "!^|BAD URI|^!";

  AbsolutePathReferenceVisitor visitorForAllTags() {
    return new AbsolutePathReferenceVisitor(
        AbsolutePathReferenceVisitor.Tags.RESOURCES,
        AbsolutePathReferenceVisitor.Tags.HYPERLINKS);
  }

  AbsolutePathReferenceVisitor visitorForHyperlinks() {
    return new AbsolutePathReferenceVisitor(
        AbsolutePathReferenceVisitor.Tags.HYPERLINKS);
  }

  AbsolutePathReferenceVisitor visitorForResources() {
    return new AbsolutePathReferenceVisitor(
        AbsolutePathReferenceVisitor.Tags.RESOURCES);
  }

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
  public void bypassObjectTag() throws Exception {
    Element objectElement = elem("object", "src", RELATIVE_URI.toString());
    assertEquals("Element with object tag should be bypassed",
                 VisitStatus.BYPASS, getVisitStatus(objectElement));
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
  public void absolutifyTagInput() throws Exception {
    checkAbsolutifyStates("input");
  }

  @Test
  public void absolutifyTagBody() throws Exception {
    checkAbsolutifyStates("body");
  }

  @Test
  public void absolutifyTagLink() throws Exception {
    Element cssLink = elem("link", "href", RELATIVE_URI.toString(),
                           "rel", "stylesheet", "type", "text/css");
    assertEquals("CSS link tag should not be bypassed",
                 VisitStatus.MODIFY, getVisitStatus(cssLink));
  }

  @Test
  public void bypassTagLinkWithNoRel() throws Exception {
    Element cssLink = elem("link", "href", RELATIVE_URI.toString(), "type", "text/css");
    assertEquals("CSS link tag should be bypassed",
                 VisitStatus.BYPASS, getVisitStatus(cssLink));
  }

  @Test
  public void bypassTagLinkWithNoType() throws Exception {
    Element cssLink = elem("link", "href", RELATIVE_URI.toString(), "rel", "stylesheet");
    assertEquals("CSS link tag should be bypassed",
                 VisitStatus.BYPASS, getVisitStatus(cssLink));
  }

  @Test
  public void bypassTagLinkAlternate() throws Exception {
    Element cssLink = elem("link", "href", RELATIVE_URI.toString(),
                           "rel", "alternate", "hreflang", "el");
    assertEquals("CSS link tag should be bypassed",
                 VisitStatus.BYPASS, getVisitStatus(cssLink));
  }

  @Test
  public void absolutifyTagScript() throws Exception {
    checkAbsolutifyStates("script");
  }

  @Test
  public void revisitDoesNothing() throws Exception {
    assertFalse(visitorForAllTags().revisit(gadget(), null));
  }

  @Test
  public void resolveRelativeToBaseTagIfPresent() throws Exception {
    Element baseTag = elem("base", "href", "http://www.example.org");
    Element img = elem("img", "src", RELATIVE_URI.toString());
    Element html = htmlDoc(null, baseTag, img);

    assertEquals(VisitStatus.BYPASS, getVisitStatus(baseTag));
    assertEquals(VisitStatus.MODIFY, getVisitStatus(img));
    assertEquals("http://www.example.org" + RELATIVE_URI.toString(),
                 img.getAttribute("src"));
  }

  @Test
  public void getBaseHrefReturnsNullIfBaseTagWithoutHrefAttribute()
      throws Exception {
    Element baseTag = elem("base");
    Element img = elem("img", "src", RELATIVE_URI.toString());
    Element html = htmlDoc(null, baseTag, img);

    AbsolutePathReferenceVisitor visitor = visitorForAllTags();
    assertEquals(VisitStatus.BYPASS, getVisitStatus(baseTag));
    assertEquals(VisitStatus.MODIFY, getVisitStatus(img));
    assertEquals(RELATIVE_RESOLVED_URI.toString(), img.getAttribute("src"));
  }

  @Test
  public void testGetBaseUri() throws Exception {
    Element baseTag1 = elem("base", "href", "http://www.example1.org");
    Element baseTag2 = elem("base", "href", "http://www.example2.org");

    Element img = elem("img", "src", RELATIVE_URI.toString());
    Element a = elem("a", "href", RELATIVE_URI.toString());

    Node[] headNodes = { baseTag1 };
    Element html = htmlDoc(headNodes, baseTag2, img, a);

    AbsolutePathReferenceVisitor visitor = visitorForAllTags();
    assertEquals("http://www.example1.org",
                 visitor.getBaseHref(html.getOwnerDocument()));
    assertEquals("http://www.example1.org",
                 visitor.getBaseUri(html.getOwnerDocument()).toString());
  }

  private void checkAbsolutifyStates(String tagName) throws Exception {
    String lcTag = tagName.toLowerCase();
    String ucTag = tagName.toUpperCase();
    Map<String, String> resourceTags = new HashMap<String, String>();
    resourceTags.putAll(AbsolutePathReferenceVisitor.Tags
        .RESOURCES.getResourceTags());
    resourceTags.putAll(AbsolutePathReferenceVisitor.Tags
        .HYPERLINKS.getResourceTags());
    String validAttr = resourceTags.get(lcTag);
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
    return visitorForAllTags().visit(gadget(), node);
  }
}

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

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GadgetHtmlNodeTest extends TestCase {
  public static ParsedHtmlAttribute makeParsedAttribute(String key, String val) {
    ParsedHtmlAttribute parsed = EasyMock.createNiceMock(ParsedHtmlAttribute.class);
    expect(parsed.getName()).andReturn(key).anyTimes();
    expect(parsed.getValue()).andReturn(val).anyTimes();
    replay(parsed);
    return parsed;
  }
  
  public static ParsedHtmlNode makeParsedTagNode(
      String tag, String[][] attribs, ParsedHtmlNode[] children) {
    ParsedHtmlNode parsed = EasyMock.createNiceMock(ParsedHtmlNode.class);
    expect(parsed.getTagName()).andReturn(tag).anyTimes();
    List<ParsedHtmlAttribute> attributes = new LinkedList<ParsedHtmlAttribute>();
    if (attribs != null) {
      for (String[] attrib : attribs) {
        attributes.add(makeParsedAttribute(attrib[0], attrib[1]));
      }
    }
    expect(parsed.getAttributes()).andReturn(attributes).anyTimes();
    List<ParsedHtmlNode> childNodes = new LinkedList<ParsedHtmlNode>();
    if (children != null) {
      for (ParsedHtmlNode child : children) {
        childNodes.add(child);
      }
    }
    expect(parsed.getChildren()).andReturn(childNodes).anyTimes();
    expect(parsed.getText()).andReturn(null).anyTimes();
    replay(parsed);
    return parsed;
  }
  
  public static ParsedHtmlNode makeParsedTextNode(String text) {
    ParsedHtmlNode parsed = EasyMock.createNiceMock(ParsedHtmlNode.class);
    expect(parsed.getText()).andReturn(text).anyTimes();
    expect(parsed.getTagName()).andReturn(null).anyTimes();
    expect(parsed.getAttributes()).andReturn(null).anyTimes();
    expect(parsed.getChildren()).andReturn(null).anyTimes();
    replay(parsed);
    return parsed;
  }
  
  private GadgetHtmlNode makeTagNodeFromNew(
      String tag, String[][] attribs, GadgetHtmlNode[] children) {
    GadgetHtmlNode node = new GadgetHtmlNode(tag, attribs);
    if (children != null) {
      for (GadgetHtmlNode child : children) {
        node.appendChild(child);
      }
    }
    return node;
  }
  
  private GadgetHtmlNode makeTextNodeFromNew(String text) {
    return new GadgetHtmlNode(text);
  }
  
  // Test: tag tree
  private static String[][] tagTreeAttribs = { { "id", "foo" }, { "name", "bar" } };
  public void testTagTreeCreatedFromParsedGetters() {
    ParsedHtmlNode[] parsedKids = {
        makeParsedTextNode("content"),
        makeParsedTagNode("span", null, null)
    };
    ParsedHtmlNode parsed = makeParsedTagNode("div", tagTreeAttribs, parsedKids);
    GadgetHtmlNode node = new GadgetHtmlNode(parsed);
    validateTagTreeGetters(node);
  }
  
  public void testTagTreeCreatedByNewGetters() {
    GadgetHtmlNode[] children = {
        makeTextNodeFromNew("content"),
        makeTagNodeFromNew("span", null, null)
    };
    GadgetHtmlNode node = makeTagNodeFromNew("div", tagTreeAttribs, children);
    validateTagTreeGetters(node);
  }
  
  private void validateTagTreeGetters(GadgetHtmlNode node) {
    assertFalse(node.isText());
    assertEquals("div", node.getTagName());
    assertNull(node.getParentNode());
    
    Set<String> attribKeys = node.getAttributeKeys();
    assertEquals(2, attribKeys.size());
    assertTrue(attribKeys.contains("id"));
    assertTrue(attribKeys.contains("name"));
    assertTrue(node.hasAttribute("id"));
    assertEquals("foo", node.getAttributeValue("id"));
    assertTrue(node.hasAttribute("name"));
    assertEquals("bar", node.getAttributeValue("name"));
    
    List<GadgetHtmlNode> children = node.getChildren();
    assertEquals(2, children.size());
    
    GadgetHtmlNode textChild = children.get(0);
    assertTrue(textChild.isText());
    assertEquals("content", textChild.getText());
    assertSame(node, textChild.getParentNode());
    
    GadgetHtmlNode tagChild = children.get(1);
    assertFalse(tagChild.isText());
    assertEquals("span", tagChild.getTagName());
    assertNotNull(tagChild.getAttributeKeys());
    assertEquals(0, tagChild.getAttributeKeys().size());
    assertNotNull(tagChild.getChildren());
    assertEquals(0, tagChild.getChildren().size());
    assertSame(node, tagChild.getParentNode());
  }
  
  // Test: basic getters
  private static String textGetterContent = "content";
  public void testTextCreatedFromParsedGetters() {
    ParsedHtmlNode parsed = makeParsedTextNode(textGetterContent);
    GadgetHtmlNode node = new GadgetHtmlNode(parsed);
    validateTextGetters(node);
  }
  
  public void testTextCreatedByNewGetters() {
    validateTextGetters(makeTextNodeFromNew(textGetterContent));
  }
  
  private void validateTextGetters(GadgetHtmlNode node) {
    assertTrue(node.isText());
    assertEquals("content", node.getText());
    assertNull(node.getParentNode());
  }
  
  // Test: tag name setter
  public void testTagCreatedFromParsedTagSetter() {
    ParsedHtmlNode parsed = makeParsedTagNode("div", null, null);
    GadgetHtmlNode node = new GadgetHtmlNode(parsed);
    validateTagNameSetter(node);
  }
  
  public void testTagCreatedFromNewTagSetter() {
    validateTagNameSetter(makeTagNodeFromNew("div", null, null));
  }
  
  private void validateTagNameSetter(GadgetHtmlNode node) {
    assertFalse(node.isText());
    assertEquals("div", node.getTagName());
    node.setTagName("span");
    assertEquals("span", node.getTagName());
  }
  
  // Test: tag attribute manipulation
  private static String[][] tagManipAttribs = { { "id", "foo" } };
  public void testTagCreatedFromParsedAttributeManipulation() {
    ParsedHtmlNode parsed =
        makeParsedTagNode("div", tagManipAttribs, null);
    GadgetHtmlNode node = new GadgetHtmlNode(parsed);
    validateTagAttributeManipulation(node);
  }
  
  public void testTagCreatedFromNewAttributeManipulation() {
    validateTagAttributeManipulation(
        makeTagNodeFromNew("div", tagManipAttribs, null));
  }
  
  private void validateTagAttributeManipulation(GadgetHtmlNode node) {
    assertFalse(node.isText());
    
    Set<String> origKeys = node.getAttributeKeys();
    assertNotNull(origKeys);
    assertEquals(1, origKeys.size());
    assertTrue(origKeys.contains("id"));
    assertTrue(node.hasAttribute("id"));
    assertEquals("foo", node.getAttributeValue("id"));
    
    // Set existing key
    node.setAttribute("id", "bar");
    Set<String> barKeys = node.getAttributeKeys();
    assertNotNull(barKeys);
    assertEquals(1, barKeys.size());
    assertTrue(barKeys.contains("id"));
    assertTrue(node.hasAttribute("id"));
    assertEquals("bar", node.getAttributeValue("id"));
    
    // Set existing key to null: null's perfectly valid
    // Also set with whitespace padding to test that bit
    assertTrue(node.setAttribute("id", null));
    Set<String> nullKeys = node.getAttributeKeys();
    assertNotNull(nullKeys);
    assertEquals(1, nullKeys.size());
    assertTrue(nullKeys.contains("id"));
    assertTrue(node.hasAttribute("id"));
    assertEquals(null, node.getAttributeValue("id"));
    
    // Remove id key
    assertTrue(node.removeAttribute("id"));
    assertFalse(node.removeAttribute("id"));
    Set<String> noKeys = node.getAttributeKeys();
    assertNotNull(noKeys);
    assertEquals(0, noKeys.size());
    assertFalse(node.hasAttribute("id"));
    
    // Add some new key
    assertTrue(node.setAttribute("name", "value"));
    Set<String> newKeys = node.getAttributeKeys();
    assertNotNull(newKeys);
    assertEquals(1, newKeys.size());
    assertTrue(node.hasAttribute("name"));
    assertEquals("value", node.getAttributeValue("name"));
    assertTrue(node.removeAttribute("name"));
    assertFalse(node.removeAttribute("name"));
  }
  
  // Test: tag node manipulation
  // We don't do created-from-new testing since html node creation types are
  // already mixed here.
  public void testTagCreatedFromParsedNodeManipulation() {
    ParsedHtmlNode[] kidNodes = { makeParsedTextNode("content") };
    ParsedHtmlNode parsed = makeParsedTagNode("div", null, kidNodes);
    GadgetHtmlNode parentNode = new GadgetHtmlNode(parsed);
    
    // Sanity check on created child
    List<GadgetHtmlNode> initialChildren = parentNode.getChildren();
    assertNotNull(initialChildren);
    assertEquals(1, initialChildren.size());
    GadgetHtmlNode textNode = initialChildren.get(0);
    assertTrue(textNode.isText());
    assertSame(parentNode, textNode.getParentNode());
    
    // appendChild
    GadgetHtmlNode afterNode = new GadgetHtmlNode(makeParsedTagNode("after", null, null));
    parentNode.appendChild(afterNode);
    List<GadgetHtmlNode> appendKids = parentNode.getChildren();
    assertNotNull(appendKids);
    assertEquals(2, appendKids.size());
    assertSame(textNode, appendKids.get(0));
    assertSame(afterNode, appendKids.get(1));
    assertSame(parentNode, textNode.getParentNode());
    assertSame(parentNode, afterNode.getParentNode());
    
    // insertBefore
    GadgetHtmlNode beforeNode = new GadgetHtmlNode(makeParsedTagNode("before", null, null));
    parentNode.insertBefore(beforeNode, textNode);
    List<GadgetHtmlNode> insertKids = parentNode.getChildren();
    assertNotNull(insertKids);
    assertEquals(3, insertKids.size());
    assertSame(beforeNode, insertKids.get(0));
    assertSame(textNode, insertKids.get(1));
    assertSame(afterNode, insertKids.get(2));
    assertSame(parentNode, beforeNode.getParentNode());
    assertSame(parentNode, textNode.getParentNode());
    assertSame(parentNode, afterNode.getParentNode());
    
    // remove before and text, leaving only after
    assertTrue(parentNode.removeChild(beforeNode));
    assertFalse(parentNode.removeChild(beforeNode));
    assertTrue(parentNode.removeChild(textNode));
    assertFalse(parentNode.removeChild(textNode));
    assertNull(beforeNode.getParentNode());
    assertNull(textNode.getParentNode());
    List<GadgetHtmlNode> remainingKids = parentNode.getChildren();
    assertNotNull(remainingKids);
    assertEquals(1, remainingKids.size());
    assertSame(parentNode, afterNode.getParentNode());
    assertSame(afterNode, remainingKids.get(0));
    
    // clear nodes
    parentNode.clearChildren();
    List<GadgetHtmlNode> clearedKids = parentNode.getChildren();
    assertNotNull(clearedKids);
    assertEquals(0, clearedKids.size());
  }
  
  // Test: text setter
  public void testTextCreatedFromParsedTextSetter() {
    ParsedHtmlNode parsed = makeParsedTextNode("content");
    GadgetHtmlNode node = new GadgetHtmlNode(parsed);
    validateTextSetter(node);
  }
  
  public void tesetTextCreatedFromNewTextSetter() {
    validateTextSetter(makeTextNodeFromNew("content"));
  }
  
  private void validateTextSetter(GadgetHtmlNode node) {
    assertTrue(node.isText());
    assertEquals("content", node.getText());
    node.setText("new content");
    assertEquals("new content", node.getText());
  }
  
  // Test: text-node API limitation
  public void testTagsFromParsedCantUseTextApis() {
    validateTagsCantUseTextApis(
        new GadgetHtmlNode(makeParsedTagNode("tag", null, null)));
  }
  
  public void testTagsFromNewCantUseTextApis() {
    validateTagsCantUseTextApis(makeTagNodeFromNew("tag", null, null));
  }
  
  private void validateTagsCantUseTextApis(GadgetHtmlNode tagNode) {
    assertFalse(tagNode.isText());
    
    try {
      tagNode.getText();
      fail("Tag nodes shouldn't be able to use getText()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      tagNode.setText("foo");
      fail("Tag nodes shouldn't be able to use setText()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
  }
  
  // Test: tag-node API limitation
  public void testTextFromParsedCantUseTagsApis() {
    validateTextCantUseTagsApis(new GadgetHtmlNode(makeParsedTextNode("content")));
  }
  
  public void testTextFromNewCantUseTagsApis() {
    validateTextCantUseTagsApis(makeTextNodeFromNew("content"));
  }
  
  private void validateTextCantUseTagsApis(GadgetHtmlNode textNode) {    
    assertTrue(textNode.isText());
    
    try {
      textNode.appendChild(null);
      fail("Text nodes shouldn't be able to use appendChild()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.getAttributeKeys();
      fail("Text nodes shouldn't be able to use getAttributeKeys()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.getAttributeValue("foo");
      fail("Text nodes shouldn't be able to use getAttributeValue()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.getChildren();
      fail("Text nodes shouldn't be able to use getChildren()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.getTagName();
      fail("Text nodes shouldn't be able to use getTagName()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.hasAttribute("foo");
      fail("Text nodes shouldn't be able to use hasAttribute()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.insertBefore(null, null);
      fail("Text nodes shouldn't be able to use insertBefore()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.removeAttribute("foo");
      fail("Text nodes shouldn't be able to use removeAttribute()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.removeChild(null);
      fail("Text nodes shouldn't be able to use removeChild()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.setAttribute("id", "foo");
      fail("Text nodes shouldn't be able to use setAttribute()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.setTagName("div");
      fail("Text nodes shouldn't be able to use setTagName()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
    
    try {
      textNode.clearChildren();
      fail("Text nodes shouldn't be able to use setTagName()");
    } catch (UnsupportedOperationException e) {
      // Expected condition
    }
  }
  
  public void testRenderOnlyTextNode() {
    String content = "  hello, world!\n  ";
    assertEquals(content, renderNode(new GadgetHtmlNode(content)));
  }
  
  public void testRenderOnlyTagNodeShortForm() {
    String[][] attribs = { { "id", "foo" } };
    GadgetHtmlNode tag = new GadgetHtmlNode("div", attribs);
    assertEquals("<div id=\"foo\"/>", renderNode(tag));
  }
  
  public void testRenderStyleSrcTag() {
    String[][] attribs = { { "src", "http://www.foo.com/bar.css" } };
    GadgetHtmlNode styleTag = new GadgetHtmlNode("style", attribs);
    assertEquals("<style src=\"http://www.foo.com/bar.css\"></style>",
                 renderNode(styleTag));
  }
  
  public void testRenderScriptSrcTag() {
    String[][] attribs = { { "src", "http://www.foo.com/bar.js" } };
    GadgetHtmlNode styleTag = new GadgetHtmlNode("script", attribs);
    assertEquals("<script src=\"http://www.foo.com/bar.js\"></script>",
                 renderNode(styleTag));
  }
  
  public void testRenderEscapedAttribute() {
    String[][] attribs = { { "foo", "<script&\"data\">" } };
    GadgetHtmlNode escapedTag = new GadgetHtmlNode("div", attribs);
    assertEquals("<div foo=\"&lt;script&amp;&quot;data&quot;&gt;\"/>",
                 renderNode(escapedTag));
  }
  
  public void testRenderNullValuedAttribute() {
    String[][] attribs = { { "marker", null } };
    GadgetHtmlNode tag = new GadgetHtmlNode("span", attribs);
    assertEquals("<span marker/>", renderNode(tag));
  }
  
  public void testRenderEscapedTextContent() {
    GadgetHtmlNode escapedTextNode = new GadgetHtmlNode("<script&\"data'>");
    assertEquals("&lt;script&amp;&quot;data'&gt;",
                 renderNode(escapedTextNode));
  }
  
  public void testRenderAdjacentStringsInTag() {
    GadgetHtmlNode container = new GadgetHtmlNode("div", null);
    container.appendChild(new GadgetHtmlNode("one"));
    container.appendChild(new GadgetHtmlNode("\n"));
    container.appendChild(new GadgetHtmlNode(" two "));
    assertEquals("<div>one\n two </div>", renderNode(container));
  }
  
  public void testRenderMixedContent() {
    // Something of a catch-all for smaller above tests.
    String[][] attribs = { { "readonly", null } };
    GadgetHtmlNode parent = new GadgetHtmlNode("div", attribs);
    parent.appendChild(new GadgetHtmlNode(" content\n"));
    parent.appendChild(new GadgetHtmlNode("<br>"));
    GadgetHtmlNode child1 = new GadgetHtmlNode("span", null);
    child1.appendChild(new GadgetHtmlNode("hr", null));
    parent.appendChild(child1);
    parent.appendChild(new GadgetHtmlNode("\"after text\""));
    GadgetHtmlNode child2 = new GadgetHtmlNode("p", null);
    child2.appendChild(new GadgetHtmlNode("paragraph!"));
    parent.appendChild(child2);
    assertEquals("<div readonly> content\n&lt;br&gt;<span><hr/>" +
                 "</span>&quot;after text&quot;<p>paragraph!</p></div>",
                 renderNode(parent));
  }
  
  public void testRenderCommentAlone() {
    String comment = "<!-- comment -->";
    GadgetHtmlNode commentNode = new GadgetHtmlNode(comment);
    assertEquals(comment, renderNode(commentNode));
  }
  
  public void testRenderCommentWithWhitespace() {
    String comment = "\n   <!--      comment\n  \n -->";
    GadgetHtmlNode commentNode = new GadgetHtmlNode(comment);
    assertEquals(comment, renderNode(commentNode));
  }
  
  public void testRenderTextWithCommentAndEscaped() {
    String text = "\n <!-- comment\n <br> --> <foo&bar>";
    GadgetHtmlNode textNode = new GadgetHtmlNode(text);
    assertEquals("\n <!-- comment\n <br> --> &lt;foo&amp;bar&gt;",
                 renderNode(textNode));
  }
  
  private String renderNode(GadgetHtmlNode node) {
    StringWriter sw = new StringWriter();
    try {
      node.render(sw);
    } catch (IOException e) {
      // Should never happen, but fail just in case.
      fail("Unexpected IOException on StringWriter operation");
    }
    return sw.toString();
  }
}

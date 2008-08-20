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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Mutable wrapper around a {@code ParsedHtmlNode}.
 * Used by rewriting to manipulate a parsed gadget DOM, and
 * to separate parsing from manipulation code. Essentially
 * a lightweight DOM1-style object.
 */
public class GadgetHtmlNode {
  private final NodeType type;
  private GadgetHtmlNode parentNode;
  private String tagName;
  private Map<String, String> attributes;
  private List<GadgetHtmlNode> children;
  private String text;
  
  private enum NodeType {
    TAG, TEXT
  }
  
  /**
   * Construct a mutable HTML node from a parsed one.
   * @param parsed HTML node object from parser.
   */
  public GadgetHtmlNode(ParsedHtmlNode parsed) {
    if (parsed.getText() == null) {
      // Tag type
      type = NodeType.TAG;
      parentNode = null;
      tagName = parsed.getTagName();
      attributes = new HashMap<String, String>();
      for (ParsedHtmlAttribute attrib : parsed.getAttributes()) {
        setAttribute(attrib.getName(), attrib.getValue());
      }
      children = new LinkedList<GadgetHtmlNode>();
      for (ParsedHtmlNode node: parsed.getChildren()) {
        appendChild(new GadgetHtmlNode(node));
      }
    } else {
      type = NodeType.TEXT;
      setText(parsed.getText());
    }
  }
  
  /**
   * Construct a tag-type HTML node.
   * @param tagName Tag name for new node, must not be null.
   * @param attributes Name/value pairs for new attributes, or null if none.
   */
  public GadgetHtmlNode(String tag, String[][] attribs) {
    type = NodeType.TAG;
    tagName = tag;
    attributes = new HashMap<String, String>();
    if (attribs != null) {
      for (String[] attrib : attribs) {
        if (attrib == null || attrib.length != 2) {
          throw new UnsupportedOperationException(
              "Coding error: Invalid GadgetHtmlNode creation");
        }
        setAttribute(attrib[0], attrib[1]);
      }
    }
    children = new LinkedList<GadgetHtmlNode>();
  }
  
  /**
   * Construct a text-type HTML node.
   * @param text Textual contents of new node.
   */
  public GadgetHtmlNode(String text) {
    type = NodeType.TEXT;
    setText(text);
  }
  
  /**
   * @return True if the node is text type
   */
  public boolean isText() {
    return type == NodeType.TEXT;
  }
  
  /**
   * @return Tag name for the HTML node.
   */
  public String getTagName() {
    validateNodeType(NodeType.TAG);
    return tagName;
  }
  
  /**
   * @param newTag New tag name to set for the node
   * @return True if the tag name was set, false if invalid
   */
  public boolean setTagName(String newTag) {
    validateNodeType(NodeType.TAG);
    if (tagName != null) {
      newTag = newTag.trim();
      if (newTag.matches("[\\w\\-_:]+")) {
        this.tagName = newTag;
        return true;
      }
    }
    return false;
  }

  /**
   * Retrieve an attribute by key.
   * @param key Attribute key to look up.
   * @return Value associated with key, or null if none.
   */
  public String getAttributeValue(String key) {
    validateNodeType(NodeType.TAG);
    return attributes.get(key);
  }
  
  /**
   * Remove an attribute by key.
   * @param key Key for attribute to remove.
   * @return Whether or not an attribute with that key was removed.
   */
  public boolean removeAttribute(String key) {
    validateNodeType(NodeType.TAG);
    boolean hasBefore = hasAttribute(key);
    attributes.remove(key);
    return hasBefore && !hasAttribute(key);
  }
  
  /**
   * Set an attribute's key/value.
   * @param key Attribute key.
   * @param value Attribute value.
   * @return Whether or not the set operation succeeded.
   */
  public boolean setAttribute(String key, String value) {
    validateNodeType(NodeType.TAG);
    String putKey = validateAttributeKey(key);
    if (putKey == null) {
      return false;
    }
    attributes.put(putKey, value);
    return true;
  }
  
  /**
   * @param key Key whose existence to test in the attribute set
   * @return Whether or not the node has an attribute for the given key
   */
  public boolean hasAttribute(String key) {
    validateNodeType(NodeType.TAG);
    return attributes.containsKey(key);
  }
  
  /**
   * @return Immutable set of attribute keys.
   */
  public Set<String> getAttributeKeys() {
    validateNodeType(NodeType.TAG);
    return Collections.unmodifiableSet(attributes.keySet());
  }
  
  // DOM-like node management helpers
  /**
   * Append a new node to this node's children.
   * @param node New node to append.
   */
  public void appendChild(GadgetHtmlNode node) {
    insertBefore(node, null);
  }
  
  /**
   * Insert a new node before another given node. If the relative
   * node is not found or null, insert the new node at the end of
   * this node's children.
   * @param node New node to insert.
   * @param before Node before which to insert {@code node}.
   */
  public void insertBefore(GadgetHtmlNode node, GadgetHtmlNode before) {
    validateNodeType(NodeType.TAG);
    node.setParentNode(this);
    if (before == null) {
      children.add(node);
      return;
    }
    int befIx = children.indexOf(before);
    if (befIx >= 0) {
      children.add(befIx, node);
    } else {
      children.add(node);
    }
  }
  
  /**
   * Remove the given node from the tree.
   * @param node Node to remove.
   * @return Whether or not the node was removed.
   */
  public boolean removeChild(GadgetHtmlNode node) {
    validateNodeType(NodeType.TAG);
    
    // For good measure, dissociate from parent
    node.setParentNode(null);
    return children.remove(node);
  }
  
  /**
   * Helper method that removes all children from
   * the current node.
   */
  public void clearChildren() {
    validateNodeType(NodeType.TAG);
    for (GadgetHtmlNode child : getChildren()) {
      removeChild(child);
    }
  }
  
  /**
   * Returns this nodes parent, or null if none exists.
   * @return
   */
  public GadgetHtmlNode getParentNode() {
    return parentNode;
  }
  
  // Internal helper: sets parent for tree-node management
  private void setParentNode(GadgetHtmlNode parent) {
    parentNode = parent;
  }
  
  /**
   * Returns an unmodifiable view of current child nodes.
   * While the list itself is unmodifiable, this node's contents
   * can be modified using list entries.
   * @return
   */
  public List<GadgetHtmlNode> getChildren() {
    validateNodeType(NodeType.TAG);
    return Collections.unmodifiableList(
        new ArrayList<GadgetHtmlNode>(children));
  }
  
  /**
   * @return Text for this node if text-type.
   */
  public String getText() {
    validateNodeType(NodeType.TEXT);
    return text;
  }
  
  /**
   * Set new text value for the node.
   * @param text New text value for the node.
   */
  public void setText(String text) {
    validateNodeType(NodeType.TEXT);
    this.text = text;
  }
  
  /**
   * Render self as HTML. Rendering is relatively simple as no
   * additional validation is performed on content beyond what
   * the rest of the class provides, such as attribute key
   * validation. All whitespace and comments are maintained. Nodes
   * with zero children are rendered short-form (&lt;foo/&gt;)
   * unless tagName is "style" or "script" since many browsers dislike short-form
   * for those.
   * One space is provided between attributes. Attribute values are surrounded
   * in double-quotes. Null-valued attributes are rendered without ="value".
   * Attributes are rendered in no particular order.
   * @param w Writer to which to send content
   * @throws IOException If the writer throws an error on append(...)
   */
  public void render(Writer w) throws IOException {
    if (isText()) {
      String rawText = getText();
      int commentStart = 0;
      int curPos = 0;
      while ((commentStart = rawText.indexOf("<!--", curPos)) >= 0) {
        // Comment found. By definition there must be an end-comment marker
        // since if there wasn't, the comment would subsume all further text.
        
        // First append up to the current point, with proper escaping.
        w.append(StringEscapeUtils.escapeHtml(rawText.substring(curPos, commentStart)));
        
        // Then append the comment verbatim.
        int commentEnd = rawText.indexOf("-->", commentStart);
        if (commentEnd == -1) {
          // Should never happen, per above comment. But we know that the comment
          // has begun, so just append the rest of the string verbatim to be safe.
          w.append(rawText.substring(commentStart));
          return;
        }
        int endPos = commentEnd + "-->".length();
        w.append(rawText.substring(commentStart, endPos));
        
        // Then set current position
        curPos = endPos;
      }
      
      // Append remaining (all, if no comment) text, escaped.
      w.append(StringEscapeUtils.escapeHtml(rawText.substring(curPos)));
    } else {
      w.append('<').append(tagName);
      for (String attrKey : getAttributeKeys()) {
        String attrValue = getAttributeValue(attrKey);
        w.append(' ').append(attrKey);
        if (attrValue != null) {
          w.append("=\"").append(StringEscapeUtils.escapeHtml(attrValue)).append('"');
        }
      }
      if (children.size() == 0 &&
          !tagName.equalsIgnoreCase("style") &&
          !tagName.equalsIgnoreCase("script")) {
        w.append("/>");
      } else {
        w.append('>');
        for (GadgetHtmlNode child : children) {
          child.render(w);
        }
        w.append("</").append(tagName).append('>');
      }
    }
  }
  
  // Helper that cleans up and validates an attribute key
  private String validateAttributeKey(String key) {
    if (key == null) {
      return null;
    }
    key = key.trim();
    if (!key.matches("[\\w\\d_\\-:]+")) {
      return null;
    }
    return key;
  }
  
  // Helper that enforces correct API usage by type
  private void validateNodeType(NodeType expected) {
    if (type != expected) {
      throw new UnsupportedOperationException("Code error: " +
          "Attempted " + expected + " operation on node of type " + type);
    }
  }
}

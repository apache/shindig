/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.parse.GadgetHtmlNode;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParsedHtmlNode;

/**
 * Object that maintains a String representation of arbitrary contents
 * and a consistent view of those contents as an HTML parse tree.
 */
public class MutableContent {
  private String content;
  private GadgetHtmlNode parseTree;
  private ContentEditListener editListener;
  private int parseEditId;
  private int contentParseId;
  private final GadgetHtmlParser contentParser;

  public MutableContent(GadgetHtmlParser contentParser) {
    this.contentParser = contentParser;
    this.contentParseId = parseEditId = 0;
  }

  public static final String ROOT_NODE_TAG_NAME = "gadget-root";
  
  /**
   * Retrieves the current content for this object in String form.
   * If content has been retrieved in parse tree form and has
   * been edited, the String form is computed from the parse tree by
   * rendering it. It is <b>strongly</b> encouraged to avoid switching
   * between retrieval of parse tree (through {@code getParseTree}),
   * with subsequent edits and retrieval of String contents to avoid
   * repeated serialization and deserialization.
   * @return Renderable/active content.
   */
  public String getContent() {
    if (parseEditId > contentParseId) {
      // Regenerate content from parse tree node, since the parse tree
      // was modified relative to the last time content was generated from it.
      // This is an expensive operation that should happen only once
      // per rendering cycle: all rewriters (or other manipulators)
      // operating on the parse tree should happen together.
      contentParseId = parseEditId;
      StringWriter sw = new StringWriter();
      for (GadgetHtmlNode node : parseTree.getChildren()) {
        try {
          node.render(sw);
        } catch (IOException e) {
          // Never happens.
        }
      }
      content = sw.toString();
    }
    return content;
  }
  
  /**
   * Sets the object's content as a raw String. Note, this operation
   * may be done at any time, even after a parse tree node has been retrieved
   * and modified (though a warning will be emitted in this case). Once
   * new content has been set, all subsequent edits to parse trees generated
   * from the <i>previous</i> content will be invalid, throwing an
   * {@code IllegalStateException}.
   * @param newContent New content.
   */
  public void setContent(String newContent) {
    if (content == null ||
        !content.equals(newContent)) {
      content = newContent;
      if (editListener != null) {
        editListener.stringEdited();
      }
    }
  }
  
  /**
   * Retrieves the object contents in parse tree form, if a
   * {@code GadgetHtmlParser} is configured and is able to parse the string
   * contents appropriately. The resultant parse tree has a special,
   * single top-level node that wraps all subsequent content, with
   * tag name {@code ROOT_NODE_TAG_NAME}. While it may be edited just
   * as any other node may, doing so is pointless since the root node
   * is stripped out during rendering. Any edits to the returned parse
   * tree performed after the source {@code MutableHtmlContent} has new content
   * set via {@code setContent} will throw an {@code IllegalStateException}
   * to maintain content consistency in the object. To modify the object's
   * contents by parse tree after setting new String contents,
   * this method must be called again. However, this practice is highly
   * discouraged, as parsing a tree from String is a costly operation.
   * @return Top-level node whose children represent the gadget's contents, or
   *         null if no parser is configured, String contents are null, or contents unparseable.
   */
  public GadgetHtmlNode getParseTree() {
    if (parseTree != null && !editListener.stringWasEdited()) {
      return parseTree;
    }
  
    if (content == null || contentParser == null) {
      return null;
    }
  
    // One ContentEditListener per parse tree.
    editListener = new ContentEditListener();
    parseTree = new GadgetHtmlNode(ROOT_NODE_TAG_NAME, null);
    List<ParsedHtmlNode> parsed = null;
    try {
      parsed = contentParser.parse(content);
    } catch (GadgetException e) {
      // TODO: emit info message
      return null;
    }
  
    if (parsed == null) {
      return null;
    }
    
    for (ParsedHtmlNode parsedNode : parsed) {
      parseTree.appendChild(new GadgetHtmlNode(parsedNode, editListener));
    }
  
    // Parse tree created from content: edit IDs are the same
    contentParseId = parseEditId;
    return parseTree;
  }
  
  // Intermediary object tracking edit behavior for the MutableHtmlContent to help maintain
  // state consistency. GadgetHtmlNode calls nodeEdited whenever a modification
  // is made to its original source.
  private class ContentEditListener implements GadgetHtmlNode.EditListener {
    private boolean stringEdited = false;
  
    public void nodeEdited() {
      ++parseEditId;
      if (stringEdited) {
        // Parse tree is invalid: a new String representation was set
        // as tree source in the meantime.
        throw new IllegalStateException("Edited parse node after setting String content");
      }
    }
  
    private void stringEdited() {
      stringEdited = true;
    }
  
    private boolean stringWasEdited() {
      return stringEdited;
    }
  }
}

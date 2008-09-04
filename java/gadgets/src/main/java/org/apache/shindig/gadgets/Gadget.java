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
package org.apache.shindig.gadgets;

import org.apache.shindig.common.ContainerConfig;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.GadgetHtmlNode;
import org.apache.shindig.gadgets.parse.GadgetHtmlParser;
import org.apache.shindig.gadgets.parse.ParsedHtmlNode;
import org.apache.shindig.gadgets.spec.GadgetSpec;
import org.apache.shindig.gadgets.spec.LocaleSpec;
import org.apache.shindig.gadgets.spec.Preload;
import org.apache.shindig.gadgets.spec.View;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Intermediary representation of all state associated with processing
 * of a single gadget request.
 */
public class Gadget {
  private final GadgetContext context;
  
  /**
   * @return The context in which this gadget was created.
   */
  public GadgetContext getContext() {
    return context;
  }

  private final GadgetSpec spec;
  
  /**
   * @return The spec from which this gadget was originally built.
   */
  public GadgetSpec getSpec() {
    return spec;
  }

  private final Collection<JsLibrary> jsLibraries;
  
  /**
   * @return A mutable collection of JsLibrary objects attached to this Gadget.
   */
  public Collection<JsLibrary> getJsLibraries() {
    return jsLibraries;
  }

  private final Map<Preload, Future<HttpResponse>> preloads
      = new HashMap<Preload, Future<HttpResponse>>();
  
  /**
   * @return A mutable map of preloads.
   */
  public Map<Preload, Future<HttpResponse>> getPreloadMap() {
    return preloads;
  }

  /**
   * Convenience function for getting the locale spec for the current context.
   *
   * Identical to:
   * Locale locale = gadget.getContext().getLocale();
   * gadget.getSpec().getModulePrefs().getLocale(locale);
   */
  public LocaleSpec getLocale() {
    return spec.getModulePrefs().getLocale(context.getLocale());
  }
  
  private final View currentView;
  
  /**
   * @return The (immutable) View applicable for the current request (part of GadgetSpec).
   */
  public View getCurrentView() {
	return currentView;
  }

  /**
   * Attempts to extract the "current" view for this gadget.
   *
   * @param config The container configuration; used to look for any view name
   *        aliases for the container specified in the context.
   */
  View getView(ContainerConfig config) {
    String viewName = context.getView();
    View view = spec.getView(viewName);
    if (view == null) {
      JSONArray aliases = config.getJsonArray(context.getContainer(),
          "gadgets.features/views/" + viewName + "/aliases");
      if (aliases != null) {
        try {
          for (int i = 0, j = aliases.length(); i < j; ++i) {
            viewName = aliases.getString(i);
            view = spec.getView(viewName);
            if (view != null) {
              break;
            }
          }
        } catch (JSONException e) {
          view = null;
        }
      }

      if (view == null) {
        view = spec.getView(GadgetSpec.DEFAULT_VIEW);
      }
    }
    return view;
  }
  
  private String content;
  private int contentParseId;
  
  /**
   * Retrieves the current content for this gadget in String form.
   * If gadget content has been retrieved in parse tree form and has
   * been edited, the String form is computed from the parse tree by
   * rendering it. It is <b>strongly</b> encouraged to avoid switching
   * between retrieval of parse tree (through {@code getParseTree}),
   * with subsequent edits and retrieval of String contents to avoid
   * repeated serialization and deserialization.
   * @return Renderable/active content for the gadget.
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
   * Sets the content for the gadget as a raw String. Note, this operation
   * may be done at any time, even after a parse tree node has been retrieved
   * and modified (though a warning will be emitted in this case). Once
   * new content has been set, all subsequent edits to parse trees generated
   * from the <i>previous</i> content will be invalid, throwing an
   * {@code IllegalStateException}.
   * @param newContent New content for the gadget.
   */
  public void setContent(String newContent) {
	if (!content.equals(newContent)) {
      content = newContent;
	  if (editListener != null) {
	    editListener.stringEdited();
	  }
	}
  }
  
  private GadgetHtmlNode parseTree;
  public static final String ROOT_NODE_TAG_NAME = "gadget-root";
  
  /**
   * Retrieves the contents of the gadget in parse tree form, if a
   * {@code GadgetHtmlParser} is configured and is able to parse the string
   * contents appropriately. The resultant parse tree has a special,
   * single top-level node that wraps all subsequent content, with
   * tag name {@code ROOT_NODE_TAG_NAME}. While it may be edited just
   * as any other node may, doing so is pointless since the root node
   * is stripped out during rendering. Any edits to the returned parse
   * tree performed after the source {@code Gadget} has new content
   * set via {@code setContent} will throw an {@code IllegalStateException}
   * to maintain content consistency in the gadget. To modify a gadget's
   * contents by parse tree after setting new String contents,
   * this method must be called again. However, this practice is highly
   * discouraged, as parsing a tree from String is a costly operation.
   * @return Top-level node whose children represent the gadget's contents, or
   *         null if no parser is configured or if String contents are null.
   * @throws GadgetException Throw by the GadgetHtmlParser generating the tree from String.
   */
  public GadgetHtmlNode getParseTree() throws GadgetException {
	if (parseTree != null && !editListener.stringWasEdited()) {
	  return parseTree;
	}
	
	if (content == null || contentParser == null) {
	  return null;
	}
	
	// One ContentEditListener per parse tree.
	editListener = new ContentEditListener();
	parseTree = new GadgetHtmlNode(ROOT_NODE_TAG_NAME, null);
	List<ParsedHtmlNode> parsed = contentParser.parse(content);
	for (ParsedHtmlNode parsedNode : parsed) {
	  parseTree.appendChild(new GadgetHtmlNode(parsedNode, editListener));
	}
	
	// Parse tree created from content: edit IDs are the same
	contentParseId = parseEditId;
	return parseTree;
  }

  private ContainerConfig containerConfig;
  private GadgetHtmlParser contentParser;
  private int parseEditId;
  private ContentEditListener editListener;
  
  public Gadget(GadgetContext context, GadgetSpec spec,
      Collection<JsLibrary> jsLibraries, ContainerConfig containerConfig,
      GadgetHtmlParser contentParser) {
    this.context = context;
    this.spec = spec;
    this.jsLibraries = jsLibraries;
    this.containerConfig = containerConfig;
    this.contentParser = contentParser;
    this.currentView = getView(this.containerConfig);
    if (this.currentView != null) {
      // View might be invalid or associated with no content (type=URL)
      this.content = this.currentView.getContent();
    } else {
      this.content = null;
    }
    contentParseId = 0;
  }
  
  // Intermediary object tracking edit behavior for the Gadget to help maintain
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
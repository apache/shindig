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

// A pseudo namespace.
var domutil = {};

/**
 * Helper functions adapted from Selenium code.
 *
 * Determines if the specified element is visible. An element can be rendered
 * invisible by setting the CSS "visibility" property to "hidden", or the
 * "display" property to "none", either for the element itself or one if its
 * ancestors. This method will fail if the element is not present.
 * @param {Node} node The node to check.
 * @return {boolean} Whether the node is visible.
 */
domutil.isVisible = function(node) {
  // This test is necessary because getComputedStyle returns nothing
  // for WebKit-based browsers if node not in document. See comment below.
  if (node.style.display == 'none' || node.style.visibility == 'hidden') {
    return false;
  }
  var visibility = this.findEffectiveStyleProperty(node, 'visibility');
  var display = this.findEffectiveStyleProperty(node, 'display');
  return visibility != 'hidden' && display != 'none';
};


/**
 * Returns the value of the effective style specified by {@code property}.
 * @param {Node} element The node to query.
 * @param {string} property The name of a style that is of interest.
 * @return {string} The value of style {@code property}.
 */
domutil.findEffectiveStyleProperty = function(element, property) {
  var effectiveStyle = this.findEffectiveStyle(element);
  var propertyValue = effectiveStyle[property];
  if (propertyValue == 'inherit' && element.parentNode.style) {
    return this.findEffectiveStyleProperty(element.parentNode, property);
  }
  return propertyValue;
};


/**
 * Returns the effective style object.
 * @param {Node} element The node to query.
 * @return {CSSStyleDeclaration|undefined} The style object.
 */
domutil.findEffectiveStyle = function(element) {
  if (!element.style) {
    return undefined; // not a styled element
  }
  if (window.getComputedStyle) {
    // DOM-Level-2-CSS
    // WebKit-based browsers (Safari included) return nothing if the element
    // is not a descendent of document ...
    return window.getComputedStyle(element, null);
  }
  if (element.currentStyle) {
    // non-standard IE alternative
    return element.currentStyle;
    // TODO: this won't really work in a general sense, as
    //   currentStyle is not identical to getComputedStyle()
    //   ... but it's good enough for 'visibility'
  }
  throw new Error('cannot determine effective stylesheet in this browser');
};


/**
 * Returns the text content of the current node, without markup and invisible
 * symbols. New lines are stripped and whitespace is collapsed,
 * such that each character would be visible.
 *
 * @param {Node} node The node from which we are getting content.
 * @return {string} The text content.
 */
domutil.getVisibleText = function(node) {
  var textContent;
  // NOTE(kjin): IE innerText is more like Firefox textContent -- visibility
  // is not concerned. Safari 3 and Chrome innerText is just the visible text.
  var buf = [];
  domutil.getVisibleText_(node, buf, true);
  textContent = buf.join('');

  textContent = textContent.replace(/\xAD/g, '');

  textContent = textContent.replace(/ +/g, ' ');
  if (textContent != ' ') {
    textContent = textContent.replace(/^\s*/, '');
  }

  return textContent;
};


/**
 * Returns the domutil.getVisibleText without trailing space, if any.
 *
 * @param {Node} node The node from which we are getting content.
 * @return {string} The text content.
 */
domutil.getVisibleTextTrim = function(node) {
  return domutil.getVisibleText(node).replace(/^[\s\xa0]+|[\s\xa0]+$/g, '');
};


/**
 * Recursive support function for text content retrieval.
 *
 * @param {Node} node The node from which we are getting content.
 * @param {Array} buf string buffer.
 * @param {boolean} normalizeWhitespace Whether to normalize whitespace.
 * @private
 */
domutil.getVisibleText_ = function(node, buf, normalizeWhitespace) {
  var TAGS_TO_IGNORE_ = {
    'SCRIPT': 1,
    'STYLE': 1,
    'HEAD': 1,
    'IFRAME': 1,
    'OBJECT': 1
  };
  var PREDEFINED_TAG_VALUES_ = {'IMG': ' ', 'BR': '\n'};

  if (node.nodeName in TAGS_TO_IGNORE_) {
    // ignore certain tags
  } else if (node.nodeType == 3) {
    if (normalizeWhitespace) {
      buf.push(String(node.nodeValue).replace(/(\r\n|\r|\n)/g, ''));
    } else {
      buf.push(node.nodeValue);
    }
  } else if (!domutil.isVisible(node)) {
    // ignore invisible node
    // this has to be after the check for NodeType.TEXT because text node
    // does not have style.
  } else if (node.nodeName in PREDEFINED_TAG_VALUES_) {
    buf.push(PREDEFINED_TAG_VALUES_[node.nodeName]);
  } else {
    var child = node.firstChild;
    while (child) {
      domutil.getVisibleText_(child, buf, normalizeWhitespace);
      child = child.nextSibling;
    }
  }
};


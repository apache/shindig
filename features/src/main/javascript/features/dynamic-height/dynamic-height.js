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

/**
 * @fileoverview This library augments gadgets.window with functionality
 * to change the height of a gadget dynamically.
 */

var gadgets = gadgets || {};

/**
 * @static
 * @class Provides operations for getting information about and modifying the
 *     window the gadget is placed in.
 * @name gadgets.window
 */
gadgets.window = gadgets.window || {};

// we wrap these in an anonymous function to avoid storing private data
// as members of gadgets.window.
(function() {

  var oldHeight;

  /**
   * Parse out the value (specified in px) for a CSS attribute of an element.
   *
   * @param {Object} elem the element with the attribute to look for.
   * @param {String} attr the CSS attribute name of interest.
   * @returns {int} the value of the px attr of the elem.
   */
  function parseIntFromElemPxAttribute(elem, attr) {
    var style = window.getComputedStyle(elem, "");
    var value = style.getPropertyValue(attr);
    value.match(/^([0-9]+)/);
    return parseInt(RegExp.$1, 10);
  }

  /**
   * For Webkit-based browsers, calculate the height of the gadget iframe by
   * iterating through all elements in the gadget, starting with the body tag.
   * It is not sufficient to only account body children elements, because
   * CSS style position "float" may place a child element outside of the
   * containing parent element. Not counting "float" elements may lead to
   * undercounting.
   *
   * @returns {int} the height of the gadget.
   */
  function getHeightForWebkit() {
    var result = 0;
    var queue = [ document.body ];

    while (queue.length > 0) {
      var elem = queue.shift();
      var children = elem.childNodes;

      for (var i = 0; i < children.length; i++) {
        var child = children[i];
        if (typeof child.offsetTop !== 'undefined' &&
            typeof child.scrollHeight !== 'undefined') {
          // scrollHeight already accounts for border-bottom, padding-bottom.
          var bottom = child.offsetTop + child.scrollHeight +
              parseIntFromElemPxAttribute(child, "margin-bottom");
          result = Math.max(result, bottom);
        }
        queue.push(child);
      }
    } 

    // Add border, padding and margin of the containing body.
    return result
        + parseIntFromElemPxAttribute(document.body, "border-bottom")
        + parseIntFromElemPxAttribute(document.body, "margin-bottom")
        + parseIntFromElemPxAttribute(document.body, "padding-bottom");
  }

  /**
   * Adjusts the gadget height
   * @param {Number} opt_height An optional preferred height in pixels. If not
   *     specified, will attempt to fit the gadget to its content.
   * @member gadgets.window
   */
  gadgets.window.adjustHeight = function(opt_height) {
    var newHeight = parseInt(opt_height, 10);
    var heightAutoCalculated = false;
    if (isNaN(newHeight)) {
      heightAutoCalculated = true;

      // Resize the gadget to fit its content.

      // Calculating inner content height is hard and different between
      // browsers rendering in Strict vs. Quirks mode.  We use a combination of
      // three properties within document.body and document.documentElement:
      // - scrollHeight
      // - offsetHeight
      // - clientHeight
      // These values differ significantly between browsers and rendering modes.
      // But there are patterns.  It just takes a lot of time and persistence
      // to figure out.

      // Get the height of the viewport
      var vh = gadgets.window.getViewportDimensions().height;
      var body = document.body;
      var docEl = document.documentElement;
      if (document.compatMode === 'CSS1Compat' && docEl.scrollHeight) {
        // In Strict mode:
        // The inner content height is contained in either:
        //    document.documentElement.scrollHeight
        //    document.documentElement.offsetHeight
        // Based on studying the values output by different browsers,
        // use the value that's NOT equal to the viewport height found above.
        newHeight = docEl.scrollHeight !== vh ?
                     docEl.scrollHeight : docEl.offsetHeight;
      } else if (navigator.userAgent.indexOf('AppleWebKit') >= 0) {
        // In Webkit:
        // Property scrollHeight and offsetHeight will only increase in value.
        // This will incorrectly calculate reduced height of a gadget
        // (ie: made smaller).
        newHeight = getHeightForWebkit();
      } else if (body && docEl) {
        // In Quirks mode:
        // documentElement.clientHeight is equal to documentElement.offsetHeight
        // except in IE.  In most browsers, document.documentElement can be used
        // to calculate the inner content height.
        // However, in other browsers (e.g. IE), document.body must be used
        // instead.  How do we know which one to use?
        // If document.documentElement.clientHeight does NOT equal
        // document.documentElement.offsetHeight, then use document.body.
        var sh = docEl.scrollHeight;
        var oh = docEl.offsetHeight;
        if (docEl.clientHeight !== oh) {
          sh = body.scrollHeight;
          oh = body.offsetHeight;
        }

        // Detect whether the inner content height is bigger or smaller
        // than the bounding box (viewport).  If bigger, take the larger
        // value.  If smaller, take the smaller value.
        if (sh > vh) {
          // Content is larger
          newHeight = sh > oh ? sh : oh;
        } else {
          // Content is smaller
          newHeight = sh < oh ? sh : oh;
        }
      }
    }

    // Only make the IFPC call if height has changed
    if (newHeight !== oldHeight &&
        !isNaN(newHeight) &&
        !(heightAutoCalculated && newHeight === 0)) {
      oldHeight = newHeight;
      gadgets.rpc.call(null, "resize_iframe", null, newHeight);
    }
  };
}());

// Alias for legacy code
var _IG_AdjustIFrameHeight = gadgets.window.adjustHeight;

// TODO Attach gadgets.window.adjustHeight to the onresize event


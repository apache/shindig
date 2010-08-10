/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

// TODO: Define a more convenient set of methods for iframe resizing in wave.

/**
 * @fileoverview This library augments gadgets.window with functionality
 * to change the width of a gadget dynamically. Derived from the
 * dynamic-height feature source code.
 * See:
 * http://svn.apache.org/repos/asf/shindig/trunk/features/src/main/javascript/features/dynamic-height/dynamic-height.js
 */

/**
 * @static
 * @class This namespace is used by the Gadgets API for the features it offers
 * in all containers, including Wave. Those are documented here:
 * http://code.google.com/apis/gadgets/docs/reference/
 * @name gadgets
 */

/**
 * @static
 * @class This namespace is defined by the Gadgets API, and documented here:
 * http://code.google.com/apis/gadgets/docs/reference/#gadgets.window <br>
 * The Wave Gadgets API adds an additional method on top of the set documented
 * there.
 * @name gadgets.window
 */
gadgets.window = gadgets.window || {};

// we wrap these in an anonymous function to avoid storing private data
// as members of gadgets.window.
(function() {

  var oldWidth;

  /**
   * Parse out the value (specified in px) for a CSS attribute of an element.
   *
   * @param {Element} elem the element with the attribute to look for.
   * @param {string} attr the CSS attribute name of interest.
   * @returns {number} the value of the px attr of the elem.
   * @private
   */
  function parseIntFromElemPxAttribute(elem, attr) {
    var style = window.getComputedStyle(elem, "");
    var value = style.getPropertyValue(attr);
    value.match(/^([0-9]+)/);
    return parseInt(RegExp.$1, 10);
  }

  /**
   * For Webkit-based browsers, calculate the width of the gadget iframe by
   * iterating through all elements in the gadget, starting with the body tag.
   * It is not sufficient to only account body children elements, because
   * CSS style position "float" may place a child element outside of the
   * containing parent element. Not counting "float" elements may lead to
   * undercounting.
   *
   * @returns {number} the width of the gadget.
   * @private
   */
  function getWidthForWebkit() {
    var result = 0;
    var queue = [ document.body ];

    while (queue.length > 0) {
      var elem = queue.shift();
      var children = elem.childNodes;

      for (var i = 0; i < children.length; i++) {
        var child = children[i];
        if (typeof child.offsetLeft !== 'undefined' &&
            typeof child.scrollWidth !== 'undefined') {
          // scrollHeight already accounts for border-bottom, padding-bottom.
          var right = child.offsetLeft + child.scrollWidth +
              parseIntFromElemPxAttribute(child, "margin-right");
          result = Math.max(result, right);
        }
        queue.push(child);
      }
    }

    // Add border, padding and margin of the containing body.
    return result
        + parseIntFromElemPxAttribute(document.body, "border-right")
        + parseIntFromElemPxAttribute(document.body, "margin-right")
        + parseIntFromElemPxAttribute(document.body, "padding-right");
  }

  /**
   * Adjusts the gadget width
   * @param {number=} opt_width An optional preferred width in pixels. If not
   *     specified, will attempt to fit the gadget to its content.
   * @member gadgets.window
   */
  gadgets.window.adjustWidth = function(opt_width) {
    var newWidth = parseInt(opt_width, 10);
    var widthAutoCalculated = false;
    if (isNaN(newWidth)) {
      widthAutoCalculated = true;

      // Resize the gadget to fit its content.

      // Get the width of the viewport
      var vw = gadgets.window.getViewportDimensions().width;
      var body = document.body;
      var docEl = document.documentElement;
      if (document.compatMode === 'CSS1Compat' && docEl.scrollWidth) {
        // In Strict mode:
        // The inner content height is contained in either:
        //    document.documentElement.scrollWidth
        //    document.documentElement.offsetWidth
        // Based on studying the values output by different browsers,
        // use the value that's NOT equal to the viewport width found above.
        newWidth = docEl.scrollWidth !== vw ?
                     docEl.scrollWidth : docEl.offsetWidth;
      } else if (navigator.userAgent.indexOf('AppleWebKit') >= 0) {
        // In Webkit:
        // Property scrollWidth and offsetWidth will only increase in value.
        // This will incorrectly calculate reduced width of a gadget
        // (ie: made smaller).
        newWidth = getWidthForWebkit();
      } else if (body && docEl) {
        // In Quirks mode:
        // documentElement.clientWidth is equal to documentElement.offsetWidth
        // except in IE.  In most browsers, document.documentElement can be used
        // to calculate the inner content width.
        // However, in other browsers (e.g. IE), document.body must be used
        // instead.  How do we know which one to use?
        // If document.documentElement.clientWidth does NOT equal
        // document.documentElement.offsetWidth, then use document.body.
        var sw = docEl.scrollWidth;
        var ow = docEl.offsetWidth;
        if (docEl.clientWidth !== ow) {
          sw = body.scrollWidth;
          ow = body.offsetWidth;
        }

        // Detect whether the inner content width is bigger or smaller
        // than the bounding box (viewport).  If bigger, take the larger
        // value.  If smaller, take the smaller value.
        if (sw > vw) {
          // Content is larger
          newWidth = sw > ow ? sw : ow;
        } else {
          // Content is smaller
          newWidth = sw < ow ? sw : ow;
        }
      }
    }

    // Only make the RPC call if width has changed
    if (newWidth !== oldWidth &&
        !isNaN(newWidth) &&
        !(widthAutoCalculated && newWidth === 0)) {
      oldWidth = newWidth;
      gadgets.rpc.call(null, "setIframeWidth", null, newWidth);
    }
  };
}());

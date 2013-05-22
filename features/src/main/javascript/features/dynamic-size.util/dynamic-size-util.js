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
 * @fileoverview This library augments gadgets.window with functionality to get
 *    the frame's viewport dimensions.
 */

gadgets.window = gadgets.window || {};

// we wrap these in an anonymous function to avoid storing private data
// as members of gadgets.window.
(function() {

  /**
   * @private
   */
  function getElementComputedStyle(elem, attr) {
    var n = navigator;
    var dua = n.userAgent,dav = n.appVersion;
    var isWebKit = parseFloat(dua.split("WebKit/")[1]) || undefined;
    var isIE = parseFloat(dav.split("MSIE ")[1]) || undefined;
    var gcs;
    if(isWebKit){
      /**
       * Get the computed style from the dom node, implementation of this function differs in browsers.
       * @private
       * @param {DomNode} node the dom node.
       * @return {Object} the style object.
       */
      gcs = function(node){
        var s;
        if(node.nodeType == DOM_ELEMENT_NODE){
          var dv = node.ownerDocument.defaultView;
          s = dv.getComputedStyle(node, null);
          if(!s && node.style){
            node.style.display = "";
            s = dv.getComputedStyle(node, null);
          }
        }
        return s || {};
      };
    } else if (isIE && !window.getComputedStyle) {
      gcs = function(node){
        // IE (as of 7) doesn't expose Element like sane browsers
        return node.nodeType == DOM_ELEMENT_NODE ? node.currentStyle : {};
      };
    } else {
      gcs = function(node){
        return node.nodeType == DOM_ELEMENT_NODE ?
          node.ownerDocument.defaultView.getComputedStyle(node, null) : {};
      };
    }

    var style = gcs(elem);
    return attr && style ? style[attr] : style;
  }

  /**
   * Parse out the value (specified in px) for a CSS attribute of an element.
   *
   * @param {Element} elem the element with the attribute to look for.
   * @param {string} attr the CSS attribute name of interest.
   * @return {number} the value of the px attr of the elem, undefined if the attr was undefined.
   * @private
   */
  function parseIntFromElemPxAttribute(elem, attr) {
    var value = getElementComputedStyle(elem, attr);
    if (value) {
      value.match(/^([0-9]+)/);
      return parseInt(RegExp.$1, 10);
    }
  }

  /**
   * Get the height (truthy) or width (falsey)
   */
  gadgets.window.getDimen = function (height) {
    var result = 0;
    var queue = [document.body];

    while (queue.length > 0) {
      var elem = queue.shift();
      var children = elem.childNodes;

      /*
       * Here, we are checking if we are a container that clips its overflow with
       * a specific height, because if so, we should ignore children
       */

      // check that elem is actually an element, could be a text node otherwise
      if (typeof elem.style !== 'undefined' && elem !== document.body) {
        // Get the overflowY value, looking in the computed style if necessary
        var overflow = elem.style[height ? 'overflowY' : 'overflowX'];
        if (!overflow) {
          overflow = getElementComputedStyle(elem, height ? 'overflowY' : 'overflowX');
        }

        // The only non-clipping values of overflow is 'visible'. We assume that 'inherit'
        // is also non-clipping at the moment, but should we check this?
        if (overflow != 'visible' && overflow != 'inherit') {
          // Make sure this element explicitly specifies a height
          var size = elem.style[height ? 'height' : 'width'];
          if (!size) {
            size = getElementComputedStyle(elem, height ? 'height' : 'width');
          }
          if (size && size.length > 0 && size != 'auto') {
            // We can safely ignore the children of this element,
            // so move onto the next in the queue
            continue;
          }
        }
      }

      for (var i = 0; i < children.length; i++) {
        var child = children[i];
        if (typeof child.style != 'undefined') {  // Don't measure text nodes
          var start = child.offsetTop,
              dimenEnd = 'marginBottom',
              size = child.offsetHeight,
              dir = getElementComputedStyle(child, 'direction');

          if (!height) {
            start = child.offsetLeft;
            dimenEnd = 'marginRight';
            size = child.offsetWidth;

            // compute offsetRight
            if (dir == 'rtl' && typeof start != 'undefined' && typeof size != 'undefined' && child.offsetParent) {
              start = child.offsetParent.offsetWidth - start - size;
            }
          }

          if (typeof start != 'undefined' && typeof size != 'undefined') {
            // offsetHeight already accounts for borderBottom, paddingBottom.
            var end = start + size + (parseIntFromElemPxAttribute(child, dimenEnd) || 0);
            result = Math.max(result, end);
          }
        }
        queue.push(child);
      }
    }

    // Add border, padding and margin of the containing body.
    return result +
        (parseIntFromElemPxAttribute(document.body, height ? 'borderBottom' : 'borderRight') || 0) +
        (parseIntFromElemPxAttribute(document.body, height ? 'marginBottom' : 'marginRight') || 0) +
        (parseIntFromElemPxAttribute(document.body, height ? 'paddingBottom' : 'paddingRight') || 0);
  };

  /**
   * Detects the inner dimensions of a frame. See:
   * http://www.quirksmode.org/viewport/compatibility.html for more information.
   *
   * @return {Object} An object with width and height properties.
   * @member gadgets.window
   */
  gadgets.window.getViewportDimensions = function() {
    var x = 0;
    var y = 0;
    if (self.innerHeight) {
      // all except Explorer
      x = self.innerWidth;
      y = self.innerHeight;
    } else if (document.documentElement && document.documentElement.clientHeight) {
      // Explorer 6 Strict Mode
      x = document.documentElement.clientWidth;
      y = document.documentElement.clientHeight;
    } else if (document.body) {
      // other Explorers
      x = document.body.clientWidth;
      y = document.body.clientHeight;
    }
    return {
      width : x,
      height : y
    };
  };
})();

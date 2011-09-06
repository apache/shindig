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
 * to change the width of a gadget dynamically.
 */

/**
 * @static
 * @class Provides operations for getting information about the window the gadget is placed in.
 * @name gadgets.window
 */
gadgets.window = gadgets.window || {};

(function() {

  /**
   * Calculate inner content width is hard and different between browsers rendering in Strict vs.
   * Quirks mode. We use a combination of three properties within document.body and
   * document.documentElement: - scrollWidth - offsetWidth - clientWidth These values differ
   * significantly between browsers and rendering modes. But there are patterns. It just takes a lot
   * of time and persistence to figure out.
   * 
   * @return The width of the content within the iframe
   */
  gadgets.window.getWidth = function() {
    var viewportWidth = gadgets.window.getViewportDimensions().width;
    var body = document.body;
    var docEl = document.documentElement;
    if (document.compatMode === 'CSS1Compat' && docEl && docEl.scrollWidth) {
      // In Strict mode:
      // The inner content width is contained in either:
      // document.documentElement.scrollWidth
      // document.documentElement.offsetWidth
      // Based on studying the values output by different browsers,
      // use the value that's NOT equal to the viewport width found above.
      return docEl.scrollWidth !== viewportWidth ? docEl.scrollWidth : docEl.offsetWidth;
    } else if (body) {
      var scrollWidth = body.scrollWidth;
      var offsetWidth = body.offsetWidth;

      // Detect whether the inner content width is bigger or smaller
      // than the bounding box (viewport). If bigger, take the larger
      // value. If smaller, take the smaller value.
      if ((scrollWidth > viewportWidth) && (scrollWidth > offsetWidth)) {
        return scrollWidth;
      }
      return offsetWidth;
    }
  };

}());

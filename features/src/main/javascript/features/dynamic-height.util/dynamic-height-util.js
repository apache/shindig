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
 * to get the frame's viewport dimensions.
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
  /**
   * Detects the inner dimensions of a frame.
   * See: http://www.quirksmode.org/viewport/compatibility.html for more
   * information.
   * @returns {Object} An object with width and height properties.
   * @member gadgets.window
   */
  gadgets.window.getViewportDimensions = function() {
    var x,y;
    if (self.innerHeight) {
      // all except Explorer
      x = self.innerWidth;
      y = self.innerHeight;
    } else if (document.documentElement &&
               document.documentElement.clientHeight) {
      // Explorer 6 Strict Mode
      x = document.documentElement.clientWidth;
      y = document.documentElement.clientHeight;
    } else if (document.body) {
      // other Explorers
      x = document.body.clientWidth;
      y = document.body.clientHeight;
    } else {
      x = 0;
      y = 0;
    }
    return {width: x, height: y};
  };
})();

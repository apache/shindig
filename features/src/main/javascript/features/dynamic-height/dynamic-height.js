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
   * Adjusts the gadget height
   * @param {number=} opt_height An optional preferred height in pixels. If not
   *     specified, will attempt to fit the gadget to its content.
   * @member gadgets.window
   */
  gadgets.window.adjustHeight = function(opt_height) {
    var newHeight = parseInt(opt_height, 10);
    var heightAutoCalculated = false;
    if (isNaN(newHeight)) {
      heightAutoCalculated = true;
      newHeight = gadgets.window.getHeight();
    }

    // Only make the IFPC call if height has changed
    if (newHeight !== oldHeight &&
        !isNaN(newHeight) &&
        !(heightAutoCalculated && newHeight === 0)) {
      oldHeight = newHeight;
      gadgets.rpc.call(null, 'resize_iframe', null, newHeight);
    }
  };
}());

/**
 * @see gadgets.window#adjustHeight
 */
var _IG_AdjustIFrameHeight = gadgets.window.adjustHeight;

// TODO Attach gadgets.window.adjustHeight to the onresize event

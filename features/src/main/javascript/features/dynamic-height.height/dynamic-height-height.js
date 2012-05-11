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
 * @class Provides operations for getting information about the window the
 *        gadget is placed in.
 * @name gadgets.window
 */
gadgets.window = gadgets.window || {};

(function() {

  /**
   * Adjusts the gadget height
   * @param {number=} opt_height An optional preferred height in pixels. If not
   *     specified, will attempt to fit the gadget to its content.
   * @member gadgets.window
   */

  /**
   * Calculate inner content height is hard and different between
   * browsers rendering in Strict vs. Quirks mode.  We use a combination of
   * three properties within document.body and document.documentElement:
   * - scrollHeight
   * - offsetHeight
   * - clientHeight
   * These values differ significantly between browsers and rendering modes.
   * But there are patterns.  It just takes a lot of time and persistence
   * to figure out.
   */
  gadgets.window.getHeight = function() {
    return gadgets.window.getDimen(1);
  };
}());

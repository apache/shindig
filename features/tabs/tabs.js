/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview Tabs library for gadgets.
 */

var gadgets = gadgets || {};

/**
 * Tabs class.
 * @param {string} opt_moduleId Optional suffix for the ID of tab container.
 * @param {string} opt_defaultTab Optional tab name that specifies the name of
 *                   of the tab that is selected after initialization.
 *                   If this parameter is omitted, the first tab is selected by
 *                   default.
 * @param {Element} opt_container The HTML element to contain the tabs.  If
 *                    omitted, a new div element is created and inserted at the
 *                    very top.
 * @constructor
 */
gadgets.Tabs = function(opt_moduleId, opt_defaultTab, opt_container) {
  // TODO
};

/**
 * Adds a new tab based on the name-value pairs specified in opt_params.
 * @param {string} tabName Label of the tab to create.
 * @param {Object} opt_params Optional parameter object. The following
 *                   properties are supported:
 *                   .contentContainer An existing HTML element to be used as
 *                     the tab content container. If omitted, the tabs
 *                     library creates one.
 *                   .callback A callback function to be executed when the tab
 *                     is selected.
 *                   .tooltip A tooltip description that pops up when user moves
 *                     the mouse cursor over the tab.
 *                   .index The index at which to insert the tab. If omitted,
 *                     the new tab is appended to the end.
 * @return {string} DOM id of the tab container.
 */
gadgets.Tabs.prototype.addTab = function(tabName, opt_params) {
  // TODO
};

/**
 * Removes a tab at tabIndex and all of its associated content.
 * @param {number} tabIndex Index of the tab to remove.
 */
gadgets.Tabs.prototype.removeTab = function(tabIndex) {
  // TODO
};

/**
 * Returns the currently selected tab object.
 * @return {Object} The currently selected tab object.
 */
gadgets.Tabs.prototype.getSelectedTab = function() {
  // TODO
};

/**
 * Selects the tab at tabIndex and fires the tab's callback function if it
 * exists. If the tab is already selected, the callback is not fired.
 * @param {number} tabIndex Index of the tab to select.
 */
gadgets.Tabs.prototype.setSelectedTab = function(tabIndex) {
  // TODO
};

/**
 * Swaps the positions of tabs at tabIndex1 and tabIndex2. The selected tab
 * does not change, and no callback functions are called.
 * @param {number} tabIndex1 Index of the first tab to swap.
 * @param {number} tabIndex2 Index of the secnod tab to swap.
 */
gadgets.Tabs.prototype.swapTabs = function(tabIndex1, tabIndex2) {
  // TODO
};


/**
 * Returns an array of all existing tab objects.
 * @return {Array.<Object>} Array of all existing tab objects.
 */
gadgets.Tabs.prototype.getTabs = function() {
  // TODO
};

/**
 * Sets the alignment of tabs. Tabs are center-aligned by default.
 * @param {string} align 'left', 'center', or 'right'.
 * @param {number} opt_offset Optional parameter to set the number of pixels
 *                   to offset tabs from the left or right edge. The default
 *                   value is 3px.
 */
gadgets.Tabs.prototype.alignTabs = function(align, opt_offset) {
  // TODO
};

/**
 * Shows or hides tabs and all associated content.
 * @param {boolean} display true to show tabs; false to hide tabs.
 */
gadgets.Tabs.prototype.displayTabs = function(display) {
  // TODO
};

/**
 * Returns the tab headers container element.
 * @return {Object} The tab headers container element.
 */
gadgets.Tabs.prototype.getHeaderContainer = function() {
  // TODO
};

// Aliases for legacy code

var _IG_Tabs = gadgets.Tabs;
_IG_Tabs.prototype.moveTabs = _IG_Tabs.prototype.swapTabs;
_IG_Tabs.prototype.addDynamicTab = function(tabName, callback) {
  return this.addTab(tabName, {callback: callback});
};


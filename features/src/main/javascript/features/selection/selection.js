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
 * @fileoverview Gadget-side library for participating in selection eventing.
 */

/**
 * @static
 * @class Selection class for gadgets.
 * Provides framework for selection eventing.
 * @name gadgets.selection
 */
gadgets['selection'] = function() {
  var listeners,
      currentSelection;

  gadgets.util.registerOnLoadHandler(function() {
    gadgets.rpc.register('gadgets.selection.selectionChanged', function(selection) {
      currentSelection = selection;
      for (var i=0, currentListener; currentListener=listeners[i]; i++) {
        listeners[i](selection);
      }
    });
  });

  return /** @scope gadgets.selection */ {
    /**
     * Sets the current selection.
     * @param {string} selection Selected object.
     */
    setSelection: function(selection) {
      currentSelection = selection;
      gadgets.rpc.call('..', 'gadgets.selection.set', null, selection);
    },

    /**
     * Gets the current selection.
     * @return {Object} the current selection.
     */
    getSelection: function() {
      return currentSelection;
    },

    /**
     * Registers a listener for selection.
     * @param {function} listener The listener to remove.
     */
    addListener: function(listener) {
      if (!listeners) {
        listeners  = [];
        gadgets.rpc.call('..', 'gadgets.selection.register');
      }
      if (typeof listener === 'function') {
        listeners.push(listener); // add the listener to the list
      }
    },

    /**
     * Removes a listener for selection.
     * @param {function} listener The listener to remove.
     */
    removeListener: function(listener) {
      for (var i = 0, currentListener; currentListener=listeners[i]; i++) {
        if (currentListener === listener) {
          listeners.splice(i, 1);
          break;
        }
      }
    }
  };
}();

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
 * @fileoverview Container-side selection manager.
 */

/**
 * @static
 * @class Manages selection and selection listeners.
 * @name gadgets.selectionmanager
 */
(function() {

  var listeners = new Array();
  var _selection;

  function notifySelection(selection) {
    _selection = selection;
    for(var i=0, currentListener; currentListener=listeners[i]; i++) {
      listeners[i](selection);
    }
  }

  function addSelectionListener(listener) {
    if (typeof listener === 'function') {
      listeners.push(listener);
    }
  }

  function removeSelectionListener(listener) {
    for (var i = 0, currentListener; currentListener=listeners[i]; i++) {
      if (currentListener === listener) {
        listeners.splice(i, 1);
        break;
      }
    }
  }

  function router(command, param) {
    switch (command) {
    case 'set':
      notifySelection(param);
      break;
    case 'add':
      addSelectionListener(param);
      break;
    default:
      throw new Error('Unknown selection command');
    }
  }

  osapi.container.Container.addMixin('selection', function(context) {
    gadgets.rpc.register('gadgets.selection', router);
    return /** @scope gadgets.selection */ {
      /**
       * Sets the current selection.
       * @param {string} selection Selected object.
       */
      setSelection: function(selection) {
        notifySelection(selection);
      },
      /**
       * Gets the current selection.
       * @return {Object} the current selection.
       */
      getSelection: function() {
        return _selection;
      },

      /**
       * Registers a listener for selection.
       * @param {function} listener The listener to remove.
       */
      addListener: function(listener) {
        addSelectionListener(listener);
      },

      /**
       * Removes a listener for selection.
       * @param {function} listener The listener to remove.
       */
      removeListener: function(listener) {
        removeSelectionListener(listener);
      }
    };
  });

})();

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

  var _selection,
      listeners = [],
      listeningGadgets = {};

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

  osapi.container.Container.addMixin('selection', function(context) {

    function notifySelection(selection) {
      _selection = selection;
      for(var i=0, currentListener; currentListener=listeners[i]; i++) {
        listeners[i](selection);
      }

      // Call rpc endpoint in all gadgets that have registered
      for (var to in listeningGadgets) {
        if (!context.getGadgetSiteByIframeId_(to)) {
          delete listeningGadgets[to];  // Remove sites that are no longer with us
        }
        else {
          gadgets.rpc.call(to, 'gadgets.selection.selectionChanged', null, selection);
        }
      }
    }

    context.rpcRegister('gadgets.selection.set', function(rpcArgs, selection) {
      notifySelection(selection);
    });

    context.rpcRegister('gadgets.selection.register', function(rpcArgs) {
      listeningGadgets[rpcArgs.f] = 1;
      return _selection;
    });

    return /** @scope gadgets.selection */ {
      /**
       * Sets the current selection.
       * @param {string} selection Selected object.
       */
      setSelection: notifySelection,

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
      addListener: addSelectionListener,

      /**
       * Removes a listener for selection.
       * @param {function} listener The listener to remove.
       */
      removeListener: removeSelectionListener
    };
  });

})();

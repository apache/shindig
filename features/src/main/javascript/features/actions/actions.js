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
 * @fileoverview Provides facilities for registering action callback
 *               functions to actions that may be rendered anywhere in
 *               the container.  Available to every gadget.
 */
gadgets['actions'] = (function() {

  /**
   * Runs the callback function associated with the specified action id.
   *
   * Example:
   *
   * <pre>
   * gadgets.actions.runAction(action);
   * </pre>
   *
   * @param {Object}
   *          action The action object.
   *
   * @member gadgets.actions
   */
  function runAction(actionData) {
    var actionId = actionData.actionId;
    // optional
    var selectionObj = actionData.selectionObj;

    var callback = callbackRegistry.getCallback(actionId);
    if (callback) {
      var args = selectionObj ? [selectionObj] : null;
      callback.apply(this, args);
    }
  };

  /**
   * @constructor Object that maps action ids to callback functions.
   */
  function ActionCallbackRegistry() {
    this.registryById = {};
    this.addAction = function(actionId, callbackFn) {
      this.registryById[actionId] = callbackFn;
    };
    this.removeAction = function(actionId) {
      delete this.registryById[actionId];
    };
    this.getCallback = function(actionId) {
      return this.registryById[actionId];
    };
  };

  // router function called to run actions
  function router(channel, object) {
    var actionData = object;
    if (channel == 'runAction') {
      runAction(actionData);
    }
  };

  // create the callback registry and
  // initialize the rpc router
  var callbackRegistry = new ActionCallbackRegistry();
  var _init;
  var init = function() {
    if (!_init) {
      gadgets.rpc.register('actions', router);
      _init = true;
    }
  };

  return /** @scope gadgets.actions */ {
    /**
     * Registers an action with the actions feature.
     *
     * Example:
     *
     * <pre>
     * gadgets.actions.addAction(actionObj);
     * </pre>
     *
     * @param {function(Object)}
     *          actionObj The action object.
     *
     * @member gadgets.actions
     */
    addAction: function(actionObj) {
      init();
      var actionId = actionObj.id;
      var actionCallback = actionObj.callback;
      callbackRegistry.addAction(actionId, actionCallback);

      // notify the container that an action has been added.
      gadgets.rpc.call('..', 'actions', null, 'bindAction', actionObj);
    },

    /**
     * Updates an action that has already been registered.
     *
     * Example:
     *
     * <pre>
     * gadgets.actions.updateAction(actionObj);
     * </pre>
     *
     * @param {function(Object)}
     *          actionObj The action object.
     *
     * @member gadgets.actions
     */
    updateAction: function(actionObj) {
      // TODO for now we only support updating the callback
      // to support the declaratively contributed actions,
      // we need to support updating the label as well.
      init();
      var actionId = actionObj.id;
      var actionCallback = actionObj.callback;
      callbackRegistry.addAction(actionId, actionCallback);

      // notify the container that an action has been added.
      gadgets.rpc.call('..', 'actions', null, 'bindAction', actionObj);
    },

    /**
     * Executes the action callback associated with the specified actionId
     * in the context of the gadget which contributed that action. The
     * gadget should call this method whenever an action is triggered by
     * the user.
     *
     * @param {String, Object}
     *          actionId The id of the action to execute.
     *          opt_selection The current selection. This is optional.
     *
     * @member gadgets.actions
     */
    runAction: function(actionId, opt_selection) {
      actionData = {"id" : actionId, "selection" : opt_selection};
      gadgets.rpc.call('..', 'actions', null, 'runAction', actionData);
    },

    /**
     * Removes the association of a callback function with an action id.
     *
     * Example:
     *
     * <pre>
     * gadgets.actions.removeAction(actionId);
     * </pre>
     *
     * @param {string}
     *          actionId The action identifier.
     *
     * @member gadgets.actions
     */
    removeAction: function(actionId) {
      init();
      callbackRegistry.removeAction(actionId);

      // notify the container to remove action from its UI
      gadgets.rpc.call('..', 'actions', null, 'removeAction', actionId);
    },

    /**
     * Gets array of actions at the specified path and passes the result
     * to the callback function.
     *
     * Example:
     *
     * <pre>
     * var callback = function(actions){
     *  ...
     * }
     * gadgets.actions.getActionsByPath("container/navigationLinks", callback);
     * </pre>
     *
     * @param {string}
     *          path The path to the actions.
     * @param {function}
     *          callback A callback function to handle the returned actions
     *          array.
     *
     * @member gadgets.actions
     */
    getActionsByPath: function(path, callback) {
      gadgets.rpc.call('..', 'actions', callback, 'getActionsByPath', path);
    },

    /**
     * Gets array of actions for the specified data type and passes the result
     * to the callback function.
     *
     * Example:
     *
     * <pre>
     * var callback = function(actions){
     *  ...
     * }
     * gadgets.actions.getActionsByDataType("opensocial.Person", callback);
     * </pre>
     *
     * @param {string}
     *          dataType The String representation of an OpenSocial data type.
     * @param {function}
     *          callback A callback function to handle the returned actions
     *          array.
     *
     * @member gadgets.actions
     */
    getActionsByDataType: function(dataType, callback) {
      gadgets.rpc.call('..', 'actions', callback, 'getActionsByDataType',
          dataType);
    },

    /**
     * Registers a function to display actions in the gadget.
     *
     * @param {function}
     *          The gadget's function to render actions
     *          in its UI. The function takes the action object as
     *          a parameter.
     */
    registerShowActionsListener: function(listener) {
      if (typeof listener === 'function') {
        gadgets.rpc.call('..', 'actions', null, 'addShowActionListener', listener);
      }
    },

    /**
     * Registers a function to hide (remove) actions in the gadget
     *
     * @param {function}
     *          The gadget's function to hide (remove) actions
     *          in its UI. The function takes the action object as
     *          a parameter.
     */
    registerHideActionsListener: function(listener) {
      if (typeof listener === 'function') {
	gadgets.rpc.call('..', 'actions', null, 'addHideActionListener', listener);
      }
    }
  };
})();

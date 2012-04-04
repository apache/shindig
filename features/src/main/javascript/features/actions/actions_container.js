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
 * @fileoverview Provides facilities for contributing actions to various parts
 *               of the UI. Available to the common container.
 */
(function() {

  /**
   * @constructor Object that tracks the actions currently registered with the
   *              container.
   */
  function ActionRegistry() {

    // maps action ids to action objects
    this.registryById = {};

    // maps actions by contribution path
    this.registryByPath = {};

    // maps actions to OS data types
    this.registryByDataType = {};

    // maps actions to URL of the contributor
    this.registryByUrl = {};

    // one-to-many association of urls to gadget sites
    this.urlToSite = {};

    // one-to-one relationship of each action to the url
    this.actionToUrl = {};

    /**
     * Adds an action object to the registry
     *
     * @param {Object}
     *          actionObj JSON object that represents an action.
     * @param {String}
     *          url gadget spec URL, from which the action contribution
     *          originated.
     */
    this.addAction = function(actionObj, url) {
      var id = actionObj.id;
      if (!id) { /* invalid object */
        return;
      }

      var path = actionObj.path;
      if (path) {
        /**
         * We maintain a tree of arrays for actions that are contributed
         * to paths.  This is necessary to realize actions in hierarchical
         * menus, sub-menus and drop-down toolbar buttons.
         */
        var partsOfPath = path.split('/');
        var parent = this.registryByPath;
        for (var i = 0; i < partsOfPath.length; i++) {
          var currentNode = partsOfPath[i];
          if (!parent[currentNode]) {
            parent[currentNode] = {};
          }
          parent = parent[currentNode];
        }
        // store actions as array under attribute "@actions"
        var actionsAtPath = parent['@actions'];
        if (!actionsAtPath) {
          parent['@actions'] = [actionObj];
        } else {
          parent['@actions'] = actionsAtPath.concat(actionObj);
        }
      } else if (actionObj.dataType) {
        /**
         * We maintain a simple map for actions that are bound to an
         * OpenSocial data object type such as the person object.
         */
        var dataType = actionObj.dataType;
        this.registryByDataType[dataType] =
          this.registryByDataType[dataType] ?
              this.registryByDataType[dataType].concat(actionObj) :
                [actionObj];
      } else {
        // invalid object, no valid path or dataType to bind action
        return;
      }

      // add action to id registry
      this.registryById[id] = actionObj;

      // map actions to url, used by runAction to render gadget
      if (url) {
        this.actionToUrl[actionObj.id] = url;
        this.registryByUrl[url] =
          this.registryByUrl[url] ?
              this.registryByUrl[url].concat(actionObj) :
                [actionObj];
      }
    };

    /**
     * Removes an action object from the registry
     *
     * @param {String}
     *          actionId unique identifier for the action, as specified in the
     *          action object.
     */
    this.removeAction = function(actionId) {
      var actionObj = this.registryById[actionId];

      // remove from registryById
      delete this.registryById[actionId];

      // remove from the other registries
      var path = actionObj.path;
      if (path) { // remove from registryByPath
          var actionsAtPath = this.getActionsByPath(path);
          var i = actionsAtPath.indexOf(actionObj);
          if (i != -1) {
            actionsAtPath.splice(i, 1);
          }
      } else { // remove from registryByDataType
        var dataType = actionObj.dataType;
        var actionsForDataType = this.registryByDataType[dataType];
        var actionIndex = actionsForDataType.indexOf(actionObj);
        actionsForDataType.splice(actionIndex, 1);
        if (actionsForDataType.length == 0) {
          delete this.registryByDataType[dataType];
        }
      }

      // remove from url mappings
      var url = this.actionToUrl[actionId];
      if (url) {
        delete this.actionToUrl[actionId];
        var actionsForUrl = this.registryByUrl[url];
        var actionIndex = actionsForUrl.indexOf(actionObj);
        actionsForUrl.splice(actionIndex, 1);
        if (actionsForUrl.length == 0) {
          delete this.registryByUrl[url];
        }
      }
    };

    /**
     * Returns the action associated with the specified id
     *
     * @param {String}
     *          id Unique identifier for the action object.
     */
    this.getItemById = function(id) {
      var children = this.registryById ? this.registryById : {};
      return children[id];
    };

    /**
     * Returns all actions in the registry
     */
    this.getAllActions = function() {
      var actions = [];
      for (actionId in this.registryById) {
        if(this.registryById.hasOwnProperty(actionId)) {
          actions = actions.concat(this.registryById[actionId]);
        }
      }
      return actions;
    };

    /**
     * Returns all items associated with the given path
     *
     * @param {String}
     *          path Navigation path to the action, as specified in the action
     *          object.
     */
    this.getActionsByPath = function(path) {
      var actions = [];
      var partsOfPath = path.split('/');
      var children = this.registryByPath ? this.registryByPath : {};
      for (var i = 0; i < partsOfPath.length; i++) {
        var currentNode = partsOfPath[i];
        if (children[currentNode]) {
          children = children[currentNode];
        } else {
          // if path doesn't exist, return empty array
          return actions;
        }
      }
      if (children) {
        actions = children['@actions'];
      }
      return actions;
    };

    /**
     * Returns the actions associated with the specified data object
     *
     * @param {String}
     *          dataType The Open Social data type associated with the action.
     */
    this.getActionsByDataType = function(dataType) {
      var actions = [];
      if (this.registryByDataType[dataType]) {
        actions = this.registryByDataType[dataType];
      }
      return actions;
    };

    /**
     * Returns the actions associated with the specified url
     *
     * @param {String}
     *          url The gadget spec url associated with the action(s).
     */
    this.getActionsByUrl = function(url) {
      var children = [];
      if (this.registryByUrl[url]) {
        children = children.concat(this.registryByUrl[url]);
      }
      return children;
    };

    /**
     * Adds a new active gadget site to the registry
     *
     * @param {String}
     *          url The gadget spec url associated with the gadget site.
     * @param {osapi.container.GadgetSite}
     *          site The instance of the gadget site.
     */
    this.addGadgetSite = function(url, site) {
      var existingSites = this.urlToSite[url];
      if (existingSites) {
        this.urlToSite[url] = existingSites.concat(site);
      } else {
        this.urlToSite[url] = [site];
      }
    };

    /**
     * Removes a gadget site from the registry
     *
     * @param {String}
     *          siteId The unique identifier for the gadget site instance.
     */
    this.removeGadgetSite = function(siteId) {
      for (var url in this.urlToSite) {
        if(this.urlToSite.hasOwnProperty(url)) {
          var sites = this.urlToSite[url];
          if(!sites) {
           continue;
          }
          for (var i = 0; i < sites.length; i++) {
            var site = sites[i];
            if (site && site.getId() == siteId) {
              sites.splice(i, 1);
              if (sites.length == 0) {
                delete this.urlToSite[url];
              }
            }
          }
        }
      }
    };

    /**
     * Return the gadget sites associated with the specified action object.
     *
     * @param {Object}
     *          actionId The id of the action.
     * @return {Array} Always an array of the gadget site instances associated
     *         with the action object.
     */
    this.getGadgetSites = function(actionId) {
      var action = this.getItemById(actionId);
      var url = this.actionToUrl[actionId];
      var sites = [];
      var candidates = this.urlToSite[url];

      if (candidates) {
        // Return subset of matching sites (gadget view matches declared action view,
        // if the action declared a view) Do not modify existing array.
        for (var i = 0; i < candidates.length; i++) {
          var site = candidates[i];
          var holder = site.getActiveSiteHolder();
          if (!action.view || (holder && holder.getView() === action.view)) {
            sites.push(site);
          }
        }
      }

      return sites;
    };

    /**
     * Returns the url associated with an action
     *
     * @param {String}
     *          actionId The id of the action.
     * @return {String} url Gadget spec url associated with the action.
     */
    this.getUrl = function(actionId) {
      return this.actionToUrl[actionId];
    };
  };

  /**
   * Utility function for converting a string representation of XML to a DOM
   * object
   *
   * @param {String}
   *          xmlString String representation of a valid XML object.
   * @return {Object} response JSON object whose "data" field will contain the
   *          DOM object, otherwise, the "errors" field will contain a string
   *          description.
   */
  function createDom(xmlString) {
    var response = {};
    var dom;
    if (typeof ActiveXObject != 'undefined') {
      dom = new ActiveXObject('Microsoft.XMLDOM');
      dom.async = false;
      dom.validateOnParse = false;
      dom.resolveExternals = false;
      if (!dom.loadXML(xmlString)) {
        response.errors = "500 Failed to parse XML";
        response.rc = 500;
      } else {
        response['data'] = dom;
      }
    } else {
      var parser = new DOMParser();
      dom = parser.parseFromString(xmlString, 'application/xml');
      if ('parsererror' === dom.documentElement.nodeName) {
        response.errors = "500 Failed to parse XML";
        response.rc = 500;
      } else {
        response['data'] = dom;
      }
    }
    return response;
  }

  /**
   * Container handling of an action that has been programmatically added via
   * gadgets.actions.addAction() API
   *
   * @param {Object}
   *          actionObj The action object coming from the gadget side.
   *
   */
  function bindAction(actionObj) {
    var actionId = actionObj.id;
    var containerActionObj = registry.getItemById(actionId);
    // if action is not in registry, then this is a programmatic add
    if (!containerActionObj) {
      addAction(actionObj);
    } else {
      // check if this action needs to be run
      var pendingAction = pendingActions[actionId];
      if (pendingAction) {
        runAction(actionId, pendingAction.selection);
        delete pendingActions[actionId];
      }
    }
  }

  /**
   * Adds the action to the action registry, and renders the action in the
   * container UI.
   *
   * @param {Object}
   *          actionObj The action object with id, label, title, icon, and any
   *          other information needed to render the action in the container's
   *          UI.
   * @param {String}
   *          url Optional value needed to be passed in when adding action via
   *          preload listener (for subsequent loading of the gadget).
   *
   */
  function addAction(actionObj, url) {
    registry.addAction(actionObj, url);

    // Comply with spec by passing an array of the object
    // TODO: Update spec, since there will never be more than 1 element in the array
    showActionHandler([actionObj]);  // notify the container to display the action

    for (var to in showActionSiteIds) {
      if(showActionSiteIds.hasOwnProperty(to)) {
        if (!container_.getGadgetSiteByIframeId_(to)) {
          delete showActionSiteIds[to];
        }
        else {
          // Comply with spec by passing an array of the object
          // TODO: Update spec, since there will never be more than 1 element in the array
          gadgets.rpc.call(to, 'actions.onActionShow', null, [actionObj]);
        }
      }
    }
  }

  /**
   * Removes the action from the action registry, and removes the action from
   * the container UI.
   *
   * @param {String}
   *          The action id.
   *
   */
  function removeAction(id) {
    var actionObj = registry.getItemById(id);
    registry.removeAction(id);

    // Comply with spec by passing an array of the object
    // TODO: Update spec, since there will never be more than 1 element in the array
    hideActionHandler([actionObj]);  // notify the container to hide the action

    for (var to in hideActionSiteIds) {
      if (hideActionSiteIds.hasOwnProperty(to)) {
        if (!container_.getGadgetSiteByIframeId_(to)) {
          delete hideActionSiteIds[to];
        }
        else {
          // Comply with spec by passing an array of the object
          // TODO: Update spec, since there will never be more than 1 element in the array
          gadgets.rpc.call(to, 'actions.onActionHide', null, [actionObj]);
        }
      }
    }
  }

  /**
   * A map of all listeners.
   *
   * @type {Object.<string, Array.<function(string, Array.<Object>)>>}
   */
  var actionListenerMap = {};

  /**
   * A list of listeners to be notified when any action is invoked.
   *
   * @type {Array.<function(string, Array.<Object>)>}
   */
  var actionListeners = [];

  /**
   * Runs the action associated with the specified actionId. If the gadget has
   * not yet been rendered, renders the gadget first, then runs the action.
   *
   * @param {string}
   *         id The unique identifier for the action.
   * @param {?Array.<Object>=}
   *         selection The selection to pass to the action.
   *
   */
  function runAction(id, selection) {
    if (!selection && container_ && container_.selection) {
      selection = container_.selection.getSelection();
    }

    // call all container listeners, if any, for this actionId
    var listenersArray = actionListenerMap[id];
    var i;
    if (listenersArray) {
      for (i = 0; i < listenersArray.length; i++) {
        var listener = listenersArray[i];
        listener.call(null, id, selection);
      }
    }
    for (i = 0;  i < actionListeners.length; i++) {
      var listener = actionListeners[i];
      listener.call(null, id, selection);
    }

    // make rpc call to get gadgets to run callback based on action id
    var gadgetSites = registry.getGadgetSites(id);
    if (gadgetSites) {
      for (i = 0; i < gadgetSites.length; i++) {
        var site = gadgetSites[i];
        var holder = site.getActiveSiteHolder();
        if (holder) {
          gadgets.rpc.call(holder.getIframeId(), 'actions.runAction', null, id, selection);
        }
      }
    }
  }

  /**
   * Fix list of actions from actions contributions to check if it has been wrapped with <actions>
   * tag to avoid DOM parser error.
   *
   * @param {string} actionsContributionsParam the string containing the action tags
   * @return {string} the corrected actions list wrapped with <actions> tag to avoid DOM parser error.
   */
  function fixActionContributions(actionsContributionsParam) {
    var actions = actionsContributionsParam;
    if(typeof actions !== 'string') {
      actions = actions.toString();
    }

    // cleanup the newlines and extra spaces
    actions = actions.replace(/\n/g, '');
    actions = actions.replace(/\s+</g, '<');
    actions = actions.replace(/>\s+/g, '>');

    // check if actions content is wrapped with <actions> tag
    if (actions.indexOf("<actions>") === -1) {
     actions = "<actions>" + actions + "</actions>";
    }
    return actions;
  }

  /**
   * Callback for loading actions after gadget has been preloaded.
   *
   * @param {Object}
   *          Response from container's lifecycle handling of preloading the
   *          gadget.
   */
  var preloadCallback = function(response) {
    for (var url in response) {
      if(!response.hasOwnProperty(url)) {
        continue;
      }
      var metadata = response[url];
      if (metadata.error || !metadata.modulePrefs) {
        continue; // bail
      }

      var feature = metadata.modulePrefs.features['actions'],
          desc = feature && feature.params ? feature.params['action-contributions'] : null;
      if (!desc) {
        continue; // bail
      }

      // fix action-contributions param until OpenSocial specs change is implemented:
      // http://code.google.com/p/opensocial-resources/issues/detail?id=1264
      desc = fixActionContributions(desc);

      var domResponse = createDom(desc);
      if (!domResponse || domResponse.errors) {
        continue; // bail
      }

      var jsonDesc = gadgets.json.xml.convertXmlToJson(domResponse['data']),
          actionsJson = jsonDesc['actions'];
      if (!actionsJson) {
        continue; // bail
      }

      var actions = [].concat(actionsJson['action']);
      for (var i = 0; i < actions.length; i++) {
        var actionObj = actions[i];
        var actionClone = {};
        // replace @ for attribute keys;
        for (var key in actionObj) {
          if(actionObj.hasOwnProperty(key)) {
            actionClone[key.substring(1)] = actionObj[key];
          }
        }
        // check if action already exists
        if (!registry.getItemById(actionClone.id)) {
          addAction(actionClone, url);
        }
      }
    }
  };

  /**
   * Callback for when gadget site has been navigated.
   *
   * @param {Object}
   *          Gadget site that has been navigated.
   */
  var navigatedCallback = function(site) {
    var holder = site.getActiveSiteHolder();
    if (holder) {
      var url = holder.getUrl();
      registry.addGadgetSite(url, site);
    }
  };

  /**
   * Callback for when a gadget site has been closed.
   *
   * @param {Object}
   *          Gadget site that has been closed.
   */
  var closedCallback = function(site) {
    var siteId = site.getId();
    registry.removeGadgetSite(siteId);
  };

  /**
   * Callback for when a gadget has been unloaded.
   *
   * @param {String}
   *          Gadget spec url for the gadget that has been unloaded.
   */
  var unloadedCallback = function(url) {
    var actionsForUrl = registry.getActionsByUrl(url);
    for (var i = 0; i < actionsForUrl.length; i++) {
      var action = actionsForUrl[i];
      removeAction(action.id);
    }
  };

  // Object containing gadget lifecycle listeners
  var actionsLifecycleCallback = {};
  actionsLifecycleCallback[osapi.container.CallbackType.ON_PRELOADED] =
    preloadCallback;
  actionsLifecycleCallback[osapi.container.CallbackType.ON_NAVIGATED] =
    navigatedCallback;
  actionsLifecycleCallback[osapi.container.CallbackType.ON_BEFORE_CLOSE] =
    closedCallback;
  actionsLifecycleCallback[osapi.container.CallbackType.ON_UNLOADED] =
    unloadedCallback;

  /**
   * Function that renders actions in the container's UI
   *
   * @param {Object}
   *          actionObj The object with id, label, tooltip, icon and any other
   *          information for the container to use to render the action.
   */
  var showActionHandler = function(actions) {},
      showActionSiteIds = {};

  /**
   * Function that hides actions from the container's UI
   *
   * @param {Object}
   *          actionObj The object with id, label, tooltip, icon and any other
   *          information for the container to use to render the action.
   */
  var hideActionHandler = function(actions) {},
      hideActionSiteIds = {};

  /**
   * Function that renders gadgets in container's UI
   *
   * @param {String}
   *          gadgetSpecUrl The gadget spec url.
   * @param {Object}
   *          opt_params  The optional parameters for rendering the gadget.
   */
  var renderGadgetInContainer = function(gadgetSpecUrl, opt_params) {};

  // instantiate the singleton action registry
  var registry = new ActionRegistry();

  // a map to track actions that are scheduled to run after
  // pre-loaded gadget has been rendered
  var pendingActions = {};

  // container instance
  var container_ = null;

  /**
   * Add the Container API for the action service.
   */
  osapi.container.Container.addMixin('actions', function(container) {
    container_ = container;

    container_.rpcRegister('actions.registerHideCallback', function(rpcArgs) {
        hideActionSiteIds[rpcArgs.f] = 1;
      });
    container_.rpcRegister('actions.registerShowCallback', function(rpcArgs) {
        showActionSiteIds[rpcArgs.f] = 1;
      });
    container_.rpcRegister('actions.bindAction', function(rpcArgs, actionObj) {
        bindAction(actionObj);
      });
    container_.rpcRegister('actions.get_actions_by_type', function (rpcArgs, dataType) {
        return [].concat(registry.getActionsByDataType(dataType));
      });
    container_.rpcRegister('actions.get_actions_by_path', function(rpcArgs, path) {
        return [].concat(registry.getActionsByPath(path));
      });
    container_.rpcRegister('actions.removeAction', function(rpcArgs, id) {
        return removeAction(id);
      });
    container_.rpcRegister('actions.runAction', function (rpcArgs, id, selection) {
        container_.actions.runAction(id, selection);
      });

    if (container_.addGadgetLifecycleCallback) {
      container_.addGadgetLifecycleCallback('actions', actionsLifecycleCallback);
    }

    return /** @scope osapi.container.actions */ {
      /**
       * Registers a function to display actions in the container.
       *
       * @param {function}
       *          The container's function to render actions
       *          in its UI. The function takes the action object as
       *          a parameter.
       */
      registerShowActionsHandler: function(handler) {
        if (typeof handler === 'function') {
          showActionHandler = handler;
        }
      },

      /**
       * Registers a function to hide (remove) actions in the container.
       *
       * @param {function}
       *          The container's function to hide (remove) actions
       *          in its UI. The function takes the action object as
       *          a parameter.
       */
      registerHideActionsHandler: function(handler) {
        if (typeof handler === 'function') {
          hideActionHandler = handler;
        }
      },

      /**
       * Registers a function to render gadgets in the container.
       *
       * @param {function}
       *          The container's function to render gadgets in its UI.
       *          The function takes in two parameters: the gadget spec
       *          url and optional parameters.
       */
      registerNavigateGadgetHandler: function(renderGadgetFunction) {
        if (typeof renderGadgetFunction === 'function') {
          renderGadgetInContainer = renderGadgetFunction;
        }
      },

      /*
       * Uncomment the below two functions to run full jsunit tests.
       */
      // addAction : function(actionObj) { addAction(actionObj); },
      // removeAction : function(actionId) { removeAction(actionId); },

      /**
       * Executes the action associated with the action id.
       *
       * @param {String, Object}
       *          The id of the action to execute..
       *          The current selection. This is an optional parameter.
       */
      runAction: function(actionId, opt_selection) {
        var action = registry.getItemById(actionId);
        if (action) {
          // if gadget site has not been registered yet
          // the gadget needs to be rendered
          var gadgetSites = registry.getGadgetSites(actionId);
          if (!gadgetSites || (gadgetSites.length === 0)) {
            var gadgetUrl = registry.getUrl(actionId);
            pendingActions[actionId] = {
              selection: opt_selection || container_.selection.getSelection()
            };

            // set optional params
            var opt_params = {};
            if (action.view) {
              opt_params[osapi.container.actions.OptParam.VIEW] = action.view;
            }
            if (action.viewTarget) {
              opt_params[osapi.container.actions.OptParam.VIEW_TARGET] = action.viewTarget;
            }

            // render the gadget
            renderGadgetInContainer(gadgetUrl, opt_params);
          } else {
            runAction(actionId, opt_selection);
          }
        }
      },

      /**
       * Gets the action object from the registry based on the action id.
       *
       * @param {String}
       *          id The action id.
       * @return {Object} The action object.
       */
      getAction: function(id) {
        return registry.getItemById(id);
      },

      /**
       * Gets all action objects in the registry.
       *
       * @return {Array} An array with any action objects in the
       *         registry.
       */
      getAllActions: function() {
        return registry.getAllActions();
      },

      /**
       * Gets action object from registry based on the path.
       *
       * @param {String}
       *          The path for the action.
       * @return {Array} An array with any action objects in the
       *         specified path.
       */
      getActionsByPath: function(path) {
        return registry.getActionsByPath(path);
      },

      /**
       * Gets action object from registry based on the dataType.
       *
       * @param {String}
       *          The String representation of the Open Social data type.
       * @return {Array} An array of action objects bound to the specified
       *         data type.
       */
      getActionsByDataType: function(dataType) {
        return registry.getActionsByDataType(dataType);
      },

      /**
       * Adds a listener to be notified when an action is invoked.
       *
       * @param {function(string, Array.<Object>)} listener
       *          A callback to fire when a matching action is run.
       * @param {string=} opt_actionId
       *          An optional action id.  If not provided, listener will be
       *          notified for all action ids.
       */
      addListener: function(listener, opt_actionId) {
        if (listener && typeof(listener) != 'function') {
          throw new Error('listener param must be a function');
        }
        if (opt_actionId) {
          (actionListenerMap[opt_actionId] = actionListenerMap[opt_actionId] || []).push(listener);
        }
        else {
          actionListeners.push(listener);
        }
      },

      /**
       * Removes the specified listener.
       *
       * @param {function(string, Array.<Object>)} listener
       *          The listener to remove.
       */
      removeListener: function(listener) {
        var index = listeners.indexOf(listener);
        if (index != -1) {
          listeners.splice(index, 1);
        }
      }
    };
  });
})();

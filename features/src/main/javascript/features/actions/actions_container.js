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
        for (var i in partsOfPath) {
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
        actions = actions.concat(this.registryById[actionId]);
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
      for (var i in partsOfPath) {
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
      var existingSite = this.urlToSite[url];
      if (existingSite) {
        this.urlToSite[url] = existingSite.concat(site);
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
        var sites = this.urlToSite[url];
        for (var i in sites) {
          var site = sites[i];
          if (site && site.getId() == siteId) {
            sites.splice(i, 1);
            if (sites.length == 0) {
              delete this.urlToSite[url];
            }
          }
        }
      }
    };

    /**
     * Returns the gadget site associated with the specified action object.
     *
     * @param {Object}
     *          actionId The id of the action.
     * @return {osapi.container.GadgetSite} The gadget site instance associated
     *         with the action object.
     */
    this.getGadgetSite = function(actionId) {
      var url = this.actionToUrl[actionId];
      var sites = this.urlToSite[url];
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
        response['errors'].push('500 Failed to parse XML');
        response['rc'] = 500;
      } else {
        response['data'] = dom;
      }
    } else {
      var parser = new DOMParser();
      dom = parser.parseFromString(xmlString, 'text/xml');
      if ('parsererror' === dom.documentElement.nodeName) {
        response['errors'].push('500 Failed to parse XML');
        response['rc'] = 500;
      } else {
        response['data'] = dom;
      }
    }
    return response;
  };

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
  };

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
    // notify the container to display the action
    showActionHandlerProxy([actionObj]);
  };

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
    // notify the container to hide the action
    hideActionHandlerProxy([actionObj]);
  };

  /**
   * Runs the action associated with the specified actionId. If the gadget has
   * not yet been rendered, renders the gadget first, then runs the action.
   *
   * @param {String}
   *          The unique identifier for the action.
   *
   */
  function runAction(actionId, selection) {
    var actionData = {};
    actionData.actionId = actionId;
    actionData.selectionObj = selection;
    if (!selection && container_ && container_.selection) {
      actionData.selectionObj = container_.selection.getSelection();
    }
    // make rpc call to get gadget to run callback based on action id
    var gadgetSites = registry.getGadgetSite(actionId);
    if (gadgetSites && gadgetSites.length > 0) {
      var frameId = gadgetSites[0].getActiveGadgetHolder().getIframeId();
    }
    gadgets.rpc.call(frameId, 'actions', null, 'runAction', actionData);
  };

  /**
   * Callback for loading actions after gadget has been preloaded.
   *
   * @param {Array}
   *          Response from container's lifecycle handling of preloading the
   *          gadget.
   */
  var preloadCallback = function(response) {
    for (var url in response) {
      var metadata = response[url];
      if (!metadata.error) {
        if (metadata.modulePrefs) {
          var feature = metadata.modulePrefs.features['actions'];
          if (feature && feature.params) {
            var desc = feature.params['action-contributions'];
            if (desc) {
              var domResponse = createDom(desc);
              if (domResponse && !domResponse['errors']) {
                var jsonDesc = gadgets.json.xml
                  .convertXmlToJson(domResponse['data']);
                var actionsJson = jsonDesc['actions'];
                if (actionsJson) {
                  var actions = actionsJson['action'];
                  if (!(actions instanceof Array)) {
                    actions = [actions];
                  }
                  for (var i in actions) {
                    var actionObj = actions[i];
                    // replace @ for attribute keys;
                    for (itemAttr in actionObj) {
                      var attrStr = itemAttr.substring(1);
                      var attrVal = actionObj[itemAttr];
                      actionObj[attrStr] = attrVal;
                      delete actionObj[itemAttr];
                    }
                    // check if action already exists
                    if (!registry.getItemById(actionObj.id)) {
                      addAction(actionObj, url);
                    }
                  }
                }
              }
            }
          }
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
    var gadgetHolder = site.getActiveGadgetHolder();
    if (gadgetHolder) {
      var url = gadgetHolder.getUrl();
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
    for (var i in actionsForUrl) {
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
  actionsLifecycleCallback[osapi.container.CallbackType.ON_CLOSED] =
    closedCallback;
  actionsLifecycleCallback[osapi.container.CallbackType.ON_UNLOADED] =
    unloadedCallback;

  // Function to handle RPC calls from the gadgets side
  function router(channel, object) {
    switch (channel) {
    case 'bindAction':
      bindAction(object);
      break;
    case 'runAction':
      container_.actions.runAction(object.id, object.selection);
      break;
    case 'removeAction':
      hideActionHandlerProxy([object]);
      break;
    case 'getActionsByPath':
      return container_.actions.getActionsByPath(object);
    case 'getActionsByDataType':
      return container_.actions.getActionsByDataType(object);
    case 'addShowActionListener':
      addShowActionListener(object);
      break;
    case 'addHideActionListener':
      addHideActionListener(object);
      break;
    }
  };

  /**
   * Function that renders actions in the container's UI
   *
   * @param {Object}
   *          actionObj The object with id, label, tooltip, icon and any other
   *          information for the container to use to render the action.
   */
  var showActionHandler = function(actions) {};
  var showActionListeners = [];
  var showActionHandlerProxy = function(actions) {
    showActionHandler(actions);
    for (var i in showActionListeners)
      showActionListeners[i](actions);
  };

  /**
   * Function that adds a listener to the list of listeners that will
   * be notified of show action events.
   */
  function addShowActionListener(listener) {
    showActionListeners.push(listener);
  };

  /**
   * Function that hides actions from the container's UI
   *
   * @param {Object}
   *          actionObj The object with id, label, tooltip, icon and any other
   *          information for the container to use to render the action.
   */
  var hideActionHandler = function(actions) {};
  var hideActionListeners = [];
  var hideActionHandlerProxy = function(actions) {
    hideActionHandler(actions);
    for (var i in hideActionListeners)
      hideActionListeners[i](actions);
  };

  /**
   * Function that adds a listener to the list of listeners that will
   * be notified of hide action events.
   */
  function addHideActionListener(listener) {
    hideActionListeners.push(listener);
  };

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
    gadgets.rpc.register('actions', router);

    if (container.addGadgetLifecycleCallback) {
      container.addGadgetLifecycleCallback('actions',
          actionsLifecycleCallback);
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
      //addAction : function(actionObj) { addAction(actionObj); },
      //removeAction : function(actionId) { removeAction(actionId); },

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
          var gadgetSite = registry.getGadgetSite(actionId);
          if (!gadgetSite) {
            var gadgetUrl = registry.getUrl(actionId);
            pendingActions[actionId] = {
              selection: container_.selection.getSelection()
            };

            // set selection
	    if (opt_selection != null) {
	      pendingActions[actionId].selection = opt_selection;
	    }

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
        var actions = [];
        actions = actions.concat(registry.getActionsByPath(path));
        return actions;
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
        var actions = [];
        actions = actions.concat(registry.getActionsByDataType(dataType));
        return actions;
      }
    };
  });
})();

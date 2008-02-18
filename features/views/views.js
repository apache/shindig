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
 * @fileoverview This library provides functions for navigating to and dealing
 *     with views of the current gadget.
 */

var gadgets = gadgets || {};

/**
 * Implements the gadgets.views API spec. See
 * http://code.google.com/apis/gadgets/docs/reference/gadgets.views.html
 */
gadgets.views = function() {

  /**
   * Reference to the current view object.
   */
  var currentView = null;

  /**
   * Map of all supported views for this container.
   */
  var supportedViews = {};

  /**
   * Map of parameters passed to the current request.
   */
  var params = {};

  /**
   * Initializes views. Assumes that the current view is the "view"
   * url parameter (or default if "view" isn't supported), and that
   * all view parameters are in the form view-<name>
   * TODO: Use unified configuration when it becomes available.
   *
   */
  function init(config) {
    var supported = config["views"];

    for (var s in supported) if (supported.hasOwnProperty(s)) {
      var obj = supported[s];
      supportedViews[s] = new gadgets.views.View(s, obj.isOnlyVisible);
      var aliases = obj.aliases || [];
      for (var i = 0, alias; alias = aliases[i]; ++i) {
        supportedViews[alias] = new gadgets.views.View(s, obj.isOnlyVisible);
      }
    }

    var urlParams = gadgets.util.getUrlParameters();
    // View parameters are passed as a single parameter.
    if (urlParams["view-params"]) {
      var tmpParams = gadgets.json.parse(
          decodeURIComponent(urlParams["view-params"]));
      if (tmpParams) {
        params = tmpParams;
      }
    }
    currentView = supportedViews[urlParams.view] || supportedViews["default"];
  }

  var requiredConfig = {
    "default": new gadgets.config.LikeValidator({
      "isOnlyVisible" : gadgets.config.BooleanValidator
    })
  };

  gadgets.config.register("views", requiredConfig, init);

  return {
    requestNavigateTo : function(view, opt_params) {
      gadgets.rpc.call(
          null, "requestNavigateTo", null, view.getName(), opt_params);
    },

    getCurrentView : function() {
      return currentView;
    },

    getSupportedViews : function() {
      return supportedViews;
    },

    getParams : function() {
      return params;
    }
  };
}();

gadgets.views.View = function(name, opt_isOnlyVisible) {
  this.name_ = name;
  this.isOnlyVisible_ = !!opt_isOnlyVisible;
};

gadgets.views.View.prototype.getName = function() {
  return this.name_;
};

gadgets.views.View.prototype.isOnlyVisibleGadget = function() {
  return this.isOnlyVisible_;
};

gadgets.views.ViewType = gadgets.util.makeEnum([
  "FULL_PAGE", "DASHBOARD", "POPUP"
]);
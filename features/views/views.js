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
      if (!obj) {
        continue;
      }
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
        for (var p in params) if (params.hasOwnProperty(p)) {
          params[p] = gadgets.util.escapeString(params[p]);
        }
      }
    }
    currentView = supportedViews[urlParams.view] || supportedViews["default"];
  }

  gadgets.config.register("views", null, init);

  return {

    /**
     * Binds a URL template with variables in the passed environment
     * to produce a URL string.
     *
     * The URL template conforms to the IETF draft spec:
     * http://bitworking.org/projects/URI-Templates/spec/draft-gregorio-uritemplate-03.html
     *
     * @param {string} urlTemplate A URL template for a container view.
     * @param {Map&lt;string, string&gt;} environment A set of named variables.
     * @return {string} A URL string with substituted variables.
     */
    bind : function(urlTemplate, environment) {
      function getVar(varName, defaultVal) {
        return environment.hasOwnProperty(varName) ?
               environment[varName] : defaultVal;
      }

      // TODO Validate environment
      // TODO Validate urlTemplate

      var varRE = /^([a-zA-Z0-9][a-zA-Z0-9_.-]*)$/,
          expansionRE = /\{([^}]*)\}/g,
          result = [],
          textStart = 0,
          group,
          match,
          varName;

      while (group = expansionRE.exec(urlTemplate)) {
        result.push(urlTemplate.substring(textStart, group.index));
        textStart = expansionRE.lastIndex;
        if (match = group[1].match(varRE)) {
          // TODO Add support for "var=default_value" syntax
          varName = match[1];
          result.push(getVar(varName, ''));
        } else {
          // TODO Add support for "-op|arg|vars" syntax
          // TODO Parse the "-opt" operator
          // TODO Parse the "-neg" operator
          // TODO Parse the "-prefix" operator
          // TODO Parse the "-suffix" operator
          // TODO Parse the "-join" operator
          // TODO Parse the "-list" operator
          throw new Error('Invalid syntax : ' + group[0]);
        }
      }
      // TODO Throw an exception if no variable is defined at all.
      result.push(urlTemplate.substr(textStart));

      return result.join('');
    },

    /**
     * Attempts to navigate to this gadget in a different view. If the container
     * supports parameters will pass the optional parameters along to the gadget
     * in the new view.
     *
     * @param {gadgets.views.View} view The view to navigate to
     * @param {Map.&lt;String, String&gt;} opt_params Parameters to pass to the
     *     gadget after it has been navigated to on the surface
     * @param {string} opt_ownerId The ID of the owner of the page to navigate to;
     *                 defaults to the current owner.
     */
    requestNavigateTo : function(view, opt_params, opt_ownerId) {
      gadgets.rpc.call(
          null, "requestNavigateTo", null, view.getName(), opt_params, opt_ownerId);
    },

    /**
     * Returns the current view.
     *
     * @return {gadgets.views.View} The current view
     */
    getCurrentView : function() {
      return currentView;
    },

    /**
     * Returns a map of all the supported views. Keys each gadgets.view.View by
     * its name.
     *
     * @return {Map&lt;gadgets.views.ViewType | String, gadgets.views.View&gt;}
     *   All supported views, keyed by their name attribute.
     */
    getSupportedViews : function() {
      return supportedViews;
    },

    /**
     * Returns the parameters passed into this gadget for this view. Does not
     * include all url parameters, only the ones passed into
     * gadgets.views.requestNavigateTo
     *
     * @return {Map.&lt;String, String&gt;} The parameter map
     */
    getParams : function() {
      return params;
    }
  };
}();

gadgets.views.View = function(name, opt_isOnlyVisible) {
  this.name_ = name;
  this.isOnlyVisible_ = !!opt_isOnlyVisible;
};

/**
 * @return {String} The view name.
 */
gadgets.views.View.prototype.getName = function() {
  return this.name_;
};

/**
 * Returns the associated URL template of the view.
 * The URL template conforms to the IETF draft spec:
 * http://bitworking.org/projects/URI-Templates/spec/draft-gregorio-uritemplate-03.html
 * @return {string} A URL template.
 */
gadgets.views.View.prototype.getUrlTemplate = function() {
  return gadgets.config &&
         gadgets.config.views &&
         gadgets.config.views[this.name_] &&
         gadgets.config.views[this.name_].urlTemplate;
};

/**
 * Binds the view's URL template with variables in the passed environment
 * to produce a URL string.
 * @param {Map&lt;string, string&gt;} environment A set of named variables.
 * @return {string} A URL string with substituted variables.
 */
gadgets.views.View.prototype.bind = function(environment) {
  return gadgets.views.bind(this.getUrlTemplate(), environment);
};

/**
 * @return {Boolean} True if this is the only visible gadget on the page.
 */
gadgets.views.View.prototype.isOnlyVisibleGadget = function() {
  return this.isOnlyVisible_;
};

gadgets.views.ViewType = gadgets.util.makeEnum([
  "CANVAS", "HOME", "PREVIEW", "PROFILE",
  // TODO Deprecate the following ViewTypes.
  "FULL_PAGE", "DASHBOARD", "POPUP"
]);

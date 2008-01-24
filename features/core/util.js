/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var gadgets = gadgets || {};

/**
 * @fileoverview General purpose utilities that gadgets can use.
 */

/**
 * @static
 * @class Provides general purpose utility functions.
 * @name gadgets.util
 */

gadgets.util = function() {
  /**
   * Parses url parameters into an object.
   * @return {Array.<String>} the parameters.
   */
  function parseUrlParams() {
    // Get settings from url, 'hash' takes precedence over 'search' component
    // don't use document.location.hash due to browser differences.
    var query;
    var l = document.location.href;
    var queryIdx = l.indexOf("?");
    var hashIdx = l.indexOf("#");
    if (hashIdx === -1) {
      query = l.substr(queryIdx + 1);
    } else {
      // essentially replaces "#" with "&"
      query = [l.substr(queryIdx + 1, hashIdx - queryIdx - 1), "&",
               l.substr(hashIdx + 1)].join("");
    }
    return query.split("&");
  }

  var parameters = null;
  var features = {};
  var onLoadHandlers = [];

  return /** @scope gadgets.util */ {

    /**
     * Gets the url parameters.
     *
     * @return {Object} Parameters passed into the query string.
     */
    getUrlParameters : function () {
      if (parameters !== null) {
        return parameters;
      }
      parameters = {};
      var pairs = parseUrlParams();
      var unesc = window.decodeURIComponent ? decodeURIComponent : unescape;
      for (var i = 0, j = pairs.length; i < j; ++i) {
        var pos = pairs[i].indexOf('=');
        if (pos === -1) {
          continue;
        }
        var argName = pairs[i].substring(0, pos);
        var value = pairs[i].substring(pos + 1);
        // difference to IG_Prefs, is that args doesn't replace spaces in
        // argname. Unclear on if it should do:
        // argname = argname.replace(/\+/g, " ");
        value = value.replace(/\+/g, " ");
        parameters[argName] = unesc(value);
      }
      return parameters;
    },

    /**
     * Creates a closure which is suitable for passing as a callback.
     *
     * @param {Object} scope The execution scope. May be null if there is no
     *     need to associate a specific instance of an object with this
     *     callback.
     * @param {Function} callback The callback to invoke when this is run.
     *     any arguments passed in will be passed after your initial arguments.
     * @param {Object} var_args Any number of arguments may be passed to the
     *     callback. They will be received in the order they are passed in.
     */
    makeClosure : function (scope, callback, var_args) {
      // arguments isn't a real array, so we copy it into one.
      var tmpArgs = [];
      for (var i = 2, j = arguments.length; i < j; ++i) {
       tmpArgs.push(arguments[i]);
      }
      return function() {
        // append new arguments.
        for (var i = 0, j = arguments.length; i < j; ++i) {
          tmpArgs.push(arguments[i]);
        }
        callback.apply(scope, tmpArgs);
      };
    },

    /**
     * Gets the feature parameters.
     *
     * @param {String} feature The feature to get parameters for.
     * @return {Object} The parameters for the given feature, or null.
     */
    getFeatureParameters : function (feature) {
      return typeof features[feature] === "undefined"
          ? null : features[feature];
    },

    /**
     * Returns whether the current feature is supported.
     *
     * @param {String} feature The feature to test for.
     * @return {Boolean} True if the feature is supported.
     */
    hasFeature : function (feature) {
      return typeof features[feature] === "undefined";
    },

    /**
     * Registers an onload handler.
     * @param {Function} callback The handler to run.
     */
    registerOnLoadHandler : function (callback) {
      onLoadHandlers.push(callback);
    },

    /**
     * Runs all functions registered via registerOnLoadHandler.
     * @private Only to be used by the container, not gadgets.
     */
    runOnLoadHandlers : function () {
      for (var i = 0, j = onLoadHandlers.length; i < j; ++i) {
        onLoadHandlers[i]();
      }
    },

    /**
     * @param {Object} featureData The features that are supported, and
     *    their parameters.
     * @private Only to be used by the container, not gadgets.
     */
    init : function (featureData) {
      features = featureData;
    }
  };
}();

// TODO: Check for any other commonly used aliases

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

var gadgets = gadgets || {};

/**
 * General purpose utilities.
 */
gadgets.Util = function() {
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

  /**
   * @return {Object} Parameters passed into the query string.
   */
  function getUrlParameters() {
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
      // difference to IG_Prefs, is that args doesn't replace spaces in argname:
      // unclear on if it should do: argname = argname.replace(/\+/g, " ");
      value = value.replace(/\+/g, " ");
      parameters[argName] = unesc(value);
    }
    return parameters;
  }

  /**
   * Creates a closure which is suitable for passing as a callback.
   *
   * @param {Object} scope The execution scope. May be null if there is no
   *     need to associate a specific instance of an object with this callback.
   * @param {Function} callback The callback to invoke when this is run.
   *     any arguments passed in will be passed after your initial arguments.
   * @param {Object} var_args Any number of arguments may be passed to the
   *     callback. They will be received in the order they are passed in.
   */
  function makeClosure(scope, callback, var_args) {
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
  }

  var features = {};

  /**
   * @param {String} feature The feature to get parameters for.
   * @return {Object} The parameters for the given feature, or null.
   */
  function getFeatureParameters(feature) {
    return typeof features[feature] === "undefined" ? null : features[feature];
  }

  /**
   * @param {String} feature The feature to test for.
   * @return {Boolean} True if the feature is supported.
   */
  function hasFeature(feature) {
    return typeof features[feature] === "undefined";
  }

  /**
   *  @param {Object} featureData The features that are supported, and
   *    their parameters.
   */
  function init(featureData) {
    features = featureData;
  }

  // Export public API.
  return {
    getUrlParameters: getUrlParameters,
    getFeatureParameters: getFeatureParameters,
    hasFeature: hasFeature,
    makeClosure: makeClosure,
    init: init
  };
}();

// TODO: Check for any other commonly used aliases

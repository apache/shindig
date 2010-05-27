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
 * @fileoverview Utility methods common container.
 */
var shindig = shindig || {};
shindig.container = shindig.container || {};
shindig.container.util = shindig.container.util || {};


/**
 * Extract protocol and domain of container page. Valid values:
 * http://www.cnn.com, chrome-extension://www.cnn.com
 * @param {string} url The URL to extract path domain from.
 * @return {string} The protocol and domain of container page.
 */
shindig.container.util.parseOrigin = function(uri) {
  var indexAtStartOfAuthority = uri.indexOf('//') + 2;
  var indexAtEndOfAuthority = uri.indexOf('/', indexAtStartOfAuthority);
  return uri.substring(0, indexAtEndOfAuthority);
};


/**
 * Extract prefix path of a URL, not including opt_postfixPath.
 * @param {string} url The URL to extract path from.
 * @param {string=} opt_postfixPath The URL postfix to avoid extracting.
 * @return {string} The path in URL, before postfixPath.
 */
shindig.container.util.parsePrefixPath = function(uri, opt_postfixPath) {
  var path = shindig.container.util.parsePath(uri);
  if (path && opt_postfixPath) {
    var endIndex = path.length - opt_postfixPath.length;
    if (path.lastIndexOf(opt_postfixPath) == endIndex) {
      return path.substring(0, endIndex);
    }
  }
  return path;
};

/**
 * Extract path of a URL.
 * @param {string} url The URL to extract path from.
 * @return {string} The path in URL.
 */
shindig.container.util.parsePath = function(uri) {
  var match = uri.match(new RegExp("//[^/]+(/[^?#]*)"));
  return match ? match[1] : null;
};


/**
 * Extract the parameter value in path with name paramName.
 * @param {string} path The path to extract parameter from.
 * @param {string} paramName The name of parameter to exact value from.
 * @return {string} The value of the parameter. Null otherwise.
 */
shindig.container.util.getParamValue = function(path, paramName) {
  var match = path.match(new RegExp("[?&]" + paramName + "=([^&#]+)"));
  return match ? match[1] : null;
};


/**
 * Return value of json at key, if valid. Otherwise, return defaultValue.
 * @param {Object} json The JSON to look up key param from.
 * @param {string} key Key in config.
 * @param {Object?} defaultValue The default value to return.
 * @return {Object?}
 */
shindig.container.util.getSafeJsonValue = function(json, key, defaultValue) {
  return (json[key] != undefined && json[key] != null)
      ? json[key] : defaultValue;
};


/**
 * Merge two JSON together. Keys in json2 will replace than in json1.
 * @param {Object} json1 JSON to start merge with.
 * @param {Object} json2 JSON to append/replace json1.
 * @return {Object} the resulting JSON.
 */
shindig.container.util.mergeJsons = function(json1, json2) {
  var result = {};
  for (var key in json1) {
    result[key] = json1[key];
  }
  for (var key in json2) {
    result[key] = json2[key];
  }
  return result;
};


/**
 * Extract keys from a JSON to an array.
 * @param {Object} json to extract keys from.
 * @return {array} keys in the json.
 */
shindig.container.util.toArrayOfJsonKeys = function(json) {
  var result = [];
  for (var key in json) {
    result.push(key);
  }
  return result;
};


/**
 * Count the number of own/self properties in json.
 * @param {Object} json the JSON to act on.
 * @return {number} Number of elements in json.
 */
shindig.container.util.countProperties = function(json) {
  var count = 0;
  for (var key in json) {
    if (json.hasOwnProperty(key)) {
      count++;
    }
  }
  return count;
};

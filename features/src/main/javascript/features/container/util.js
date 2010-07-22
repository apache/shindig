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
 * @fileoverview Utility methods for common container.
 */


/**
 * @type {Object}
 */
shindig.container = {};


/**
 * @type {Object}
 */
shindig.container.util = {};


/**
 * Extract protocol and domain of container page. Valid values:
 * http://www.cnn.com, chrome-extension://www.cnn.com
 * @param {string} uri The URL to extract protocol and domain from.
 * @return {string} The protocol and domain of container page.
 */
shindig.container.util.parseOrigin = function(uri) {
  var indexAtStartOfAuthority = uri.indexOf('//') + 2;
  var indexAtEndOfAuthority = uri.indexOf('/', indexAtStartOfAuthority);
  return uri.substring(0, indexAtEndOfAuthority);
};


/**
 * Extract path of a URL.
 * @param {string} uri The URL to extract path from.
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
 * @param {*=} defaultValue The default value to return.
 * @return {*}
 */
shindig.container.util.getSafeJsonValue = function(json, key, defaultValue) {
  return (json[key] != undefined && json[key] != null)
      ? json[key] : defaultValue;
};


/**
 * Return URI with its query/fragment parameter either:
 * - if exist, added with name=value.
 * - otherwise, replaced with name=value.
 * @param {string} uri URL to work with.
 * @param {string} name of parameter to add/replace.
 * @param {string} value of parameter to add/replace.
 * @return {string}
 */
shindig.container.util.updateQueryParam = function(uri, name, value) {
  var fragmentString = '';
  var fragmentIndex = uri.indexOf('#');
  if (fragmentIndex >= 0) {
	fragmentString = uri.substring(fragmentIndex);
    uri = uri.substring(0, fragmentIndex);
  }

  var re = new RegExp('([&?])' + name + '[^&]*');
  if (uri.match(re)) {
	uri = uri.replace(re, '$1' + encodeURIComponent(name) + '=' +
        encodeURIComponent(value));
  } else {
	uri = shindig.container.util.addQueryParam(uri, name, value);
  }
  return uri + fragmentString;
};


/**
 * Adds a hash parameter to a URI.
 * @param {string} uri The URI.
 * @param {string} key The param key.
 * @param {string} value The param value.
 * @return {string} The new URI.
 */
shindig.container.util.addFragmentParam = function(uri, key, value) {
  return uri + ((uri.indexOf('#') == -1) ? '#' : '&')
      + encodeURIComponent(key) + '=' + encodeURIComponent(value);
};


/**
 * Adds a query parameter to a URI.
 * @param {string} uri The URI.
 * @param {string} key The param key.
 * @param {string} value The param value.
 * @return {string} The new URI.
 */
shindig.container.util.addQueryParam = function(uri, key, value) {
  var hasQuery = uri.indexOf('?') != -1;
  var insertPos = (uri.indexOf('#') != -1) ? uri.indexOf('#') : uri.length;
  return uri.substring(0, insertPos) + (hasQuery ? '&' : '?') +
      encodeURIComponent(key) + '=' + encodeURIComponent(value) +
      uri.substring(insertPos);
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
 * @return {Array.<string>} keys in the json.
 */
shindig.container.util.toArrayOfJsonKeys = function(json) {
  var result = [];
  for (var key in json) {
    result.push(key);
  }
  return result;
};

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
osapi.container.util = {};


/**
 * @param {Object} json The JSON to look up key param from.
 * @param {string} key Key in config.
 * @param {*=} defaultValue The default value to return.
 * @return {*} value of json at key, if valid. Otherwise, return defaultValue.
 */
osapi.container.util.getSafeJsonValue = function(json, key, defaultValue) {
  return (typeof(json[key]) != 'undefined' && json[key] != null) ?
      json[key] : defaultValue;
};


/**
 * Merge two JSON together. Keys in json2 will replace than in json1.
 * @param {Object} json1 JSON to start merge with.
 * @param {Object} json2 JSON to append/replace json1.
 * @return {Object} the resulting JSON.
 */
osapi.container.util.mergeJsons = function(json1, json2) {
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
 * Construct a JSON request to get gadget metadata. For now, this will request
 * a super-set of data needed for all CC APIs requiring gadget metadata, since
 * the caching of response is not additive.
 * @param {Array} gadgetUrls An array of gadget URLs.
 * @return {Object} the resulting JSON.
 */
osapi.container.util.newMetadataRequest = function(gadgetUrls) {
  if (!osapi.container.util.isArray(gadgetUrls)) {
    gadgetUrls = [gadgetUrls];
  }
  return {
    'container': window.__CONTAINER,
    'ids': gadgetUrls,
    'fields': [
      'iframeUrls',
      'modulePrefs.*',
      'needsTokenRefresh',
      'userPrefs.*',
      'views.preferredHeight',
      'views.preferredWidth',
      'expireTimeMs',
      'responseTimeMs',
      'rpcServiceIds',
      'tokenTTL'
    ]
  };
};


/**
 * Construct a JSON request to get gadget token.
 * @param {Array} gadgetUrls A list of gadget URLs.
 * @return {Object} the resulting JSON.
 */
osapi.container.util.newTokenRequest = function(gadgetUrls) {
  if (!osapi.container.util.isArray(gadgetUrls)) {
    gadgetUrls = [gadgetUrls];
  }
  return {
    'container': window.__CONTAINER,
    'ids': gadgetUrls,
    'fields': [
      'token',
      'tokenTTL',
      'moduleId'
    ]
  };
};


/**
 * Extract keys from a JSON to an array.
 * @param {Object} json to extract keys from.
 * @return {Array.<string>} keys in the json.
 */
osapi.container.util.toArrayOfJsonKeys = function(json) {
  var result = [];
  for (var key in json) {
    result.push(key);
  }
  return result;
};


/**
 * Tests an object to see if it is an array or not.
 * @param {object} obj Object to test.
 * @return {boolean} If obj is an array.
 */
osapi.container.util.isArray = function(obj) {
  return Object.prototype.toString.call(obj) == '[object Array]';
};


/**
 * @param {Object} json to check.
 * @return {Boolean} true if json is empty.
 */
osapi.container.util.isEmptyJson = function(json) {
  for (var key in json) {
    return false;
  }
  return true;
};

/**
 * @return {number} current time in ms.
 */
osapi.container.util.getCurrentTimeMs = function() {
  return new Date().getTime();
};

/**
 * Creates the HTML for the iFrame
 * @param {Object.<string,string>} iframeParams iframe Params.
 * @return {string} the HTML for the iFrame.
 */
osapi.container.util.createIframeHtml = function(iframeParams) {

  // Do not use DOM API (createElement(), setAttribute()), since it is slower,
  // requires more code, and creating an element with it results in a click
  // sound in IE (unconfirmed), setAttribute('class') may need browser-specific
  // variants.
  var out = [], n = 0;
  out[n++] = '<iframe ';
  for (var key in iframeParams) {
      var value = iframeParams[key];
      if (typeof(value) != 'undefined') {
          out[n++] = key;
          out[n++] = '="';
          out[n++] = value;
          out[n++] = '" ';
      }
  }
  out[n++] = '></iframe>';

  return out.join('');
};

/**
 * Constructs a url for token refresh given a gadgetUrl and moduleId.
 *
 * @param {string} url The gadget's url
 * @param {number} moduleId A moduleId.
 * @return {string} A url to use in a TokenRequest for a security token.
 */
osapi.container.util.buildTokenRequestUrl = function(url, moduleId) {
  url = shindig.uri(url);
  if (moduleId) {
    url.setFragment(moduleId);
  }
  return url.toString();
};

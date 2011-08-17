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
 * @fileoverview This represents the service layer that talks to OSAPI
 * endpoints. All RPC requests should go into this class.
 */


/**
 * @param {Object=} opt_config Configuration JSON.
 * @constructor
 */
osapi.container.Service = function(opt_config) {
  var config = opt_config || {};

  /**
   * @type {string}
   * @private
   */
  this.apiHost_ = String(osapi.container.util.getSafeJsonValue(config,
      osapi.container.ServiceConfig.API_HOST, window.__API_URI.getOrigin()));

  /**
   * @type {string}
   * @private
   */
  this.apiPath_ = String(osapi.container.util.getSafeJsonValue(config,
      osapi.container.ServiceConfig.API_PATH, '/rpc'));

  /**
   * Map of gadget URLs to cached gadgetInfo response.
   * @type {Object}
   * @private
   */
  this.cachedMetadatas_ = {};

  /**
   * Map of gadget URLs to cached tokenInfo response.
   * @type {Object}
   * @private
   */
  this.cachedTokens_ = {};

  /**
   * @see osapi.container.Container.prototype.getLanguage
   */
  if (config.GET_LANGUAGE) {
    this.getLanguage = config.GET_LANGUAGE;
  }

  /**
   * @see osapi.container.Container.prototype.getCountry
   */
  if (config.GET_COUNTRY) {
    this.getCountry = config.GET_COUNTRY;
  }

  this.registerOsapiServices();

  this.onConstructed(config);
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 * @param {Object=} opt_config Configuration JSON.
 */
osapi.container.Service.prototype.onConstructed = function(opt_config) {};


/**
 * Return a possibly-cached gadgets metadata for gadgets in request.ids, for
 * container request.container. If metadata is not cache, fetch from server
 * only for the uncached gadget URLs. The optional callback opt_callback will be
 * called, after a response is received.
 * @param {Object} request JSON object representing the request.
 * @param {function(Object)=} opt_callback function to call upon data receive.
 */
osapi.container.Service.prototype.getGadgetMetadata = function(request, opt_callback) {
  // TODO: come up with an expiration mechanism to evict cached gadgets.
  // Can be based on renderParam['nocache']. Be careful with preloaded and
  // arbitrarily-navigated gadgets. The former should be indefinite, unless
  // unloaded. The later can done without user knowing.
  var callback = opt_callback || function() {};

  var uncachedUrls = osapi.container.util.toArrayOfJsonKeys(
      this.getUncachedDataByRequest_(this.cachedMetadatas_, request));
  var finalResponse = this.getCachedDataByRequest_(this.cachedMetadatas_, request);

  // If fully cached, return from cache.
  if (uncachedUrls.length == 0) {
    callback(finalResponse);

  // Otherwise, request for uncached metadatas.
  } else {
    var self = this;
    request['ids'] = uncachedUrls;
    request['language'] = this.getLanguage();
    request['country'] = this.getCountry();
    osapi['gadgets']['metadata'](request).execute(function(response) {

      // If response entirely fails, augment individual errors.
      if (response['error']) {
        for (var i = 0; i < request['ids'].length; i++) {
          finalResponse[id] = { 'error' : response['error'] };
        }

      // Otherwise, cache response. Augment final response with server response.
      } else {
        var currentTimeMs = osapi.container.util.getCurrentTimeMs();
        for (var id in response) {
          var resp = response[id];
          self.updateResponse_(resp, id, currentTimeMs);
          self.cachedMetadatas_[id] = resp;
          finalResponse[id] = resp;
        }
      }

      callback(finalResponse);
    });
  }
};


/**
 * Add preloaded gadgets to cache
 * @param {Object} response hash of gadgets metadata.
 * @param {Object} refTime time to override responseTime (in order to support external caching).
 */
osapi.container.Service.prototype.addGadgetMetadatas = function(response, refTime) {
  this.addToCache_(response, refTime, this.cachedMetadatas_);
};


/**
 * Add preloaded tokens to cache
 * @param {Object} response hash of gadgets metadata.
 * @param {Object} refTime data time to override responseTime
 *     (in order to support external caching).
 */
osapi.container.Service.prototype.addGadgetTokens = function(response, refTime) {
  this.addToCache_(response, refTime, this.cachedTokens_);
};


/**
 * Utility function to add data to cache
 * @param {Object} response hash of gadgets metadata.
 * @param {Object} refTime data time to override responseTime (in order to support external caching).
 * @param {Object} cache the cache to update.
 * @private
 */
osapi.container.Service.prototype.addToCache_ = function(response, refTime, cache) {
  var currentTimeMs = osapi.container.util.getCurrentTimeMs();
  for (var id in response) {
    var resp = response[id];
    this.updateResponse_(resp, id, currentTimeMs, refTime);
    cache[id] = resp;
  }
};


/**
 * Update gadget data, set gadget id and calculate expiration time
 * @param {Object} resp gadget metadata item.
 * @param {string} id gadget id.
 * @param {Object} currentTimeMs current time.
 * @param {Object} opt_refTime data time to override responseTime (support external caching).
 * @private
 */
osapi.container.Service.prototype.updateResponse_ = function(
    resp, id, currentTimeMs, opt_refTime) {
  resp[osapi.container.MetadataParam.URL] = id;
  // This ignores time to fetch metadata. Okay, expect to be < 2s.
  resp[osapi.container.MetadataParam.LOCAL_EXPIRE_TIME] =
      resp[osapi.container.MetadataResponse.EXPIRE_TIME_MS] -
      (opt_refTime == null ?
          resp[osapi.container.MetadataResponse.RESPONSE_TIME_MS] : opt_refTime) +
      currentTimeMs;
};


/**
 * @param {Object} request JSON object representing the request.
 * @param {function(Object)=} opt_callback function to call upon data receive.
 */
osapi.container.Service.prototype.getGadgetToken = function(
    request, opt_callback) {
  var callback = opt_callback || function() {};

  // Do not check against cache. Always do a server fetch.
  var self = this;
  osapi['gadgets']['token'](request).execute(function(response) {
    var finalResponse = {};

    // If response entirely fails, augment individual errors.
    if (response['error']) {
      for (var i = 0; i < request['ids'].length; i++) {
        finalResponse[id] = { 'error' : response['error'] };
      }

    // Otherwise, cache response. Augment final response with server response.
    } else {
      for (var id in response) {
        response[id]['url'] = id; // make sure url is set
        self.cachedTokens_[id] = response[id];
        finalResponse[id] = response[id];
      }
    }

    callback(finalResponse);
  });
};


/**
 * @param {string} url gadget URL to use as key to get cached metadata.
 * @return {string} the gadgetInfo referenced by this URL.
 */
osapi.container.Service.prototype.getCachedGadgetMetadata = function(url) {
  return this.cachedMetadatas_[url];
};


/**
 * @param {string} url gadget URL to use as key to get cached token.
 * @return {string} the tokenInfo referenced by this URL.
 */
osapi.container.Service.prototype.getCachedGadgetToken = function(url) {
  return this.cachedTokens_[url];
};


/**
 * @param {Object} urls JSON containing gadget URLs to avoid removing.
 */
osapi.container.Service.prototype.uncacheStaleGadgetMetadataExcept =
    function(urls) {
  for (var url in this.cachedMetadatas_) {
    if (typeof urls[url] === 'undefined') {
      var gadgetInfo = this.cachedMetadatas_[url];
      if (gadgetInfo[osapi.container.MetadataParam.LOCAL_EXPIRE_TIME] <
          osapi.container.util.getCurrentTimeMs()) {
        delete this.cachedMetadatas_[url];
      }
    }
  }
};


/**
 * Initialize OSAPI endpoint methods/interfaces.
 */
osapi.container.Service.prototype.registerOsapiServices = function() {
  var endPoint = this.apiHost_ + this.apiPath_;

  var osapiServicesConfig = {};
  osapiServicesConfig['gadgets.rpc'] = ['container.listMethods'];
  osapiServicesConfig[endPoint] = [
    'gadgets.metadata',
    'gadgets.token'
  ];

  gadgets.config.init({
    'osapi': { 'endPoints': [endPoint] },
    'osapi.services': osapiServicesConfig
  });
};


/**
 * Get cached data by ids listed in request.
 * @param {Object} cache JSON containing cached data.
 * @param {Object} request containing ids.
 * @return {Object} JSON containing requested and cached entries.
 * @private
 */
osapi.container.Service.prototype.getCachedDataByRequest_ = function(
    cache, request) {
  return this.filterCachedDataByRequest_(cache, request,
      function(data) { return (typeof data !== 'undefined') });
};


/**
 * Get uncached data by ids listed in request.
 * @param {Object} cache JSON containing cached data.
 * @param {Object} request containing ids.
 * @return {Object} JSON containing requested and uncached entries.
 * @private
 */
osapi.container.Service.prototype.getUncachedDataByRequest_ = function(
    cache, request) {
  return this.filterCachedDataByRequest_(cache, request,
      function(data) { return (typeof data === 'undefined') });
};


/**
 * Helper to filter out cached data
 * @param {Object} data JSON containing cached data.
 * @param {Object} request containing ids.
 * @param {Function} filterFunc function to filter result.
 * @return {Object} JSON containing requested and filtered entries.
 * @private
 */
osapi.container.Service.prototype.filterCachedDataByRequest_ = function(
    data, request, filterFunc) {
  var result = {};
  for (var i = 0; i < request['ids'].length; i++) {
    var id = request['ids'][i];
    var cachedData = data[id];
    if (filterFunc(cachedData)) {
      result[id] = cachedData;
    }
  }
  return result;
};


/**
 * @return {string} Best-guess locale for current browser.
 * @private
 */
osapi.container.Service.prototype.getLocale_ = function() {
  var nav = window.navigator;
  return nav.userLanguage || nav.systemLanguage || nav.language;
};


/**
 * A callback function that will return the correct language locale part to use when
 * asking the server to render a gadget or when asking the server for 1 or more
 * gadget's metadata.
 * <br>
 * May be overridden by passing in a config parameter during container construction.
 *  * @return {string} Language locale part.
 */
osapi.container.Service.prototype.getLanguage = function() {
  try {
    return this.getLocale_().split('-')[0] || 'ALL';
  } catch (e) {
    return 'ALL';
  }
};


/**
 * A callback function that will return the correct country locale part to use when
 * asking the server to render a gadget or when asking the server for 1 or more
 * gadget's metadata.
 * <br>
 * May be overridden by passing in a config parameter during container construction.
 * @return {string} Country locale part.
 */
osapi.container.Service.prototype.getCountry = function() {
  try {
    return this.getLocale_().split('-')[1] || 'ALL';
  } catch (e) {
    return 'ALL';
  }
};


// -----------------------------------------------------------------------------
// Configuration
// -----------------------------------------------------------------------------


/**
 * Enumeration of configuration keys for this service. This is specified in
 * JSON to provide extensible configuration.
 * @enum {string}
 */
osapi.container.ServiceConfig = {};

/**
 * Host to fetch gadget information, via XHR.
 * @type {string}
 * @const
 */
osapi.container.ServiceConfig.API_HOST = 'apiHost';

/**
 * Path to fetch gadget information, via XHR.
 * @type {string}
 * @const
 */
osapi.container.ServiceConfig.API_PATH = 'apiPath';

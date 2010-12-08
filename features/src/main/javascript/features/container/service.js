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
shindig.container.Service = function(opt_config) {
  var config = opt_config || {};

  /**
   * @type {string}
   * @private
   */
  this.apiHost_ = String(shindig.container.util.getSafeJsonValue(config,
      shindig.container.ServiceConfig.API_HOST, window.__API_URI.getOrigin()));

  /**
   * @type {string}
   * @private
   */
  this.apiPath_ = String(shindig.container.util.getSafeJsonValue(config,
      shindig.container.ServiceConfig.API_PATH, '/api/rpc/cs'));

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

  this.registerOsapiServices();

  this.onConstructed(config);
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 * @param {Object=} opt_config Configuration JSON.
 */
shindig.container.Service.prototype.onConstructed = function(opt_config) {};


/**
 * Return a possibly-cached gadgets metadata for gadgets in request.ids, for
 * container request.container. If metadata is not cache, fetch from server
 * only for the uncached gadget URLs. The optional callback opt_callback will be
 * called, after a response is received.
 * @param {Object} request JSON object representing the request.
 * @param {function(Object)=} opt_callback function to call upon data receive.
 */
shindig.container.Service.prototype.getGadgetMetadata = function(
    request, opt_callback) {
  // TODO: come up with an expiration mechanism to evict cached gadgets.
  // Can be based on renderParam['nocache']. Be careful with preloaded and
  // arbitrarily-navigated gadgets. The former should be indefinite, unless
  // unloaded. The later can done without user knowing.
  var callback = opt_callback || function() {};

  var uncachedUrls = shindig.container.util.toArrayOfJsonKeys(
      this.getUncachedDataByRequest_(this.cachedMetadatas_, request));
  var finalResponse = this.getCachedDataByRequest_(this.cachedMetadatas_, request);

  // If fully cached, return from cache.
  if (uncachedUrls.length == 0) {
    callback(finalResponse);

  // Otherwise, request for uncached metadatas.
  } else {
    var self = this;
    request = shindig.container.util.newMetadataRequest(uncachedUrls);
    osapi.gadgets.metadata(request).execute(function(response) {

      // If response entirely fails, augment individual errors.
      if (response.error) {
        for (var i = 0; i < request.ids.length; i++) {
          finalResponse[id] = { 'error' : response.error };
        }

      // Otherwise, cache response. Augment final response with server response.
      } else {
        var currentTimeMs = shindig.container.util.getCurrentTimeMs();
        for (var id in response) {
          var resp = response[id]; 
          resp[shindig.container.MetadataParam.URL] = id;
          
          // This ignores time to fetch metadata. Okay, expect to be < 2s.
          resp[shindig.container.MetadataParam.LOCAL_EXPIRE_TIME]
              = resp[shindig.container.MetadataResponse.EXPIRE_TIME_MS]
              - resp[shindig.container.MetadataResponse.RESPONSE_TIME_MS]
              + currentTimeMs;
          self.cachedMetadatas_[id] = resp;
          finalResponse[id] = resp;
        }
      }

      callback(finalResponse);
    });
  }
};


/**
 * @param {Object} request JSON object representing the request.
 * @param {function(Object)=} opt_callback function to call upon data receive.
 */
shindig.container.Service.prototype.getGadgetToken = function(
    request, opt_callback) {
  var callback = opt_callback || function() {};

  // Do not check against cache. Always do a server fetch.
  var self = this;
  osapi.gadgets.token(request).execute(function(response) {
    var finalResponse = {};

    // If response entirely fails, augment individual errors.
    if (response.error) {
      for (var i = 0; i < request.ids.length; i++) {
        finalResponse[id] = { 'error' : response.error };
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
shindig.container.Service.prototype.getCachedGadgetMetadata = function(url) {
  return this.cachedMetadatas_[url];
};


/**
 * @param {string} url gadget URL to use as key to get cached token.
 * @return {string} the tokenInfo referenced by this URL.
 */
shindig.container.Service.prototype.getCachedGadgetToken = function(url) {
  return this.cachedTokens_[url];
};


/**
 * @param {Object} urls JSON containing gadget URLs to avoid removing.
 */
shindig.container.Service.prototype.uncacheStaleGadgetMetadataExcept = function(urls) {
  for (var url in this.cachedMetadatas_) {
    if (typeof urls[url] === 'undefined') {
      var gadgetInfo = this.cachedMetadatas_[url];
      if (gadgetInfo[shindig.container.MetadataParam.LOCAL_EXPIRE_TIME]
          < shindig.container.util.getCurrentTimeMs()) {
        delete this.cachedMetadatas_[url];
      }
    }
  }
};


/**
 * Initialize OSAPI endpoint methods/interfaces.
 */
shindig.container.Service.prototype.registerOsapiServices = function() {
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
shindig.container.Service.prototype.getCachedDataByRequest_ = function(
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
shindig.container.Service.prototype.getUncachedDataByRequest_ = function(
    cache, request) {
  return this.filterCachedDataByRequest_(cache, request,
      function(data) { return (typeof data === 'undefined') });
};


/**
 * Helper to filter out cached data 
 * @param {Object} cache JSON containing cached data.
 * @param {Object} request containing ids.
 * @param {Function} filterFunc function to filter result.
 * @return {Object} JSON containing requested and filtered entries.
 * @private
 */
shindig.container.Service.prototype.filterCachedDataByRequest_ = function(
    data, request, filterFunc) {
  var result = {};
  for (var i = 0; i < request.ids.length; i++) {
    var id = request.ids[i];
    var cachedData = data[id];
    if (filterFunc(cachedData)) {
      result[id] = cachedData;
    }
  }
  return result;
};


// -----------------------------------------------------------------------------
// Configuration
// -----------------------------------------------------------------------------


/**
 * Enumeration of configuration keys for this service. This is specified in
 * JSON to provide extensible configuration.
 * @enum {string}
 */
shindig.container.ServiceConfig = {};

/**
 * Host to fetch gadget information, via XHR.
 * @type {string}
 * @const
 */
shindig.container.ServiceConfig.API_HOST = 'apiHost';

/**
 * Path to fetch gadget information, via XHR.
 * @type {string}
 * @const
 */
shindig.container.ServiceConfig.API_PATH = 'apiPath';

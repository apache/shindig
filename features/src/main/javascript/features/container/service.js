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

  this.initializeOsapi_();

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
  var uncachedUrls = this.getUncachedUrls_(request, this.cachedMetadatas_);
  var finalResponse = this.getCachedData_(request, this.cachedMetadatas_);

  // If fully cached, return from cache.
  if (uncachedUrls.length == 0) {
    callback(finalResponse);

  // Otherwise, request for uncached metadatas.
  } else {
    var self = this;
    request = shindig.container.util.newMetadataRequest(uncachedUrls);
    osapi.gadgets.metadata.get(request).execute(function(response) {

      // If response entirely fails, augment individual errors.
      if (response.error) {
        for (var i = 0; i < request.ids.length; i++) {
          var id = request.ids[i];
          var message = [ 'Server failure to fetch metadata for gadget ', id, '.' ].join('');
          finalResponse[id] = { error : message };
        }

      // Otherwise, cache response. Augment final response with server response.
      } else {
        for (var id in response) {
          self.cachedMetadatas_[id] = response[id];
          finalResponse[id] = response[id];
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
  osapi.gadgets.token.get(request).execute(function(response) {
    var finalResponse = {};

    // If response entirely fails, augment individual errors.
    if (response.error) {
      for (var i = 0; i < request.ids.length; i++) {
        var id = request.ids[i];
        var message = [ 'Server failure to fetch token for gadget ' + id + '.' ].join('');
        finalResponse[id] = { error : message };
      }

    // Otherwise, cache response. Augment final response with server response.
    } else {
      for (var id in response) {
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
 * Initialize OSAPI endpoint methods/interfaces.
 * @private
 */
shindig.container.Service.prototype.initializeOsapi_ = function() {
  var endPoint = this.apiHost_ + this.apiPath_;

  var osapiServicesConfig = {};
  osapiServicesConfig['gadgets.rpc'] = ['container.listMethods'];
  osapiServicesConfig[endPoint] = [
    'gadgets.metadata.get',
    'gadgets.token.get'
  ];

  gadgets.config.init({
    'osapi': { 'endPoints': [endPoint] },
    'osapi.services': osapiServicesConfig
  });
};


/**
 * Filter cache with requested ids.
 * @param {Object} request containing ids.
 * @param {Object} cache JSON containing cached data.
 * @return {Object} JSON containing requested and cached entries.
 * @private
 */
shindig.container.Service.prototype.getCachedData_ = function(request, cache) {
  var result = {};
  for (var i = 0; i < request.ids.length; i++) {
    var id = request.ids[i];
    if (cache[id]) {
      result[id] = cache[id];
    }
  }
  return result;
};


/**
 * Extract ids in request not in cache.
 * @param {Object} request containing ids.
 * @param {Object} cache JSON containing cached data.
 * @return {Array.<string>} keys in the json.
 * @private
 */
shindig.container.Service.prototype.getUncachedUrls_ = function(request, cache) {
  var result = [];
  for (var i = 0; i < request.ids.length; i++) {
    var id = request.ids[i];
    if (!cache[id]) {
      result.push(id);
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

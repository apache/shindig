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
 * @param {osapi.container.Container} The container that this service services.
 * @constructor
 */
osapi.container.Service = function(container) {
  /**
   * The container that this service services.
   * @type {osapi.container.Container}
   * @private
   */
  this.container_ = container;

  var config = this.config_ = container.config_ || {};

  var injectedEndpoint = ((gadgets.config.get('osapi') || {}).endPoints ||
          [window.__API_URI.getOrigin() + '/rpc'])[0];
  var matches = /^([^\/]*\/\/[^\/]+)(.*)$/.exec(injectedEndpoint);
  /**
   * @type {string}
   * @private
   */
  this.apiHost_ = String(osapi.container.util.getSafeJsonValue(config,
      osapi.container.ServiceConfig.API_HOST, matches[1]));

  /**
   * @type {string}
   * @private
   */
  this.apiPath_ = String(osapi.container.util.getSafeJsonValue(config,
      osapi.container.ServiceConfig.API_PATH, matches[2]));

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
    this.updateContainerSecurityToken(function() {
      osapi['gadgets']['metadata'](request).execute(function(response) {
        // If response entirely fails, augment individual errors.
        if (response['error']) {
          for (var i = 0; i < request['ids'].length; i++) {
            var id = request['ids'][i];
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
osapi.container.Service.prototype.getGadgetToken = function(request, opt_callback) {
  var callback = opt_callback || function() {};

  // Do not check against cache. Always do a server fetch.
  var self = this;
  var tokenResponseCallback = function(response) {
    var finalResponse = {};

    // If response entirely fails, augment individual errors.
    if (response['error']) {
      for (var i = 0; i < request['ids'].length; i++) {
        finalResponse[request['ids'][i]] = { 'error' : response['error'] };
      }

    // Otherwise, cache response. Augment final response with server response.
    } else {
      for (var id in response) {
        var mid = response[osapi.container.TokenResponse.MODULE_ID],
            url = osapi.container.util.buildTokenRequestUrl(id, mid);

        //response[id]['url'] = id; // make sure url is set
        self.cachedTokens_[url] = response[id];
        finalResponse[id] = response[id];
      }
    }

    callback(finalResponse);
  };

  // If we have a custom token fetch function, call it -- otherwise use the default
  self.updateContainerSecurityToken(function() {
    if (self.config_[osapi.container.ContainerConfig.GET_GADGET_TOKEN]) {
      self.config_[osapi.container.ContainerConfig.GET_GADGET_TOKEN](request, tokenResponseCallback);
    } else {
      osapi['gadgets']['token'](request).execute(tokenResponseCallback);
    }
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

/**
 * Container Token Refresh
 */
(function() {
  var containerTimeout, lastRefresh, fetching,
      containerTokenTTL = 1800000 * 0.8, // 30 min default token ttl
      callbacks = [];


  function runCallbacks(callbacks) {
    while (callbacks.length) {
      callbacks.shift().call(null); // Window context
    }
  }

  function refresh(fetch_once) {
    fetching = true;
    if (containerTimeout) {
      clearTimeout(containerTimeout);
      containerTimeout = 0;
    }

    var fetch = fetch_once || this.config_[osapi.container.ContainerConfig.GET_CONTAINER_TOKEN];
    if (fetch) {
      var self = this;
      fetch(function(token, ttl) { // token and ttl may be undefined in the case of an error
        fetching = false;

        // Use last known ttl if there was an error
        containerTokenTTL = token ? (ttl * 1000 * 0.8) : containerTokenTTL;
        if (containerTokenTTL) {
          // Refresh again in 80% of the reported ttl
          // Pass null in to closure because FF behaves un-expectedly when that param is not explicitly provided.
          containerTimeout = setTimeout(gadgets.util.makeClosure(self, refresh, null), containerTokenTTL);
        }

        if (token) {
          // Looks like everything worked out...  let's update the token.
          shindig.auth.updateSecurityToken(token);
          lastRefresh =  osapi.container.util.getCurrentTimeMs();
          // And then run all the callbacks waiting for this.
          runCallbacks(callbacks);
        }
      });
    } else {
      fetching = false;
      // Fail gracefully, container supplied no fetch function. Do not hold on to callbacks.
      runCallbacks(callbacks);
    }
  }

  /**
   * @see osapi.container.Container.prototype.updateContainerSecurityToken
   */
  osapi.container.Service.prototype.updateContainerSecurityToken = function(callback, token, ttl) {
    var now = osapi.container.util.getCurrentTimeMs(),
        needsRefresh = containerTokenTTL &&
            (fetching || token || !lastRefresh || now > lastRefresh + containerTokenTTL);
    if (needsRefresh) {
      // Hard expire in 95% of originial ttl.
      var expired = !lastRefresh || now > lastRefresh + (containerTokenTTL * 95 / 80);
      if (!expired && callback) {
        // Token not expired, but needs refresh.  Don't block operations that need a valid token.
        callback();
      } else if (callback) {
        // We have a callback, there's either a fetch happening, or we otherwise need to refresh the
        // token.  Place it in the callbacks queue to be run after the fetch (currently running or
        // soon to be launched) completes.
        callbacks.push(callback);
      }

      if (token) {
        // We are trying to set a token initially.  Run refresh with a fetch_once function that simply
        // returns the canned values.  Then schedule the refresh using the function in the config
        refresh.call(this, function(result) {
          result(token, ttl);
        });
      } else if (!fetching) {
        // There's no fetch going on right now. We need to start one because the token needs a refresh
        refresh.call(this);
      }
    } else if (callback) {
      // No refresh needed, run the callback because the token is fine.
      callback();
    }
  };
})();
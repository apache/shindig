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
 * @fileoverview This represents the container for the current window or create
 * the container if none already exists.
 */


/**
 * @param {Object=} opt_config Configuration JSON.
 * @constructor
 */
osapi.container.Container = function(opt_config) {
  var config = this.config_ = opt_config || {};

  /**
   * A list of objects containing functions to be invoked when gadgets are
   * preloaded, navigated, closed or unloaded. Sample object:
   *
   * var callback = new Object();
   * callback[osapi.container.CallbackType.ON_PRELOADED]
   *            = function(response){};
   * callback[osapi.container.CallbackType.ON_CLOSED]
   *            = function(gadgetSite){};
   * callback[osapi.container.CallbackType.ON_NAVIGATED]
   *            = function(gadgetSite){};
   * callback[osapi.container.CallbackType.ON_UNLOADED]
   *            = function(gadgetURL){};
   * @type {Array}
   * @private
   */
  this.gadgetLifecycleCallbacks_ = {};

  /**
   * A JSON list of preloaded gadget URLs.
   * @type {Object}
   * @private
   */
  this.preloadedGadgetUrls_ = {};

  /**
   * @type {Object}
   * @private
   */
  this.sites_ = {};

  /**
   * @type {boolean}
   * @private
   */
  this.allowDefaultView_ = Boolean(
      osapi.container.util.getSafeJsonValue(config,
      osapi.container.ContainerConfig.ALLOW_DEFAULT_VIEW, true));

  /**
   * @type {boolean}
   * @private
   */
  this.renderCajole_ = Boolean(
      osapi.container.util.getSafeJsonValue(config,
      osapi.container.ContainerConfig.RENDER_CAJOLE, false));

  /**
   * @type {string}
   * @private
   */
  this.renderDebugParam_ = String(osapi.container.util.getSafeJsonValue(
      config, osapi.container.ContainerConfig.RENDER_DEBUG_PARAM,
      osapi.container.ContainerConfig.RENDER_DEBUG));

  /**
   * @type {boolean}
   * @private
   */
  var param = window.__CONTAINER_URI.getQP(this.renderDebugParam_);
  this.renderDebug_ = (typeof param === 'undefined') ?
      Boolean(osapi.container.util.getSafeJsonValue(config,
          osapi.container.ContainerConfig.RENDER_DEBUG, false)) :
      (param === '1');

  /**
   * @type {boolean}
   * @private
   */
  this.renderTest_ = Boolean(osapi.container.util.getSafeJsonValue(config,
      osapi.container.ContainerConfig.RENDER_TEST, false));

  /**
   * @see osapi.container.ContainerConfig.TOKEN_REFRESH_INTERVAL
   * @type {number}
   * @private
   */
  this.tokenRefreshInterval_ = Number(osapi.container.util.getSafeJsonValue(
      config, osapi.container.ContainerConfig.TOKEN_REFRESH_INTERVAL, 0));

  /**
   * The time of the last token refresh.
   * @type {number}
   * @private
   */
  this.lastRefresh_ = 0;

  /**
   * @type {number}
   * @private
   */
  this.navigateCallback_ = String(osapi.container.util.getSafeJsonValue(
      config, osapi.container.ContainerConfig.NAVIGATE_CALLBACK,
      null));

  /**
   * @type {osapi.container.Service}
   * @private
   */
  this.service_ = new osapi.container.Service(this);

  /**
   * result from calling window.setTimeout()
   * @type {?number}
   * @private
   */
  this.tokenRefreshTimer_ = null;

  var self = this;
  window[osapi.container.CallbackType.GADGET_ON_LOAD] = function(gadgetUrl, siteId){
      self.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_RENDER, gadgetUrl, siteId);
  };

  this.initializeMixins_();

  this.setupRpcArbitrator_(config);

  this.preloadCaches(config);

  this.registerRpcServices_();

  this.onConstructed(config);
};


/**
 * Create a new gadget site.
 * @param {Element} gadgetEl HTML element into which to render.
 * @param {Element=} opt_bufferEl Optional HTML element for double buffering.
 * @return {osapi.container.GadgetSite} site created for client to hold to.
 */
osapi.container.Container.prototype.newGadgetSite = function(
    gadgetEl, opt_bufferEl) {
  var bufferEl = opt_bufferEl || null;
  var site = new osapi.container.GadgetSite(this, this.service_, {
      'navigateCallback' : this.navigateCallback_,
      'gadgetEl' : gadgetEl,
      'bufferEl' : bufferEl,
      'gadgetOnLoad' : osapi.container.CallbackType.GADGET_ON_LOAD
  });
  this.sites_[site.getId()] = site;
  return site;
};


/**
 * Called when gadget is navigated.
 *
 * @param {osapi.container.GadgetSite} site destination gadget to navigate to.
 * @param {string} gadgetUrl The URI of the gadget.
 * @param {Object} viewParams view params for the gadget.
 * @param {Object} renderParams render parameters, including the view.
 * @param {function(Object)=} opt_callback Callback after gadget is loaded.
 */
osapi.container.Container.prototype.navigateGadget = function(
    site, gadgetUrl, viewParams, renderParams, opt_callback) {
  var callback = opt_callback || function() {},
    ContainerConfig = osapi.container.ContainerConfig,
    RenderParam = osapi.container.RenderParam;

  if (this.allowDefaultView_) {
    renderParams[RenderParam.ALLOW_DEFAULT_VIEW] = true;
  }
  if (this.renderCajole_) {
    renderParams[RenderParam.CAJOLE] = true;
  }
  if (this.renderDebug_) {
    renderParams[RenderParam.NO_CACHE] = true;
    renderParams[RenderParam.DEBUG] = true;
  }
  if (this.renderTest_) {
    renderParams[RenderParam.TEST_MODE] = true;
  }

  this.refreshService_();

  var
    self = this,
    finishNavigate = function(preferences) {
      renderParams[RenderParam.USER_PREFS] = preferences;
      self.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_BEFORE_NAVIGATE,
              gadgetUrl, site.getId());
      // TODO: Lifecycle, add ability for current gadget to cancel nav.
      site.navigateTo(gadgetUrl, viewParams, renderParams, function(gadgetInfo) {
        // TODO: Navigate to error screen on primary gadget load failure
        // TODO: Should display error without doing a standard navigate.
        // TODO: Bad if the error gadget fails to load.
        if (gadgetInfo.error) {
          gadgets.warn(['Failed to possibly schedule token refresh for gadget ',
              gadgetUrl, '.'].join(''));
        } else if (gadgetInfo[osapi.container.MetadataResponse.NEEDS_TOKEN_REFRESH]) {
          self.scheduleRefreshTokens_(gadgetInfo[osapi.container.MetadataResponse.TOKEN_TTL]);
        }

        self.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_NAVIGATED, site);
        callback(gadgetInfo);
      });
    };

  // Try to retrieve preferences for the gadget if no preferences were explicitly provided.
  if (this.config_[ContainerConfig.GET_PREFERENCES] && !renderParams[RenderParam.USER_PREFS]) {
    this.config_[ContainerConfig.GET_PREFERENCES](site.getId(), gadgetUrl, finishNavigate);
  }
  else {
    finishNavigate(renderParams[RenderParam.USER_PREFS]);
  }
};


/**
 * Called when gadget is closed. This may stop refreshing of tokens.
 * @param {osapi.container.GadgetSite} site navigate gadget to close.
 */
osapi.container.Container.prototype.closeGadget = function(site) {
  var id = site.getId();
  this.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_BEFORE_CLOSE, site);
  site.close();
  this.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_CLOSED, site);
  delete this.sites_[id];
  if (site instanceof osapi.container.GadgetSite) {
    this.unscheduleRefreshTokens_();
  }
};


/**
 * Add a callback to be called when one or more gadgets are preloaded,
 * navigated to or closed.
 *
 * @param {string} name name of the lifecycle callback.
 * @param {Object} lifeCycleCallback callback object to call back when a gadget is
 *     preloaded, navigated to or closed.  called via preloaded, navigated
 *     and closed methods.
 *
 * @return {boolean} true if added successfully, false if a callback
 *     with that name is already registered.
 */
osapi.container.Container.prototype.addGadgetLifecycleCallback = function(name, lifeCycleCallback) {
  if (!this.gadgetLifecycleCallbacks_[name]) {
    this.gadgetLifecycleCallbacks_[name] = lifeCycleCallback;
    return true;
  }
  return false;
};

/**
 * remove a lifecycle callback previously registered with the container
 * @param {string} name callback object to be removed.
 */
osapi.container.Container.prototype.removeGadgetLifecycleCallback = function(name) {
  delete this.gadgetLifecycleCallbacks_[name];
};

/**
 * Pre-load one gadget metadata information. More details on preloadGadgets().
 * @param {string} gadgetUrl gadget URI to preload.
 * @param {function(Object)=} opt_callback function to call upon data receive.
 */
osapi.container.Container.prototype.preloadGadget = function(gadgetUrl, opt_callback) {
  this.preloadGadgets([gadgetUrl], opt_callback);
};


/**
 * Pre-load gadgets metadata information. This is done by priming the cache,
 * and making an immediate call to fetch metadata of gadgets fully specified at
 * gadgetUrls. This will not render, and do additional callback operations.
 * @param {Array} gadgetUrls gadgets URIs to preload.
 * @param {function(Object)=} opt_callback function to call upon data receive.
 */
osapi.container.Container.prototype.preloadGadgets = function(gadgetUrls, opt_callback) {
  var callback = opt_callback || function() {};
  var request = osapi.container.util.newMetadataRequest(gadgetUrls);
  var self = this;

  this.refreshService_();
  this.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_BEFORE_PRELOAD, gadgetUrls);
  this.service_.getGadgetMetadata(request, function(response) {
    self.addPreloadGadgets_(response);
    self.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_PRELOADED,
        response);
    callback(response);
  });
};


/**
 * Unload preloaded gadget. Makes future preload request possibly uncached.
 * @param {string} gadgetUrl gadget URI to unload.
 */
osapi.container.Container.prototype.unloadGadget = function(gadgetUrl) {
  this.unloadGadgets([gadgetUrl]);
};


/**
 * Unload preloaded gadgets. Makes future preload request possibly uncached.
 * @param {Array} gadgetUrls gadgets URIs to unload.
 */
osapi.container.Container.prototype.unloadGadgets = function(gadgetUrls) {
  for (var i = 0; i < gadgetUrls.length; i++) {
    var url = gadgetUrls[i];
    this.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_BEFORE_UNLOAD,
            url);
    delete this.preloadedGadgetUrls_[url];
    this.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_UNLOADED,
        url);
  }
};


/**
 * Fetch the gadget metadata commonly used by container for user preferences.
 * @param {string} gadgetUrl gadgets URI to fetch metadata for. to preload.
 * @param {function(Object)} callback Function called with gadget metadata.
 */
osapi.container.Container.prototype.getGadgetMetadata = function(
    gadgetUrl, callback) {
  var request = osapi.container.util.newMetadataRequest([gadgetUrl]);

  this.refreshService_();
  this.service_.getGadgetMetadata(request, callback);
};


/**
 * @param {string} service name of RPC service to register.
 * @param {Function} callback post-RPC function to call, with RPC-related
 *                   arguments (with the calling GadgetSite augmented) and the
 *                   callback response itself.
 */
osapi.container.Container.prototype.rpcRegister = function(service, callback) {
  var self = this;
  gadgets.rpc.register(service, function() {
    // this['f'] is set by calling iframe via gadgets.rpc.
    this[osapi.container.GadgetSite.RPC_ARG_KEY] =
        self.getGadgetSiteByIframeId_(this['f']);
    var argsCopy = [this];
    for (var i = 0; i < arguments.length; ++i) {
      argsCopy.push(arguments[i]);
    }
    return callback.apply(self, argsCopy);
  });
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 * @param {Object=} opt_config Configuration JSON.
 */
osapi.container.Container.prototype.onConstructed = function(opt_config) {};


/**
 * Adds a new namespace to the Container object.  The namespace
 * will contain the result of calling the function passed in.
 *
 * @param {string} namespace the namespace to add.
 * @param {function} func to call when creating the namespace.
 */
osapi.container.Container.addMixin = function(namespace, func) {
   osapi.container.Container.prototype.mixins_[namespace] = func;
};


// -----------------------------------------------------------------------------
// Private variables and methods.
// -----------------------------------------------------------------------------


/**
 * Adds the ability for features to extend the container with
 * their own functionality that may be specific to that feature.
 * @type {Object<string,function>}
 * @private
 */
osapi.container.Container.prototype.mixins_ = {};


/**
 * Called from the constructor to add any namespace extensions.
 * @private
 */
osapi.container.Container.prototype.initializeMixins_ = function() {
  for (var i in this.mixins_) {
    this[i] = new this.mixins_[i](this);
  }
};


/**
 * Add list of gadgets to preload list
 * @param {Object} response hash of gadgets data.
 * @private
 */
osapi.container.Container.prototype.addPreloadGadgets_ = function(response) {
  for (var id in response) {
    if (response[id].error) {
      gadgets.warn(['Failed to preload gadget ', id, '.'].join(''));
    } else {
      this.addPreloadedGadgetUrl_(id);
      if (response[id][osapi.container.MetadataResponse.NEEDS_TOKEN_REFRESH]) {
        // Safe to re-schedule many times.
        this.scheduleRefreshTokens_(response[id][osapi.container.MetadataResponse.TOKEN_TTL]);
      }
    }
  }
};


/**
 * Preload gadget metadata and tokens to avoid the need for XHR's when navigating gadget sites.
 * This function is safe to call repeatedly if needed to incrementally build up the internal caches.
 * Support caching by providing server time to override response time usage.
 * @param {Object} preloadData object containing data to be preloaded.
 */
osapi.container.Container.prototype.preloadCaches = function(preloadData) {
  var gadgets = osapi.container.util.getSafeJsonValue(
      preloadData, osapi.container.ContainerConfig.PRELOAD_METADATAS, {});
  var tokens = osapi.container.util.getSafeJsonValue(
      preloadData, osapi.container.ContainerConfig.PRELOAD_TOKENS, {});
  var refTime = osapi.container.util.getSafeJsonValue(
      preloadData, osapi.container.ContainerConfig.PRELOAD_REF_TIME, null);

  this.service_.addGadgetMetadatas(gadgets, refTime);
  this.service_.addGadgetTokens(tokens, refTime);
  this.addPreloadGadgets_(gadgets);
};


/**
 * Deletes stale cached data in service. The container knows what data are safe
 * to be marked for deletion.
 * @private
 */
osapi.container.Container.prototype.refreshService_ = function() {
  var urls = this.getActiveGadgetUrls_();
  this.service_.uncacheStaleGadgetMetadataExcept(urls);
  // TODO: also uncache stale gadget tokens.
};


/**
 * @param {string} iframeId Iframe ID of gadget holder contained in the gadget
 *     site to get.
 * @return {osapi.container.GadgetSite} The gadget site.
 * @private
 */
osapi.container.Container.prototype.getGadgetSiteByIframeId_ = function(iframeId) {
  // TODO: Support getting only the loading/active gadget in 2x buffers.
  for (var siteId in this.sites_) {
    var site = this.sites_[siteId];
    var holder = site.getActiveSiteHolder();
    if (holder && holder.getIframeId() === iframeId) {
      return site;
    }
  }
  return null;
};

/**
 * @param {string} siteId ID of gadget site to get.
 * @return {osapi.container.GadgetSite|osapi.container.UrlSite} The gadget site.
 */
osapi.container.Container.prototype.getSiteById = function(siteId) {
  return this.sites_[siteId];
};

/**
 * Update and schedule refreshing of container token.  This function will use the config function
 * osapi.container.ContainerConfig.GET_CONTAINER_TOKEN to fetch a container token, if needed,
 * unless the token is specified in the optional parameter, in which case the token will be
 * updated with the provided value immediately.
 *
 * @param {function=} callback Function to run when container token is valid.
 * @param {String=} token The containers new security token.
 * @param {number=} ttl The token's ttl in seconds. If token is specified and ttl is 0,
 *   token refresh will be disabled.
 * @see osapi.container.ContainerConfig.GET_CONTAINER_TOKEN (constants.js)
 */
osapi.container.Container.prototype.updateContainerSecurityToken = function(callback, token, ttl) {
  this.service_.updateContainerSecurityToken(callback, token, ttl);
}

/**
 * Start to schedule refreshing of tokens.
 * @param {number} Encountered token time to live in seconds.
 * @private
 */
osapi.container.Container.prototype.scheduleRefreshTokens_ = function(tokenTTL) {
  var self = this,
      oldInterval = this.tokenRefreshInterval_,
      newInterval = tokenTTL ? this.setRefreshTokenInterval_(tokenTTL * 1000) : oldInterval,
      refresh = function() {
        self.updateContainerSecurityToken(function() {
          self.lastRefresh_ = osapi.container.util.getCurrentTimeMs();
          // Schedule the next refresh.
          self.tokenRefreshTimer_ = setTimeout(refresh, newInterval);

          // Do this last so that if it ever errors, we maintain the refresh schedule.
          self.refreshTokens_();
        });
      };

  // If enabled, check to see if we no schedule or if the two intervals are different and update the schedule.
  if (this.isRefreshTokensEnabled_() && (!this.tokenRefreshTimer_ || newInterval != oldInterval)) {
    var now = osapi.container.util.getCurrentTimeMs();
    if (!this.tokenRefreshTimer_) {
      this.lastRefresh_ = now;
      this.tokenRefreshTimer_ = setTimeout(refresh, newInterval);
    }
    else {
      var futureRefresh = (this.lastRefresh_ || 0) + oldInterval;
      if (futureRefresh < now) {
        // This really shouldn't happen, but if for some reason we missed a
        // refresh, make sure we cancel any timer we have and schedule
        // a new one.
        futureRefresh = now + newInterval;
        newInterval = 1;
      }
      if (futureRefresh > now + newInterval) {
        // Cancel the old timer and create a new one if the next refresh is
        // too far away.
        clearTimeout(this.tokenRefreshTimer_);
        this.tokenRefreshTimer_ = setTimeout(refresh, newInterval);
      }
    }
  }
};

/**
 * Stop already-scheduled refreshing of tokens.
 * @private
 */
osapi.container.Container.prototype.unscheduleRefreshTokens_ = function() {
  if (this.tokenRefreshTimer_) {
    var urls = this.getTokenRefreshableGadgetUrls_();
    if (urls.length <= 0) {
      clearTimeout(this.tokenRefreshTimer_);
      this.tokenRefreshTimer_ = null;
    }
  }
};


/**
 * Token refresh gets enabled if the value of refresh interval is > 0;
 *
 * @return {Boolean} if token refresh interval is of valid value.
 * @private
 */
osapi.container.Container.prototype.isRefreshTokensEnabled_ = function() {
  return this.tokenRefreshInterval_ > 0;
};

/**
 * If the refresh interval is < 0, does nothing.  Otherwise updates the tokenTTL
 * to the smallest value encountered.
 *
 * @param {number} Encountered token time to live in milliseconds.
 * @return {Boolean} The ttl if the set succeeded, otherwise false.
 * @private
 */
osapi.container.Container.prototype.setRefreshTokenInterval_ = function(tokenTTL) {
  // TODO: Handle the case where we've closed the gadget responsible for the
  // shortest refresh time, and can now safely extend this.tokenRefreshInterval_
  if (tokenTTL) {
    tokenTTL *= .8; // 80% of the TTL value, for buffer.
    var refresh = this.tokenRefreshInterval_;
    if (refresh < 0) {
      return refresh;
    }
    else {
      return this.tokenRefreshInterval_ =
        refresh == 0 ? tokenTTL : Math.min(refresh, tokenTTL);
    }
  }
};


/**
 * Register standard RPC services
 * @private
 */
osapi.container.Container.prototype.registerRpcServices_ = function() {
  var self = this;

  this.rpcRegister('resize_iframe', function(rpcArgs, data) {
    var site = rpcArgs[osapi.container.GadgetSite.RPC_ARG_KEY];
    if (site) { // Check if site is not already closed.
      site.setHeight(data);
    }
  });

  this.rpcRegister('resize_iframe_width', function(rpcArgs, newWidth) {
    var site = rpcArgs[osapi.container.GadgetSite.RPC_ARG_KEY];
    if (site) { // Check if site is not already closed.
      site.setWidth(newWidth);
    }
    return true;
  });

  /**
   * @see setprefs.js setprefs feature.
   */
  this.rpcRegister('set_pref', function(rpcArgs, key, value) {
    var site = rpcArgs[osapi.container.GadgetSite.RPC_ARG_KEY];
    var setPrefs = self.config_[osapi.container.ContainerConfig.SET_PREFERENCES];
    if (site && setPrefs) { // Check if site is not already closed.
      var data = {};
      for (var i = 2, j = arguments.length; i < j; i += 2) {
        data[arguments[i]] = arguments[i + 1];
      }
      setPrefs(site.getId(), site.getActiveSiteHolder().getUrl(), data);
    }
  });
};

/**
 * Sets up the RPC arbitrator if enabled in the container js.  If
 * a function is provided in the containers config the container will use
 * that, if not it will use the default arbitrator.
 * @private
 */
osapi.container.Container.prototype.setupRpcArbitrator_ = function(config) {
  var container = gadgets.config.get('container');
  if(typeof container.enableRpcArbitration !== 'undefined' &&
          container.enableRpcArbitration) {
    var arbitrate = osapi.container.util.getSafeJsonValue(
            config, osapi.container.ContainerConfig.RPC_ARBITRATOR, null);
    if(!arbitrate) {
      var self = this;
      //This implementation uses the metadata cache to check for allowed rpc service ids
      arbitrate = function(serviceId, from) {
        var site = self.getGadgetSiteByIframeId_(from);
        if(site && site.getActiveSiteHolder()) {
          var cachedResponse = self.service_.getCachedGadgetMetadata(
                  site.getActiveSiteHolder().getUrl());
          if(!cachedResponse.error && cachedResponse.rpcServiceIds) {
            for(var i = 0, rpcServiceId; rpcServiceId = cachedResponse.rpcServiceIds[i]; i++) {
              if(rpcServiceId == serviceId) {
                return true;
              }
            }
          }
        }
        gadgets.warn('RPC call to ' + serviceId + ' was not allowed.');
        return false;
      };
    }
    gadgets.rpc.config({'arbitrator' : arbitrate});
  }
};


/**
 * Keep track of preloaded gadget URLs. These gadgets will have their tokens
 * refreshed as part of batched token fetch.
 * @param {string} gadgetUrl URL of preloaded gadget.
 * @private
 */
osapi.container.Container.prototype.addPreloadedGadgetUrl_ = function(gadgetUrl) {
  this.preloadedGadgetUrls_[gadgetUrl] = null;
};


/**
 * Collect all URLs of gadgets that require tokens refresh. This comes from both
 * preloaded gadgets and navigated-to gadgets.
 * @return {Array} An array of URLs of gadgets.
 * @private
 */
// TODO: this function needs to be renamed, perhaps: getTokenRequestUrls_
osapi.container.Container.prototype.getTokenRefreshableGadgetUrls_ = function() {
  var result = {};

  for (var url in this.getActiveGadgetUrls_()) {
    var metadata = this.service_.getCachedGadgetMetadata(url);
    if (metadata[osapi.container.MetadataResponse.NEEDS_TOKEN_REFRESH]) {
      result[url] = 1;
    }
  }

  /* Now add all gadget site urls that have moduleIds
   *
   * We're basically refreshing a security token for any given
   * non-persisted (no moduleId) navigated or preloaded gadget instance, as
   * well as each persisted instance (each moduleId) for any given gadgetUrl.
   *
   * In other words, if we've got a gadget preloaded or navigated:
   *   http://foo.com/gadget.xml
   * We will refresh a token for non-persisted (no moduleId) instances of that
   * gadget.
   * If we've got a navigated persisted gadget on the page, we'll refresh that
   * security token as well.
   */
  for (var siteId in this.sites_) {
    var site = this.sites_[siteId];
    if (site instanceof osapi.container.GadgetSite) {
      var holder = site.getActiveSiteHolder();
      if (holder) {
        var url = holder.getUrl();
            mid = site.getModuleId();

        // If this gadget token does not require refresh
        // (baseurl is not already in result), don't add it.
        if (result[url]) {
          result[osapi.container.util.buildTokenRequestUrl(url, mid)] = 1;
        }
      }
    }
  }

  return osapi.container.util.toArrayOfJsonKeys(result);
};


/**
 * Get gadget urls that are either navigated or preloaded.
 * @return {Object} JSON of gadget URLs.
 * @private
 */
osapi.container.Container.prototype.getActiveGadgetUrls_ = function() {
  return osapi.container.util.mergeJsons(
      this.getNavigatedGadgetUrls_(),
      this.preloadedGadgetUrls_);
};


/**
 * Get gadget urls that are navigated on page.
 * @return {Object} JSON of gadget URLs.
 * @private
 */
osapi.container.Container.prototype.getNavigatedGadgetUrls_ = function() {
  var result = {};
  for (var siteId in this.sites_) {
    var site = this.sites_[siteId];
    if (site instanceof osapi.container.GadgetSite) {
      var holder = site.getActiveSiteHolder();
      if(holder) {
        result[holder.getUrl()] = 1;
      }
    }
  }
  return result;
};


/**
 * Refresh security tokens immediately. This will fetch gadget metadata, along
 * with its token and have the token cache updated.
 * @private
 */
osapi.container.Container.prototype.refreshTokens_ = function() {
  var ids = this.getTokenRefreshableGadgetUrls_();
  var request = osapi.container.util.newTokenRequest(ids);

  var self = this;
  this.service_.getGadgetToken(request, function(response) {
    // Update active token-requiring gadgets with new tokens. Do not need to
    // update pre-loaded gadgets, since new tokens will take effect when they
    // are navigated to, from cache.
    for (var siteId in self.sites_) {
      var site = self.sites_[siteId];
      if (site instanceof osapi.container.GadgetSite) {
        var holder = site.getActiveSiteHolder();
        var gadgetInfo = self.service_.getCachedGadgetMetadata(holder.getUrl());
        if (gadgetInfo[osapi.container.MetadataResponse.NEEDS_TOKEN_REFRESH]) {
          var mid = site.getModuleId(),
              url = osapi.container.util.buildTokenRequestUrl(holder.getUrl(), mid),
              tokenInfo = response[url];

          if (tokenInfo.error) {
            gadgets.warn(['Failed to get token for gadget ',
                url, '.'].join(''));
          } else {
            gadgets.rpc.call(holder.getIframeId(), 'update_security_token', null,
                tokenInfo[osapi.container.TokenResponse.TOKEN]);
          }
        }
      }
    }
  });
};


/**
 * invokes methods on the gadget lifecycle callback registered with the
 * container.  The callback will be passed the remainder of the arguments after methodName.
 * @param {string} methodName of the callback method to be called.
 * @private
 */
osapi.container.Container.prototype.applyLifecycleCallbacks_ = function(methodName) {
  var args = Array.prototype.slice.call(arguments, 1);
  for (name in this.gadgetLifecycleCallbacks_) {
    var method = this.gadgetLifecycleCallbacks_[name][methodName];
    if (method) {
      method.apply(null, args);
    }
  }
};

/**
 * Creates a new URL site
 * @param {Element} element the element to put the site in.
 * @return {osapi.container.UrlSite} a new site.
 */
osapi.container.Container.prototype.newUrlSite = function(element) {
  var args = {};
  args[osapi.container.UrlSite.URL_ELEMENT] = element;
  var site = new osapi.container.UrlSite(this, this.service_, args);
  this.sites_[site.getId()] = site;
  return site;
};


/**
 * Navigates to a URL.
 * @param {osapi.container.UrlSite} site the URL site to render the URL in.
 * @param {string} url the URL to render.
 * @param {Object} renderParams params to augment the rendering. Valid rendering parameters
 * include osapi.container.RenderParam.CLASS, osapi.container.RenderParam.HEIGHT,
 * and osapi.container.RenderParam.WIDTH.
 * @return {osapi.container.UrlSite} the site you passed in.
 *
 */
osapi.container.Container.prototype.navigateUrl = function(site, url, renderParams) {
  site.render(url, renderParams);
  return site;
};

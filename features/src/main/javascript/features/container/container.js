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
shindig.container.Container = function(opt_config) {
  var config = opt_config || {};

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
   * @type {string}
   * @private
   */
  this.renderDebugParam_ = String(shindig.container.util.getSafeJsonValue(
      config, shindig.container.ContainerConfig.RENDER_DEBUG_PARAM,
      shindig.container.ContainerConfig.RENDER_DEBUG));

  /**
   * @type {boolean}
   */
  var param = window.__CONTAINER_URI.getQP(this.renderDebugParam_);
  this.renderDebug_ = (typeof param === 'undefined')
      ? Boolean(shindig.container.util.getSafeJsonValue(config,
          shindig.container.ContainerConfig.RENDER_DEBUG, false))
      : (param === '1');

  /**
   * @type {boolean}
   * @private
   */
  this.renderTest_ = Boolean(shindig.container.util.getSafeJsonValue(config,
      shindig.container.ContainerConfig.RENDER_TEST, false));

  /**
   * Security token refresh interval (in ms) for debugging.
   * @type {number}
   * @private
   */
  this.tokenRefreshInterval_ = Number(shindig.container.util.getSafeJsonValue(
      config, shindig.container.ContainerConfig.TOKEN_REFRESH_INTERVAL,
      30 * 60 * 1000));

  /**
   * @type {shindig.container.Service}
   * @private
   */
  this.service_ = new shindig.container.Service(config);

  /**
   * result from calling window.setInterval()
   * @type {?number}
   * @private
   */
  this.tokenRefreshTimer_ = null;

  this.registerRpcServices_();

  this.onConstructed(config);
};


/**
 * Create a new gadget site.
 * @param {Element} gadgetEl HTML element into which to render.
 * @param {Element=} opt_bufferEl Optional HTML element for double buffering.
 * @return {shindig.container.GadgetSite} site created for client to hold to.
 */
shindig.container.Container.prototype.newGadgetSite = function(
    gadgetEl, opt_bufferEl) {
  var site = new shindig.container.GadgetSite(
      this.service_, gadgetEl, opt_bufferEl);
  this.sites_[site.getId()] = site;
  return site;
};


/**
 * @param {string} id Iframe ID of gadget site to get.
 * @return {shindig.container.GadgetSite} The gadget site with the given id.
 */
shindig.container.Container.prototype.getGadgetSite = function(id) {
  // TODO: Support getting only the loading/active gadget in 2x buffers.
  for (var siteId in this.sites_) {
    var site = this.sites_[siteId];
    if (site.getGadgetHolder(id)) {
      return site;
    }
  }
  return null;
};


/**
 * @param {string} id Iframe ID of gadget holder to get.
 * @return {shindig.container.GadgetHolder} The gadget holder with iframe id.
 */
shindig.container.Container.prototype.getGadgetHolder = function(id) {
  var site = this.getGadgetSite(id);
  if (site) {
    return site.getGadgetHolder(id);
  } else {
    return null;
  }
};


/**
 * Called when gadget is navigated.
 *
 * @param {shindig.container.GadgetSite} site destination gadget to navigate to.
 * @param {string} gadgetUrl The URI of the gadget.
 * @param {Object} viewParams view params for the gadget.
 * @param {Object} renderParams render parameters, including the view.
 * @param {function(Object)=} opt_callback Callback after gadget is loaded.
 */
shindig.container.Container.prototype.navigateGadget = function(
    site, gadgetUrl, viewParams, renderParams, opt_callback) {
  var callback = opt_callback || function() {};
  if (this.renderDebug_) {
    renderParams['nocache'] = true;
    renderParams['debug'] = true;
  }
  if (this.renderTest_) {
    renderParams['testmode'] = true;
  }

  var self = this;
  // TODO: Lifecycle, add ability for current gadget to cancel nav.
  site.navigateTo(gadgetUrl, viewParams, renderParams, function(gadgetInfo) {
    // TODO: Navigate to error screen on primary gadget load failure
    // TODO: Should display error without doing a standard navigate.
    // TODO: Bad if the error gadget fails to load.
    if (gadgetInfo[shindig.container.MetadataResponse.NEEDS_TOKEN_REFRESH]) {
      self.scheduleRefreshTokens_();
    }
    callback(gadgetInfo);
  });
};


/**
 * Called when gadget is closed. This may stop refreshing of tokens.
 * @param {shindig.container.GadgetSite} site navigate gadget to close.
 */
shindig.container.Container.prototype.closeGadget = function(site) {
  var id = site.getId();
  site.close();
  delete this.sites_[id];
  this.unscheduleRefreshTokens_();
};


/**
 * Pre-load one gadget metadata information. More details on preloadGadgets().
 * @param {string} gadgetUrl gadget URI to preload.
 */
shindig.container.Container.prototype.preloadGadget = function(gadgetUrl) {
  this.preloadGadgets([gadgetUrl]);
};


/**
 * Pre-load gadgets metadata information. This is done by priming the cache,
 * and making an immediate call to fetch metadata of gadgets fully specified at
 * gadgetUrls. This will not render, and do additional callback operations.
 * @param {Array} gadgetUrls gadgets URIs to preload.
 */
shindig.container.Container.prototype.preloadGadgets = function(gadgetUrls) {
  var request = shindig.container.util.newMetadataRequest(gadgetUrls);
  var self = this;
  this.service_.getGadgetMetadata(request, function(response) {
    if (!response.error) {
      for (var id in response) {
        self.addPreloadedGadgetUrl_(id);
        if (response[id][shindig.container.MetadatResponse.NEEDS_TOKEN_REFRESH]) {
          // Safe to re-schedule many times.
          self.scheduleRefreshTokens_();
        }
      }
    }
  });
};


/**
 * Fetch the gadget metadata commonly used by container for user preferences.
 * @param {string} gadgetUrl gadgets URI to fetch metadata for. to preload.
 * @param {function(Object)} callback Function called with gadget metadata.
 */
shindig.container.Container.prototype.getGadgetMetadata = function(
    gadgetUrl, callback) {
  var request = shindig.container.util.newMetadataRequest([gadgetUrl]);
  this.service_.getGadgetMetadata(request, callback);
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 * @param {Object=} opt_config Configuration JSON.
 */
shindig.container.Container.prototype.onConstructed = function(opt_config) {};


// -----------------------------------------------------------------------------
// Valid JSON keys.
// -----------------------------------------------------------------------------

/**
 * Enumeration of configuration keys for this container. This is specified in
 * JSON to provide extensible configuration. These enum values are for
 * documentation purposes only, it is expected that clients use the string
 * values.
 * @enum {string}
 */
shindig.container.ContainerConfig = {};
/**
 * Whether debug mode is turned on.
 * @type {string}
 * @const
 */
shindig.container.ContainerConfig.RENDER_DEBUG = 'renderDebug';
/**
 * The debug param name to look for in container URL for per-request debugging.
 * @type {string}
 * @const
 */
shindig.container.ContainerConfig.RENDER_DEBUG_PARAM = 'renderDebugParam';
/**
 * Whether test mode is turned on.
 * @type {string}
 * @const
 */
shindig.container.ContainerConfig.RENDER_TEST = 'renderTest';
/**
 * Security token refresh interval (in ms) for debugging.
 * @type {string}
 * @const
 */
shindig.container.ContainerConfig.TOKEN_REFRESH_INTERVAL = 'tokenRefreshInterval';


/**
 * Enum keys for gadget rendering params. Gadget rendering params affect which
 * view of a gadget of displayed and how the gadget site is rendered, and are
 * not passed on to the actual gadget. These enum values are for documentation
 * purposes only, it is expected that clients use the string values.
 * @enum {string}
 */
shindig.container.ContainerRender = {};
/**
 * Style class to associate to iframe.
 * @type {string}
 * @const
 */
shindig.container.ContainerRender.CLASS = 'class';
/**
 * Whether to turn off debugging.
 * @type {string}
 * @const
 */
shindig.container.ContainerRender.DEBUG = 'debug';
/**
 * The starting/default gadget iframe height (in pixels).
 * @type {string}
 * @const
 */
shindig.container.ContainerRender.HEIGHT = 'height';
/**
 * Whether to turn off debugging.
 * @type {string}
 * @const
 */
shindig.container.ContainerRender.TEST = 'test';
/**
 * The gadget view name.
 * @type {string}
 * @const
 */
shindig.container.ContainerRender.VIEW = 'view';
/**
 * The starting/default gadget iframe width (in pixels).
 * @type {string}
 * @const
 */
shindig.container.ContainerRender.WIDTH = 'width';


// -----------------------------------------------------------------------------
// Private variables and methods.
// -----------------------------------------------------------------------------


/**
 * Start to schedule refreshing of tokens.
 * @private
 */
shindig.container.Container.prototype.scheduleRefreshTokens_ = function() {
  // TODO: Obtain the interval time by taking the minimum of expiry time of
  // token in all preloaded- and navigated-to- gadgets. This should be obtained
  // from the server. For now, constant on 50% of long-lived tokens (1 hour),
  // which is 30 minutes.
  if (this.isRefreshTokensEnabled_() && !this.tokenRefreshTimer_) {
    var self = this;
    this.tokenRefreshTimer_ = window.setInterval(function() {
      self.refreshTokens_();
    }, this.tokenRefreshInterval_);
  }
};


/**
 * Stop already-scheduled refreshing of tokens.
 * @private
 */
shindig.container.Container.prototype.unscheduleRefreshTokens_ = function() {
  if (this.tokenRefreshTimer_) {
    var urls = this.getTokenRefreshableGadgetUrls_();
    if (urls.length <= 0) {
      window.clearInterval(this.tokenRefreshTimer_);
      this.tokenRefreshTimer_ = null;
    }
  }
};


/**
 * Provides a manual override to disable token refresh to avoid gadgets.rpc
 * warning of service not found. We can do better to detect if token refresh is
 * even necessary, by inspecting the gadget transitively depend on
 * feature=security-token.
 * @return {Boolean} if token refresh interval is of valid value.
 * @private
 */
shindig.container.Container.prototype.isRefreshTokensEnabled_ = function() {
  return this.tokenRefreshInterval_ > 0;
};


/**
 * Register standard RPC services
 * @private
 */
shindig.container.Container.prototype.registerRpcServices_ = function() {
  var self = this;
  gadgets.rpc.register('resize_iframe', function(height) {
    // this['f'] is set by calling iframe via gadgets.rpc.
    var site = self.getGadgetSite(this['f']);
    site.setHeight(height);
  });
};


/**
 * Keep track of preloaded gadget URLs. These gadgets will have their tokens
 * refreshed as part of batched token fetch.
 * @param {string} gadgetUrl URL of preloaded gadget.
 * @private
 */
shindig.container.Container.prototype.addPreloadedGadgetUrl_ = function(
    gadgetUrl) {
  this.preloadedGadgetUrls_[gadgetUrl] = null;
};


/**
 * Collect all URLs of gadgets that require tokens refresh. This comes from both
 * preloaded gadgets and navigated-to gadgets.
 * @return {Array} An array of URLs of gadgets.
 * @private
 */
shindig.container.Container.prototype.getTokenRefreshableGadgetUrls_ =
    function() {
  var result = {};

  // Collect preloaded gadget urls.
  for (var url in this.preloadedGadgetUrls_) {
    var metadata = this.service_.getCachedGadgetMetadata(url);
    if (metadata[shindig.container.MetadataResponse.NEEDS_TOKEN_REFRESH]) {
      result[url] = null;
    }
  }

  // Collect active gadget urls.
  for (var siteIndex in this.sites_) {
    var holder = this.sites_[siteIndex].getActiveGadgetHolder();
    var url = holder.getUrl();
    var metadata = this.service_.getCachedGadgetMetadata(url);
    if (metadata[shindig.container.MetadataResponse.NEEDS_TOKEN_REFRESH]) {
      result[url] = null;
    }
  }

  return shindig.container.util.toArrayOfJsonKeys(result);
};



/**
 * Refresh security tokens immediately. This will fetch gadget metadata, along
 * with its token and have the token cache updated.
 * @private
 */
shindig.container.Container.prototype.refreshTokens_ = function() {
  var ids = this.getTokenRefreshableGadgetUrls_();
  var request = shindig.container.util.newTokenRequest(ids);

  var self = this;
  this.service_.getGadgetToken(request, function(response) {
    if (!response.error) {
      // Update active token-requiring gadgets with new tokens. Do not need to
      // update pre-loaded gadgets, since new tokens will take effect when they
      // are navigated to, from cache.
      for (var key in self.sites_) {
        var holder = self.sites_[key].getActiveGadgetHolder();
        var gadgetInfo = self.service_.getCachedGadgetMetadata(holder.getUrl());
        if (gadgetInfo[shindig.container.MetadataResponse.NEEDS_TOKEN_REFRESH]) {
          gadgets.rpc.call(holder.getIframeId(), 'update_security_token', null,
              response[holder.getUrl()][shindig.container.TokenResponse.TOKEN]);
        }
      }
    }
    // TODO: Tokens will be stale, but error should not be ignored.
  });
};

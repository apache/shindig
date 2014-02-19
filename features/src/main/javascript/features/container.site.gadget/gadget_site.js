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
 * @fileoverview This manages rendering of gadgets in a place holder, within an
 * HTML element in the container. The API for this is low-level. Use the
 * container APIs to work with gadget sites.
 */

/**
 * @param {osapi.container.Container} container The container that hosts this gadget site.
 * @param {osapi.container.Service} service The container's service.
 * @param {Object} args containing:
 *          {osapi.container.Service} service to fetch gadgets metadata, token.
 *          {string} navigateCallback name of callback function on navigateTo().
 *          {Element} gadgetEl Element into which to render the gadget.
 *          {Element} bufferEl Optional element for double buffering.
 * @constructor
 * @extends {osapi.container.Site}
 */
osapi.container.GadgetSite = function(container, service, args) {
  var undef;

  osapi.container.Site.call(this, container, service, args['gadgetEl']); // call super

  /**
   * @type {string}
   * @private
   */
  this.navigateCallback_ = args['navigateCallback'];

  /**
   * @type {Element}
   * @private
   */
  this.loadingGadgetEl_ = args['bufferEl'];

  /**
   * @type {string}
   * @private
   */
  this.gadgetOnLoad_ = args['gadgetOnLoad'];

  /**
   * Unique numeric module ID for this gadget instance.  A module id is used to
   * identify persisted instances of gadgets.
   *
   * @type {number}
   * @private
   */
  this.moduleId_ = 0;

  /**
   * Information about the currently visible gadget.
   * @type {osapi.container.GadgetHolder?}
   * @private
   */
  this.currentGadgetHolder_ = undef;

  /**
   * Information about the currently loading gadget.
   * @type {osapi.container.GadgetHolder?}
   * @private
   */
  this.loadingGadgetHolder_ = undef;

  this.onConstructed();
};

osapi.container.GadgetSite.prototype = new osapi.container.Site;

/**
 * @return {undefined|null|number} The numerical moduleId of this gadget, if
 *   set.  May return null or undefined if not set.
 */
osapi.container.GadgetSite.prototype.getModuleId = function() {
  return this.moduleId_;
};

/**
 * If you want to change the moduleId after a gadget has rendered, re-navigate the site.
 *
 * @param {string} url This gadget's url (may not yet be accessible in all cases from the holder).
 * @param {number} mid The numerical moduleId for this gadget to use.
 * @param {function} opt_callback Optional callback to run when the moduleId is set.
 * @private
 */
osapi.container.GadgetSite.prototype.setModuleId_ = function(url, mid, opt_callback) {
  if (mid && this.moduleId_ != mid) {
    var self = this,
        url = osapi.container.util.buildTokenRequestUrl(url, mid);

    if (!self.service_.getCachedGadgetToken(url)) {
      // We need to request a security token for this gadget instance.
      var request = osapi.container.util.newTokenRequest([url]);
      self.service_.getGadgetToken(request, function(response) {
        var ttl, mid;
        if (response && response[url]) {
          if (ttl = response[url][osapi.container.TokenResponse.TOKEN_TTL]) {
            self.container_.scheduleRefreshTokens_(ttl);
          }
          var mid = response[url][osapi.container.TokenResponse.MODULE_ID];
          if (mid || mid == 0) {
            self.moduleId_ = mid;
          }
        }
        if (opt_callback) {
          opt_callback();
        }
      });
      return;
    }
  }
  if (opt_callback) {
    opt_callback();
  }
};

/**
 * @inheritDoc
 */
osapi.container.GadgetSite.prototype.getActiveSiteHolder = function() {
  return this.loadingGadgetHolder_ || this.currentGadgetHolder_;
};

/**
 * @inheritDoc
 */
osapi.container.GadgetSite.prototype.setTitle = function(title) {
  osapi.container.Site.prototype.setTitle.call(this, title);
  // sometimes there are 2 holders
  if (this.loadingGadgetHolder_ && this.currentGadgetHolder_) {
    // loadingGadgetHolder_ was set by super call
    this.currentGadgetHolder_.setTitle(title); // set my other one.
  }
  return this;
};

/**
 * Returns configuration of a feature with a given name. Defaults to current
 * loading or visible gadget if no metadata is passed in.
 * @param {string} name Name of the feature.
 * @param {Object=} opt_gadgetInfo Optional gadget info.
 * @return {Object} JSON representing the feature.
 */
osapi.container.GadgetSite.prototype.getFeature = function(name, opt_gadgetInfo) {
  var gadgetInfo = opt_gadgetInfo || this.getActiveSiteHolder().getGadgetInfo();
  return gadgetInfo[osapi.container.MetadataResponse.FEATURES] &&
      gadgetInfo[osapi.container.MetadataResponse.FEATURES][name];
};

/**
 * Render a gadget in the site, by URI of the gadget XML.
 * @param {string} gadgetUrl The absolute URL to gadget.
 * @param {Object} viewParams Look at osapi.container.ViewParam.
 * @param {Object} renderParams Look at osapi.container.RenderParam.
 * @param {function(Object)=} opt_callback Function called with gadget info
 *     after navigation has occurred.
 */
osapi.container.GadgetSite.prototype.navigateTo = function(
    gadgetUrl, viewParams, renderParams, opt_callback) {
  var start = osapi.container.util.getCurrentTimeMs();
  var cached = this.service_.getCachedGadgetMetadata(gadgetUrl);
  var callback = opt_callback || function() {};
  var request = osapi.container.util.newMetadataRequest([gadgetUrl]);
  var self = this;

  this.service_.getGadgetMetadata(request, function(response) {
    var xrt = (!cached) ? (osapi.container.util.getCurrentTimeMs() - start) : 0;
    var gadgetInfo = response[gadgetUrl];
    if (gadgetInfo.error) {
      var message = ['Failed to navigate for gadget ', gadgetUrl, '.'].join('');
      gadgets.warn(message);

      message = ['Detailed error: ', gadgetInfo.error.code || '', ' ', gadgetInfo.error.message || ''].join('');
      gadgets.log(message);
    } else {
      var moduleId = renderParams[osapi.container.RenderParam.MODULE_ID] || 0;
      self.setModuleId_(gadgetUrl, moduleId, function() {
        self.container_.applyLifecycleCallbacks_(osapi.container.CallbackType.ON_BEFORE_RENDER,
                gadgetInfo);
        self.render(gadgetInfo, viewParams, renderParams);
      });
    }

    // Return metadata server response time.
    var timingInfo = {};
    timingInfo[osapi.container.NavigateTiming.URL] = gadgetUrl;
    timingInfo[osapi.container.NavigateTiming.ID] = self.id_;
    timingInfo[osapi.container.NavigateTiming.START] = start;
    timingInfo[osapi.container.NavigateTiming.XRT] = xrt;
    self.onNavigateTo(timingInfo);

    // Possibly with an error. Leave to user to deal with raw response.
    callback(gadgetInfo);
  });
};


/**
 * Provide overridable callback invoked when navigateTo is completed.
 * @param {Object} data the statistic/timing information to return.
 */
osapi.container.GadgetSite.prototype.onNavigateTo = function(data) {
  if (this.navigateCallback_) {
    var func = window[this.navigateCallback_];
    if (typeof func === 'function') {
      func(data);
    }
  }
};


/**
 * Render a gadget in this site, using a JSON gadget description.
 *
 * Note: A view provided in either renderParams or viewParams is subject to aliasing if the gadget
 * does not support the view specified.
 *
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {Object} viewParams Look at osapi.container.ViewParam.
 * @param {Object} renderParams Look at osapi.container.RenderParam.
 * @override
 */
osapi.container.GadgetSite.prototype.render = function(
    gadgetInfo, viewParams, renderParams) {
  var curUrl = this.currentGadgetHolder_ ? this.currentGadgetHolder_.getUrl() : null;

  var previousView = null;
  if (curUrl == gadgetInfo['url']) {
    previousView = this.currentGadgetHolder_.getView();
  }

  // Simple function to find a suitable alias
  var findAliasInfo = function(viewConf) {
    if (typeof viewConf !== 'undefined' && viewConf != null) {
      var aliases = viewConf['aliases'] || [];
      for (var i = 0; i < aliases.length; i++) {
        if (gadgetInfo[osapi.container.MetadataResponse.VIEWS][aliases[i]]) {
          return {'view':aliases[i],
                  'viewInfo':gadgetInfo[osapi.container.MetadataResponse.VIEWS][aliases[i]]};
        }
      }
    }
    return null;
  };

  // Find requested view.
  var view = renderParams[osapi.container.RenderParam.VIEW] ||
      viewParams[osapi.container.ViewParam.VIEW] ||
      previousView;
  var viewInfo = gadgetInfo[osapi.container.MetadataResponse.VIEWS][view];
  if (view && !viewInfo) {
    var aliasInfo = findAliasInfo(gadgets.config.get('views')[view]);
    if (aliasInfo) {
      view = aliasInfo['view'];
      viewInfo = aliasInfo['viewInfo'];
    }
  }

  // Allow default view if requested view is not found.  No sense doing this if the view is already "default".
  if (!viewInfo &&
          renderParams[osapi.container.RenderParam.ALLOW_DEFAULT_VIEW]  &&
          view != osapi.container.GadgetSite.DEFAULT_VIEW_) {
    view = osapi.container.GadgetSite.DEFAULT_VIEW_;
    viewInfo = gadgetInfo[osapi.container.MetadataResponse.VIEWS][view];
    if (!viewInfo) {
      var aliasInfo = findAliasInfo(gadgets.config.get('views')[view]);
      if (aliasInfo) {
        view = aliasInfo['view'];
        viewInfo = aliasInfo['viewInfo'];
      }
    }
  }

  // Check if view exists.
  if (!viewInfo) {
    gadgets.warn(['Unsupported view ', view, ' for gadget ', gadgetInfo['url'], '.'].join(''));
    return;
  }

  // Set the loading gadget holder:
  // 1. If the gadget site already has currentGadgetHolder_ set and no loading element passed,
  //    simply set the current gadget holder as the loading gadget holder.
  // 2. Else, check if caller pass the loading gadget element. If it does then use it to create new
  //    instance of osapi.container.GadgetHolder as the loading gadget holder.
  if (this.currentGadgetHolder_ && !this.loadingGadgetEl_) {
    this.loadingGadgetHolder_ = this.currentGadgetHolder_;
    this.currentGadgetHolder_ = null;
  }
  else {
    // Check if we are passed the loading gadget element
    var el = this.loadingGadgetEl_ || this.el_;
    this.loadingGadgetHolder_ = new osapi.container.GadgetHolder(this, el, this.gadgetOnLoad_);
  }

  var localRenderParams = {};
  for (var key in renderParams) {
    localRenderParams[key] = renderParams[key];
  }

  localRenderParams[osapi.container.RenderParam.VIEW] = view;
  localRenderParams[osapi.container.RenderParam.HEIGHT] =
      renderParams[osapi.container.RenderParam.HEIGHT] ||
      viewInfo[osapi.container.MetadataResponse.PREFERRED_HEIGHT] ||
      gadgetInfo[osapi.container.MetadataResponse.MODULE_PREFS][osapi.container.MetadataResponse.HEIGHT] ||
      String(osapi.container.GadgetSite.DEFAULT_HEIGHT_);
  localRenderParams[osapi.container.RenderParam.WIDTH] =
      renderParams[osapi.container.RenderParam.WIDTH] ||
      viewInfo[osapi.container.MetadataResponse.PREFERRED_WIDTH] ||
      gadgetInfo[osapi.container.MetadataResponse.MODULE_PREFS][osapi.container.MetadataResponse.WIDTH] ||
      String(osapi.container.GadgetSite.DEFAULT_WIDTH_);

  this.updateSecurityToken_(gadgetInfo, localRenderParams);

  this.loadingGadgetHolder_.render(gadgetInfo, viewParams, localRenderParams);
};


/**
 * Called when a gadget loads in the site. Uses double buffer, if present.
 */
osapi.container.GadgetSite.prototype.onRender = function() {
  this.swapBuffers_();

  // Only dispose the current holder set it to the loading holder if a loading holder exists.
  // This protects this method from re-entrant code that can cause the current holder to be
  // removed without a loading holder to take its place
  if (this.loadingGadgetHolder_) {
    if (this.currentGadgetHolder_) {
      this.currentGadgetHolder_.dispose();
    }

    this.currentGadgetHolder_ = this.loadingGadgetHolder_;
    this.loadingGadgetHolder_ = null;
  }
};


/**
 * Sends RPC call to the current/visible gadget.
 * @param {string} serviceName RPC service name to call.
 * @param {function(Object)} callback Function to call upon RPC completion.
 * @param {...number} var_args payload to pass to the recipient.
 */
osapi.container.GadgetSite.prototype.rpcCall = function(
    serviceName, callback, var_args) {
  if (this.currentGadgetHolder_) {
    gadgets.rpc.call(this.currentGadgetHolder_.getIframeId(),
        serviceName, callback, var_args);
  }
};


/**
 * If token has been fetched at least once, set the token to the most recent
 * one. Otherwise, leave it.
 * @param {Object} gadgetInfo The gadgetInfo used to update security token.
 * @param {Object} renderParams Look at osapi.container.RenderParam.
 * @private
 */
osapi.container.GadgetSite.prototype.updateSecurityToken_ = function(gadgetInfo, renderParams) {
  var url = osapi.container.util.buildTokenRequestUrl(gadgetInfo['url'], this.moduleId_),
    tokenInfo = this.service_.getCachedGadgetToken(url);

  if (tokenInfo) {
    var token = tokenInfo[osapi.container.TokenResponse.TOKEN];
    this.loadingGadgetHolder_.setSecurityToken(token);
  }
};

/**
 * @inheritDoc
 */
osapi.container.GadgetSite.prototype.close = function() {
  if (this.loadingGadgetHolder_) {
    this.loadingGadgetHolder_.dispose();
  }
  if (this.currentGadgetHolder_) {
    this.currentGadgetHolder_.dispose();
  }
};

/**
 * Swap the double buffer elements, if there is a double buffer.
 * @private
 */
osapi.container.GadgetSite.prototype.swapBuffers_ = function() {
  // Only process double buffering if loading gadget exists
  if (this.loadingGadgetEl_) {
    this.loadingGadgetEl_.style.left = '';
    this.loadingGadgetEl_.style.position = '';
    this.el_.style.position = 'absolute';
    this.el_.style.left = '-2000px';

    // Swap references;  cur_ will now again be what's visible
    var oldCur = this.el_;
    this.el_ = this.loadingGadgetEl_;
    this.loadingGadgetEl_ = oldCur;
  }
};


/**
 * Key to identify the calling gadget site.
 * @type {string}
 */
osapi.container.GadgetSite.RPC_ARG_KEY = 'gs';


/**
 * Default height of gadget. Refer to --
 * http://code.google.com/apis/gadgets/docs/legacy/reference.html.
 * @type {number}
 * @private
 */
osapi.container.GadgetSite.DEFAULT_HEIGHT_ = 200;


/**
 * Default width of gadget. Refer to --
 * http://code.google.com/apis/gadgets/docs/legacy/reference.html.
 * @type {number}
 * @private
 */
osapi.container.GadgetSite.DEFAULT_WIDTH_ = 320;


/**
 * Default view of gadget.
 * @type {string}
 * @private
 */
osapi.container.GadgetSite.DEFAULT_VIEW_ = 'default';

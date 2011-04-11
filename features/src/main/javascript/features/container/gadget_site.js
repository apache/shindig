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
 * @param {Object} args containing:
 *        {osapi.container.Service} service to fetch gadgets metadata, token.
 *        {string} navigateCallback name of callback function on navigateTo().
 *        {Element} gadgetEl Element into which to render the gadget.
 *        {Element} bufferEl Optional element for double buffering.
 * @constructor
 */
osapi.container.GadgetSite = function(args) {
  /**
   * @type {osapi.container.Service}
   * @private
   */
  this.service_ = args['service'];

  /**
   * @type {string}
   * @private
   */
  this.navigateCallback_ = args['navigateCallback'];

  /**
   * @type {Element}
   * @private
   */
  this.currentGadgetEl_ = args['gadgetEl'];

  /**
   * @type {Element}
   * @private
   */
  this.loadingGadgetEl_ = args['bufferEl'];

  /**
   * Unique ID of this site.
   * @type {number}
   * @private
   */
  this.id_ = osapi.container.GadgetSite.nextUniqueId_++;

  /**
   * ID of parent gadget.
   * @type {string?}
   * @private
   */
  this.parentId_ = null;

  /**
   * Information about the currently visible gadget.
   * @type {osapi.container.GadgetHolder?}
   * @private
   */
  this.currentGadgetHolder_ = null;

  /**
   * Information about the currently loading gadget.
   * @type {osapi.container.GadgetHolder?}
   * @private
   *
  this.loadingGadgWetHolder_ = null;

  this.onConstructed();
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 */
osapi.container.GadgetSite.prototype.onConstructed = function() {};


/**
 * Set the height of the gadget iframe.
 * @param {number} value The new height.
 * @return {osapi.container.GadgetSite} This instance.
 */
osapi.container.GadgetSite.prototype.setHeight = function(value) {
  var holder = this.getActiveGadgetHolder();
  if (holder) {
    var iframeEl = holder.getIframeElement();
    if (iframeEl) {
      iframeEl.style.height = value + 'px';
    }
  }
  return this;
};


/**
 * Set the width of the gadget iframe.
 * @param {number} value The new width.
 * @return {osapi.container.GadgetSite} This instance.
 */
osapi.container.GadgetSite.prototype.setWidth = function(value) {
  var holder = this.getActiveGadgetHolder();
  if (holder) {
    var iframeEl = holder.getIframeElement();
    if (iframeEl) {
      iframeEl.style.width = value + 'px';
    }
  }
  return this;
};


/**
 * @param {string} value ID of parent element containing this site.
 * @return {osapi.container.GadgetSite} This instance.
 */
osapi.container.GadgetSite.prototype.setParentId = function(value) {
  this.parentId_ = value;
  return this;
};


/**
 * @return {number} The ID of this gadget site.
 */
osapi.container.GadgetSite.prototype.getId = function() {
  return this.id_;
};


/**
 * Returns the currently-active gadget, the loading gadget if a gadget is
 * loading, or the currently visible gadget.
 * @return {osapi.container.GadgetHolder} The gadget holder.
 */
osapi.container.GadgetSite.prototype.getActiveGadgetHolder = function() {
  return this.loadingGadgetHolder_ || this.currentGadgetHolder_;
};


/**
 * Returns configuration of a feature with a given name. Defaults to current
 * loading or visible gadget if no metadata is passed in.
 * @param {string} name Name of the feature.
 * @param {Object=} opt_gadgetInfo Optional gadget info.
 * @return {Object} JSON representing the feature.
 */
osapi.container.GadgetSite.prototype.getFeature = function(name, opt_gadgetInfo) {
  var gadgetInfo = opt_gadgetInfo || this.getActiveGadgetHolder().getGadgetInfo();
  return gadgetInfo[osapi.container.MetadataResponse.FEATURES] &&
      gadgetInfo[osapi.container.MetadataResponse.FEATURES][name];
};


/**
 * @return {string?} ID parent element containing this site.
 */
osapi.container.GadgetSite.prototype.getParentId = function() {
  return this.parentId_;
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
      osapi.container.util.warn(message);
    } else {
      self.render(gadgetInfo, viewParams, renderParams);
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
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {Object} viewParams Look at osapi.container.ViewParam.
 * @param {Object} renderParams Look at osapi.container.RenderParam.
 */
osapi.container.GadgetSite.prototype.render = function(
    gadgetInfo, viewParams, renderParams) {
  var curUrl = this.currentGadgetHolder_ ? this.currentGadgetHolder_.getUrl() : null;

  var previousView = null;
  if (curUrl == gadgetInfo['url']) {
    previousView = this.currentGadgetHolder_.getView();
  }
  
  // Find requested view.
  var view = renderParams[osapi.container.RenderParam.VIEW]
      || viewParams[osapi.container.ViewParam.VIEW]
      || previousView;
  var viewInfo = gadgetInfo[osapi.container.MetadataResponse.VIEWS][view];

  // Allow default view if requested view is not found.
  if (!viewInfo && renderParams[osapi.container.RenderParam.ALLOW_DEFAULT_VIEW]) {
    view = osapi.container.GadgetSite.DEFAULT_VIEW_;
    viewInfo = gadgetInfo[osapi.container.MetadataResponse.VIEWS][view];
  }

  // Check if view exists.
  if (!viewInfo) {
    gadgets.warn(['Unsupported view ', view, ' for gadget ', gadgetInfo['url'], '.'].join(''));
    return;
  }

  // Load into the double-buffer if there is one.
  var el = this.loadingGadgetEl_ || this.currentGadgetEl_;
  this.loadingGadgetHolder_ = new osapi.container.GadgetHolder(this.id_, el);

  var localRenderParams = {};
  for (var key in renderParams) {
    localRenderParams[key] = renderParams[key];
  }

  localRenderParams[osapi.container.RenderParam.VIEW] = view;
  localRenderParams[osapi.container.RenderParam.HEIGHT]
      = renderParams[osapi.container.RenderParam.HEIGHT]
      || viewInfo[osapi.container.MetadataResponse.PREFERRED_HEIGHT]
      || gadgetInfo[osapi.container.MetadataResponse.MODULE_PREFS][osapi.container.MetadataResponse.HEIGHT]
      || String(osapi.container.GadgetSite.DEFAULT_HEIGHT_);
  localRenderParams[osapi.container.RenderParam.WIDTH]
      = renderParams[osapi.container.RenderParam.WIDTH]
      || viewInfo[osapi.container.MetadataResponse.PREFERRED_WIDTH]
      || gadgetInfo[osapi.container.MetadataResponse.MODULE_PREFS][osapi.container.MetadataResponse.WIDTH]
      || String(osapi.container.GadgetSite.DEFAULT_WIDTH_);

  this.updateSecurityToken_(gadgetInfo, localRenderParams);

  this.loadingGadgetHolder_.render(gadgetInfo, viewParams, localRenderParams);

  this.onRender(gadgetInfo, viewParams, renderParams);
};


/**
 * Called when a gadget loads in the site. Uses double buffer, if present.
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {Object} viewParams Look at osapi.container.ViewParam.
 * @param {Object} renderParams Look at osapi.container.RenderParam.
 */
osapi.container.GadgetSite.prototype.onRender = function(
    gadgetInfo, viewParams, renderParams) {
  this.swapBuffers_();

  if (this.currentGadgetHolder_) {
    this.currentGadgetHolder_.dispose();
  }

  this.currentGadgetHolder_ = this.loadingGadgetHolder_;
  this.loadingGadgetHolder_ = null;
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
 */
osapi.container.GadgetSite.prototype.updateSecurityToken_
    = function(gadgetInfo, renderParams) {
  var tokenInfo = this.service_.getCachedGadgetToken(gadgetInfo['url']);
  if (tokenInfo) {
    var token = tokenInfo[osapi.container.TokenResponse.TOKEN];
    this.loadingGadgetHolder_.setSecurityToken(token);
  }
};


/**
 * Close the gadget in this site. Removes the gadget elements from the
 * containing document. Clients should only call this if they know it is OK
 * for removal.
 */
osapi.container.GadgetSite.prototype.close = function() {
  if (this.loadingGadgetEl_ && this.loadingGadgetEl_.firstChild) {
    this.loadingGadgetEl_.removeChild(this.loadingGadgetEl_.firstChild);
  }
  if (this.currentGadgetEl_ && this.currentGadgetEl_.firstChild) {
    this.currentGadgetEl_.removeChild(this.currentGadgetEl_.firstChild);
  }
  if (this.loadingGadgetHolder_) {
    this.loadingGadgetHolder_.dispose();
  }
  if (this.currentGadgetHolder_) {
    this.currentGadgetHolder_.dispose();
  }
};


/**
 * Unique ID of gadget site
 * @type {number}
 * @private
 */
osapi.container.GadgetSite.nextUniqueId_ = 0;


/**
 * Swap the double buffer elements, if there is a double buffer.
 * @private
 */
osapi.container.GadgetSite.prototype.swapBuffers_ = function() {
  // Only process double buffering if loading gadget exists
  if (this.loadingGadgetEl_) {
    this.loadingGadgetEl_.style.left = '';
    this.loadingGadgetEl_.style.position = '';
    this.currentGadgetEl_.style.position = 'absolute';
    this.currentGadgetEl_.style.left = '-2000px';

    // Swap references;  cur_ will now again be what's visible
    var oldCur = this.currentGadgetEl_;
    this.currentGadgetEl_ = this.loadingGadgetEl_;
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

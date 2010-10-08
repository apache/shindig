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
 * @param {shindig.container.Service} service To fetch gadgets metadata, token.
 * @param {Element} gadgetEl Element into which to render the gadget.
 * @param {Element=} opt_bufferEl Optional element for double buffering.
 * @constructor
 */
shindig.container.GadgetSite = function(service, gadgetEl, opt_bufferEl) {
  /**
   * Service to fetch gadgets metadata and token.
   * @type {shindig.container.Service}
   * @private
   */
  this.service_ = service;

  /**
   * Element holding the current gadget.
   * @type {Element}
   * @private
   */
  this.currentGadgetEl_ = gadgetEl;

  /**
   * Element holding the loading gadget for 2x buffering.
   * @type {Element?}
   * @private
   */
  this.loadingGadgetEl_ = opt_bufferEl || null;

  /**
   * Unique ID of this site.
   * @type {number}
   * @private
   */
  this.id_ = shindig.container.GadgetSite.nextUniqueId_++;

  /**
   * ID of parent gadget.
   * @type {string?}
   * @private
   */
  this.parentId_ = null;

  /**
   * Information about the currently visible gadget.
   * @type {shindig.container.GadgetHolder?}
   * @private
   */
  this.currentGadgetHolder_ = null;

  /**
   * Information about the currently loading gadget.
   * @type {shindig.container.GadgetHolder?}
   * @private
   */
  this.loadingGadgetHolder_ = null;

  this.onConstructed();
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 */
shindig.container.GadgetSite.prototype.onConstructed = function() {};


/**
 * Set the height of the gadget iframe.
 * @param {number} value The new height.
 * @return {shindig.container.GadgetSite} This instance.
 */
shindig.container.GadgetSite.prototype.setHeight = function(value) {
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
 * @return {shindig.container.GadgetSite} This instance.
 */
shindig.container.GadgetSite.prototype.setWidth = function(value) {
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
 * @return {shindig.container.GadgetSite} This instance.
 */
shindig.container.GadgetSite.prototype.setParentId = function(value) {
  this.parentId_ = value;
  return this;
};


/**
 * @return {number} The ID of this gadget site.
 */
shindig.container.GadgetSite.prototype.getId = function() {
  return this.id_;
};


/**
 * Returns the currently-active gadget, the loading gadget if a gadget is
 * loading, or the currently visible gadget.
 * @return {shindig.container.GadgetHolder} The gadget holder.
 */
shindig.container.GadgetSite.prototype.getActiveGadgetHolder = function() {
  return this.loadingGadgetHolder_ || this.currentGadgetHolder_;
};


/**
 * Returns configuration of a feature with a given name. Defaults to current
 * loading or visible gadget if no metadata is passed in.
 * @param {string} name Name of the feature.
 * @param {Object=} opt_gadgetInfo Optional gadget info.
 * @return {Object} JSON representing the feature.
 */
shindig.container.GadgetSite.prototype.getFeature = function(name, opt_gadgetInfo) {
  var gadgetInfo = opt_gadgetInfo || this.getActiveGadgetHolder().getGadgetInfo();
  return gadgetInfo[shindig.container.MetadataResponse.FEATURES] &&
      gadgetInfo[shindig.container.MetadataResponse.FEATURES][name];
};


/**
 * Returns the loading or visible gadget with the given ID.
 * @param {string} id The iframe ID of gadget to return.
 * @return {shindig.container.GadgetHolder?} The gadget. Null, if not exist.
 */
shindig.container.GadgetSite.prototype.getGadgetHolder = function(id) {
  if (this.currentGadgetHolder_ && this.currentGadgetHolder_.getIframeId() == id) {
    return this.currentGadgetHolder_;
  }
  if (this.loadingGadgetHolder_ && this.loadingGadgetHolder_.getIframeId() == id) {
    return this.loadingGadgetHolder_;
  }
  return null;
};


/**
 * @return {string?} ID parent element containing this site.
 */
shindig.container.GadgetSite.prototype.getParentId = function() {
  return this.parentId_;
};


/**
 * Render a gadget in the site, by URI of the gadget XML.
 * @param {string} gadgetUrl The absolute URL to gadget.
 * @param {Object} viewParams View parameters for the gadget.
 * @param {Object} renderParams. Render parameters for the gadget, including:
 *     view, width, height.
 * @param {function(Object)=} opt_callback Function called with gadget info after
 *     navigation has occurred.
 */
shindig.container.GadgetSite.prototype.navigateTo = function(
    gadgetUrl, viewParams, renderParams, opt_callback) {
  var callback = opt_callback || function() {};

  // If metadata has been loaded/cached.
  var gadgetInfo = this.service_.getCachedGadgetMetadata(gadgetUrl);
  if (gadgetInfo) {
    this.render(gadgetInfo, viewParams, renderParams);
    callback(gadgetInfo);

  // Otherwise, fetch gadget metadata.
  } else {
    var request = shindig.container.util.newMetadataRequest([gadgetUrl]);
    var self = this;
    this.service_.getGadgetMetadata(request, function(response) {
      if (!response.error) {
        var gadgetInfo = response[gadgetUrl];
        self.render(gadgetInfo, viewParams, renderParams);
        callback(gadgetInfo);
      } else {
        callback(response);
      }
    });
  }
};


/**
 * Render a gadget in this site, using a JSON gadget description.
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {Object} viewParams View parameters for the gadget.
 * @param {Object} renderParams. Render parameters for the gadget, including:
 *     view, width, height.
 */
shindig.container.GadgetSite.prototype.render = function(
    gadgetInfo, viewParams, renderParams) {
  var curUrl = this.currentGadgetHolder_ ? this.currentGadgetHolder_.getUrl() : null;

  var previousView = null;
  if (curUrl == gadgetInfo['url']) {
    previousView = this.currentGadgetHolder_.getView();
  }

  // Load into the double-buffer if there is one
  var el = this.loadingGadgetEl_ || this.currentGadgetEl_;
  this.loadingGadgetHolder_ = new shindig.container.GadgetHolder(this.id_, el);

  var view = renderParams[shindig.container.RenderParam.VIEW]
      || viewParams[shindig.container.ViewParam.VIEW]
      || previousView
      || 'default';
  var viewInfo = gadgetInfo[shindig.container.MetadataResponse.VIEWS][view];

  var delayLoad = this.getFeature('loadstate', gadgetInfo) ||
      this.getFeature('shell', gadgetInfo);

  var localRenderParams = {};
  for (var key in renderParams) {
    localRenderParams[key] = renderParams[key];
  }

  // Delay load for now means we autosize.
  if (delayLoad) {
    localRenderParams[shindig.container.RenderParam.HEIGHT] = '0';
  }
  localRenderParams[shindig.container.RenderParam.VIEW] = view;
  localRenderParams[shindig.container.RenderParam.HEIGHT]
      = renderParams[shindig.container.RenderParam.HEIGHT]
      || viewInfo[shindig.container.MetadataResponse.PREFERRED_HEIGHT]
      || gadgetInfo[shindig.container.MetadataResponse.MODULE_PREFS][shindig.container.MetadataResponse.HEIGHT]
      || String(shindig.container.GadgetSite.DEFAULT_HEIGHT_);
  localRenderParams[shindig.container.RenderParam.WIDTH]
      = renderParams[shindig.container.RenderParam.WIDTH]
      || viewInfo[shindig.container.MetadataResponse.PREFERRED_WIDTH]
      || gadgetInfo[shindig.container.MetadataResponse.MODULE_PREFS][shindig.container.MetadataResponse.WIDTH]
      || String(shindig.container.GadgetSite.DEFAULT_WIDTH_);

  this.updateSecurityToken_(gadgetInfo, localRenderParams);

  this.loadingGadgetHolder_.render(gadgetInfo, viewParams, localRenderParams);

  this.loaded_ = false;

  // Resize on load only if load is delayed. If immediate, height is 0
  this.resizeOnLoad_ = delayLoad;

  if (!delayLoad) {
    this.setLoadState_('loaded');
  }
};


/**
 * Sends RPC call to the current/visible gadget.
 * @param {string} serviceName RPC service name to call.
 * @param {function(Object)} callback Function to call upon RPC completion.
 * @param {...number} var_args payload to pass to the recipient.
 */
shindig.container.GadgetSite.prototype.rpcCall = function(
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
 * @param {Object} renderParams Render parameters for the gadget, including:
 *     view, width, and height.
 */
shindig.container.GadgetSite.prototype.updateSecurityToken_
    = function(gadgetInfo, renderParams) {
  var tokenInfo = this.service_.getCachedGadgetToken(gadgetInfo['url']);
  if (tokenInfo) {
    var token = tokenInfo[shindig.container.TokenResponse.TOKEN];
    this.loadingGadgetHolder_.setSecurityToken(token);
  }
};


/**
 * Close the gadget in this site. Removes the gadget elements from the
 * containing document. Clients should only call this if they know it is OK
 * for removal.
 */
shindig.container.GadgetSite.prototype.close = function() {
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
shindig.container.GadgetSite.nextUniqueId_ = 0;


/**
 * Sets the load state of the currently loading / visible gadget.
 * @param {string} state The current state.
 * @private
 */
shindig.container.GadgetSite.prototype.setLoadState_ = function(state) {
  if (!this.loaded_ && state == 'loaded') {
    this.onload_();
  }
};


/**
 * Called when a gadget loads in the site. Uses double buffer, if present.
 * @private
 */
shindig.container.GadgetSite.prototype.onload_ = function() {
  this.loaded_ = true;
  try {
    gadgets.rpc.call(this.loadingGadgetHolder_.getIframeId(), 'onLoad', null);
    if (this.resizeOnLoad_) {
      // TODO need a value for setHeight
      this.setHeight();
    }
  } catch (e) {
    // This can throw for same domain, although it shouldn't
    gadgets.log(e);
  }

  this.swapBuffers_();

  if (this.currentGadgetHolder_) {
    this.currentGadgetHolder_.dispose();
  }

  this.currentGadgetHolder_ = this.loadingGadgetHolder_;
  this.loadingGadgetHolder_ = null;
};


/**
 * Swap the double buffer elements, if there is a double buffer.
 * @private
 */
shindig.container.GadgetSite.prototype.swapBuffers_ = function() {
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
 * Default height of gadget. Refer to --
 * http://code.google.com/apis/gadgets/docs/legacy/reference.html.
 * @type {number}
 * @private
 */
shindig.container.GadgetSite.DEFAULT_HEIGHT_ = 200;


/**
 * Default width of gadget. Refer to --
 * http://code.google.com/apis/gadgets/docs/legacy/reference.html.
 * @type {number}
 * @private
 */
shindig.container.GadgetSite.DEFAULT_WIDTH_ = 320;

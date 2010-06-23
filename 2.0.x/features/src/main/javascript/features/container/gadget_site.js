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
 * @param {Element} gadgetEl Element into which to render the gadget
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
  this.curGadgetEl_ = gadgetEl;

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
  this.curGadget_ = null;

  /**
   * Information about the currently loading gadget.
   * @type {shindig.container.GadgetHolder?}
   * @private
   */
  this.loadingGadget_ = null;

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
  var activeGadget = this.getActiveGadget();
  if (activeGadget) {
    var iframeEl = activeGadget.getIframeElement();
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
  var activeGadget = this.getActiveGadget();
  if (activeGadget) {
    var iframeEl = activeGadget.getIframeElement();
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
shindig.container.GadgetSite.prototype.getActiveGadget = function() {
  return this.loadingGadget_ || this.curGadget_;
};


/**
 * Returns configuration of a feature with a given name. Defaults to current
 * loading or visible gadget if no metadata is passed in.
 * @param {string} name Name of the feature.
 * @param {Object=} opt_gadgetInfo Optional gadget info.
 * @return {Object} JSON representing the feature.
 */
shindig.container.GadgetSite.prototype.getFeature = function(name, opt_gadgetInfo) {
  var gadgetInfo = opt_gadgetInfo || this.getActiveGadget().getGadgetInfo();
  return gadgetInfo['features'] && gadgetInfo['features'][name];
};


/**
 * Returns the loading or visible gadget with the given ID.
 * @param {string} id The iframe ID of gadget to return.
 * @return {shindig.container.GadgetHolder?} The gadget. Null, if not exist.
 */
shindig.container.GadgetSite.prototype.getGadgetHolder = function(id) {
  if (this.curGadget_ && this.curGadget_.getIframeId() == id) {
    return this.curGadget_;
  }
  if (this.loadingGadget_ && this.loadingGadget_.getIframeId() == id) {
    return this.loadingGadget_;
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
 * @param {Object} gadgetParams View parameters for the gadget.
 * @param {Object} renderParams. Render parameters for the gadget, including:
 *     view, width, height.
 * @param {function(Object)=} opt_callback Function called with gadget info after
 *     navigation has occurred.
 */
shindig.container.GadgetSite.prototype.navigateTo = function(
    gadgetUrl, gadgetParams, renderParams, opt_callback) {
  var callback = opt_callback || function() {};
  
  // If metadata has been loaded/cached.
  var gadgetInfo = this.service_.getCachedGadgetMetadata(gadgetUrl);
  if (gadgetInfo) {
    this.render(gadgetInfo, gadgetParams, renderParams);
    callback(gadgetInfo);

  // Otherwise, fetch gadget metadata.
  } else {
    var request = {
      'container': window.__CONTAINER,
      'ids': [ gadgetUrl ]
    };
    var self = this;
    this.service_.getGadgetMetadata(request, function(response) {
      if (!response.error) {
        var gadgetInfo = response[gadgetUrl];
        self.render(gadgetInfo, gadgetParams, renderParams);
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
 * @param {Object} gadgetParams View parameters for the gadget.
 * @param {Object} renderParams. Render parameters for the gadget, including:
 *     view, width, height.
 */
shindig.container.GadgetSite.prototype.render = function(
    gadgetInfo, gadgetParams, renderParams) {
  var curUrl = this.curGadget_ ? this.curGadget_.getUrl() : null;

  var previousView = null;
  if (curUrl == gadgetInfo['url']) {
    previousView = this.curGadget_.getView();
  }

  // Load into the double-buffer if there is one
  var el = this.loadingGadgetEl_ || this.curGadgetEl_;
  this.loadingGadget_ = new shindig.container.GadgetHolder(this.id_, el);

  var view = renderParams['view'] || gadgetParams['view'] || previousView
      || 'default';
  var viewInfo = gadgetInfo['views'][view];

  var delayLoad = this.getFeature('loadstate', gadgetInfo) ||
      this.getFeature('shell', gadgetInfo);

  var localRenderParams = {};
  for (var key in renderParams) {
    localRenderParams[key] = renderParams[key];
  }

  // Delay load for now means we autosize.
  if (delayLoad) {
    localRenderParams['height'] = '0';
  }
  localRenderParams['view'] = view;
  localRenderParams['width'] = localRenderParams['width'] ||
      viewInfo['preferredWidth'] || null;
  localRenderParams['height'] = localRenderParams['height'] ||
      viewInfo['preferredHeight'] || '150';

  this.updateSecurityToken_(gadgetInfo, localRenderParams);

  this.loadingGadget_.render(gadgetInfo, gadgetParams, localRenderParams);

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
  if (this.curGadget_) {
    gadgets.rpc.call(this.curGadget_.getIframeId(), serviceName, callback, var_args);
  }
};


/**
 * If token has been fetched at least once, set the token to the most recent
 * one. Otherwise, leave it.
 * @param {Object} gadgetInfo The gadgetInfo used to update security token.
 * @param {Object} renderParams. Render parameters for the gadget, including:
 *     view, width, and height.
 */
shindig.container.GadgetSite.prototype.updateSecurityToken_
    = function(gadgetInfo, renderParams) {
  var tokenInfo = this.service_.getCachedGadgetToken(gadgetInfo['url']);
  if (tokenInfo) {
    var token = tokenInfo['token'];
    this.loadingGadget_.setSecurityToken(token);
  }
};


/**
 * Close the gadget in this site. Removes the gadget elements from the
 * containing document. Clients should only call this if they know it is OK
 * for removal.
 */
shindig.container.GadgetSite.prototype.close = function() {
  // Only remove the element (iframe) created by this, not by the container.
  if (this.loadingGadgetEl_) {
    this.loadingGadgetEl_.removeChild(this.loadingGadgetEl_.firstChild);
  }
  if (this.curGadgetEl_) {
    this.curGadgetEl_.removeChild(this.curGadgetEl_.firstChild);
  }
  if (this.loadingGadget_) {
    this.loadingGadget_.dispose();
  }
  if (this.curGadget_) {
    this.curGadget_.dispose();
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
    gadgets.rpc.call(this.loadingGadget_.getIframeId(), 'onLoad', null);
    if (this.resizeOnLoad_) {
      // TODO need a value for setHeight
      this.setHeight();
    }
  } catch (e) {
    // This can throw for same domain, although it shouldn't
    gadgets.log(e);
  }

  this.swapBuffers_();

  if (this.curGadget_) {
    this.curGadget_.dispose();
  }

  this.curGadget_ = this.loadingGadget_;
  this.loadingGadget_ = null;
};


/**
 * Swap the double buffer elements, if there is a double buffer.
 */
shindig.container.GadgetSite.prototype.swapBuffers_ = function() {
  // Only process double buffering if loading gadget exists
  if (this.loadingGadgetEl_) {
    this.loadingGadgetEl_.style.left = '';
    this.loadingGadgetEl_.style.position = '';
    this.curGadgetEl_.style.position = 'absolute';
    this.curGadgetEl_.style.left = '-2000px';

    // Swap references;  cur_ will now again be what's visible
    var oldCur = this.curGadgetEl_;
    this.curGadgetEl_ = this.loadingGadgetEl_;
    this.loadingGadgetEl_ = oldCur;
  }
};

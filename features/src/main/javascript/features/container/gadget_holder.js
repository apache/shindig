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
 * @fileoverview This represents an HTML element and the associated gadget.
 */


/**
 * @param {number} siteId The id of site containing this holder.
 * @param {Element} el The element to render gadgets in.
 * @constructor
 */
shindig.container.GadgetHolder = function(siteId, el) {
  /**
   * Unique numeric gadget ID.
   * @type {number}
   * @private
   */
  this.siteId_ = siteId;

  /**
   * The element into which the gadget is rendered.
   * @type {Element}
   * @private
   */
  this.el_ = el;

  /**
   * JSON metadata for gadget
   * @type {Object?}
   * @private
   */
  this.gadgetInfo_ = null;

  /**
   * View parameters to pass to gadget.
   * @type {Object?}
   * @private
   */
  this.viewParams_ = null;

  /**
   * Gadget rendering parameters
   * @type {Object?}
   * @private
   */
  this.renderParams_ = null;

  /**
   * Unique string gadget ID. Used for DOM IDs/names.
   * @type {string?}
   * @private
   */
  this.iframeId_ = null;

  /**
   * A dynamically set social/security token.
   * Social tokens are sent with original view URLs but may need
   * to be refreshed for long lived gadgets.
   * @type {string?}
   * @private
   */
  this.securityToken_ = null;

  this.onConstructed();
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 */
shindig.container.GadgetHolder.prototype.onConstructed = function() {};


/**
 * @return {Element} The HTML element containing the rendered gadget.
 */
shindig.container.GadgetHolder.prototype.getElement = function() {
  return this.el_;
};


/**
 * @return {?string} The unique string ID for gadget iframe.
 */
shindig.container.GadgetHolder.prototype.getIframeId = function() {
  return this.iframeId_;
};


/**
 * @return {?Object} The metadata of gadget.
 */
shindig.container.GadgetHolder.prototype.getGadgetInfo = function() {
  return this.gadgetInfo_;
};


/**
 * Remove the gadget from this.
 */
shindig.container.GadgetHolder.prototype.dispose = function() {
  this.gadgetInfo_ = null;
};


/**
 * @return {?string} The URL of current gadget.
 */
shindig.container.GadgetHolder.prototype.getUrl = function() {
  return (this.gadgetInfo_) ? this.gadgetInfo_['url'] : null;
};


/**
 * @return {?string} The view of current gadget.
 */
shindig.container.GadgetHolder.prototype.getView = function() {
  return this.renderParams_[shindig.container.RenderParam.VIEW];
};


/**
 * @return {Node} The iframe element containing gadget.
 */
shindig.container.GadgetHolder.prototype.getIframeElement = function() {
  return this.el_.firstChild;
};


/**
 * @param {string} value The value to set this social/security token to.
 * @return {shindig.container.GadgetHolder}
 */
shindig.container.GadgetHolder.prototype.setSecurityToken = function(value) {
  this.securityToken_ = value;
  return this;
};


/**
 * Render a gadget into the element.
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {Object} viewParams View parameters for the gadget.
 * @param {Object} renderParams Render parameters for the gadget, including:
 *     view, width, height.
 */
shindig.container.GadgetHolder.prototype.render = function(
    gadgetInfo, viewParams, renderParams) {
  this.iframeId_ = shindig.container.GadgetHolder.IFRAME_ID_PREFIX_
      + this.siteId_;
  this.gadgetInfo_ = gadgetInfo;
  this.viewParams_ = viewParams;
  this.renderParams_ = renderParams;
  if (!this.gadgetInfo_[shindig.container.MetadataResponse.VIEWS][this.getView()]) {
    throw 'View ' + this.view_ + ' unsupported in ' + this.gadgetInfo_['url'];
  }

  this.el_.innerHTML = this.getIframeHtml_();

  // Set up RPC channel. RPC relay url is on gmodules, relative to base of the
  // container. Assumes container has set up forwarding to gmodules at /gadgets.
  var iframeUri = shindig.uri(
      this.gadgetInfo_[shindig.container.MetadataResponse.IFRAME_URL]);
  var relayUri = shindig.uri()
      .setSchema(iframeUri.getSchema())
      .setAuthority(iframeUri.getAuthority())
      .setPath('/gadgets/files/container/rpc_relay.html');
  gadgets.rpc.setRelayUrl(this.iframeId_, relayUri.toString());
  gadgets.rpc.setAuthToken(this.iframeId_, iframeUri.getFP('rpctoken'));
};


// -----------------------------------------------------------------------------
// Private variables and methods.
// -----------------------------------------------------------------------------


/**
 * Prefix for gadget HTML IDs/names.
 * @type {string}
 * @private
 */
shindig.container.GadgetHolder.IFRAME_ID_PREFIX_ = '__gadget_';


/**
 * Get HTML text content that can be used to render the gadget IFRAME
 * @return {string} The HTML content of this gadget that can be rendered.
 * @private
 */
shindig.container.GadgetHolder.prototype.getIframeHtml_ = function() {
  var iframeParams = {
    'id': this.iframeId_,
    'name': this.iframeId_,
    'src': this.getIframeUrl_(),
    'scrolling': 'no',
    'marginwidth': '0',
    'marginheight': '0',
    'frameborder': '0',
    'vspace': '0',
    'hspace': '0',
    'class': this.renderParams_[shindig.container.RenderParam.CLASS],
    'height': this.renderParams_[shindig.container.RenderParam.HEIGHT],
    'width': this.renderParams_[shindig.container.RenderParam.WIDTH]
  };

  // Do not use DOM API (createElement(), setAttribute()), since it is slower,
  // requires more code, and creating an element with it results in a click
  // sound in IE (unconfirmed), setAttribute('class') may need browser-specific
  // variants.
  var out = [];
  out.push('<iframe ');
  for (var key in iframeParams) {
    var value = iframeParams[key];
    if (value) {
      out.push(key + '="' + value + '" ');
    }
  }
  out.push('></iframe>');

  return out.join('');
};


/**
 * Get the rendering iframe URL.
 * @private
 * @return {string} the rendering iframe URL.
 */
shindig.container.GadgetHolder.prototype.getIframeUrl_ = function() {
  var uri = shindig.uri(this.gadgetInfo_[shindig.container.MetadataResponse.IFRAME_URL]);
  uri.setQP('debug', this.renderParams_[shindig.container.RenderParam.DEBUG] ? '1' : '0');
  uri.setQP('nocache', this.renderParams_[shindig.container.RenderParam.NO_CACHE] ? '1' : '0');
  uri.setQP('testmode', this.renderParams_[shindig.container.RenderParam.TEST_MODE] ? '1' : '0');
  uri.setQP('view', this.getView());
  this.updateUserPrefParams_(uri);

  // TODO: Share this base container logic
  // TODO: Two SD base URIs - one for container, one for gadgets
  // Need to add parent at end of query due to gadgets parsing bug
  uri.setQP('parent', window.__CONTAINER_URI.getOrigin());

  // Remove existing social token if we have a new one
  if (this.securityToken_) {
    uri.setExistingP('st', this.securityToken_);
  }

  uri.setFP('mid', String(this.siteId_));

  if (this.hasViewParams_()) {
    var gadgetParamText = gadgets.json.stringify(this.viewParams_);
    uri.setFP('view-params', gadgetParamText);
  }

  return uri.toString();
};


/**
 * Replace user prefs specified in url with only those specified. This will
 * maintain each user prefs existence (or lack of), order (from left to right)
 * and its appearance (in query params or fragment).
 * @param {shindig.uri} uri The URL possibly containing user preferences
 *     parameters prefixed by up_.
 * @return {string} The URL with up_ replaced by those specified in userPrefs.
 * @private
 */
shindig.container.GadgetHolder.prototype.updateUserPrefParams_ = function(uri) {
  var userPrefs = this.renderParams_[shindig.container.RenderParam.USER_PREFS];
  if (userPrefs) {
    for (var up in userPrefs) {
      var upKey = 'up_' + up;
      var upValue = userPrefs[up];
      if (upValue instanceof Array) {
        upValue = upValue.join('|');
      }
      uri.setExistingP(upKey, upValue);
    }
  }
};


/**
 * Return true if this has view parameters.
 * @private
 * @return {Boolean}
 */
shindig.container.GadgetHolder.prototype.hasViewParams_ = function() {
  for (var key in this.viewParams_) {
    return true;
  }
  return false;
};

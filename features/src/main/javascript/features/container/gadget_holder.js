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
   * Unique numeric gadget ID. Should be the same as siteId.
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
  this.gadgetParams_ = null;

  /**
   * Whether there are any view parameters.
   * @type {boolean}
   * @private
   */
  this.hasGadgetParams_ = false;

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
   * Name of current view being rendered.
   * @type {string?}
   * @private
   */
  this.view_ = null;

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
 * @return {string|null} The unique string ID for gadget iframe.
 */
shindig.container.GadgetHolder.prototype.getIframeId = function() {
  return this.iframeId_;
};


/**
 * @return {Object|null} The metadata of gadget.
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
 * @return {string|null} The URL of current gadget.
 */
shindig.container.GadgetHolder.prototype.getUrl = function() {
  return (this.gadgetInfo_) ? this.gadgetInfo_['url'] : null;
};


/**
 * @return {string|null} The view of current gadget.
 */
shindig.container.GadgetHolder.prototype.getView = function() {
  return this.view_;
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
 * @param {Object} gadgetParams View parameters for the gadget.
 * @param {Object} renderParams Render parameters for the gadget, including:
 *     view, width, height.
 */
shindig.container.GadgetHolder.prototype.render = function(
    gadgetInfo, gadgetParams, renderParams) {
  this.iframeId_ = shindig.container.GadgetHolder.IFRAME_ID_PREFIX_
      + this.siteId_;
  this.gadgetInfo_ = gadgetInfo;
  this.gadgetParams_ = gadgetParams;
  this.hasGadgetParams_ = false;
  for (var key in this.gadgetParams_) {
    this.hasGadgetParams_ = true;
    break;
  }
  this.renderParams_ = renderParams;
  this.view_ = renderParams['view'];
  if (!this.gadgetInfo_['views'][this.view_]) {
    throw 'View ' + this.view_ + ' unsupported in ' + this.gadgetInfo_['url'];
  }

  this.el_.innerHTML = this.getIframeHtml_();

  // Set up RPC channel. RPC relay url is on gmodules, relative to base of the
  // container. Assumes container has set up forwarding to gmodules at /gadgets.
  gadgets.rpc.setRelayUrl(this.iframeId_, this.gadgetInfo_['iframeHost'] +
      '/gadgets/files/container/rpc_relay.html');
  // Pull RPC token from gadget URI
  gadgets.rpc.setAuthToken(this.iframeId_, this.getRpcToken_());
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
      'class': this.renderParams_['class'],
      'height': this.renderParams_['height'],
      'width': this.renderParams_['width']
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
 */
shindig.container.GadgetHolder.prototype.getIframeUrl_ = function() {
  var uri = this.gadgetInfo_['iframeUrl'];
  uri = shindig.container.util.updateQueryParam(uri, 'view', this.view_);
  uri = this.updateBooleanQueryParam_(uri, 'debug');
  uri = this.updateBooleanQueryParam_(uri, 'nocache');
  uri = this.updateBooleanQueryParam_(uri, 'testmode');
  uri = this.updateUserPrefParams_(uri);

  // TODO: Share this base container logic
  // TODO: Two SD base URIs - one for container, one for gadgets
  // Need to add parent at end of query due to gadgets parsing bug
  var parent = shindig.container.util.parseOrigin(document.location.href);
  uri = shindig.container.util.addQueryParam(uri, 'parent', parent);

  // Remove existing social token if we have a new one
  if (this.securityToken_) {
    var securityTokenMatch = uri.match(/([&#?])(st=[^&#]*)/);
    if (securityTokenMatch) {
      uri = uri.replace(securityTokenMatch[0], securityTokenMatch[1] +
          'st=' + encodeURIComponent(this.securityToken_));
    }
  }

  uri = shindig.container.util.addFragmentParam(uri, 'mid',
      String(this.siteId_));

  if (this.hasGadgetParams_) {
    var gadgetParamText = gadgets.json.stringify(this.gadgetParams_);
    uri = shindig.container.util.addFragmentParam(uri, 'view-params',
        gadgetParamText);
  }
  return uri;
};


/**
 * Updates query params of interest.
 * @param {string} uri The URL to append query param to.
 * @param {string} param The query param to update uri with.
 * @return {string} The URL with param append to.
 * @private
 */
shindig.container.GadgetHolder.prototype.updateBooleanQueryParam_
    = function(uri, param) {
  var value = this.renderParams_[param] ? "1" : "0";
  return shindig.container.util.updateQueryParam(uri, param, value);
};


/**
 * Replace user prefs specified in url with only those specified. This will
 * maintain each user prefs existence (or lack of), order (from left to right)
 * and its appearance (in query params or fragment).
 * @param {string} uri The URL possibly containing user preferences parameters
 *     prefixed by up_.
 * @return {string} The URL with up_ replaced by those specified in userPrefs.
 * @private
 */
shindig.container.GadgetHolder.prototype.updateUserPrefParams_ = function(uri) {
  var userPrefs = this.renderParams_['userPrefs'];
  if (userPrefs) {
    for (var up in userPrefs) {
      // Maybe more efficient to have a pre-compiled regex that looks for
      // up_ANY_TEXT and match all instances.
      var re = new RegExp('([&#?])up_' + up + '[^&#]*');
      if (re) {
        var key = encodeURIComponent('up_' + up);
        var val = userPrefs[up];
        if (val instanceof Array) {
          val = val.join('|');
        }
        val = encodeURIComponent(val);
        uri = uri.replace(re, '$1' + key + '=' + val);
      }
    }
  }
  return uri;
};


/**
 * @return {string} The current RPC token.
 * @private
 */
shindig.container.GadgetHolder.prototype.getRpcToken_ = function() {
  return shindig.container.util.getParamValue(
      this.gadgetInfo_['iframeUrl'], 'rpctoken');
};

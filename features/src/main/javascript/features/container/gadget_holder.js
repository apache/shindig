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
osapi.container.GadgetHolder = function(siteId, el) {
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
   * @type {Object}
   * @private
   */
  this.gadgetInfo_ = null;

  /**
   * View parameters to pass to gadget.
   * @type {Object}
   * @private
   */
  this.viewParams_ = null;

  /**
   * Gadget rendering parameters
   * @type {Object}
   * @private
   */
  this.renderParams_ = null;

  /**
   * Unique string gadget ID. Used for DOM IDs/names.
   * @type {string}
   * @private
   */
  this.iframeId_ = null;

  /**
   * A dynamically set social/security token.
   * Social tokens are sent with original view URLs but may need
   * to be refreshed for long lived gadgets.
   * @type {string}
   * @private
   */
  this.securityToken_ = null;

  this.onConstructed();
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 */
osapi.container.GadgetHolder.prototype.onConstructed = function() {};


/**
 * @return {Element} The HTML element containing the rendered gadget.
 */
osapi.container.GadgetHolder.prototype.getElement = function() {
  return this.el_;
};


/**
 * @return {string} The unique string ID for gadget iframe.
 */
osapi.container.GadgetHolder.prototype.getIframeId = function() {
  return this.iframeId_;
};


/**
 * @return {Object} The metadata of gadget.
 */
osapi.container.GadgetHolder.prototype.getGadgetInfo = function() {
  return this.gadgetInfo_;
};


/**
 * Remove the gadget from this.
 */
osapi.container.GadgetHolder.prototype.dispose = function() {
  this.gadgetInfo_ = null;
};


/**
 * @return {string} The URL of current gadget.
 */
osapi.container.GadgetHolder.prototype.getUrl = function() {
  return (this.gadgetInfo_) ? this.gadgetInfo_['url'] : null;
};


/**
 * @return {string} The view of current gadget.
 */
osapi.container.GadgetHolder.prototype.getView = function() {
  return this.renderParams_[osapi.container.RenderParam.VIEW];
};


/**
 * @return {Node} The iframe element containing gadget.
 */
osapi.container.GadgetHolder.prototype.getIframeElement = function() {
  return this.el_.firstChild;
};


/**
 * @param {string} value The value to set this social/security token to.
 * @return {osapi.container.GadgetHolder}
 */
osapi.container.GadgetHolder.prototype.setSecurityToken = function(value) {
  this.securityToken_ = value;
  return this;
};


/**
 * Render a gadget into the element.
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {Object} viewParams Look at osapi.container.ViewParam.
 * @param {Object} renderParams Look at osapi.container.RenderParam.
 */
osapi.container.GadgetHolder.prototype.render = function(
    gadgetInfo, viewParams, renderParams) {
  this.iframeId_ = osapi.container.GadgetHolder.IFRAME_ID_PREFIX_
      + this.siteId_;
  this.gadgetInfo_ = gadgetInfo;
  this.viewParams_ = viewParams;
  this.renderParams_ = renderParams;

  if (this.hasFeature_(gadgetInfo, 'pubsub-2')) {
    this.doOaaIframeHtml_();
  } else {
    this.doNormalIframeHtml_();
  }
};


// -----------------------------------------------------------------------------
// Private variables and methods.
// -----------------------------------------------------------------------------


/**
 * Prefix for gadget HTML IDs/names.
 * @type {string}
 * @private
 */
osapi.container.GadgetHolder.IFRAME_ID_PREFIX_ = '__gadget_';


/**
 * @private
 */
osapi.container.GadgetHolder.prototype.doNormalIframeHtml_ = function() {
  this.el_.innerHTML = this.getIframeHtml_();

  // Set up RPC channel. RPC relay url is on gmodules, relative to base of the
  // container. Assumes container has set up forwarding to gmodules at /gadgets.
  var iframeUri = shindig.uri(
      this.gadgetInfo_[osapi.container.MetadataResponse.IFRAME_URL]);
  var relayUri = shindig.uri()
      .setSchema(iframeUri.getSchema())
      .setAuthority(iframeUri.getAuthority())
      .setPath('/gadgets/files/container/rpc_relay.html');
  gadgets.rpc.setupReceiver(this.iframeId_, relayUri.toString(),
      iframeUri.getFP('rpctoken'));
};


/**
 * @private
 */
osapi.container.GadgetHolder.prototype.doOaaIframeHtml_ = function() {
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
      'class': this.renderParams_[osapi.container.RenderParam.CLASS],
      'height': this.renderParams_[osapi.container.RenderParam.HEIGHT],
      'width': this.renderParams_[osapi.container.RenderParam.WIDTH]
  };
  new OpenAjax.hub.IframeContainer(
      gadgets.pubsub2router.hub,
      this.iframeId_,
      {
        Container: {
          onSecurityAlert: function(source, alertType) {
            gadgets.error(['Security error for container ', source.getClientID(), ' : ', alertType].join(''));
            source.getIframe().src = 'about:blank';
          },
          onConnect: function(container) {
            gadgets.log(['connected: ', container.getClientID()].join(''));
          }
        },
        IframeContainer: {
          parent: this.el_,
          uri: this.getIframeUrl_(),
          tunnelURI: shindig.uri('/gadgets/' + '../container/rpc_relay.html').resolve(shindig.uri(window.location.href)),
          iframeAttrs: iframeParams
        }
      }
  );
};


/**
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {string} feature the feature to look for.
 * @private
 */
osapi.container.GadgetHolder.prototype.hasFeature_ = function(gadgetInfo, feature) {
  var modulePrefs = gadgetInfo[osapi.container.MetadataResponse.MODULE_PREFS];
  if (modulePrefs) {
    var features = modulePrefs[osapi.container.MetadataResponse.FEATURES];
    if (features && features[feature]) {
      return true;
    }
  }
  return false;
};


/**
 * Get HTML text content that can be used to render the gadget IFRAME
 * @return {string} The HTML content of this gadget that can be rendered.
 * @private
 */
osapi.container.GadgetHolder.prototype.getIframeHtml_ = function() {
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
    'class': this.renderParams_[osapi.container.RenderParam.CLASS],
    'height': this.renderParams_[osapi.container.RenderParam.HEIGHT],
    'width': this.renderParams_[osapi.container.RenderParam.WIDTH]
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
      out.push(key);
      out.push('="');
      out.push(value);
      out.push('" ');
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
osapi.container.GadgetHolder.prototype.getIframeUrl_ = function() {
  var uri = shindig.uri(this.gadgetInfo_[osapi.container.MetadataResponse.IFRAME_URL]);
  uri.setQP('debug', this.renderParams_[osapi.container.RenderParam.DEBUG] ? '1' : '0');
  uri.setQP('nocache', this.renderParams_[osapi.container.RenderParam.NO_CACHE] ? '1' : '0');
  uri.setQP('testmode', this.renderParams_[osapi.container.RenderParam.TEST_MODE] ? '1' : '0');
  uri.setQP('view', this.getView());
  if (this.renderParams_[osapi.container.RenderParam.CAJOLE]) {
    var libs = uri.getQP('libs');
    if (libs == null || libs == '') uri.setQP('libs', 'caja');
    else uri.setQP('libs', [ libs, ':caja' ].join(''));
    uri.setQP('caja', '1');
  }
  this.updateUserPrefParams_(uri);

  // TODO: Share this base container logic
  // TODO: Two SD base URIs - one for container, one for gadgets
  // Need to add parent at end of query due to gadgets parsing bug
  uri.setQP('parent', window.__CONTAINER_URI.getOrigin());

  // Remove existing social token if we have a new one
  if (this.securityToken_) {
    uri.setExistingP('st', this.securityToken_);
  }

  // Uniquely identify possibly-same gadgets on a page.
  uri.setQP('mid', String(this.siteId_));

  if (!osapi.container.util.isEmptyJson(this.viewParams_)) {
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
osapi.container.GadgetHolder.prototype.updateUserPrefParams_ = function(uri) {
  var userPrefs = this.renderParams_[osapi.container.RenderParam.USER_PREFS];
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

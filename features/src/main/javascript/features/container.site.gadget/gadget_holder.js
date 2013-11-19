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
 * @param {osapi.container.GadgetSite} site The site containing this holder.
 * @param {Element} el The element to render gadgets in.
 * @param {string} onLoad The name of an onLoad function to call in window scope
 *          to assign as the onload handler of this holder's iframe.
 * @constructor
 * @extends {osapi.container.SiteHolder}
 */
osapi.container.GadgetHolder = function(site, el, onLoad) {
  osapi.container.SiteHolder.call(this, site, el, onLoad); // call super
  var undef;

  /**
   * JSON metadata for gadget
   * @type {Object}
   * @private
   */
  this.gadgetInfo_ = undef;

  /**
   * View parameters to pass to gadget.
   * @type {Object}
   * @private
   */
  this.viewParams_ = undef;

  /**
   * A dynamically set social/security token.
   * Social tokens are sent with original view URLs but may need
   * to be refreshed for long lived gadgets.
   * @type {string}
   * @private
   */
  this.securityToken_ = undef;

  /**
   * Whether or not this holder is for an OpenAjax Iframe, i.e.,
   * if the gadget is using pubsub-2
   * @type {boolean}
   * @private
   */
  this.isOaaIframe_ = false;

  this.onConstructed();
};
osapi.container.GadgetHolder.prototype = new osapi.container.SiteHolder;

/**
 * Url points to the rpc_relay.html which allows cross-domain communication between
 *     a gadget and container
 * @type {string}
 * @private
 */
osapi.container.GadgetHolder.prototype.relayPath_ = null;

/**
 * @return {Object} The metadata of gadget.
 */
osapi.container.GadgetHolder.prototype.getGadgetInfo = function() {
  return this.gadgetInfo_;
};


/**
 * @inheritDoc
 */
osapi.container.GadgetHolder.prototype.dispose = function() {
  if (this.isOaaIframe_) {
    this.removeOaaContainer_(this.iframeId_);
  }
  osapi.container.SiteHolder.prototype.dispose.call(this); // super.dispose();
  this.gadgetInfo_ = null;
};


/**
 * @inheritDoc
 */
osapi.container.GadgetHolder.prototype.getUrl = function() {
  return this.gadgetInfo_ && this.gadgetInfo_['url'];
};

/**
 * @return {string} The view of current gadget. This is the view that was actually rendered once
 *         view aliases were applied.
 * @see osapi.container.GadgetSite.prototype.render
 */
osapi.container.GadgetHolder.prototype.getView = function() {
  return this.renderParams_[osapi.container.RenderParam.VIEW];
};


/**
 * @inheritDoc
 * @see osapi.container.GadgetHolder.prototype.doOaaIframeHtml_ and org.openajax.hub-2.0.7/iframe.js:createIframe()
 */
osapi.container.GadgetHolder.prototype.getIframeElement = function() {
  return this.el_.getElementsByTagName('iframe')[0];
};


/**
 * @param {string} value The value to set this social/security token to.
 * @return {osapi.container.GadgetHolder} the current GadgetHolder.
 */
osapi.container.GadgetHolder.prototype.setSecurityToken = function(value) {
  this.securityToken_ = value;
  return this;
};


/**
 * Render a gadget into the element.
 *
 * @override
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {Object} viewParams Look at osapi.container.ViewParam.
 * @param {Object} renderParams Look at osapi.container.RenderParam.
 */
osapi.container.GadgetHolder.prototype.render = function(gadgetInfo, viewParams, renderParams) {
  this.iframeId_ = osapi.container.GadgetHolder.IFRAME_ID_PREFIX_ + this.site_.getId();
  this.gadgetInfo_ = gadgetInfo;
  this.viewParams_ = viewParams;
  this.renderParams_ = renderParams;

  this.isOaaIframe_ = this.hasFeature_(gadgetInfo, 'pubsub-2');
  this.isOaaIframe_ ? this.doOaaIframeHtml_() : this.doNormalIframeHtml_();
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
  var uri = this.getIframeUrl_();
  this.el_.innerHTML = this.createIframeHtml(uri, {title:this.site_.getTitle()});

  // Set up RPC channel.
  var iframeUri = shindig.uri(uri);
  var relayUri = shindig.uri()
      .setSchema(iframeUri.getSchema())
      .setAuthority(iframeUri.getAuthority())
      .setPath(this.relayPath_);
  gadgets.rpc.setupReceiver(this.iframeId_, relayUri.toString(),
      iframeUri.getFP('rpctoken'));
};


/**
 * @private
 */
osapi.container.GadgetHolder.prototype.doOaaIframeHtml_ = function() {
  //Remove any prior container for the iframe id from the OpenAjax hub prior to registering the new one
  this.removeOaaContainer_(this.iframeId_);
  var self = this;
  new OpenAjax.hub.IframeContainer(
      gadgets.pubsub2router.hub,
      this.iframeId_,
      {
        Container: {
          onSecurityAlert: function(source, alertType) {
            gadgets.error(['Security error for container ',
                source.getClientID(), ' : ', alertType].join(''));
            source.getIframe().src = 'about:blank';
          },
          onConnect: function(container) {
            gadgets.log(['connected: ', container.getClientID()].join(''));
          }
        },
        IframeContainer: {
          parent: this.el_,
          uri: this.getIframeUrl_(),
          //tunnelURI: shindig.uri('/test1/gadgets/' + '../container/rpc_relay.html')
          //   .resolve(shindig.uri(window.location.href)),
          tunnelURI: shindig.uri(this.relayPath_).resolve(shindig.uri(window.location.href)),
          iframeAttrs: this.createIframeAttributeMap(this.getIframeUrl_(), {title:this.site_.getTitle()}),
          onGadgetLoad: function() {
            if(self.onLoad_) {
              window[self.onLoad_](self.getUrl(), self.site_.getId());
            }
          }
        }
      }
  );
};

/**
 * Removes the specified container from the registered pubsub2router hub
 *
 * @param {String} containerId the id of the container to remove from the hub
 * @private
 */
osapi.container.GadgetHolder.prototype.removeOaaContainer_ = function(containerId) {
    var container = gadgets.pubsub2router.hub.getContainer(containerId);
    //Null is returned from the getContainer function per the OpenAjax spec if the container is not found
    if(container) {
        gadgets.pubsub2router.hub.removeContainer(container);
    }
};


/**
 * @param {Object} gadgetInfo the JSON gadget description.
 * @param {string} feature the feature to look for.
 * @private
 * @return {boolean} true if feature is set.
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
 * Get the rendering iframe URL.
 * @private
 * @return {string} the rendering iframe URL.
 */
osapi.container.GadgetHolder.prototype.getIframeUrl_ = function() {
  var uri = shindig.uri(this.gadgetInfo_[osapi.container.MetadataResponse.IFRAME_URLS][this.getView()]);
  uri.setQP('debug', this.renderParams_[osapi.container.RenderParam.DEBUG] ? '1' : '0');
  uri.setQP('nocache', this.renderParams_[osapi.container.RenderParam.NO_CACHE] ? '1' : '0');
  uri.setQP('testmode', this.renderParams_[osapi.container.RenderParam.TEST_MODE] ? '1' : '0');
  uri.setQP('view', this.getView());
  if (this.renderParams_[osapi.container.RenderParam.CAJOLE]) {
    var libs = uri.getQP('libs');
    if (libs == null || libs == '') uri.setQP('libs', 'caja');
    else uri.setQP('libs', [libs, ':caja'].join(''));
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
  uri.setQP('mid', String(this.site_.getModuleId()));

  if (!osapi.container.util.isEmptyJson(this.viewParams_)) {
    var gadgetParamText = gadgets.json.stringify(this.viewParams_);
    uri.setFP('view-params', gadgetParamText);
  }

  // add rpctoken fragment to support flash transport if not in the uri
  if(typeof(uri.getFP('rpctoken')) === 'undefined' ) {
    var rpcToken = (0x7FFFFFFF * Math.random()) | 0;
    uri.setFP('rpctoken', rpcToken);
  }
  var lang = this.site_.service_.getLanguage();
  var country = this.site_.service_.getCountry();
  var templateLang = uri.getQP('lang'), templateCountry = uri.getQP('country');
  if(templateLang.indexOf('%') != -1){
    uri.setQP('lang', lang);
  }
  if(templateCountry.indexOf('%') != -1){
    uri.setQP('country', country);
  }
  return uri.toString();
};


/**
 * Replace user prefs specified in url with only those specified. This will
 * maintain each user prefs existence (or lack of), order (from left to right)
 * and its appearance (in query params or fragment).
 * @param {shindig.uri} uri The URL possibly containing user preferences
 *     parameters prefixed by up_.
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


// We do run this in the container mode in the new common container
if (gadgets.config) {
  gadgets.config.register('container', null, function (config) {
    if (config['container']) {
      var rpath = config['container']['relayPath'];
      osapi.container.GadgetHolder.prototype.relayPath_ = rpath;
    }
  });
}

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
 * @fileoverview Constructs a new URL holder. This class is similar in
 * functionality to GadgetHolder from the common container.
 */

/**
 * @param {osapi.container.UrlSite} site The site containing this holder.
 * @param {Element} el The element to render gadgets in.
 * @param {string} onLoad The name of an onLoad function to call in window scope
 *          to assign as the onload handler of this holder's iframe.
 * @constructor
 * @extends {osapi.container.SiteHolder}
 */
osapi.container.UrlHolder = function(site, el, onLoad) {
  osapi.container.SiteHolder.call(this, site, el, onLoad); // call super
  var undef;

  /**
   * @type {string}
   * @private
   */
  this.url_ = undef;

  this.onConstructed();
};
osapi.container.UrlHolder.prototype = new osapi.container.SiteHolder;

/**
 * @inheridDoc
 */
osapi.container.UrlHolder.prototype.dispose = function() {
  osapi.container.SiteHolder.prototype.dispose.call(this); // super.dispose();
  this.url_ = null;
};

/**
 * @inheritDoc
 */
osapi.container.UrlHolder.prototype.getIframeElement = function() {
  return this.el_.firstChild;
};

/**
 * @inheritDoc
 */
osapi.container.UrlHolder.prototype.getUrl = function() {
  return this.url_;
};

/**
 * Renders the URL.
 *
 * @override
 * @param {string} url the URL to render.
 * @param {object} renderParams params to apply to the iFrame.
 */
osapi.container.UrlHolder.prototype.render = function(url, renderParams) {
  this.iframeId_ = osapi.container.UrlHolder.IFRAME_PREFIX_ + this.site_.getId();
  this.renderParams_ = renderParams;
  this.el_.innerHTML = this.createIframeHtml(this.url_ = url, {scrolling: 'auto'});
};

/**
 * Prefix for iFrame ids.
 * @private
 */
osapi.container.UrlHolder.IFRAME_PREFIX_ = '__url_';

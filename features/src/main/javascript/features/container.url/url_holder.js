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
 * @constructor
 * @param {number} siteId The id of the URL site.
 * @param {Element} el The element to contain the URL renders in.
 */
osapi.container.UrlHolder = function(siteId, el) {

  /**
   * @type {number}
   * @private
   */
  this.siteId_ = siteId;

  /**
   * @type {Element}
   * @private
   */
  this.el_ = el;

  /**
   * @type {string}
   * @private
   */
  this.iframeId_ = null;

  /**
   * @type {string}
   * @private
   */
  this.url_ = null;

  /**
   * @type {object}
   * @private
   */
  this.renderParams_ = null;

  this.onConstructed();
};

/**
 * Callback for when the holder is constructed.
 */
osapi.container.UrlHolder.prototype.onConstructed = function() {};

/**
 * Gets the element containing the iFrame.
 * @return {Element} the element containing this embedded experience URL.
 */
osapi.container.UrlHolder.prototype.getElement = function() {
  return this.el_;
};

/**
 * Gets the id of the iFrame.
 * @return {number} the id of the iFrame rendering this embedded experience URL.
 */
osapi.container.UrlHolder.prototype.getIframeId = function() {
  return this.iframeId_;
};

/**
 * Disposes the gadget holder.
 */
osapi.container.UrlHolder.prototype.dispose = function() {
  this.url_ = null;
};

/**
 * Gets the URL rendered in the iFrame.
 * @return {string} the URL of the embedded experience.
 */
osapi.container.UrlHolder.prototype.getUrl = function() {
  return this.url_;
};

/**
 * Gets the iFrame element itself.
 * @return {Element} the iFrame rendering the embedded experience URL.
 */
osapi.container.UrlHolder.prototype.getIframeElement = function() {
  return this.el_.firstChild;
};

/**
 * Renders the embedded experience URL.
 * @param {string} url the URL to render.
 * @param {object} renderParams params to apply to the iFrame.
 */
osapi.container.UrlHolder.prototype.render = function(url, renderParams) {
  this.iframeId_ = osapi.container.UrlHolder.IFRAME_PREFIX_ + this.siteId_;
  this.url_ = url;
  this.renderParams_ = renderParams;
  var iFrameHtml = this.createIframeHtml_();
  this.el_.innerHTML = iFrameHtml;
};

/**
 * Crates the HTML for the iFrame.
 * @return {string} the HTML for the iFrame.
 * @private
 */
osapi.container.UrlHolder.prototype.createIframeHtml_ = function() {
  var iframeParams = {
          'id' : this.iframeId_,
          'name' : this.iframeId_,
          'src' : this.getUrl(),
          'scrolling' : 'auto',
          'marginwidth' : '0',
          'marginheight' : '0',
          'frameborder' : '0',
          'vspace' : '0',
          'hspace' : '0',
          'class' : this.renderParams_[osapi.container.RenderParam.CLASS],
          'height' : this.renderParams_[osapi.container.RenderParam.HEIGHT],
          'width' : this.renderParams_[osapi.container.RenderParam.WIDTH]
  };
  return osapi.container.util.createIframeHtml(iframeParams);
};

/**
 * Prefix for iFrame ids.
 * @private
 */
osapi.container.UrlHolder.IFRAME_PREFIX_ = '__url_';

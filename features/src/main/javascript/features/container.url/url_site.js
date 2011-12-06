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
 * @fileoverview Constructs a new URL site.  This class is very similar in functionality to
 * the GadgetSite class which is part of the common container.
 */

/**
 * @constructor
 * @param {Object} args containing DOM element to rende the iFrame in, and URL
 *                 to render in the iFrame.
 */
osapi.container.UrlSite = function(args) {
  /**
   * @type {number}
   * @private
   */
  this.parentId_ = null;

  /**
   * @type {osapi.container.UrlHolder}
   * @private
   */
  this.holder_ = null;

  /**
   * @type {number}
   * @private
   */
  this.id_ = osapi.container.Container.prototype.nextUniqueSiteId_++;

  /**
   * @type {Element}
   * @private
   */
  this.el_ = args[osapi.container.UrlSite.URL_ELEMENT];

  /**
   * @type {string}
   * @private
   */
  this.url_ = null;

  /**
   * Used primarily by open-views feature.
   * @type {string} ownerId_ The rpc targetId of the gadget that requested this site's creation.
   *
   * @private
   */
  this.ownerId_ = null;

  this.onConstructed();
};

/**
 * Callback when this object is constructed.
 */
osapi.container.UrlSite.prototype.onConstructed = function() {};

/**
 * Sets the height of the iFrame.
 * @param {number} height the height of the embedded experience.
 * @return {osapi.container.UrlSite} this gadget site.
 */
osapi.container.UrlSite.prototype.setHeight = function(height) {
  if (this.holder_) {
    var iframeElement = this.holder_.getIframeElement();
    if (iframeElement) {
      iframeElement.style.height = height + 'px';
    }
  }
  return this;
};

/**
 * Set the width of the iFrame.
 * @param {number} width the width of the embedded experience.
 * @return {osapi.container.UrlSite} this instance.
 */
osapi.container.UrlSite.prototype.setWidth = function(width) {
  if (this.holder_) {
    var iframeElement = this.holder_.getIframeElement();
    if (iframeElement) {
      iframeElement.style.width = width + 'px';
    }
  }
  return this;
};

/**
 * Sets the id of the parent DOM element containing this embedded experience.
 * @param {number} parentId the id of the parent DOM element.
 * @return {osapi.container.UrlSite} this instance.
 */
osapi.container.UrlSite.prototype.setParentId = function(parentId) {
  this.parentId_ = parentId;
  return this;
};

/**
 * Gets the id of this site.
 * @return {number} the id of this site.
 */
osapi.container.UrlSite.prototype.getId = function() {
  return this.id_;
};

/**
 * Gets the id of the parent DOM element.
 * @return {number} the id of the parent DOM element.
 */
osapi.container.UrlSite.prototype.getParentId = function() {
  return this.parentId_;
};

/**
 * Gets the URL holder for this site.
 * @return {osapi.container.UrlHolder} the URL holder for this site.
 */
osapi.container.UrlSite.prototype.getActiveUrlHolder = function() {
  return this.holder_;
};

/**
 * Closes this site.
 */
osapi.container.UrlSite.prototype.close = function() {
  if (this.el_ && this.el_.firstChild) {
    this.el_.removeChild(this.el_.firstChild);
  }

  if (this.holder_) {
    this.holder_.dispose();
  }
};

/**
 * Renders the URL in this site
 * @param {string} url to render in the iFrame.
 * @param {object} renderParams the parameters to render the site.
 */
osapi.container.UrlSite.prototype.render = function(url, renderParams) {
  this.holder_ = new osapi.container.UrlHolder(this.id_, this.el_);

  var localRenderParams = {};
  for (var key in renderParams) {
    localRenderParams[key] = renderParams[key];
  }

  this.holder_.render(url, localRenderParams);
};

/**
 * The next unique id for URL sites.
 * @private
 * @type {number}
 */
osapi.container.UrlSite.nextUniqueId_ = 0;

/**
 * The URL element key
 * @const
 * @type {string}
 */
osapi.container.UrlSite.URL_ELEMENT = 'urlEl';

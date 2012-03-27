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
 * @fileoverview Abstract base-class for gadget sites and url sites.
 */

/**
 * @param {osapi.container.Container} container The container that hosts this gadget site.
 * @param {osapi.container.Service} service The container's service.
 * @param {Element} element The main element for this site.
 */
osapi.container.Site = function(container, service, element, args) {
  var undef;
  /**
   * @type {osapi.container.Container}
   * @protected
   */
  this.container_ = container;

  /**
   * @type {osapi.container.Service}
   * @protected
   */
  this.service_ = service;

  /**
   * @type {Element}
   * @protected
   */
  this.el_ = element;

  /**
   * Unique ID of this site.  Uses the ID of the site element, if set, or an
   * auto-generated number.
   *
   * @type {string}
   * @protected
   */
  this.id_ = (this.el_ && this.el_.id) ? this.el_.id :
    osapi.container.Site.prototype.nextUniqueSiteId_++;

  /**
   * @type {number?}
   * @protected
   */
  this.parentId_ = undef;

  /**
   * Used primarily by open-views feature.
   *
   * @type {string?} ownerId_ The rpc targetId of the gadget that requested
   *   this site's creation.
   * @protected
   */
  this.ownerId_ = undef;
};

/**
 * Unique counter for sites.  Used if no explicit ID was provided in their creation.
 * @type {number}
 */
osapi.container.Site.prototype.nextUniqueSiteId_ = 0;

// Public impl

/**
 * Callback that occurs after instantiation/construction of any site.
 * Override on Site to provide your specific functionalities for all sites.
 * Override on any subclass of Site to provide your specific functionalities
 * for that subclass of sites. Overriding subclass onConstructed will not fire
 * generic Site onConstructed unless you do so manually.
 */
osapi.container.Site.prototype.onConstructed = function() {};

/**
 * @return {string} The ID of this site.
 */
osapi.container.Site.prototype.getId = function() {
  return this.id_;
};

/**
 * Set the width of the site's iframe.
 *
 * @param {number} value The new width.
 * @return {osapi.container.Site} this
 */
osapi.container.Site.prototype.setWidth = function(value) {
  var holder = this.getActiveSiteHolder();
  if (holder) {
    var iframeEl = holder.getIframeElement();
    if (iframeEl) {
      iframeEl.style.width = value + 'px';
    }
  }
  return this;
};

/**
 * Set the height of the site's iframe.
 *
 * @param {number} value The new height.
 * @return {osapi.container.Site} this.
 */
osapi.container.Site.prototype.setHeight = function(value) {
  var holder = this.getActiveSiteHolder();
  if (holder) {
    var iframeEl = holder.getIframeElement();
    if (iframeEl) {
      iframeEl.style.height = value + 'px';
    }
  }
  return this;
};

/**
 * Closes this site.
 */
osapi.container.Site.prototype.close = function() {
  var holder = this.getActiveSiteHolder();
  holder && holder.dispose();
};

/**
 * @return {string?} The id of parent DOM element containing this site, if
 *   previously set by osapi.container.Site.prototype.setParentId.
 */
osapi.container.Site.prototype.getParentId = function() {
  return this.parentId_;
};

/**
 * Sets the id of the parent DOM element containing this site.
 *
 * @param {number} parentId the id of the parent DOM element.
 * @return {osapi.container.Site} this.
 */
osapi.container.Site.prototype.setParentId = function(parentId) {
  this.parentId_ = parentId;
  return this;
};

// Abstract methods

/**
 * Gets the active site holder for this site.
 * @abstract
 * @return {osapi.container.SiteHolder} the holder for this site.
 */
osapi.container.Site.prototype.getActiveSiteHolder = function() {
  throw new Error("This method must be implemented by a subclass.");
};

/**
 * Renders this site.
 * @abstract
 */
osapi.container.Site.prototype.render = function() {
  throw new Error("This method must be implemented by a subclass.");
};
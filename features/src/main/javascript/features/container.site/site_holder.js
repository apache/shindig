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
 * @fileoverview Abstract base-class for site holders.
 */

/**
 * @param {osapi.container.Site} site The site containing this holder.
 * @param {Element} el The element that this holder manages.
 * @param {string} onLoad The name of an onLoad function to call in window scope
 *          to assign as the onload handler of this holder's iframe.
 *
 * @constructor
 */
osapi.container.SiteHolder = function(site, el, onLoad) {
  var undef;

  /**
   * The site containing this holder.
   * @type {osapi.container.Site}
   * @protected
   */
  this.site_ = site;

  /**
   * The element that this holder manages.
   * @type {Element}
   * @protected
   */
  this.el_ = el;

  /**
   * On load function for gadget iFrames in window scope
   * @type {string}
   * @protected
   */
  this.onLoad_ = onLoad;

  /**
   * Id of the iframe contained within this holder.
   * @type {string}
   * @protected
   */
  this.iframeId_ = undef;

  /**
   * @type {object}
   * @protected
   */
  this.renderParams_ = undef;

  this.onConstructed();
};

/**
 * Callback that occurs after instantiation/construction of any SiteHolder.
 * Override on SiteHolder to provide your specific functionalities for all sites.
 * Override on any subclass of SiteHolder to provide your specific functionalities
 * for that subclass of SiteHolder. Overriding subclass onConstructed will not fire
 * generic SiteHolder onConstructed unless you do so manually.
 */
osapi.container.SiteHolder.prototype.onConstructed = function() {};

/**
 * @return {Element} The holder's HTML element.
 */
osapi.container.SiteHolder.prototype.getElement = function() {
  return this.el_;
};

/**
 * Gets the id of the iframe contained within this holder.
 * @return {string} the id of the iframe contained within this holder.
 */
osapi.container.SiteHolder.prototype.getIframeId = function() {
  return this.iframeId_;
};

/**
 * Creates the iframe element source for this holder's iframe element.
 * @param {string} url The src url for the iframe.
 * @param {Object.<string, string>=} overrides A bag of iframe attribute overrides.
 * @return {string} The new iframe element source.
 * @protected
 */
osapi.container.SiteHolder.prototype.createIframeHtml = function(url, overrides) {
   return osapi.container.util.createIframeHtml(
     this.createIframeAttributeMap(url, overrides)
   );
};

/**
 * Creates the iframe element source for this holder's iframe element.
 * @param {string} url The src url for the iframe.
 * @param {Object=} overrides A bag of iframe attribute overrides.
 * @return {string} The new iframe element source.
 * @protected
 */
osapi.container.SiteHolder.prototype.createIframeAttributeMap = function(url, overrides) {
  var undef,
      renderParams = this.renderParams_ || {},
      params = {
        id: this.iframeId_,
        name: this.iframeId_,
        src: url,
        scrolling: 'no',
        marginwidth: 0,
        marginheight: 0,
        frameborder: 0,
        vspace: 0,
        hspace: 0,
        'class': renderParams[osapi.container.RenderParam.CLASS],
        height: renderParams[osapi.container.RenderParam.HEIGHT],
        width: renderParams[osapi.container.RenderParam.WIDTH],
        onload: this.onLoad_ ?
                ('window.' + this.onLoad_ + "('" + this.getUrl() + "', '" + this.site_.getId() + "');") : undef
      };
   if (overrides) {
     for(var i in overrides) {
       params[i] = overrides[i];
     }
   }
   return params;
};

/**
 * Disposes the gadget holder and performs cleanup of any holder state.
 * @abstract
 */
osapi.container.SiteHolder.prototype.dispose = function() {
  if (this.el_ && this.el_.firstChild) {
    this.el_.removeChild(this.el_.firstChild);
  }
};

//Abstract methods

/**
 * Gets the iFrame element itself.
 * @abstract
 * @return {Element} The iframe element in this holder.
 */
osapi.container.SiteHolder.prototype.getIframeElement = function() {
  throw new Error("This method must be implemented by a subclass.");
};

/**
 * Gets the iFrame element itself.
 * @abstract
 * @return {Element} The iframe element in this holder.
 */
osapi.container.SiteHolder.prototype.render = function() {
  throw new Error("This method must be implemented by a subclass.");
};

/**
 * @abstract
 * @return {string} The URL associated with the holder.
 */
osapi.container.SiteHolder.prototype.getUrl = function() {
  throw new Error("This method must be implemented by a subclass.");
};
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
 * @param {osapi.container.Container} container The container that hosts this gadget site.
 * @param {osapi.container.Service} service The container's service.
 * @param {Object} args containing DOM element to rende the iFrame in, and URL
 *                 to render in the iFrame.
 * @constructor
 * @extends {osapi.container.Site}
 */
osapi.container.UrlSite = function(container, service, args) {
  var undef;

  osapi.container.Site.call(this, container, service,
    args[osapi.container.UrlSite.URL_ELEMENT]
  ); // call super

  /**
   * @type {osapi.container.UrlHolder}
   * @private
   */
  this.holder_ = undef;

  /**
   * @type {string}
   * @private
   */
  this.url_ = undef;

  this.onConstructed();
};
osapi.container.UrlSite.prototype = new osapi.container.Site;

/**
 * @inheritDoc
 */
osapi.container.UrlSite.prototype.getActiveSiteHolder = function() {
  return this.holder_;
};

/**
 * Renders the URL in this site
 * @param {string} url to render in the iFrame.
 * @param {object} renderParams the parameters to render the site.
 * @override
 */
osapi.container.UrlSite.prototype.render = function(url, renderParams) {
  this.holder_ = new osapi.container.UrlHolder(this, this.el_);

  var localRenderParams = {};
  for (var key in renderParams) {
    localRenderParams[key] = renderParams[key];
  }

  this.holder_.render(url, localRenderParams);
};

/**
 * The URL element key
 * @const
 * @type {string}
 */
osapi.container.UrlSite.URL_ELEMENT = 'urlEl';

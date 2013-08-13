/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview Container-side common script.
 */

osapi.container.Container.addMixin('views', function(container) {
  var self = this;

  /**
   * Closes an opened site. If the opt_id parameter is null the container will
   * close the calling site.
   *
   * @param {Object=}
   *          opt_site: Optional parameter which specifies what site to close.
   *          If not provided or null, it will close the current gadget site.
   */
  container.rpcRegister('gadgets.views.close', function(rpcArgs, opt_site) {
    // opt_site may be 0, do not do a truthy test on the value.
    var orig_site = container.getGadgetSiteByIframeId_(rpcArgs.f),
        site = typeof(opt_site) != 'undefined' && opt_site != null ?
                container.getSiteById(opt_site) : orig_site;

    if (site && (site == orig_site || site.ownerId_ == rpcArgs.f)) {
      // The provided method must ultimately call container.closeGadget(site);
      self.destroyElement(site);
    }
  });

  /**
   * Gets the dimensions of the container displaying the gadget.
   */
  container.rpcRegister('gadgets.window.getContainerDimensions', function(rpcArgs) {
    var el = document.documentElement; // Container element
    return {
      width : el ? el.clientWidth : -1,
      height: el ? el.clientHeight : -1
    };
  });

  /**
   * Method will be called when a gadget wants to close itself or the
   * parent gadget wants to close a gadget or url site it has opened.
   *
   * @param {Object}
   *          site: The site to close.
   */
  this.destroyElement = function(site) {
    console.log('container needs to define destroyElement function');
  };
});
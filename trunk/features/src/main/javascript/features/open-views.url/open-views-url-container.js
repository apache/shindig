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
 * @fileoverview Container-side url script.
 */

osapi.container.Container.addMixin('views', function(container) {
  var self = this;

  /**
   * Opens a URL in the container UI. The location of the URL site will be
   * determined by the container based on the target view. The container would
   * open the view in a dialog, if opt_viewTarget=dialog or the gadgets view in
   * a tab for opt_viewTarget=tab
   *
   * @param {string}
   *          url: URL to a web page to open in a URL site in the container.
   *          (Note this should not be a URL to a gadget definition.).
   * @param {string=}
   *          opt_viewTarget: Optional parameter, the view that indicates where
   *          to open the URL.
   * @param {Object=} opt_coordinates: Object containing the desired absolute
   *          positioning css parameters (top|bottom|left|right) with
   *          appropriate values.  All values are relative to the calling
   *          gadget.
   *          You may specify top AND bottom or left AND right parameters to
   *          indirectly define height and width.
   *          It is expected that coordinates will only be used with
   *          viewTargets of FLOAT. Containers may implement the behavior
   *          for other viewTargets, and custom viewTargets at their
   *          discretion.
   * @returns {string} The ID of the site created, if a callback was registered.
   */
  container.rpcRegister('gadgets.views.openUrl', function (rpcArgs, url, opt_viewTarget, opt_coordinates) {
    var orig_site = container.getGadgetSiteByIframeId_(rpcArgs.f),
      rel = orig_site.getActiveSiteHolder().getIframeElement();

    function callback(content_div) {
      var site = container.newUrlSite(content_div);

      var renderParams = {}; // (height, width, class,userPrefsObject)
      renderParams[osapi.container.RenderParam.WIDTH] = '100%';
      renderParams[osapi.container.RenderParam.HEIGHT] = '100%';

      container.navigateUrl(site, url, renderParams);

      // record who opened this site, so that if they use the siteId to close it later,
      // we don't inadvertently allow other gadgets to guess the id and close the site.
      site.ownerId_ = rpcArgs.f;
      rpcArgs.callback([site.getId()]);
    };

    var content_div = self.createElementForUrl(rel, opt_viewTarget, opt_coordinates, orig_site, callback);
    if (content_div) {
      callback(content_div);
    }
  });

  /**
   * Method will be called to create the DOM element to place the UrlSite
   * in. An implementation must either return an element or call
   * the provided callback asynchronously, but not both.
   *
   * @param {Element}
   *          rel: The element to which opt_coordinates values are
   *          relative.
   * @param {string=}
   *          opt_view: Optional parameter, the view to open. If not
   *          included the container should use its default view.
   * @param {Object=}
   *          opt_coordinates: Object containing the desired absolute
   *          positioning css parameters (top|bottom|left|right) with
   *          appropriate values. All values are relative to the calling
   *          gadget.
   * @param {osapi.container.Site} parentSite
   *          The site opening the url.
   * @param {function(element)} opt_callback
   *          A callback to asynchronously provide the result of the createElement call.
   * @return {Object} The DOM element to place the UrlSite object in.
   */
  this.createElementForUrl = function(rel, opt_viewTarget, opt_coordinates, parentSite, opt_callback) {
    console.log('container needs to define createElementForUrl function');
  };
});
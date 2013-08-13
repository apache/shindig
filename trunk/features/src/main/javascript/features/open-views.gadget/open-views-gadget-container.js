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
   * Opens a gadget in the container UI. The location of the gadget site in the
   * container will be determined by the view target passed in. The container
   * would open the view in a dialog, if view target is dialog or the gadgets
   * view in a tab for view target is tab.
   *
   * @param {number}
   *          resultCallback: Callback id of function to be called when the gadget
   *          closes. The function will be called with the return value as a
   *          parameter.
   * @param {Object.<string, string|Object>=}
   *          opt_params: These are optional parameters which can be used to
   *            open gadgets. The following parameters may be included in this
   *            object.
   *            {string} view: The view to render. Should be one of the
   *              views returned by calling gadgets.views.getSupportedViews. If
   *              the view is not included the default view will be rendered.
   *            {string} viewTarget: The view that indicates where to open the
   *              gadget. For example, tab, dialog or modaldialog
   *            {Object} viewParams: View parameters for the view being
   *              rendered.
   *            {Object} coordinates: Object containing the desired absolute
   *              positioning css parameters (top|bottom|left|right) with
   *              appropriate values. All values are relative to the calling
   *              gadget.
   *              Do not specify top AND bottom or left AND right parameters to
   *              indirectly define height and width, use viewParams for that.
   *              The result of doing so here is undefined.
   *              It is expected that coordinates will only be used with
   *              viewTargets of FLOAT. Containers may implement the behavior
   *              for other viewTargets, and custom viewTargets at their
   *              discretion.
   */
  container.rpcRegister('gadgets.views.openGadget', function (rpcArgs, resultCallback, opt_params) {
    var navigateCallback = rpcArgs.callback,
        siteOwnerId = rpcArgs.f,
        gadgetUrl = '',
        orig_site = container.getGadgetSiteByIframeId_(rpcArgs.f);

    if ((typeof orig_site != 'undefined') &&
            (typeof orig_site.getActiveSiteHolder() != 'undefined')) {
      // get url through gadget holder
      gadgetUrl = orig_site.getActiveSiteHolder().getUrl();
    }

    var view = '',
        viewTarget = '',
        viewParams = {},
        coordinates;
    if (opt_params) {
      if (opt_params.view)
        view = opt_params.view;
      if (opt_params.viewTarget)
        viewTarget = opt_params.viewTarget;
      if (opt_params.viewParams)
        viewParams = opt_params.viewParams;
      if(opt_params.coordinates) {
        coordinates = opt_params.coordinates;
      }
    }

    var rel = container.getGadgetSiteByIframeId_(rpcArgs.f).getActiveSiteHolder()
    .getIframeElement();

    container.preloadGadget(gadgetUrl, function(result) {
      /*
       * result[gadgetUrl] : metadata
       */
      var metadata = {};
      if ((typeof result != 'undefined') && (typeof result[gadgetUrl] != 'undefined')) {
        if (result[gadgetUrl].error) {
          gadgets.error('Failed to preload gadget : ' + gadgetUrl);
          if (navigateCallback != null) {
            navigateCallback([null, result[gadgetUrl]]);
          }
          return;
        } else {
          metadata = result[gadgetUrl];
        }
      }

      function callback(elem) {
        var renderParams = {},
            site = container.newGadgetSite(elem);

        site.ownerId_ = siteOwnerId;

        if ((typeof view != 'undefined') && view !== '') {
          renderParams[osapi.container.RenderParam.VIEW] = view;
        }
        renderParams[osapi.container.RenderParam.WIDTH] = '100%';
        renderParams[osapi.container.RenderParam.HEIGHT] = '100%';

        container.navigateGadget(site, gadgetUrl, viewParams, renderParams, function(metadata) {
          if (metadata) {
            self.resultCallbacks_[site.getId()] = resultCallback;
          }
          if (navigateCallback) {
            navigateCallback([site.getId(), metadata]);
          }
        });
      }

      var elem = self.createElementForGadget(
        metadata, rel, view, viewTarget, coordinates, orig_site, callback
      );
      if (elem) {
        callback(elem);
      }
    });
  });

  /**
   * Method will be called to create the DOM element to place the Gadget
   * Site in. An implementation must either return an element or call
   * the provided callback asynchronously, but not both.
   *
   * @param {Object}
   *          metadata: Gadget meta data for the gadget being opened in
   *          this GadgetSite.
   * @param {Element}
   *          rel: The element to which opt_coordinates values are
   *          relative.
   * @param {string=}
   *          opt_view: Optional parameter, the view that indicates the
   *          type of GadgetSite.
   * @param {string=}
   *          opt_viewTarget: Optional parameter, the view target indicates
   *          where to open the gadget.
   * @param {Object=}
   *          opt_coordinates: Object containing the desired absolute
   *          positioning css parameters (top|bottom|left|right) with
   *          appropriate values. All values are relative to the calling
   *          gadget.
   * @param {osapi.container.Site} parentSite
   *          The site opening the gadget view.
   * @param {function(element)} opt_callback
   *          A callback to asynchronously provide the result of the createElement call.
   * @return {Object} The DOM element to place the GadgetSite in.
   */
  this.createElementForGadget = function(metadata, rel, opt_view, opt_viewTarget,
      opt_coordinates, parentSite, opt_callback) {
    console.log('container needs to define createElementForGadget function');
  };
});
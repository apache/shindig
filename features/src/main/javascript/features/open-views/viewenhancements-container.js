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
 * @fileoverview Container-side view enhancements.
 */

(function() {

  var context;

  // Mapping between id and callback function
  var resultCallbackMap;

  // Mapping between id and return value
  var returnValueMap;

  function init(container) {

    context = container;

    gadgets.rpc.register('gadgets.views.openGadget', openGadget);

    gadgets.rpc.register('gadgets.views.openEmbeddedExperience', openEE);

    gadgets.rpc.register('gadgets.views.openUrl', openUrl);

    gadgets.rpc.register('gadgets.views.close', close);

    gadgets.rpc.register('gadgets.views.setReturnValue', setReturnValue);

    gadgets.rpc.register('gadgets.window.getContainerDimensions', getContainerDimensions);

    resultCallbackMap = {};

    returnValueMap = {};
  };
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
  function openGadget(resultCallback, opt_params) {

    var navigateCallback = this.callback,
        siteOwnerId = this.f,
        gadgetUrl = '',
        orig_site = context.getGadgetSiteByIframeId_(this.f);

    if ((typeof orig_site != 'undefined') &&
            (typeof orig_site.getActiveSiteHolder() != 'undefined')) {
      // get url through gadget holder
      gadgetUrl = orig_site.getActiveSiteHolder().getUrl();
    }

    var view = '',
        viewTarget = '',
        viewParams = {},
        coordinates,
        rel;
    if (opt_params) {
      if (opt_params.view)
        view = opt_params.view;
      if (opt_params.viewTarget)
        viewTarget = opt_params.viewTarget;
      if (opt_params.viewParams)
        viewParams = opt_params.viewParams;
      if(opt_params.coordinates) {
        coordinates = opt_params.coordinates;
        rel = context.getGadgetSiteByIframeId_(this.f).getActiveSiteHolder()
          .getIframeElement();
      }
    }
    context.preloadGadget(gadgetUrl, function(result) {
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
        }else {
          metadata = result[gadgetUrl];
        }
      }

      var renderParams = {},
           site = context.newGadgetSite(
	     context.views.createElementForGadget(
               metadata, view, viewTarget, coordinates, rel
             )
           );

      site.ownerId_ = siteOwnerId;

      if ((typeof view != 'undefined') && view !== '') {
        renderParams[osapi.container.RenderParam.VIEW] = view;
      }
      renderParams[osapi.container.RenderParam.WIDTH] = '100%';
      renderParams[osapi.container.RenderParam.HEIGHT] = '100%';

      context.navigateGadget(site, gadgetUrl, viewParams, renderParams, function(metadata) {
        if (metadata) {
          resultCallbackMap[site.getId()] = resultCallback;
        }
        if (navigateCallback) {
          navigateCallback([site.getId(), metadata]);
        }
      });
    });
  }

  /**
   * Opens an embedded experience in the container UI. The location of the site
   * in the container will be determined by the view target passed in. The
   * container would open the embedded experience in a dialog, if view target is
   * dialog or the embedded experience view in a tab for view target is tab.
   *
   * @param {number}
   *          resultCallback: Callback function id to be called when the embedded
   *          experience closes. The function will be called with the return
   *          value as a parameter.
   * @param {Object}
   *          dataModel: The embedded experiences data model.
   * @param {Object}
   *          opt_params: These are optional parameters which can be used to
   *            open gadgets. The following parameters may be included in this
   *            object.
   *            {string} viewTarget: The view that indicates where to open
   *              the gadget. For example, tab, dialog or modaldialog
   *            {Object} viewParams: View parameters for the view being
   *              rendered.
   *            {Object} coordinates: Object containing the desired absolute
   *              positioning css parameters (top|bottom|left|right) with
   *              appropriate values.  All values are relative to the calling
   *              gadget.
   *              Do not specify top AND bottom or left AND right parameters to
   *              indirectly define height and width. Use viewParams for that.
   *              The result of doing so here is undefined.
   *              It is expected that coordinates will only be used with
   *              viewTargets of FLOAT. Containers may implement the behavior
   *              for other viewTargets, and custom viewTargets at their
   *              discretion.
   */
  function openEE(resultCallback, dataModel, opt_params) {
    var navigateCallback = this.callback,
        siteOwnerId = this.f,
        gadgetUrl = dataModel.gadget;
    var navigateEE = function() {
      var viewTarget = '',
          viewParams = {},
          coordinates,
          rel;
      if (opt_params) {
        if (opt_params.viewTarget)
	        viewTarget = opt_params.viewTarget;
        if (opt_params.viewParams)
          viewParams = opt_params.viewParams;
        if (opt_params.coordinates) {
          coordinates = opt_params.coordinates;
          rel = context.getGadgetSiteByIframeId_(siteOwnerId).getActiveSiteHolder()
            .getIframeElement();
        }
      }

      var element = context.views.createElementForEmbeddedExperience(
        viewTarget, coordinates, rel
      );

      var gadgetRenderParams = {};
      gadgetRenderParams[osapi.container.RenderParam.VIEW] =
          osapi.container.ee.RenderParam.EMBEDDED;
      gadgetRenderParams[osapi.container.RenderParam.WIDTH] = '100%';
      gadgetRenderParams[osapi.container.RenderParam.HEIGHT] = '100%';

      var urlRenderParams = {};
      urlRenderParams[osapi.container.RenderParam.WIDTH] = '100%';
      urlRenderParams[osapi.container.RenderParam.HEIGHT] = '100%';

      var eeRenderParams = {};
      eeRenderParams[osapi.container.ee.RenderParam.GADGET_RENDER_PARAMS] =
          gadgetRenderParams;
      eeRenderParams[osapi.container.ee.RenderParam.URL_RENDER_PARAMS] =
          urlRenderParams;
      eeRenderParams[osapi.container.ee.RenderParam.GADGET_VIEW_PARAMS] =
          viewParams;

      context.ee.navigate(element, dataModel, eeRenderParams, function(site, result) {
        site.ownerId_ = siteOwnerId;
        if (result) {
          resultCallbackMap[site.getId()] = resultCallback;
        }
        if (navigateCallback) {
          navigateCallback([site.getId(), result]);
        }
      });
    };

    if(gadgetUrl) {
      //Check to make sure we can actually reach the gadget we are going to try
      //to render before we do anything else
      context.preloadGadget(gadgetUrl, function(result) {
        if (!result[gadgetUrl] || result[gadgetUrl].error) {
          //There was an error, check to see if there is still the option to
          //render the url, else just call the navigateCallback
          if (!dataModel.url) {
            if (navigateCallback != null) {
              navigateCallback([null, result[gadgetUrl] || {"error" : result}]);
            }
            return;
          }
        }
        navigateEE();
      });
    } else {
      navigateEE();
    }
  }


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
  function openUrl(url, opt_viewTarget, opt_coordinates) {
    var rel;
    if (opt_coordinates) {
      rel = context.getGadgetSiteByIframeId_(this.f).getActiveSiteHolder()
        .getIframeElement();
    }
    var content_div = context.views.createElementForUrl(
      opt_viewTarget, opt_coordinates, rel
    );

    var site = context.newUrlSite(content_div);

    var renderParams = {}; // (height, width, class,userPrefsObject)
    renderParams[osapi.container.RenderParam.WIDTH] = '100%';
    renderParams[osapi.container.RenderParam.HEIGHT] = '100%';

    context.navigateUrl(site, url, renderParams);

    // record who opened this site, so that if they use the siteId to close it later,
    // we don't inadvertently allow other gadgets to guess the id and close the site.
    site.ownerId_ = this.f;
    return site.getId();
  }

  /**
   * Closes an opened site. If the opt_id parameter is null the container will
   * close the calling site.
   *
   * @param {Object=}
   *          opt_site: Optional parameter which specifies what site to close.
   *          If not provided or null, it will close the current gadget site.
   */
  function close(opt_site) {
    // opt_site may be 0, do not do a truthy test on the value.
    var orig_site = context.getGadgetSiteByIframeId_(this.f),
        site = typeof(opt_site) != 'undefined' && opt_site != null ? 
          context.getSiteById(opt_site) : orig_site;

    if (!site) {
      return;
    }

    // A side effect of this check is that if a gadget tries to close a site it did not
    // create, the gadget itself will be closed.  That's the price to pay for trying to
    // be evil, I guess :)
    var siteId = site.getId(),
        allowed = site == orig_site || site.ownerId_ == this.f;

    if (typeof(siteId) != 'undefined' && allowed) {
      var returnValue = returnValueMap[siteId],
          resultCallback = resultCallbackMap[siteId];

      if (typeof(resultCallback) != 'undefined') { // may be 0
        if (typeof(returnValue) != 'undefined' && site.ownerId_) {
          gadgets.rpc.call(site.ownerId_, 'gadgets.views.deliverResult', null,
            resultCallback, returnValue
          );
        }
        delete resultCallbackMap[siteId];
      }
    }

    context.views.destroyElement(site);
  }

  /**
   * Sets the return value for the current window. This method should only be
   * called inside those secondary view types defined in gadgets.views.ViewType.
   * For example, DIALOG or MODALDIALOG
   *
   * @param {object}
   *          returnValue: Return value for this window.
   */
  function setReturnValue(returnValue) {
    var site;
    if (site = context.getGadgetSiteByIframeId_(this.f)) {
      returnValueMap[site.getId()] = returnValue;
    }
  }

  /**
   * Gets the dimensions of the container displaying the gadget.
   */
  function getContainerDimensions() {
    var el = document.documentElement; // Container element
    return {
      width : el ? el.clientWidth : -1,
      height: el ? el.clientHeight : -1
    };
  }

  osapi.container.Container.addMixin('views', function(container) {

    init(container);

    return { // this is a map of the public API in the namespace
      /**
       * Method will be called to create the DOM element to place the Gadget
       * Site in.
       *
       * @param {Object}
       *          metadata: Gadget meta data for the gadget being opened in
       *          this GadgetSite.
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
       * @param {Element=}
       *          opt_rel: The element to which opt_coordinates values are
       *          relative.
       * @return {Object} The DOM element to place the GadgetSite in.
       */
      'createElementForGadget' : function(metadata, opt_view, opt_viewTarget,
          opt_coordinates, opt_rel) {
        console.log('container need to define createElementForGadget function');
      },

      /**
       * Method will be called to create the DOM element to place the embedded
       * experience in.
       *
       * @param {string=}
       *          opt_viewTarget:  Optional parameter, the view target indicates
       *          where to open.
       *
       * @param {Object=}
       *          opt_coordinates: Object containing the desired absolute
       *          positioning css parameters (top|bottom|left|right) with
       *          appropriate values. All values are relative to the calling
       *          gadget.
       * @param {Element=}
       *          opt_rel: The element to which opt_coordinates values are
       *          relative.
       * @return {Object} The DOM element to place the embedded experience in.
       */

      'createElementForEmbeddedExperience' : function(opt_viewTarget,
          opt_coordinates, opt_rel) {
        console.log('container need to define ' +
            'createElementForEmbeddedExperience function');
      },

      /**
       * Method will be called to create the DOM element to place the UrlSite
       * in.
       *
       * @param {string=}
       *          opt_view: Optional parameter, the view to open. If not
       *          included the container should use its default view.
       * @param {Object=}
       *          opt_coordinates: Object containing the desired absolute
       *          positioning css parameters (top|bottom|left|right) with
       *          appropriate values. All values are relative to the calling
       *          gadget.
       * @param {Element=}
       *          opt_rel: The element to which opt_coordinates values are
       *          relative.
       * @return {Object} The DOM element to place the UrlSite object in.
       */

      'createElementForUrl' : function(opt_viewTarget, opt_coordinates,
          opt_rel) {
        console.log('container need to define createElementForUrl function');
      },

      /**
       * Method will be called when a gadget wants to close itself or the
       * parent gadget wants to close a gadget or url site it has opened.
       *
       * @param {Object}
       *          site: The site to close.
       */
      'destroyElement' : function(site) {
        console.log('container need to define destroyElement function');
      }
    };
  }); //end addMixin
}());

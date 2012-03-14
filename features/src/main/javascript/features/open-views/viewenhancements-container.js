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

    context.rpcRegister('gadgets.views.openGadget', openGadget);

    context.rpcRegister('gadgets.views.openEmbeddedExperience', openEE);

    context.rpcRegister('gadgets.views.openUrl', openUrl);

    context.rpcRegister('gadgets.views.close', close);

    context.rpcRegister('gadgets.views.setReturnValue', setReturnValue);

    context.rpcRegister('gadgets.window.getContainerDimensions', getContainerDimensions);

    resultCallbackMap = {};

    returnValueMap = {};

    var lifecyclecb = {};
    lifecyclecb[osapi.container.CallbackType.ON_BEFORE_CLOSE] = function(site) {
      var id = site.getId(),
          returnValue = returnValueMap[id],
          resultCallback = resultCallbackMap[id];

      //Checking the truthiness of resultCallback is bad because 0 is a value value
      //check whether it is undefined
      if (typeof resultCallback !== 'undefined') {
        if (site.ownerId_) {
          gadgets.rpc.call(site.ownerId_, 'gadgets.views.deliverResult', null,
            resultCallback, returnValue
          );
        }
      }

      delete returnValueMap[id];
      delete resultCallbackMap[id];
    };
    context.addGadgetLifecycleCallback("open-views", lifecyclecb);
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
  function openGadget(rpcArgs, resultCallback, opt_params) {

    var navigateCallback = rpcArgs.callback,
        siteOwnerId = rpcArgs.f,
        gadgetUrl = '',
        orig_site = context.getGadgetSiteByIframeId_(rpcArgs.f);

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
    var rel = context.getGadgetSiteByIframeId_(rpcArgs.f).getActiveSiteHolder()
    .getIframeElement();
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
          elem = context.views.createElementForGadget(
            metadata, rel, view, viewTarget, coordinates, orig_site
          ),
          site = context.newGadgetSite(elem);

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
  function openEE(rpcArgs, resultCallback, dataModel, opt_params) {
    var navigateCallback = rpcArgs.callback,
        siteOwnerId = rpcArgs.f,
        gadgetUrl = dataModel.gadget;
    var navigateEE = function(opt_metadata) {
      var viewTarget = '',
          viewParams = {},
          coordinates;
      if (opt_params) {
        if (opt_params.viewTarget)
	        viewTarget = opt_params.viewTarget;
        if (opt_params.viewParams)
          viewParams = opt_params.viewParams;
        if (opt_params.coordinates) {
          coordinates = opt_params.coordinates;
        }
      }
      var orig_site = context.getGadgetSiteByIframeId_(siteOwnerId),
          rel = orig_site.getActiveSiteHolder().getIframeElement();

      var element = context.views.createElementForEmbeddedExperience(
        rel, opt_metadata, viewTarget, coordinates, orig_site
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
        navigateEE(result[gadgetUrl]);
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
  function openUrl(rpcArgs, url, opt_viewTarget, opt_coordinates) {
    var orig_site = context.getGadgetSiteByIframeId_(rpcArgs.f),
        rel = orig_site.getActiveSiteHolder().getIframeElement();
    var content_div = context.views.createElementForUrl(
      rel, opt_viewTarget, opt_coordinates, orig_site
    );

    var site = context.newUrlSite(content_div);

    var renderParams = {}; // (height, width, class,userPrefsObject)
    renderParams[osapi.container.RenderParam.WIDTH] = '100%';
    renderParams[osapi.container.RenderParam.HEIGHT] = '100%';

    context.navigateUrl(site, url, renderParams);

    // record who opened this site, so that if they use the siteId to close it later,
    // we don't inadvertently allow other gadgets to guess the id and close the site.
    site.ownerId_ = rpcArgs.f;
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
  function close(rpcArgs, opt_site) {
    // opt_site may be 0, do not do a truthy test on the value.
    var orig_site = context.getGadgetSiteByIframeId_(rpcArgs.f),
        site = typeof(opt_site) != 'undefined' && opt_site != null ?
          context.getSiteById(opt_site) : orig_site;

    if (!site) {
      return;
    }

    if (site == orig_site || site.ownerId_ == rpcArgs.f) {
      // The provided method must ultimately call container.closeGadget(site);
      context.views.destroyElement(site);
    }
  }

  /**
   * Sets the return value for the current window. This method should only be
   * called inside those secondary view types defined in gadgets.views.ViewType.
   * For example, DIALOG or MODALDIALOG
   *
   * @param {object}
   *          returnValue: Return value for this window.
   */
  function setReturnValue(rpcArgs, returnValue) {
    var site;
    if (site = context.getGadgetSiteByIframeId_(rpcArgs.f)) {
      returnValueMap[site.getId()] = returnValue;
    }
  }

  /**
   * Gets the dimensions of the container displaying the gadget.
   */
  function getContainerDimensions(rpcArgs) {
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
       * @return {Object} The DOM element to place the GadgetSite in.
       */
      'createElementForGadget' : function(metadata, rel, opt_view, opt_viewTarget,
          opt_coordinates, parentSite) {
        console.log('container need to define createElementForGadget function');
      },

      /**
       * Method will be called to create the DOM element to place the embedded
       * experience in.
       *
       *@param {Element}
       *          rel: The element to which opt_coordinates values are
       *          relative.
       * @param {Object}
       *          opt_gadgetInfo: Info for the gadget embedded experience,
       *          if the data model contains a gadget URL.
       * @param {string=}
       *          opt_viewTarget:  Optional parameter, the view target indicates
       *          where to open.
       * @param {Object=}
       *          opt_coordinates: Object containing the desired absolute
       *          positioning css parameters (top|bottom|left|right) with
       *          appropriate values. All values are relative to the calling
       *          gadget.
       * @param {osapi.container.Site} parentSite
       *          The site opening the EE.
       * @return {Object} The DOM element to place the embedded experience in.
       */

      'createElementForEmbeddedExperience' : function(rel, opt_gadgetInfo, opt_viewTarget,
          opt_coordinates, parentSite) {
        console.log('container need to define ' +
            'createElementForEmbeddedExperience function');
      },

      /**
       * Method will be called to create the DOM element to place the UrlSite
       * in.
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
       * @return {Object} The DOM element to place the UrlSite object in.
       */

      'createElementForUrl' : function(rel, opt_viewTarget, opt_coordinates, parentSite) {
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

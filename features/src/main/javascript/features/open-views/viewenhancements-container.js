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

  // mapping between iframe id and site
  var iframeSiteMap;

  function init(container) {

    context = container;

    gadgets.rpc.register('gadgets.views.openGadget', openGadget);

    gadgets.rpc.register('gadgets.views.openEmbeddedExperience', openEE);

    gadgets.rpc.register('gadgets.views.openUrl', openUrl);

    gadgets.rpc.register('gadgets.views.close', close);

    gadgets.rpc.register('gadgets.views.setReturnValue', setReturnValue);

    gadgets.rpc.register('gadgets.window.getContainerDimensions',
        getContainerDimensions);

    resultCallbackMap = {};

    returnValueMap = {};

    iframeSiteMap = {};
  };
  /**
   * Opens a gadget in the container UI. The location of the gadget site in the
   * container will be determined by the view target passed in. The container
   * would open the view in a dialog, if view target is dialog or the gadgets
   * view in a tab for view target is tab.
   *
   * @param {function}
   *          resultCallback: Callback function to be called when the gadget
   *          closes. The function will be called with the return value as a
   *          parameter.
   * @param {function}
   *          navigateCallback: Callback function to be called with the the
   *          Site which has been opened and metadata.
   * @param {Object.<string, string|Object>=}
   *          opt_params: These are optional parameters which can be used to
   *          open gadgets. The following parameters may be included in this
   *          object. {string} view: The view to render. Should be one of the
   *          views returned by calling gadgets.views.getSupportedViews. If the
   *          view is not included the default view will be rendered. {string}
   *          viewTarget: The view that indicates where to open the gadget. For
   *          example, tab, dialog or modaldialog {Object} viewParams: View
   *          parameters for the view being rendered.
   */
  function openGadget(resultCallback, navigateCallback, opt_params) {

    var gadgetUrl = '';

    var orig_site = context.getGadgetSiteByIframeId_(this.f);

    if (orig_site !== undefined &&
            orig_site.getActiveGadgetHolder() !== undefined) {
      // get url through gadget holder
      gadgetUrl = orig_site.getActiveGadgetHolder().getUrl();
    }

    var view = '';
    var viewTarget = '';
    var viewParams = {};
    if (opt_params !== undefined) {
      if (opt_params.view !== undefined)
        view = opt_params.view;
      if (opt_params.viewTarget !== undefined)
        viewTarget = opt_params.viewTarget;
      if (opt_params.viewParams !== undefined)
        viewParams = opt_params.viewParams;
    }
    context.preloadGadget(gadgetUrl, function(result) {
      /*
       * result[gadgetUrl] : metadata
       */
      var metadata = {};
      if (result !== undefined && result[gadgetUrl] !== undefined) {
        if (result[gadgetUrl].error) {
          gadgets.error('Failed to preload gadget : ' + gadgetUrl);
          if (navigateCallback != null) {
            navigateCallback(null, result[gadgetUrl]);
          }
          return;
        }else {
          metadata = result[gadgetUrl];
        }
      }
      var content_div = context.views.createElementForGadget(metadata, view,
          viewTarget);
      var site = context.newGadgetSite(content_div);

      var renderParams = {};

      if (view !== undefined && view !== '') {
        renderParams[osapi.container.RenderParam.VIEW] = view;
      }
      renderParams[osapi.container.RenderParam.WIDTH] = '100%';
      renderParams[osapi.container.RenderParam.HEIGHT] = '100%';

      context.navigateGadget(site, gadgetUrl, viewParams, renderParams,
          function(metadata) {
            if (metadata != null) {
              processSiteAndCallbackInfo(site, resultCallback);
            }
            if (navigateCallback != null) {
              navigateCallback(site, metadata);
            }
          });

    });
  }

  /**
   * Processes the site and callback information and stores it in global maps.
   * @param {osapi.container.GadgetSite | osapi.container.UrlSite} site the site
   * that was created for the gadget.
   * @param {Function} resultCallback called with any result the gadget chooses
   * to set.
   */
  function processSiteAndCallbackInfo(site, resultCallback) {
    var iframeId;
    if (site && site.getActiveGadgetHolder()) {
      iframeId = site.getActiveGadgetHolder().getIframeId();
    }

    iframeSiteMap[iframeId] = site;

    // use the site id as key
    if (typeof site.getId() !== 'undefined' && resultCallback != null) {
      resultCallbackMap[site.getId()] = resultCallback;
    }
  }

  /**
   * Opens an embedded experience in the container UI. The location of the site
   * in the container will be determined by the view target passed in. The
   * container would open the embedded experience in a dialog, if view target is
   * dialog or the embedded experience view in a tab for view target is tab.
   *
   * @param {Function}
   *          resultCallback: Callback function to be called when the embedded
   *          experience closes. The function will be called with the return
   *          value as a parameter.
   * @param {Function}
   *          navigateCallback: Callback function to be called with the embedded
   *          experience has rendered.
   * @param {Object}
   *          opt_params: These are optional parameters which can be used to
   *          open gadgets. The following parameters may be included in this
   *          object. {string} viewTarget: The view that indicates where to open
   *          the gadget. For example, tab, dialog or modaldialog {Object}
   *          viewParams: View parameters for the view being rendered.
   */
  function openEE(resultCallback, navigateCallback, dataModel, opt_params) {
    var gadgetUrl = dataModel.gadget;
    if (gadgetUrl) {
      //Check to make sure we can actually reach the gadget we are going to try
      //to render before we do anything else
      context.preloadGadget(gadgetUrl, function(result) {
        if (result[gadgetUrl] == null ||
                (result[gadgetUrl] != null && result[gadgetUrl].error)) {
          //There was an error, check to see if there is still the option to
          //render the url, else just call the navigateCallback
          if (!dataModel.url) {
            if (navigateCallback != null) {
              navigateCallback(null, result[gadgetUrl]);
            }
            return;
          }
        }

        var viewTarget = '';
        var viewParams = {};
        if (opt_params != undefined) {
          if (opt_params.viewTarget != undefined)
            viewTarget = opt_params.viewTarget;
          if (opt_params.viewParams != undefined)
            viewParams = opt_params.viewParams;
        }

        var element = context.views.createElementForEmbeddedExperience(
            viewTarget);

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

        context.ee.navigate(element, dataModel, eeRenderParams, function(site,
                metadata) {
              if (metadata != null) {
                processSiteAndCallbackInfo(site, resultCallback);
              }
              if (navigateCallback != null) {
                navigateCallback(site, metadata);
              }
            });
      });
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
   * @param {function}
   *          navigateCallback: Callback function to be called with the site
   *          which has been opened.
   * @param {string=}
   *          opt_viewTarget: Optional parameter,the view that indicates where
   *          to open the URL.
   */
  function openUrl(url, navigateCallback, opt_viewTarget) {
    var content_div = context.views.createElementForUrl(opt_viewTarget);

    var site = context.newUrlSite(content_div);

    var renderParams = {}; // (height, width, class,userPrefsObject)
    renderParams[osapi.container.RenderParam.WIDTH] = '100%';
    renderParams[osapi.container.RenderParam.HEIGHT] = '100%';

    context.navigateUrl(site, url, renderParams);

    if (navigateCallback !== undefined) {
      navigateCallback(site);
    }
  }

  /**
   * Closes an opened site. If the opt_id parameter is null the container will
   * close the calling site.
   *
   * @param {object=}
   *          opt_site: Optional parameter which specifies what site to close.
   *          If null it will close the current gadget site.
   */
  function close(opt_site) {
    // this.f is the frame id
    var iframeId = this.f;
    var site;

    if (opt_site == undefined || opt_site == '') {
      site = iframeSiteMap[iframeId];
    }
    else {
      site = opt_site;
    }

    if (site != null) {
      var siteId = site.getId();

      if (siteId !== undefined && resultCallbackMap[siteId] !== undefined &&
              returnValueMap[siteId] !== undefined) {
        var returnValue = returnValueMap[siteId];
        // execute the result callback function with return value as parameter
        resultCallbackMap[siteId](returnValue);
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
    if (returnValue !== undefined && iframeSiteMap[this.f] !== undefined) {
      var siteId = iframeSiteMap[this.f].getId();
      // use the site id as key
      if (siteId !== undefined) {
        returnValueMap[siteId] = returnValue;
      }
    }
  }

  /**
   * Gets the dimensions of the container displaying the gadget.
   */
  function getContainerDimensions() {
    var el = document.documentElement; // Container
    // element
    if (el !== undefined)
      // return client width and client height
      return {
        'width' : el.clientWidth,
        'height' : el.clientHeight
      };
    else
      return {
        'width' : -1,
        'height' : -1
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
       * @return {Object} The DOM element to place the GadgetSite in.
       */
      'createElementForGadget' : function(metadata, opt_view, opt_viewTarget) {
        console.log('container need to define createElementForGadget function');
      },

      /**
       * Method will be called to create the DOM element to place the embedded
       * experience in.
       *
       * @param {string=}
       *          opt_viewTarget:  Optional parameter, the view target indicates
       *          where to open.
       * @return {Object} The DOM element to place the embedded experience in.
       */

      'createElementForEmbeddedExperience' : function(opt_viewTarget) {
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
       * @return {Object} The DOM element to place the UrlSite object in.
       */

      'createElementForUrl' : function(opt_viewTarget) {
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

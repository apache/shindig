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
   * Opens an embedded experience in the container UI. The location of the site
   * in the container will be determined by the view target passed in. The
   * container would open the embedded experience in a dialog, if view target is
   * dialog or the embedded experience view in a tab for view target is tab.
   *
   * @param {number}
   *          resultCallback: Callback function id to be called when the embedded
   *          experience closes. The function will be called with the return
   *          value as a parameter.
   * @param {Object|string}
   *          dataModel: The embedded experiences data model object or the xml or
   *          json string representation of that data model.
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
  container.rpcRegister('gadgets.views.openEmbeddedExperience', function (rpcArgs, resultCallback, dataModel, opt_params) {
    var navigateCallback = rpcArgs.callback,
        siteOwnerId = rpcArgs.f,
        gadgetUrl = dataModel.gadget;

    if (typeof(dataModel) == 'string') {
      var match = new RegExp('^<(embed)>', 'i').exec(dataModel);
      if (match && match[1]) {
        try {
          var parsed = gadgets.json.xml.convertXmlToJson(opensocial.xmlutil.parseXML(dataModel));
          dataModel = parsed && parsed[match[1]] || dataModel;
        } catch(ignore){}
      } else {
        try {
          var parsed = gadgets.json.parse(dataModel);
          dataModel = parsed || dataModel;
        } catch(ignore){}
      }
    }

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
      var orig_site = container.getGadgetSiteByIframeId_(siteOwnerId),
          rel = orig_site.getActiveSiteHolder().getIframeElement();

      var opt_containerContext = self.getContainerAssociatedContext(dataModel, opt_metadata);

      function callback(element) {
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

        container.ee.navigate(element, dataModel, eeRenderParams, function(site, result) {
          site.ownerId_ = siteOwnerId;
          if (result) {
            self.resultCallbacks_[site.getId()] = resultCallback;
          }
          if (navigateCallback) {
            navigateCallback([site.getId(), result]);
          }
        }, opt_containerContext);
      }

      var element = self.createElementForEmbeddedExperience(
        rel, opt_metadata, viewTarget, coordinates, orig_site, callback
      );

      if (element) {
        callback(element);
      }
    };

    if(gadgetUrl) {
      //Check to make sure we can actually reach the gadget we are going to try
      //to render before we do anything else
      container.preloadGadget(gadgetUrl, function(result) {
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
  });

  /**
   * This function will be called to create the DOM element to place the embedded
   * experience in. An implementation must either return an element or call
   * the provided callback asynchronously, but not both.
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
   * @param {function(element)} opt_callback
   *          A callback to asynchronously provide the result of the createElement call.
   * @return {Object} The DOM element to place the embedded experience in.
   */
  this.createElementForEmbeddedExperience = function(rel, opt_gadgetInfo, opt_viewTarget,
      opt_coordinates, parentSite, opt_callback) {
    console.log('container needs to define createElementForEmbeddedExperience function');
  };

  /**
   * This function will be called to inject additional context when opening gadget in EE mode.
   *
   * @param {Object} dataModel: The embedded experiences data model.
   * @param {Object} opt_gadgetInfo: Info for the gadget embedded experience,
   *                 if the data model contains a gadget URL.
   * @return {Object} Additional context need to be passed by container.
   */
  this.getContainerAssociatedContext = function(dataModel, opt_gadgetInfo) {
    return null;
  }

});
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * This feature adds additional functionality to the common container to support
 * rendering embedded experiences.
 */
(function() {

  osapi.container.Container.addMixin('ee', function(context) {

    /**
     * Navigates to an EE gadget
     * @param {Element} element the element to put the gadget in.
     * @param {Object} dataModel the EE data model.
     * @param {Object} renderParams params to augment the rendering.
     * @param {Function} opt_callback called once the gadget has been navigated to.
     */
    function navigateGadget_(element, dataModel, renderParams, opt_callback) {
      var viewParams = renderParams[osapi.container.ee.RenderParam.GADGET_VIEW_PARAMS] || {};
      var localRenderParams =
        renderParams[osapi.container.ee.RenderParam.GADGET_RENDER_PARAMS] || {};
      localRenderParams[osapi.container.ee.RenderParam.DATA_MODEL] = dataModel;
      localRenderParams[osapi.container.RenderParam.VIEW] =
        osapi.container.ee.RenderParam.EMBEDDED;
      var site = context.newGadgetSite(element);
      var gadgetUrl = dataModel.gadget;

      context.preloadGadget(gadgetUrl, function(result) {
        if (!result[gadgetUrl] || result[gadgetUrl].error) {
          //There was an error preloading the gadget URL lets try and render the
          //URL EE if there is one
          if (dataModel.url != null) {
            navigateUrl_(element, dataModel, renderParams, opt_callback);
          }
          else if (opt_callback != null) {
            opt_callback(site, result[gadgetUrl] || {"error" : result});
          }
        }
        else {
          context.navigateGadget(site, gadgetUrl, viewParams, localRenderParams,
            function(metadata) {
              if (opt_callback != null) {
                opt_callback(site, metadata);
              }
         });
        }
      });
    };

    /**
     * Navigates to a URL
     * @param {Element} element the element to render the URL in.
     * @param {Object} dataModel the EE data model.
     * @param {Object} renderParams params to augment the rendering.
     * Valid rendering parameters include osapi.container.RenderParam.CLASS,
     * osapi.container.RenderParam.HEIGHT, and osapi.container.RenderParam.WIDTH.
     * @param {Function} opt_callback called when the URL has been navigated to.
     */
    function navigateUrl_(element, dataModel, renderParams, opt_callback) {
      var urlRenderParams =
        renderParams[osapi.container.ee.RenderParam.URL_RENDER_PARAMS] || {};
      var site = context.newUrlSite(element);
      var toReturn = context.navigateUrl(site, dataModel.url, urlRenderParams);
      if (opt_callback) {
        opt_callback(toReturn, null);
      }
    };

    /**
     * Handles the RPC request letting the container know that the embedded experience gadget is rendered.
     * @param rpcArgs the RPC args from the request.
     * @return void.
     */
    function gadgetRendered_(rpcArgs) {
      var gadgetSite = rpcArgs.gs;
      var renderParams = gadgetSite.currentGadgetHolder_.renderParams_;
      var eeDataModel = renderParams.eeDataModel;
      return eeDataModel ? eeDataModel.context : null;
    };

    //Add the RPC handler to pass the context to the gadget
    context.rpcRegister('ee_gadget_rendered', gadgetRendered_);

    return {

      /**
       * Navigate to an embedded experience.  Call this method to render any embedded experience.
       * @param {Element} element the element to render the embedded experience in.
       * @param {Object} datModel the EE data model.
       * @param {Object} renderParams parameters for the embedded experience.
       * @param {Function} opt_callback callback function which will be called after the
       * gadget has rendered.
       */
      'navigate' : function(element, dataModel, renderParams, opt_callback) {
        if (dataModel.gadget) {
          navigateGadget_(element, dataModel, renderParams, opt_callback);
        }
        else if (dataModel.url) {
          navigateUrl_(element, dataModel, renderParams, opt_callback);
        }
      },

      /**
       * Closes the embedded experience on the page.
       * @param {object} site one of osapi.container.GadgetSite or osapi.container.UrlSite.
       */
      'close' : function(site) {
        /*
         * At the moment this will work fine because an EE can be either a gadget or URL
         * and at the moment they both have a close method.  However it is hard
         * to have both the GadgetSite and UrlSite classes adhear to this contract in Javascript
         * so it may be better to wrap both classes in one class.
         */
        if (site instanceof osapi.container.GadgetSite) {
          context.closeGadget(site);
        }

        if (site instanceof osapi.container.UrlSite) {
          site.close();
        }
      }
    };
  });
})();

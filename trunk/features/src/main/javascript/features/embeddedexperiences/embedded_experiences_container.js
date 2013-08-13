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
    var ee_data_model = osapi.container.ee.DataModel;
    var ee_pe = osapi.container.ee.PreferredExperience;
    var ee_type = osapi.container.ee.TargetType;
    var ee_context = osapi.container.ee.Context;
    var ee_containerconfig = osapi.container.ee.ContainerConfig;

    /**
     * Navigates to an EE gadget
     * @param {Element} element the element to put the gadget in.
     * @param {Object} dataModel the EE data model.
     * @param {Object} renderParams params to augment the rendering.
     * @param {Function=} opt_callback called once the gadget has been navigated to.
     * @param {Object=} opt_containerContext additional context that a container could pass to gadget.
     *    The container context should contain at least "associatedContext" which is used
     *    to define context where the gadget is displayed.
     * @param {Element=} opt_bufferEl The optional element to use for double buffering when rendering the gadget.
     */
    function navigateGadget_(element, dataModel, renderParams, opt_callback, opt_containerContext, opt_bufferEl) {
      var viewParams = renderParams[osapi.container.ee.RenderParam.GADGET_VIEW_PARAMS] || {};
      var localRenderParams =
        renderParams[osapi.container.ee.RenderParam.GADGET_RENDER_PARAMS] || {};
      localRenderParams[osapi.container.RenderParam.VIEW] =
        osapi.container.ee.RenderParam.EMBEDDED;

      // Lets processes the "preferredExperience" part from the data model if available
      var preferredExperience = dataModel[ee_data_model.PREFERRED_EXPERIENCE];
      if(preferredExperience) {
        var targetPE = preferredExperience[ee_pe.TARGET];
        if(targetPE && targetPE[ee_pe.TYPE] === ee_type.GADGET) {
          if(!!targetPE[ee_pe.VIEW]) {
            localRenderParams[osapi.container.RenderParam.VIEW] = targetPE[ee_pe.VIEW];
          }
        }
      }

      // Now, lets update EE data model context if being passed additional context from container
      if(opt_containerContext) {
        // Check if the data model has context
        var eeDataModelContext = dataModel[ee_data_model.CONTEXT];
        var addContainerContext = true;
        if (eeDataModelContext) {
          // Need to check if the context of the EE model is object type to be able to append
          // container additional context.
          if (typeof eeDataModelContext != 'object') {
            addContainerContext = false;
          }
        } else {
          eeDataModelContext = {};
        }

        if(addContainerContext) {
          var openSocialContext = {};
          openSocialContext[ee_context.ASSOCIATED_CONTEXT] = {};
          for (var property in opt_containerContext) {
            if (opt_containerContext.hasOwnProperty(property)) {
              openSocialContext[property] = opt_containerContext[property];
            }
          }

          // Lets update the EE model context
          eeDataModelContext[ee_context.OPENSOCIAL] = openSocialContext;
          dataModel[ee_data_model.CONTEXT] = eeDataModelContext;
        }
      }
      localRenderParams[osapi.container.ee.RenderParam.DATA_MODEL] = dataModel;

      var site = context.newGadgetSite(element, opt_bufferEl);
      var gadgetUrl = dataModel[ee_data_model.GADGET];

      context.preloadGadget(gadgetUrl, function(result) {
        if (!result[gadgetUrl] || result[gadgetUrl].error) {
          //There was an error preloading the gadget URL lets try and render the
          //URL EE if there is one
          if (dataModel[ee_data_model.URL] != null) {
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
    }

    /**
     * Navigates to a URL
     * @param {Element} element the element to render the URL in.
     * @param {Object} dataModel the EE data model.
     * @param {Object} renderParams params to augment the rendering.
     *        Valid rendering parameters include osapi.container.RenderParam.CLASS,
     *        osapi.container.RenderParam.HEIGHT, and osapi.container.RenderParam.WIDTH.
     * @param {Function} opt_callback called when the URL has been navigated to.
     */
    function navigateUrl_(element, dataModel, renderParams, opt_callback) {
      var urlRenderParams =
        renderParams[osapi.container.ee.RenderParam.URL_RENDER_PARAMS] || {};
      var site = context.newUrlSite(element);
      var toReturn = context.navigateUrl(site, dataModel[ee_data_model.URL], urlRenderParams);
      if (opt_callback) {
        opt_callback(toReturn, null);
      }
    }

    /**
     * Try to get the target type from EE data model preferredExperience if any.
     *
     * @param {Object} dataModel the EE data model.
     * @return {String} The target type of EE data model preferredExperience or null if not set.
     */
    function getPreferredEE_(dataModel) {
      if(dataModel[ee_data_model.PREFERRED_EXPERIENCE]) {
        var pe = dataModel[ee_data_model.PREFERRED_EXPERIENCE];
        if (pe[ee_pe.TARGET]) {
          var peTarget = pe[ee_pe.TARGET];
          if(peTarget && peTarget[ee_pe.TYPE]) {
            var type = peTarget[ee_pe.TYPE];
            if((osapi.container.ee.TargetType.URL === type && typeof dataModel.url !== 'undefined') ||
                    (osapi.container.ee.TargetType.GADGET === type &&
                            typeof dataModel.gadget !== 'undefined')) {
              return type
            }
          }
        }
      }
      return null;
    }

    /**
     * Handles the RPC request letting the container know that the embedded experience gadget is rendered.
     * @param rpcArgs the RPC args from the request.
     * @return void.
     */
    function gadgetRendered_(rpcArgs) {
      var gadgetSite = rpcArgs.gs;
      var renderParams = gadgetSite.getActiveSiteHolder().renderParams_;
      var eeDataModel = renderParams.eeDataModel;
      return eeDataModel ? eeDataModel[ee_data_model.CONTEXT] : null;
    }

    //Add the RPC handler to pass the context to the gadget
    context.rpcRegister('ee_gadget_rendered', gadgetRendered_);

    return {

      /**
       * Navigate to an embedded experience.  Call this method to render any embedded experience.
       * @param {Element} element the element to render the embedded experience in.
       * @param {Object} datModel the EE data model.
       * @param {Object} renderParams parameters for the embedded experience.
       * @param {Function=} opt_callback callback function which will be called after the
       *        gadget has rendered.
       * @param {Object=} opt_containerContext additional context that a container could pass to gadget
       * @param {Element=} opt_bufferEl Element to use for double buffering when rendering a gadget.
       */
      'navigate' : function(element, dataModel, renderParams, opt_callback, opt_containerContext, opt_bufferEl) {
        var preferredEE = null;
        if (!!context.config_ && !!context.config_[ee_containerconfig.GET_EE_NAVIGATION_TYPE] &&
            (typeof context.config_[ee_containerconfig.GET_EE_NAVIGATION_TYPE] === 'function')) {
          preferredEE =
            context.config_[ee_containerconfig.GET_EE_NAVIGATION_TYPE].call(context, dataModel);
        }
        else {
          preferredEE = getPreferredEE_(dataModel);
        }

        // if no preference from the service lets check the model
        if(preferredEE === null) {
          if (dataModel[ee_data_model.GADGET]) {
            preferredEE = osapi.container.ee.TargetType.GADGET;
          }
          else if (dataModel[ee_data_model.URL]) {
            preferredEE = osapi.container.ee.TargetType.URL;
          }
        }

        // Lets navigate
        if (preferredEE === osapi.container.ee.TargetType.GADGET) {
          navigateGadget_(element, dataModel, renderParams, opt_callback, opt_containerContext, opt_bufferEl);
        }
        else if (preferredEE === osapi.container.ee.TargetType.URL) {
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

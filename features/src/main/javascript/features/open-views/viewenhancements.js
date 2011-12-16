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
 * @fileoverview view enhancement library for gadgets.
 */

gadgets['window'] = gadgets['window'] || {};

(function() {

  var resultCallbackMap = {},
      rcbnum = 0;

  gadgets.util.registerOnLoadHandler(function() {
    gadgets.rpc.register('gadgets.views.deliverResult', function(rcbnum, result) {
      var resultCallback;
      if (resultCallback = resultCallbackMap[rcbnum]) {
        delete resultCallbackMap[rcbnum];
        resultCallback(result);
      }
    });
  });

  /**
   * Opens a gadget in the container UI. The location of the gadget site in the
   * container will be determined by the view target passed in. The container
   * would open the view in a dialog, if view target is dialog or the gadgets
   * view in a tab for view target is tab
   *
   * @param {function}
   *          resultCallback: Callback function to be called when the gadget
   *          closes. The function will be called with the return value as a
   *          parameter.
   * @param {function}
   *          navigateCallback: Callback function to be called with the
   *          site and gadget metadata.
   * @param {Object}
   *          opt_params: These are optional parameters which can be used to
   *          open gadgets. The following parameters may be included in this
   *          object.
   *            {string} view: The view to render. Should be one of the views
   *              returned by calling gadgets.views.getSupportedViews. If the
   *              view is not included the default view will be rendered.
   *            {string} viewTarget: The view that indicates where to open the
   *              gadget. For example, tab, dialog or modaldialog
   *            {Object} viewParams: View parameters for the view being
   *              rendered.
   *            {Object} coordinates: Object containing the desired absolute
   *              positioning css parameters (top|bottom|left|right) with
   *              appropriate values. All values are relative to the calling
   *              gadget.
   *              Do not specify top AND bottom or left AND right parameters to
   *              indirectly define height and width. Use viewParams for that.
   *              The result of doing so here is undefined.
   */

  gadgets.views.openGadget = function(resultCallback, navigateCallback,
          opt_params) {

    resultCallbackMap[rcbnum] = resultCallback;
    gadgets.rpc.call('..', 'gadgets.views.openGadget', function(result) {
        navigateCallback.apply(this, result);
      }, rcbnum++, opt_params
    );
  };

  /**
   * Opens an embedded experience in the container UI. The location of the
   * gadget site in the container will be determined by the view target passed
   * in. The container would open the view in a dialog, if view target is dialog
   * or the gadgets view in a tab for view target is tab.
   *
   * @param {function}
   *          resultCallback: Callback function to be called when the gadget
   *          closes. The function will be called with the return value as a
   *          parameter.
   * @param {function}
   *          navigateCallback: Callback function to be called with the site and
   *          gadget metadata.
   * @param {function}
   *          dataModel: The embedded experiences data model.
   * @param {Object}
   *          opt_params: These are optional parameters which can be used to
   *            open gadgets. The following parameters may be included in this
   *            object.
   *            {string} viewTarget: The view that indicates where to open the
   *              gadget. For example, tab, dialog or modaldialog
   *            {Object} viewParams: View parameters for the view being
   *              rendered.
   *            {Object} coordinates: Object containing the desired absolute
   *              positioning css parameters (top|bottom|left|right) with
   *              appropriate values. All values are relative to the calling
   *              gadget.
   *              Do not specify top AND bottom or left AND right parameters to
   *              indirectly define height and width. Use viewParams for that.
   *              The result of doing so here is undefined.
   */
  gadgets.views.openEmbeddedExperience = function(resultCallback,
          navigateCallback, dataModel, opt_params) {

    resultCallbackMap[rcbnum] = resultCallback;
    gadgets.rpc.call('..', 'gadgets.views.openEmbeddedExperience', function(result) {
        navigateCallback.apply(this, result);
      }, rcbnum++, dataModel, opt_params
    );
  };

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
   *          navigateCallback: Callback function to be called with the
   *          site which has been opened.
   * @param {string=}
   *          opt_viewTarget: Optional parameter, the view that indicates where
   *          to open the URL.
   * @param {Object=} opt_coordinates: Object containing the desired absolute
   *          positioning css parameters (top|bottom|left|right) with
   *          appropriate values. All values are relative to the calling
   *          gadget.
   *          You may specify top AND bottom or left AND right parameters to
   *          indirectly define height and width
   */
   gadgets.views.openUrl = function(url, navigateCallback, opt_viewTarget, opt_coordinates) {
    gadgets.rpc.call('..', 'gadgets.views.openUrl', function(result) {
        navigateCallback.call(this, result);
      }, url, opt_viewTarget, opt_coordinates
    );
  }

  /**
   * Closes an opened site. If the opt_id parameter is null the container will
   * close the calling site.
   *
   * @param {Object=}
   *          opt_site: Optional parameter which specifies what site to close.
   *          If null it will close the current gadget site.
   */
  gadgets.views.close = function(opt_site) {
    gadgets.rpc.call('..', 'gadgets.views.close', null,
      opt_site
    );
  };

  /**
   * Sets the return value for the current window. This method should only be
   * called inside those secondary view types defined in gadgets.views.ViewType.
   * For example, DIALOG or MODALDIALOG
   *
   * @param {object}
   *          returnValue: Return value for this window.
   */
  gadgets.views.setReturnValue = function(returnValue) {
    gadgets.rpc.call('..', 'gadgets.views.setReturnValue', null,
      returnValue
    );
  };

  /**
   * Gets the dimensions of the container displaying this gadget through
   * callback function which will be called with the return value as a
   * parameter.
   *
   * @param {function}
   *          resultCallback: Callback function will be called with the return
   *          value as a parameter.
   */
  gadgets.window.getContainerDimensions = function(resultCallback) {
    gadgets.rpc.call('..', 'gadgets.window.getContainerDimensions', resultCallback);
  }

}());

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

gadgets.views = gadgets.views || {};

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
  gadgets.rpc.call('..', 'gadgets.views.openGadget', function(result) {
      navigateCallback.apply(this, result);
    }, gadgets.views.registerCallback_(resultCallback), opt_params
  );
};
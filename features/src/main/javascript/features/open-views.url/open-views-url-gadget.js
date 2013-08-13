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
  }, url, opt_viewTarget, opt_coordinates);
};
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
gadgets.views = gadgets.views || {};

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
};

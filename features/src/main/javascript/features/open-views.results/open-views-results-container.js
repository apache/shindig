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
 * @fileoverview Container-side common script.
 */

osapi.container.Container.addMixin('views', function(container) {
  this.resultCallbacks_ = {}; // Mapping between id and callback function
  this.returnValues_ = {}; // Mapping between id and return value

  var self = this,
      lifecyclecb = {};
  lifecyclecb[osapi.container.CallbackType.ON_BEFORE_CLOSE] = function(site) {
    var id = site.getId(),
        returnValue = self.returnValues_[id],
        resultCallback = self.resultCallbacks_[id];

    // Checking the truthiness of resultCallback is bad because 0 is a valid value.
    // Check whether it is undefined
    if (typeof resultCallback !== 'undefined') {
      if (site.ownerId_) {
        gadgets.rpc.call(site.ownerId_, 'gadgets.views.deliverResult', null,
          resultCallback, returnValue
        );
      }
    }

    delete self.returnValues_[id];
    delete self.resultCallbacks_[id];
  };
  container.addGadgetLifecycleCallback("open-views", lifecyclecb);

  /**
   * Sets the return value for the current window. This method should only be
   * called inside those secondary view types defined in gadgets.views.ViewType.
   * For example, DIALOG or MODALDIALOG
   *
   * @param {object}
   *          returnValue: Return value for this window.
   */
  container.rpcRegister('gadgets.views.setReturnValue', function (rpcArgs, returnValue) {
    var site = container.getGadgetSiteByIframeId_(rpcArgs.f);
    if (site) {
      self.returnValues_[site.getId()] = returnValue;
    }
  });
});
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview This represents the service layer that talks to OSAPI
 * endpoints. All RPC requests should go into this class.
 */


/**
 * @param {Object=} opt_config. Configuration JSON.
 * @constructor
 */
shindig.container.Service = function(opt_config) {
  var config = opt_config || {};

  /**
   * @type {boolean}
   */
  this.sameDomain_ = Boolean(shindig.container.util.getSafeJsonValue(config,
      shindig.container.ServiceConfig.SAME_DOMAIN, true));

  this.onConstructed(config);
};


/**
 * Callback that occurs after instantiation/construction of this. Override to
 * provide your specific functionalities.
 * @param {Object=} opt_config. Configuration JSON.
 */
shindig.container.Service.prototype.onConstructed = function(opt_config) {};


/**
 * Do an immediate fetch of gadgets metadata for gadgets in request.ids, for
 * container request.container, with its results mutated by
 * request.sameDomain and request.aspDomain. The appropriate optional
 * callback opt_callback will be called, after a response is received.
 * @param {Object} request JSON object representing the request.
 * @param {function(Object)=} opt_callback function to call upon data receive.
 */
shindig.container.Service.prototype.getGadgetMetadata = function(
    request, opt_callback) {
  var callback = opt_callback || function(a) {};
  var self = this;
  osapi.gadgets.getMetadata(request, function(response) {
    if (response.error) {
      // This hides internal server error.
      callback({
          error : 'Failed to retrieve gadget.',
          errorCode : 'NOLOAD'
      });
    } else {
      var data = response.data;
      var gadgetUrls = shindig.container.util.toArrayOfJsonKeys(data);
      for (var i = 0; i < gadgetUrls.length; i++) {
        var gadgetInfo = data[gadgetUrls[i]];
        self.processSameDomain_(gadgetInfo);
      }
      callback(response);
    }
  });
};


/**
 * @param {Object} gadgetInfo
 * @private
 */
shindig.container.Service.prototype.processSameDomain_ = function(gadgetInfo) {
  gadgetInfo['sameDomain'] = this.sameDomain_;
};


// -----------------------------------------------------------------------------
// Configuration
// -----------------------------------------------------------------------------

/**
 * Enumeration of configuration keys for this service. This is specified in
 * JSON to provide extensible configuration.
 * @enum {string}
 */
shindig.container.ServiceConfig = {};

//Toggle to render gadgets in the same domain.
/** @type {string} */
shindig.container.ServiceConfig.SAME_DOMAIN = 'sameDomain';

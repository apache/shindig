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

var osapi = osapi || {};

/**
 * Activity service allows retrieval and creation of Activities
 * for opensocial People.
 */
osapi.activities = function() {

  /**
   * Function to get Activities.
   * Options specifies parameters to the call as outlined in the
   * JSON RPC Opensocial Spec
   * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
   * @param {object.<JSON>} The JSON object of parameters for the specific request
   */
  var get = function(options) {
    options = options || {};
    osapi.base.ensureAppId(options);
    return osapi.newJsonRequest("activities.get", options);
  };

   /**
   * Function to create Activities.
   * Options specifies parameters to the call as outlined in the
   * JSON RPC Opensocial Spec
   * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
   * @param {object.<JSON>} The JSON object of parameters for the specific request
   */
  var create = function(options) {
    osapi.base.ensureAppId(options);
    // TODO, do expansion of template options from userPrefs if necessary
    return osapi.newJsonRequest("activities.create", options);
  };

  return {
    get: get,
    create : create
  };
}();

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
 * The appdata aservice allows storage, retrieval, and deletion of
 * small amounts of data per person used by gadgets.
 */
osapi.appdata = function() {


  var ensureFieldsFromKeys = function(options) {
    if (!options.keys) {
      options.fields = [];
    } else if (options.keys.length === 0 || options.keys[0] === '*') {
      options.fields = [];
      delete options.keys;
    } else {
      options.fields = options.keys;
      delete options['keys'];
    }
  };

  var ensureData = function(options) {
    if (!options.data) {
      options.data = {};
    }
  };

  /**
  * Function to get Appdata.
  * Options specifies parameters to the call as outlined in the
  * JSON RPC Opensocial Spec
  * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
  * @param {object.<JSON>} The JSON object of parameters for the specific request
  */
  var get = function(options) {
    osapi.base.ensureAppId(options);
    ensureFieldsFromKeys(options);
    return osapi.newJsonRequest("appdata.get", options);
  };

  /**
  * Function to create or update Appdata.
  * Options specifies parameters to the call as outlined in the
  * JSON RPC Opensocial Spec
  * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
  * @param {object.<JSON>} The JSON object of parameters for the specific request
  */

  var update = function(options) {
    osapi.base.ensureAppId(options);
    ensureData(options);
    ensureFieldsFromKeys(options);
    return osapi.newJsonRequest("appdata.update", options);
  };

  /**
  * Function to delete Appdata.
  * Options specifies parameters to the call as outlined in the
  * JSON RPC Opensocial Spec
  * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
  * @param {object.<JSON>} The JSON object of parameters for the specific request
  */
  var deleteData = function(options) {
    osapi.base.ensureAppId(options);
    ensureFieldsFromKeys(options);
    return osapi.newJsonRequest("appdata.delete", options);
  };

  return {
    get: get,
    update : update,
    deleteData : deleteData
  };
}();

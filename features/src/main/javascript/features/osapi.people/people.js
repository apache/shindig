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
 * Service to retrieve People via JSON RPC opensocial calls.
 */
osapi.people = function() {

  var defaultFields = ['id', 'displayName'];

  var ensureFields = function(options) {
    if (!options.fields) {
      options.fields = defaultFields;
    }
  };

  /**
  * Function to get People.
  * Options specifies parameters to the call as outlined in the
  * JSON RPC Opensocial Spec
  * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
  * @param {object.<JSON>} The JSON object of parameters for the specific request
  */
  var get = function(options) {    
    ensureFields(options);
    return osapi.newJsonRequest("people.get", options);
  };

  return {
    get: get,

    /**
    * Function to get Viewer profile.
    * Options specifies parameters to the call as outlined in the
    * JSON RPC Opensocial Spec
    * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
    * @param {object.<JSON>} The JSON object of parameters for the specific request
    */
    getViewer : function(options) {
      options = options || {};
      options.userId = "@viewer";
      options.groupId = "@self";
      return get(options);
    },

    /**
    * Function to get Viewer's friends'  profiles.
    * Options specifies parameters to the call as outlined in the
    * JSON RPC Opensocial Spec
    * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
    * @param {object.<JSON>} The JSON object of parameters for the specific request
    */
    getViewerFriends : function(options) {
      options = options || {};
      options.userId = "@viewer";
      options.groupId = "@friends";
      return get(options);
    },

    /**
    * Function to get Owner profile.
    * Options specifies parameters to the call as outlined in the
    * JSON RPC Opensocial Spec
    * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
    * @param {object.<JSON>} The JSON object of parameters for the specific request
    */
    getOwner : function(options) {
      options = options || {};
      options.userId = "@owner";
      options.groupId = "@self";
      return get(options);
    },

    /**
    * Function to get Owner's friends' profiles.
    * Options specifies parameters to the call as outlined in the
    * JSON RPC Opensocial Spec
    * http://www.opensocial.org/Technical-Resources/opensocial-spec-v081/rpc-protocol
    * @param {object.<JSON>} The JSON object of parameters for the specific request
    */
    getOwnerFriends : function(options) {
      options = options || {};
      options.userId = "@owner";
      options.groupId = "@friends";
      return get(options);
    }


  };
}();

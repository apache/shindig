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
 * @fileoverview Remote procedure call library for gadget-to-container,
 * container-to-gadget, and gadget-to-gadget communication.
 */

var gadgets = gadgets || {};

gadgets.rpc = function() {
  return {
    /**
     * Registers an RPC service.
     * @param {string} serviceName Service name to register.
     * @param {Function} handler Service handler.
     */
    register: function(serviceName, handler) {
      // TODO
    },

    /**
     * Unregisters an RPC service.
     * @param {string} serviceName Service name to unregister.
     */
    unregister: function(serviceName) {
      // TODO
    },

    /**
     * Registers a default service handler to processes all unknown
     * RPC calls which fail silently by default.
     * @param {Function} handler Service handler.
     */
    registerDefault: function(handler) {
      // TODO
    },

    /**
     * Unregisters the default service handler. Future unknown RPC
     * calls will fail silently.
     */
    unregisterDefault: function() {
      // TODO
    },

    /**
     * Calls an RPC service.
     * @param {string} targetId Id of the RPC service provider.
     *                          Empty if calling the parent container.
     * @param {string} serviceName Service name to call.
     * @param {Function|null} callback Callback function (if any) to process
     *                                 the return value of the RPC request.
     * @param {*} var_args Parameters for the RPC request.
     */
    call: function(targetId, serviceName, callback, var_args) {
      // TODO
    }
  };
}();


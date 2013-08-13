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
 * @fileoverview Container-side message router for PubSub, a gadget-to-gadget
 * communication library.
 * 
 * Uses OpenAjax Hub's ManagedHub class to route pubsub messages and to provide
 * manager callbacks that allow control over these messages.
 */

/**
 * @static
 * @class Routes PubSub messages.
 * @name gadgets.pubsub2router
 */
gadgets.pubsub2router = function() {
  return /** @scope gadgets.pubsub2router */ {
    /**
     * Initialize the pubsub message router.
     * 
     * 'opt_params' is passed directly to the ManagedHub constructor.
     * For example:
     * 
     *     gadgets.pubsub2router.init({
     *         onSubscribe: function(topic, container) {
     *           ...
     *           return true; // return false to reject the request.
     *         },
     *         onPublish: function(topic, data, pcont, scont) {
     *           ...
     *           return true; // return false to reject the request.
     *         },
     *         onUnsubscribe: function(topic, container) {
     *           ...
     *         }
     *     });
     * 
     * Alternatively, if you have already created a ManagedHub instance and wish
     * to use that, you can specify it in 'opt_params.hub'.
     * 
     * @param {Object} opt_params
     * @see http://openajax.org/member/wiki/OpenAjax_Hub_2.0_Specification_Managed_Hub_APIs#OpenAjax.hub.ManagedHub_constructor
     */
    init: function( opt_params ) {
      if (opt_params.hub) {
        this.hub = opt_params.hub;
      } else {
        this.hub = new OpenAjax.hub.ManagedHub({
          onPublish: opt_params.onPublish,
          onSubscribe: opt_params.onSubscribe,
          onUnsubscribe: opt_params.onUnsubscribe
        });
      }
    }
  };
}();

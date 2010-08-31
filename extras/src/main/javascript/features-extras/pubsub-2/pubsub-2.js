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
 * @fileoverview Gadget-side PubSub library for gadget-to-gadget communication.
 * 
 * Uses OpenAjax Hub in order to do pubsub.  Simple case is to do the following:
 *    
 *    gadgets.util.registerOnLoadHandler(function() {
 *      gadgets.Hub.subscribe(topic, callback);
 *      // OR
 *      gadgets.Hub.publish(topic2, message);
 *    });
 * 
 * The gadgets.Hub object implements the OpenAjax.hub.HubClient interface.
 * 
 * By default, a HubClient is instantiated automatically by the pubsub-2 code.
 * If the gadget wants to provide params to the HubClient constructor, it can
 * do so by setting values on gadgets.HubSettings object:
 * 
 *     gadgets.HubSettings = {
 *         // Parameters object for HubClient constructor.
 *         // @see http://openajax.org/member/wiki/OpenAjax_Hub_2.0_Specification_Managed_Hub_APIs#OpenAjax.hub.HubClient_constructor
 *         // @see http://openajax.org/member/wiki/OpenAjax_Hub_2.0_Specification_Managed_Hub_APIs#OpenAjax.hub.IframeHubClient_constructor
 *         params: {},
 *         // Callback that is invoked when connection to parent is established,
 *         // or when errors occur.
 *         // @see http://openajax.org/member/wiki/OpenAjax_Hub_2.0_Specification_Managed_Hub_APIs#OpenAjax.hub.HubClient.prototype.connect
 *         onConnect: <function>
 *     }
 * 
 * For example, to set a security alert callback:
 * 
 *     gadgets.HubSettings.params.HubClient.onSecurityCallback =
 *             function(alertSource, alertType) { ... };
 * 
 * @see http://openajax.org/member/wiki/OpenAjax_Hub_2.0_Specification_Managed_Hub_APIs#OpenAjax.hub.HubClient
 */

(function() {
    // Create a pubsub settings object
    gadgets.HubSettings = {
        // Set default HubClient constructor params object
        params: {
            HubClient: {
                onSecurityAlert: function(alertSource, alertType) {
                    alert( "Gadget stopped attempted security breach: " + alertType );
                    // Forces container to see Frame Phish alert and probably close this gadget
                    window.location.href = "about:blank"; 
                }
            },
            IframeHubClient: {}
        }
    };
    if (gadgets.util.getUrlParameters().forcesecure) {
        gadgets.HubSettings.params.IframeHubClient.requireParentVerifiable = true;
    }
    
    // Register an onLoad handler
    gadgets.util.registerOnLoadHandler(function() {
        try {
            // Create the HubClient.
            gadgets.Hub = new OpenAjax.hub.IframeHubClient(gadgets.HubSettings.params);
            
            // Connect to the ManagedHub
            gadgets.Hub.connect(gadgets.HubSettings.onConnect); 
        } catch(e) {
            // TODO: error handling should be consistent with other OS gadget initialization error handling
            gadgets.error("ERROR creating or connecting IframeHubClient in gadgets.Hub [" + e.message + "]");
        }
    });
})();

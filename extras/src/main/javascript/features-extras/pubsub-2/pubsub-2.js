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
 *    var gadget = gadgets.byId(__MODULE_ID__);
 *    gadget.PubSub.subscribe(topic, callback);
 *    ...
 *    gadget.PubSub.publish(topic2, message);
 * 
 * The <gadget instance>.PubSub object implements the OpenAjax.hub.HubClient
 * interface.
 * 
 * By default, a HubClient is instantiated automatically by the pubsub-2 code.
 * If the gadget wants to provide params to the HubClient constructor, it can
 * do so by setting values on <gadget instance>.PubSubSettings object:
 * 
 *     <gadget instance>.PubSubSettings = {
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
 *     <gadget instance>.PubSubSettings.params.HubClient.onSecurityCallback =
 *             function(alertSource, alertType) { ... };
 * 
 * @see http://openajax.org/member/wiki/OpenAjax_Hub_2.0_Specification_Managed_Hub_APIs#OpenAjax.hub.HubClient
 */

(function() {
    var params = gadgets.util.getUrlParameters();
    var moduleId = params.mid || 0;
    var gadgetInstance = gadgets.byId(moduleId);
    
    // Create a pubsub settings object
    gadgetInstance.PubSubSettings = {
        // Set default HubClient constructor params object
        params: {
            HubClient: {
                onSecurityAlert: function(alertSource, alertType) {
                    alert( "Gadget stopped attempted security breach: " + alertType );
                    // Forces container to see Frame Phish alert and probably close this gadget
                    window.location.href = "about:blank"; 
                },
                scope: gadgetInstance
            },
            IframeHubClient: {}
        }
    };
    if (params.forcesecure) {
        gadgetInstance.PubSubSettings.params.IframeHubClient.requireParentVerifiable = true;
    }
    
    // Register an onLoad handler
    gadgets.util.registerOnLoadHandler(function() {
        try {
            // Create the HubClient.
            gadgetInstance.PubSub = new OpenAjax.hub.IframeHubClient(gadgetInstance.PubSubSettings.params);
            
            // Connect to the ManagedHub
            gadgetInstance.PubSub.connect(gadgetInstance.PubSubSettings.onConnect); 
        } catch(e) {
            // TODO: error handling should be consistent with other OS gadget initialization error handling
            gadgets.error("ERROR creating or connecting IframeHubClient in gadgets.pubsub [" + e.message + "]");
        }
    });
})();

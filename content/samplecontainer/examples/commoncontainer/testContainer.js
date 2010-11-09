/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/

var testConfig =  testConfig || {};
testConfig[shindig.container.ServiceConfig.API_PATH] = '/rpc';	
testConfig[shindig.container.ContainerConfig.RENDER_DEBUG] = "1";

//Default the security token for testing.
shindig.auth.updateSecurityToken('john.doe:john.doe:appid:cont:url:0:default');


//  Create the new CommonContainer 
var CommonContainer = new shindig.container.Container(testConfig);

// Need to pull these from values supplied in the dialog
CommonContainer.init = function() {
	//  Override base funcitons to log OAH events
	gadgets.pubsub2router.init(
		    {
		      onSubscribe: function(topic, container) {
		        log(container.getClientID() + " subscribes to topic '" + topic + "'");
		        return true;
		        // return false to reject the request.
		      },
		      onUnsubscribe: function(topic, container) {
		        log(container.getClientID() + " unsubscribes from topic '" + topic + "'");
		        return true;
		      },
		      onPublish: function(topic, data, pcont, scont) {
		        log(pcont.getClientID() + " publishes '" + data + "' to topic '" + topic + "' subscribed by " + scont.getClientID());
		        return true;
		        // return false to reject the request.
		      }
		    });
};

//Wrapper function to set the gadget site/id and default width.  Currently have some inconsistency with width actually being set. This 
//seems to be related to the pubsub2 feature.
CommonContainer.renderGadget = function(newGadgetSpec, gadgetId) {
	//going to hardcode these values for width.  
    var el = document.getElementById("gadget-site-" + gadgetId);
    var parms ={};
    parms[shindig.container.RenderParam.WIDTH]="700px";
	var gadgetSite = CommonContainer.newGadgetSite(el);
	CommonContainer.navigateGadget(gadgetSite, newGadgetSpec, {}, parms);
  
};
//TODO:  To be implemented. Identify where to hook this into the page (in the gadget title bar/gadget management, etc)
CommonContainer.navigateView = function(newGadgetSpec, gadgetId, view) {
	
};
//TODO:  Add in UI controls in portlet header to remove gadget from the canvas
CommonContainer.removeGadget = function(gadgetId) {
  
};

//display the pubsub 2 event details
function log(message) {

  document.getElementById("output").innerHTML = gadgets.util.escapeString(message) + "<br/>" + document.getElementById("output").innerHTML;
};


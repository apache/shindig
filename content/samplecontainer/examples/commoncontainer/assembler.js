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

// url base should be <host>:<port>//<contextRoot>
var urlBase = location.href.substr(0,location.href.indexOf("/samplecontainer/examples/commoncontainer/"));
var contextRoot = urlBase.substr(urlBase.indexOf(location.port)+location.port.length);
	
var testConfig =  testConfig || {};
testConfig[osapi.container.ServiceConfig.API_PATH] = contextRoot+'/rpc';	
testConfig[osapi.container.ContainerConfig.RENDER_DEBUG] = "1";

//  Create the new CommonContainer 
var CommonContainer = new osapi.container.Container(testConfig);

// Need to pull these from values supplied in the dialog
CommonContainer.init = function() {
  //Create my new managed hub
  CommonContainer.managedHub = new OpenAjax.hub.ManagedHub({
    onSubscribe: function(topic, container) {
      log(container.getClientID() + " subscribes to this topic '" + topic + "'");
      return true;// return false to reject the request.
    },
    onUnsubscribe: function(topic, container) {
      log(container.getClientID() + " unsubscribes from tthis topic '" + topic + "'");
      return true;
    },
    onPublish: function(topic, data, pcont, scont) {
      log(pcont.getClientID() + " publishes '" + data + "' to topic '" + topic + "' subscribed by " + scont.getClientID());
      return true;
      // return false to reject the request.
    }
  });
  //  initialize managed hub for the Container
  gadgets.pubsub2router.init({
    hub: CommonContainer.managedHub
  });
};

//Wrapper function to set the gadget site/id and default width.  Currently have some inconsistency with width actually being set. This 
//seems to be related to the pubsub2 feature.
CommonContainer.renderGadget = function(gadgetURL, gadgetId) {
	//going to hardcode these values for width.  
    var el = document.getElementById("gadget-site-" + gadgetId);
    var parms ={};
    parms[osapi.container.RenderParam.WIDTH]="100%";
	var gadgetSite = CommonContainer.newGadgetSite(el);
	CommonContainer.navigateGadget(gadgetSite, gadgetURL, {}, parms);
	return gadgetSite;
  
};
//TODO:  To be implemented. Identify where to hook this into the page (in the gadget title bar/gadget management, etc)
CommonContainer.navigateView = function(gadgetSite, gadgetURL, view) {
	var renderParms ={};
	if(view===null || view===""){
		view="default";
	}
	//TODO Evaluate Parms based on configuration
    renderParms[osapi.container.RenderParam.WIDTH]="100%";
    renderParms['view']=view;

    CommonContainer.navigateGadget(gadgetSite, gadgetURL, {}, renderParms);	
};

//TODO:  Add in UI controls in portlet header to remove gadget from the canvas
CommonContainer.colapseGadget = function(gadgetSite) {
	CommonContainer.closeGadget(gadgetSite);
};

//display the pubsub 2 event details
function log(message) {
  document.getElementById("output").innerHTML = gadgets.util.escapeString(message) + "<br/>" + document.getElementById("output").innerHTML;
};


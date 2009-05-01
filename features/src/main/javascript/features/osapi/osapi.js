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
(function() {
	
  var buildRequestFunction = function(serviceName, methodName, endpointName) {
	osapi[serviceName][methodName] = function(params) {
	  params = params || {};
	  method = ""+serviceName+"."+methodName;
	  return osapi.newJsonRequest(method, params, endpointName);
	}; 
  };
  
  /**
  * @param {Object} configuration Configuration settings
  * @private
  */
  osapi.init = function(config) {
    var services = config["osapi.services"];
    if (services) {
      for (var endpointName in services) if (services.hasOwnProperty(endpointName)) {
        var endpoint = services[endpointName];
        for (var i=0; i < endpoint.length; i++) {
          var serviceMethod = endpoint[i];
          var serviceMethodArray = serviceMethod.split(".");
          var serviceName = serviceMethodArray[0];
          var methodName = serviceMethodArray[1];
          osapi[serviceName] = osapi[serviceName] || {};
          buildRequestFunction(serviceName, methodName, endpointName);
        }
      }
    }
  };

  gadgets.config.register("osapi.services", {}, osapi.init);
})();

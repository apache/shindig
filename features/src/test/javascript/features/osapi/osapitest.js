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
function OsapiTest(name) {
  TestCase.call(this, name);
};

OsapiTest.inherits(TestCase);

OsapiTest.prototype.setUp = function() {
  shindig = shindig || {};
  shindig.auth = {};
  shindig.auth.getSecurityToken = function() {
    return 'dsjk452487sdf7sdf865%&^*&^8cjhsdf';
  };

  OsapiTest.formerPeople = osapi.people;
  osapi.people = undefined;

  gadgets.config.init({ "osapi.services" : {
      "http://%host%/social/rpc" : ["system.listMethods", "people.get", "activities.get", 
        "activities.create", "appdata.get", "appdata.update", "appdata.delete"] }
  });

};

OsapiTest.prototype.tearDown = function() {
  shindig.auth = undefined;
  if (OsapiTest.formerPeople) {
	  osapi.people = OsapiTest.formerPeople;
  }
};

function debug(obj) {
	for (var o in obj) if (obj.hasOwnProperty(o)) {
		java.lang.System.out.println(o + " = " + obj[o]);
		debug(obj[o]);
	}
}

OsapiTest.prototype.testGen = function() {
  var that = this;
  var getViewerFn = osapi.people.get();
  that.assertTrue(getViewerFn !== undefined)
//  debug(getViewerFn);
  this.assertRequestPropertiesForService(getViewerFn);
  var expectedJson = [{ method : "people.get",
    params : { userId : ['@viewer'],
      groupId : '@self' } }];
  this.assertEquals("Json for request params should match", expectedJson, getViewerFn.json());
  var mockPersonResult = { data : [{
    data:{
      id:'5551212',
      isViewer:true, name:{familyName:"Evans",givenName:"Bob"}, isOwner:true, displayName:"Bob Evans"
    }
  }], errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Ids should match", "5551212", response.id);
    that.assertEquals("Displayname should match", "Bob Evans", response.displayName);
  });
  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback2, params, contentType) {
      callback2(mockPersonResult);
    };
    
    getViewerFn.execute(inspectableCallback.callback);
    this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
  } finally {
    gadgets.io.makeNonProxiedHttpRequest = oldMakeRequest;
  }
};


function OsapiTestSuite() {
  TestSuite.call(this, 'OsapiTestSuite');
  this.addTestSuite(OsapiTest);
}

OsapiTestSuite.inherits(TestSuite);

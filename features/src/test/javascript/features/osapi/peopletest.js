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
function PeopleTest(name) {
  TestCase.call(this, name);
};

PeopleTest.inherits(TestCase);

PeopleTest.prototype.setUp = function() {
  shindig = shindig || {};
  shindig.auth = {};
  shindig.auth.getSecurityToken = function() {
    return 'dsjk452487sdf7sdf865%&^*&^8cjhsdf';
  };

  PeopleTest.formerPeople = osapi.people;
  osapi.people = undefined;

  gadgets.config.init({"osapi.services" : {
      "http://%host%/social/rpc" : ["system.listMethods", "people.get", "activities.get", 
        "activities.create", "appdata.get", "appdata.update", "appdata.delete"] }
  });

};

PeopleTest.prototype.tearDown = function() {
  shindig.auth = undefined;
  if (PeopleTest.formerPeople) {
	  osapi.people = PeopleTest.formerPeople;
  }
};

PeopleTest.prototype.testJsonBuilding = function() {
  var getViewerFn = osapi.people.getViewer();
  this.assertRequestPropertiesForService(getViewerFn);

  var expectedJson = [ { method : 'people.get', params : { groupId : '@self', userId : [ '@viewer' ] } } ];
  this.assertEquals('Json for request params should match', expectedJson, getViewerFn.json());

  var argsInCallToMakeNonProxiedRequest;
  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {

    gadgets.io.makeNonProxiedRequest = function(url, callback, params, contentType) {
      argsInCallToMakeNonProxiedRequest = { url : url, callback : callback, params : params,
        contentType : contentType};
    };

    getViewerFn.execute(function() {
    });
    this.assertArgsToMakeNonProxiedRequest(argsInCallToMakeNonProxiedRequest, expectedJson);
  } finally {
    gadgets.io.makeNonProxiedRequest = oldMakeRequest;
  }
};

PeopleTest.prototype.testGetViewerResponse = function() {
  var that = this;
  var getViewerFn = osapi.people.getViewer();
  this.assertRequestPropertiesForService(getViewerFn);

  var expectedJson = [{ method : "people.get",
    params : { userId : ['@viewer'],
      groupId : '@self'} }];
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

PeopleTest.prototype.testGetViewerFriendsResponse = function() {
  var that = this;
  var getViewerFn = osapi.people.getViewerFriends();
  this.assertRequestPropertiesForService(getViewerFn);

  var expectedJson = [{ method : "people.get",
    params : { userId : ['@viewer'],
      groupId : '@friends' } }];
  this.assertEquals("Json for request params should match", expectedJson, getViewerFn.json());

  var mockPeopleResult = { data :
      [{data : {
        startIndex:0,
        totalResults:2,
        list :
            [ {id:"5551212", isViewer:false,
                name:{formatted:"Bob Evans"}, isOwner:false, displayName:"Bob Evans"},
              {id:"5551213", isViewer:false,
                name : { formatted: "John Smith"}, isOwner:false, displayName : "John Smith"}]}}], errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("DisplayName 1 should match", "Bob Evans", response[0].displayName);
    that.assertEquals("DisplayName 2 should match", "John Smith", response[1].displayName);
  });

  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback2, params, contentType) {
      callback2(mockPeopleResult);
    };
    getViewerFn.execute(inspectableCallback.callback);
    this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
  } finally {
    gadgets.io.makeNonProxiedHttpRequest = oldMakeRequest;
  }

};

// test error states
// response for badrequest, unknown person id
//

PeopleTest.prototype.testGetUnknownUserIdErrorResponse = function() {
  var that = this;
  var getViewerFn = osapi.people.get({ userId : 'fake.id'});
  this.assertRequestPropertiesForService(getViewerFn);

  var expectedJson = [{ method : "people.get",
    params : { userId : ['fake.id'],
      groupId : '@self'} }];
  this.assertEquals("Json for request params should match", expectedJson, getViewerFn.json());

  var mockPersonResult = { data : [{"error":{"code":400,"message":"badRequest: Person not found"}}],
    errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertTrue("should be an error in callback response", response.error);
    that.assertEquals("Error code should match", "badRequest", response.error.code);
    that.assertEquals("Error message should match", "badRequest: Person not found", response.error.message);
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

function PeopleTestSuite() {
  TestSuite.call(this, 'PeopleTestSuite');
  this.addTestSuite(PeopleTest);
}

PeopleTestSuite.inherits(TestSuite);

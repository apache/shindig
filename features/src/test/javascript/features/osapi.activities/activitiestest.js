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

function ActivitiesTest(name) {
  TestCase.call(this, name);
};

ActivitiesTest.inherits(TestCase);

ActivitiesTest.prototype.setUp = function() {
  shindig = shindig || {};
  shindig.auth = {};
  shindig.auth.getSecurityToken = function() {
    return 'dsjk452487sdf7sdf865%&^*&^8cjhsdf';
  };

  gadgets.config.init({ "osapi.base" : {
      "rpcUrl" : "http://%host%/social"}
  });
};

ActivitiesTest.prototype.tearDown = function() {
  shindig.auth = undefined;
};

ActivitiesTest.prototype.testJsonBuilding = function() {
  var getFn = osapi.activities.get({ userId : '@viewer', groupId : '@self'});
  this.assertRequestPropertiesForService(getFn);

  var expectedJson = [{ method : 'activities.get',
    params : {
      groupId : '@self',
      userId : ['@viewer'],
      appId : '@app'
    }
  }];
  this.assertEquals('Json for request params should match', expectedJson, getFn.json());

  var argsInCallToMakeNonProxiedRequest;
  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {

    gadgets.io.makeNonProxiedRequest = function(url, callback, params, contentType) {
      argsInCallToMakeNonProxiedRequest = { url : url, 
		  callback :callback, 
		  params :params, 
		  contentType : contentType};
    };

    getFn.execute(function() {});
    this.assertArgsToMakeNonProxiedRequest(argsInCallToMakeNonProxiedRequest, expectedJson);
  } finally {
    gadgets.io.makeNonProxiedRequest = oldMakeRequest;
  }
};

ActivitiesTest.prototype.testGetViewerActivities = function() {
  var that = this;
  var getVieweractivitiesFn = osapi.activities.get({ userId : '@viewer', groupId : '@self'});
  this.assertRequestPropertiesForService(getVieweractivitiesFn);

  var expectedJson = [{ method : "activities.get",
    params : { userId : ['@viewer'],
      groupId : '@self',
      appId : '@app'
    }
  }];
  this.assertEquals("Json for request params should match", expectedJson,
      getVieweractivitiesFn.json());

   var mockActivityResult = { data :
      [{data: 
      {list: 
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}], 
    	  totalResults :1, startIndex:0}}], 
    	  errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have one entry", 1, response.length);
    that.assertEquals("Should match title of activity", "yellow", response[0].title);
  });


  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback2, params, contentType) {
      callback2(mockActivityResult);
    };
    getVieweractivitiesFn.execute(inspectableCallback.callback);
    this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
  } finally {
    gadgets.io.makeNonProxiedHttpRequest = oldMakeRequest;
  }
};

ActivitiesTest.prototype.testGetEmptyActivitiesUsesDefaults = function() {
  var that = this;
  var getVieweractivitiesFn = osapi.activities.get();
  this.assertRequestPropertiesForService(getVieweractivitiesFn);

  var expectedJson = [{ method : "activities.get",
    params : { userId : ['@viewer'],
      groupId : '@self',
      appId : '@app'
    }
  }];
  this.assertEquals("Json for request params should match", expectedJson,
      getVieweractivitiesFn.json());

   var mockActivityResult = { data :
      [{data: 
      {list: 
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}],
    	  totalResults :1, startIndex:0}}], 
    	  errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have one entry", 1, response.length);
    that.assertEquals("Should match title of activity", "yellow", response[0].title);
  });

  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback2, params, contentType) {
      callback2(mockActivityResult);
    };
    getVieweractivitiesFn.execute(inspectableCallback.callback);
    this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
  } finally {
    gadgets.io.makeNonProxiedHttpRequest = oldMakeRequest;
  }
};

ActivitiesTest.prototype.testGetViewerFriendsActivities = function() {
  var that = this;
  var getViewerFriendActivitiesFn = osapi.activities.get({ userId : '@viewer',
    groupId : '@friends'});
  this.assertRequestPropertiesForService(getViewerFriendActivitiesFn);

  var expectedJson = [{ method : "activities.get",
    params : { userId : ['@viewer'],
      groupId : '@friends',
      appId : '@app'}
  }];
  this.assertEquals("Json for request params should match", expectedJson,
      getViewerFriendActivitiesFn.json());

  var mockActivitiesResult = { data :
      [{data: 
      {list: 
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}, 
    	   {title:"Your New Activity",id:"1234396143857", body:"Blah Blah"}],
    	   totalResults:2,startIndex:0}}],
    	   errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have two activities", 2, response.length);
    that.assertEquals("Should match title of activity", "yellow", response[0].title);
    that.assertEquals("Should match title of activity", "Your New Activity", response[1].title);
  });

  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback2, params, contentType) {
      callback2(mockActivitiesResult);
    };
    getViewerFriendActivitiesFn.execute(inspectableCallback.callback);
    this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
  } finally {
    gadgets.io.makeNonProxiedHttpRequest = oldMakeRequest;
  }
};

ActivitiesTest.prototype.testCreateActivity = function() {
  var that = this;
  var createActivityFn = osapi.activities.create({ userId : '@viewer',
    activity : { title : "New Activity", body : "Blah blah blah." }});
  this.assertRequestPropertiesForService(createActivityFn);

  var expectedJson = [{ method : "activities.create",
    params : { userId : ['@viewer'],
      groupId : '@self',
      appId : '@app',
      activity : { title : "New Activity", body : "Blah blah blah."}}
  }];
  this.assertEquals("Json for request params should match", expectedJson,
      createActivityFn.json());

  var mockActivityResult = { data : [{data: {}}], errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have no activities", undefined, response.length);
  });

  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback2, params, contentType) {
      callback2(mockActivityResult);
    };
    createActivityFn.execute(inspectableCallback.callback);
    this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
  } finally {
    gadgets.io.makeNonProxiedHttpRequest = oldMakeRequest;
  }

};


function ActivitiesTestSuite() {
  TestSuite.call(this, 'ActivitiesTestSuite');
  this.addTestSuite(ActivitiesTest);
}

ActivitiesTestSuite.inherits(TestSuite);

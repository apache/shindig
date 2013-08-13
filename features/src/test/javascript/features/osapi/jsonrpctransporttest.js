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

function JsonRpcTransportTest(name) {
  TestCase.call(this, name);
};

JsonRpcTransportTest.inherits(TestCase);

var lastXhr = {};

JsonRpcTransportTest.prototype.dummyXhr = function(url, callback, params, headers) {
  lastXhr.url = url;
  lastXhr.callback = callback;
  lastXhr.params = params;
  lastXhr.headers = headers;
  callback(lastXhr.result);
};


JsonRpcTransportTest.prototype.setUp = function() {
  shindig = shindig || {};
  shindig.auth = {};
  shindig.auth.getSecurityToken = function() {
    return 'dsjk452487sdf7sdf865%&^*&^8cjhsdf';
  };

  gadgets.io._makeNonProxiedRequest = gadgets.io.makeNonProxiedRequest;
  gadgets.io.makeNonProxiedRequest = this.dummyXhr;
  lastXhr = {};
  document.scripts = [];
  gadgets.config.init({ "osapi.services" : {
      "http://%host%/social/rpc" : ["system.listMethods", "people.get", "activities.get", 
        "activities.create", "appdata.get", "appdata.update", "appdata.delete"] }
  });

  window._setTimeout = window.setTimeout;
  window.setTimeout = function(fn, time) { fn.call()};

};

JsonRpcTransportTest.prototype.tearDown = function() {
  shindig.auth = undefined;
  gadgets.io.makeNonProxiedRequest = gadgets.io._makeNonProxiedRequest;
  window.setTimeout = window._setTimeout;
};

JsonRpcTransportTest.prototype.testJsonBuilding = function() {
  var getFn = osapi.activities.get({ userId : '@viewer', groupId : '@self'});
  this.assertRequestPropertiesForService(getFn);

  var expectedJson = [{ method : 'activities.get', id : "activities.get",
    params : {
      groupId : '@self',
      userId : '@viewer'
    }
  }];

  lastXhr.result = {data : [{ id : "activities.get", result : {}}], errors : []};

  getFn.execute(function() {});
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
};

JsonRpcTransportTest.prototype.testPluralGet = function() {
  var getVieweractivitiesFn = osapi.activities.get({ userId : '@viewer', groupId : '@self'});
  this.assertRequestPropertiesForService(getVieweractivitiesFn);

  var expectedJson = [{ method : "activities.get",
      id : "activities.get",
      params : { userId : '@viewer',
      groupId : '@self'
    }
  }];

  lastXhr.result = { data :
      [{id : "activities.get",
        data:
         {list:
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}], 
    	  totalResults :1, startIndex:0}}], 
    	  errors : []};

  var that = this;
  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have one entry", 1, response.list.length);
    that.assertEquals("Should match title of activity", "yellow", response.list[0].title);
  });


  getVieweractivitiesFn.execute(inspectableCallback.callback);
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
  this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
};

JsonRpcTransportTest.prototype.testNoParamGetsUsesDefaults = function() {
  var getVieweractivitiesFn = osapi.activities.get();
  this.assertRequestPropertiesForService(getVieweractivitiesFn);

  var expectedJson = [{ method : "activities.get",
      id : "activities.get",
      params : { userId : '@viewer',
      groupId : '@self'
    }
  }];

  lastXhr.result = { data :
      [{id : "activities.get",
        data:
         {list:
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}],
    	  totalResults :1, startIndex:0}}], 
    	  errors : []};

  var that = this;
  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have one entry", 1, response.list.length);
    that.assertEquals("Should match title of activity", "yellow", response.list[0].title);
  });

  getVieweractivitiesFn.execute(inspectableCallback.callback);
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
  this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
};

JsonRpcTransportTest.prototype.testNonDefaultGroupGet = function() {
  var getViewerFriendActivitiesFn = osapi.activities.get({ userId : '@viewer',
    groupId : '@friends'});
  this.assertRequestPropertiesForService(getViewerFriendActivitiesFn);

  var expectedJson = [{ method : "activities.get",
      id : "activities.get",
      params : { userId : '@viewer',
      groupId : '@friends'}
  }];

  lastXhr.result = { data :
      [{id : "activities.get",
        data:
         {list:
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}, 
    	   {title:"Your New Activity",id:"1234396143857", body:"Blah Blah"}],
    	   totalResults:2,startIndex:0}}],
    	   errors : []};

  var that = this;
  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have two activities", 2, response.list.length);
    that.assertEquals("Should match title of activity", "yellow", response.list[0].title);
    that.assertEquals("Should match title of activity", "Your New Activity",
        response.list[1].title);
  });

  getViewerFriendActivitiesFn.execute(inspectableCallback.callback);
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
  this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
};

JsonRpcTransportTest.prototype.testCreate = function() {
  var createActivityFn = osapi.activities.create({ userId : '@viewer',
    activity : { title : "New Activity", body : "Blah blah blah." }});
  this.assertRequestPropertiesForService(createActivityFn);

  var expectedJson = [{ method : "activities.create",
      id : "activities.create",
      params : { userId : '@viewer',
        groupId : '@self',
        activity : { title : "New Activity", body : "Blah blah blah."}}
  }];

  lastXhr.result = { data : [{id : "activities.create", data: {}}], errors : []};

  var that = this;
  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have no activities", undefined, response.length);
  });

  createActivityFn.execute(inspectableCallback.callback);
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
  this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
};


function JsonRpcTransportTestSuite() {
  TestSuite.call(this, 'JsonRpcTransportTestSuite');
  this.addTestSuite(JsonRpcTransportTest);
}

JsonRpcTransportTestSuite.inherits(TestSuite);
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

function JsonRpcTransportTest(name) {
  TestCase.call(this, name);
};

JsonRpcTransportTest.inherits(TestCase);

var lastXhr = {};

JsonRpcTransportTest.prototype.dummyXhr = function(url, callback, params, headers) {
  lastXhr.url = url;
  lastXhr.callback = callback;
  lastXhr.params = params;
  lastXhr.headers = headers;
  callback(lastXhr.result);
};


JsonRpcTransportTest.prototype.setUp = function() {
  shindig = shindig || {};
  shindig.auth = {};
  shindig.auth.getSecurityToken = function() {
    return 'dsjk452487sdf7sdf865%&^*&^8cjhsdf';
  };

  gadgets.io._makeNonProxiedRequest = gadgets.io.makeNonProxiedRequest;
  gadgets.io.makeNonProxiedRequest = this.dummyXhr;
  lastXhr = {};
  gadgets.config.init({ "osapi.services" : {
      "http://%host%/social/rpc" : ["system.listMethods", "people.get", "activities.get", 
        "activities.create", "appdata.get", "appdata.update", "appdata.delete"] }
  });

  window._setTimeout = window.setTimeout;
  window.setTimeout = function(fn, time) { fn.call()};

};

JsonRpcTransportTest.prototype.tearDown = function() {
  shindig.auth = undefined;
  gadgets.io.makeNonProxiedRequest = gadgets.io._makeNonProxiedRequest;
  window.setTimeout = window._setTimeout;
};

JsonRpcTransportTest.prototype.testJsonBuilding = function() {
  var getFn = osapi.activities.get({ userId : '@viewer', groupId : '@self'});
  this.assertRequestPropertiesForService(getFn);

  var expectedJson = [{ method : 'activities.get', id : "activities.get",
    params : {
      groupId : '@self',
      userId : '@viewer'
    }
  }];

  lastXhr.result = {data : [{ id : "activities.get", result : {}}], errors : []};

  getFn.execute(function() {});
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
};

JsonRpcTransportTest.prototype.testPluralGet = function() {
  var getVieweractivitiesFn = osapi.activities.get({ userId : '@viewer', groupId : '@self'});
  this.assertRequestPropertiesForService(getVieweractivitiesFn);

  var expectedJson = [{ method : "activities.get",
      id : "activities.get",
      params : { userId : '@viewer',
      groupId : '@self'
    }
  }];

  lastXhr.result = { data :
      [{id : "activities.get",
        data:
         {list:
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}], 
    	  totalResults :1, startIndex:0}}], 
    	  errors : []};

  var that = this;
  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have one entry", 1, response.list.length);
    that.assertEquals("Should match title of activity", "yellow", response.list[0].title);
  });


  getVieweractivitiesFn.execute(inspectableCallback.callback);
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
  this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
};

JsonRpcTransportTest.prototype.testNoParamGetsUsesDefaults = function() {
  var getVieweractivitiesFn = osapi.activities.get();
  this.assertRequestPropertiesForService(getVieweractivitiesFn);

  var expectedJson = [{ method : "activities.get",
      id : "activities.get",
      params : { userId : '@viewer',
      groupId : '@self'
    }
  }];

  lastXhr.result = { data :
      [{id : "activities.get",
        data:
         {list:
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}],
    	  totalResults :1, startIndex:0}}], 
    	  errors : []};

  var that = this;
  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have one entry", 1, response.list.length);
    that.assertEquals("Should match title of activity", "yellow", response.list[0].title);
  });

  getVieweractivitiesFn.execute(inspectableCallback.callback);
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
  this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
};

JsonRpcTransportTest.prototype.testNonDefaultGroupGet = function() {
  var getViewerFriendActivitiesFn = osapi.activities.get({ userId : '@viewer',
    groupId : '@friends'});
  this.assertRequestPropertiesForService(getViewerFriendActivitiesFn);

  var expectedJson = [{ method : "activities.get",
      id : "activities.get",
      params : { userId : '@viewer',
      groupId : '@friends'}
  }];

  lastXhr.result = { data :
      [{id : "activities.get",
        data:
         {list:
    	  [{title:"yellow",userId:"john.doe",id:"1",body:"what a color!"}, 
    	   {title:"Your New Activity",id:"1234396143857", body:"Blah Blah"}],
    	   totalResults:2,startIndex:0}}],
    	   errors : []};

  var that = this;
  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have two activities", 2, response.list.length);
    that.assertEquals("Should match title of activity", "yellow", response.list[0].title);
    that.assertEquals("Should match title of activity", "Your New Activity",
        response.list[1].title);
  });

  getViewerFriendActivitiesFn.execute(inspectableCallback.callback);
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
  this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
};

JsonRpcTransportTest.prototype.testCreate = function() {
  var createActivityFn = osapi.activities.create({ userId : '@viewer',
    activity : { title : "New Activity", body : "Blah blah blah." }});
  this.assertRequestPropertiesForService(createActivityFn);

  var expectedJson = [{ method : "activities.create",
      id : "activities.create",
      params : { userId : '@viewer',
        groupId : '@self',
        activity : { title : "New Activity", body : "Blah blah blah."}}
  }];

  lastXhr.result = { data : [{id : "activities.create", data: {}}], errors : []};

  var that = this;
  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have no activities", undefined, response.length);
  });

  createActivityFn.execute(inspectableCallback.callback);
  this.assertArgsToMakeNonProxiedRequest(lastXhr, expectedJson);
  this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
};


function JsonRpcTransportTestSuite() {
  TestSuite.call(this, 'JsonRpcTransportTestSuite');
  this.addTestSuite(JsonRpcTransportTest);
}

JsonRpcTransportTestSuite.inherits(TestSuite);

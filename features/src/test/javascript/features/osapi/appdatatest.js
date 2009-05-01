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

function AppdataTest(name) {
  TestCase.call(this, name);
};

AppdataTest.inherits(TestCase);

AppdataTest.prototype.setUp = function() {
  shindig = shindig || {};
  shindig.auth = {};
  shindig.auth.getSecurityToken = function() {
    return 'dsjk452487sdf7sdf865%&^*&^8cjhsdf';
  };

  gadgets.config.init({ "osapi.services" : {
      "http://%host%/social/rpc" : ["system.listMethods", "people.get", "activities.get", 
        "activities.create", "appdata.get", "appdata.update", "appdata.delete"] }
  });

};

AppdataTest.prototype.tearDown = function() {
  shindig.auth = undefined;
};

AppdataTest.prototype.testJsonBuilding = function() {
  var getFn = osapi.appdata.get({ userId : '@viewer', fields : ['nonexistent']});
  this.assertRequestPropertiesForService(getFn);

  var expectedJson = [{ method : 'appdata.get',
    params : {
      groupId : '@self',
      userId : ['@viewer'],
      fields : ['nonexistent']
    }
  }];
  this.assertEquals('Json for request params should match', expectedJson, getFn.json());

  var argsInCallToMakeNonProxiedRequest;
  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {

    gadgets.io.makeNonProxiedRequest = function(url, callback, params, contentType) {
      argsInCallToMakeNonProxiedRequest = { url : url, callback : callback, params : params,
        contentType : contentType};
    };

    getFn.execute(function() {});
    this.assertArgsToMakeNonProxiedRequest(argsInCallToMakeNonProxiedRequest, expectedJson);
  } finally {
    gadgets.io.makeNonProxiedRequest = oldMakeRequest;
  }
};

AppdataTest.prototype.testGetAppdata = function() {
  var that = this;
  var getAppdata = osapi.appdata.get({ userId : '@viewer', fields : ['gift']});
  this.assertRequestPropertiesForService(getAppdata);

  var expectedJson = [{ method : "appdata.get",
    params : { userId : ['@viewer'],
      groupId : '@self',
      fields : ['gift']}
  }];
  this.assertEquals("Json for request params should match", expectedJson, getAppdata.json());

   var mockActivityResult = { data :    
      [{data:
        {"john.doe":{"gift":"Ferrari"}}}],
     errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    var entry = response["john.doe"];
    that.assertTrue("Should have an entry for userid", entry)
    that.assertEquals("Should match appdata set", 'Ferrari', entry["gift"]);
  });

  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback2, params, contentType) {
      callback2(mockActivityResult);
    };
    getAppdata.execute(inspectableCallback.callback);
    this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
  } finally {
    gadgets.io.makeNonProxiedHttpRequest = oldMakeRequest;
  }
};

AppdataTest.prototype.testDeleteAppdata = function() {
  var that = this;
  var getAppdata = osapi.appdata["delete"]({ userId : '@viewer', fields : ['gift']});
  this.assertRequestPropertiesForService(getAppdata);

  var expectedJson = [{ method : "appdata.delete",
    params : { userId : ['@viewer'],
      groupId : '@self',
      fields : ['gift']}
  }];
  this.assertEquals("Json for request params should match", expectedJson, getAppdata.json());

  var mockAppdataResult = { data :
      [{data: 
        {}}],
    errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have no appdata", undefined, response.length);
  });

  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback2, params, contentType) {
      callback2(mockAppdataResult);
    };
    getAppdata.execute(inspectableCallback.callback);
    this.assertTrue("should have called the callback", inspectableCallback.wasCalled());
  } finally {
    gadgets.io.makeNonProxiedHttpRequest = oldMakeRequest;
  }

};

AppdataTest.prototype.testUpdate = function() {
  var that = this;
  var createActivityFn = osapi.appdata.update({ userId : '@viewer',
    data: {gifts: 'Ferrari'}});
  this.assertRequestPropertiesForService(createActivityFn);

  var expectedJson = [{ method : "appdata.update",
    params : { 
      groupId : '@self',
      userId : ['@viewer'],
      data: {gifts: 'Ferrari'}
  }
  }];
  this.assertEquals("Json for request params should match", expectedJson, createActivityFn.json());

  var mockActivityResult = { data :
      [{data:
        {}}],
    errors : []};

  var inspectableCallback = makeInspectableCallback(function (response) {
    that.assertTrue("callback from execute should have gotten a response", response);
    that.assertFalse("should not be an error in callback response", response.error);
    that.assertEquals("Should have no appdata", undefined, response.length);
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


function AppdataTestSuite() {
  TestSuite.call(this, 'AppdataTestSuite');
  this.addTestSuite(AppdataTest);
}

AppdataTestSuite.inherits(TestSuite);

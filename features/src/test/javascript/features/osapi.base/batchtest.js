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

function BatchTest(name) {
  TestCase.call(this, name);
};

BatchTest.inherits(TestCase);

BatchTest.prototype.setUp = function() {
  shindig = shindig || {};
  shindig.auth = {};
  shindig.auth.getSecurityToken =  function() {
    return 'dsjk452487sdf7sdf865%&^*&^8cjhsdf';
  };

   gadgets.config.init({ "osapi.base" : {
      "rpcUrl" : "http://%host%/social"}
  });
};

BatchTest.prototype.tearDown = function() {
  shindig.auth = undefined;
};

BatchTest.prototype.testAddAndExecuteOneRequests = function() {
  var batch = osapi.newBatch();
  this.assertBatchMembers(batch);

  batch.add('friends', osapi.people.getViewerFriends());

  var expectedJson = [{method:"people.get",params:
    {userId:["@viewer"],groupId:"@friends",fields:["id","displayName"]},
      id:"friends"}
    ];

  var argsInCallToMakeNonProxiedRequest;
  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback, params, contentType) {
      argsInCallToMakeNonProxiedRequest = { url : url, callback : callback, params : params,
        contentType : contentType};
    };
    batch.execute(function() {});
    this.assertArgsToMakeNonProxiedRequest(argsInCallToMakeNonProxiedRequest, expectedJson);
  } finally {
    gadgets.io.makeNonProxiedRequest = oldMakeRequest;
  }
};

BatchTest.prototype.testAddAndExecuteTwoRequests = function() {
  var batch = osapi.newBatch();
  this.assertBatchMembers(batch);

  batch.add('friends', osapi.people.getViewerFriends()).
      add('activities', osapi.activities.get());

  var expectedJson = [{method:"people.get",params:
    {userId:["@viewer"],groupId:"@friends",fields:["id","displayName"]},
      id:"friends"},
    {method:"activities.get",params:
      {userId:["@viewer"],groupId:"@self",appId:"@app"},id:"activities"}
    ];

  var argsInCallToMakeNonProxiedRequest;
  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback, params, contentType) {
      argsInCallToMakeNonProxiedRequest = { url : url, callback : callback, params : params,
        contentType : contentType};
    };
    batch.execute(function() {});
    this.assertArgsToMakeNonProxiedRequest(argsInCallToMakeNonProxiedRequest, expectedJson);
  } finally {
    gadgets.io.makeNonProxiedRequest = oldMakeRequest;
  }
};

BatchTest.prototype.testAddAndExecuteMixedJsonAndMakeRequest = function() {
  var that = this;
  var batch = osapi.newBatch();
  this.assertBatchMembers(batch);

  batch.add('friends', osapi.people.getViewerFriends()).
      add('makerequest', osapi.makeRequest('http://www.google.com', {}));

  var expectedJson = [{method:"people.get",params:
    {userId:["@viewer"],groupId:"@friends",fields:["id","displayName"]},
      id:"friends"}];

  var argsInCallToMakeNonProxiedRequest, argsInCallToMakeRequest;
  var oldMakeNonProxiedRequest = gadgets.io.makeNonProxiedRequest;
  var oldMakeRequest = gadgets.io.makeRequest;
  
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback, params, contentType) {
      argsInCallToMakeNonProxiedRequest = { url : url, callback : callback, params : params,
        contentType : contentType};
    };
    gadgets.io.makeRequest = function(url, makeRequestCallback, options) {
      argsInCallToMakeRequest = { url : url, callback : makeRequestCallback, options : options};
    };

    batch.execute(function(data) {
      that.assertTrue("There is a response", data);
    });

    this.assertArgsToMakeNonProxiedRequest(argsInCallToMakeNonProxiedRequest, expectedJson);
    this.assertArgsToMakeRequest(argsInCallToMakeRequest);
  } finally {
    gadgets.io.makeNonProxiedRequest = oldMakeNonProxiedRequest;
    gadgets.io.makeRequest = oldMakeRequest;
  }
};

BatchTest.prototype.testEmptyBatch = function() {
  var batch = osapi.newBatch();
  this.assertBatchMembers(batch);

  var oldTimeout = window.setTimeout;
  try {
    window.setTimeout = function(fn, time) { fn.call()};

    var that = this;
    batch.execute(function(data) {
      that.assertTrue("Data should be returned", data);
      that.assertTrue("Data should be empty", data.length === undefined);
    });
  } finally {
    window.setTimeout = oldTimeout;
  }
};

/**
 * Checks to see if generated batch function has the correct members. 
 *
 * @param fn (Function) The function which should have these properties
 */
BatchTest.prototype.assertBatchMembers = function(fn) {
  this.assertTrue('Should have produced a batch', fn !== undefined);
  this.assertTrue('Should have an execute method', fn.execute);
  this.assertTrue('Should have an add method', fn.add);
};



function BatchTestSuite() {
  TestSuite.call(this, 'BatchTestSuite');
  this.addTestSuite(BatchTest);
}

BatchTestSuite.inherits(TestSuite);

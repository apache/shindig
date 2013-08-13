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

  window._setTimeout = window.setTimeout;
  window.setTimeout = function(fn, time) { fn.call()};

  document.scripts = [];
  gadgets.config.init({ "osapi.services" : {
      "http://%host%/social/rpc" : ["system.listMethods", "people.get", "activities.get", 
        "activities.create", "appdata.get", "appdata.update", "appdata.delete"] }
  });
};

BatchTest.prototype.tearDown = function() {
  shindig.auth = undefined;
  window.setTimeout = window._setTimeout;
};

BatchTest.prototype.testAddAndExecuteOneRequests = function() {
  var batch = osapi.newBatch();
  this.assertBatchMembers(batch);
  batch.add('friends', osapi.people.get());
  var expectedJson = [{method:"people.get",params:
    {userId:"@viewer",groupId:"@self"},
      id:"friends"}
    ];

  var argsInCallToMakeNonProxiedRequest;
  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback, params, headers) {
      argsInCallToMakeNonProxiedRequest = { url : url, callback : callback, params : params,
        headers : headers};
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

  batch.add('friends', osapi.people.get()).
      add('activities', osapi.activities.get());

  var expectedJson = [{method:"people.get",params:
    {userId:"@viewer",groupId:"@self"},
      id:"friends"},
    {method:"activities.get",params:
      {userId:"@viewer",groupId:"@self"},id:"activities"}
    ];

  var argsInCallToMakeNonProxiedRequest;
  var oldMakeRequest = gadgets.io.makeNonProxiedRequest;
  try {
    gadgets.io.makeNonProxiedRequest = function(url, callback, params, headers) {
      argsInCallToMakeNonProxiedRequest = { url : url, callback : callback, params : params,
        headers : headers};
    };
    batch.execute(function() {});
    this.assertArgsToMakeNonProxiedRequest(argsInCallToMakeNonProxiedRequest, expectedJson);
  } finally {
    gadgets.io.makeNonProxiedRequest = oldMakeRequest;
  }
};

BatchTest.prototype.testEmptyBatch = function() {
  var batch = osapi.newBatch();
  this.assertBatchMembers(batch);

  var that = this;
  batch.execute(function(data) {
    that.assertTrue("Data should be returned", data);
    that.assertTrue("Data should be empty", typeof data.length == 'undefined');
  });
};

/**
 * Checks to see if generated batch function has the correct members. 
 *
 * @param fn (Function) The function which should have these properties
 */
BatchTest.prototype.assertBatchMembers = function(fn) {
  this.assertTrue('Should have produced a batch', typeof fn != 'undefined');
  this.assertTrue('Should have an execute method', fn.execute);
  this.assertTrue('Should have an add method', fn.add);
};



function BatchTestSuite() {
  TestSuite.call(this, 'BatchTestSuite');
  this.addTestSuite(BatchTest);
}

BatchTestSuite.inherits(TestSuite);

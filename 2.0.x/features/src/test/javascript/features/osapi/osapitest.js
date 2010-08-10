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
  window._setTimeout = window.setTimeout;
  window.setTimeout = function() {};
};

OsapiTest.prototype.tearDown = function() {
  window.setTimeout = window._setTimeout;
};

OsapiTest.prototype.testCall = function() {
  var transport = {};
  osapi._registerMethod("test.method", transport);
  var transportCalled = false;
  transport.execute = function(requests, callback) {
    transportCalled = true;
    callback([
      {id:"test.method",result:{a:"b"}}
    ]);
  };
  var callbackCalled = false;
  osapi.test.method({}).execute(function(result) {
    callbackCalled = true;
  });
  this.assertTrue("osapi transport correctly called", transportCalled);
  this.assertTrue("osapi callback correctly called", callbackCalled);
};


function OsapiTestSuite() {
  TestSuite.call(this, 'OsapiTestSuite');
  this.addTestSuite(OsapiTest);
}

OsapiTestSuite.inherits(TestSuite);

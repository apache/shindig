/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */



function RequestNavigateToTest(name) {
  TestCase.call(this, name);
}

RequestNavigateToTest.inherits(TestCase);

(function() {

var rpcs, oldRpc = gadgets.rpc;

RequestNavigateToTest.prototype.setUp = function() {
  rpcs = [];
  gadgets.rpc = {
    call: function() {
      rpcs.push(arguments);
    }
  };
};

RequestNavigateToTest.prototype.tearDown = function() {
  gadgets.rpc.call = oldRpc;
};

RequestNavigateToTest.prototype.testBasic = function() {
  gadgets.views.requestNavigateTo("canvas");

  this.assertEquals("requestNavigateTo", rpcs[0][1]);
  this.assertEquals("canvas", rpcs[0][3]);
};

RequestNavigateToTest.prototype.testViewObject = function() {
  gadgets.views.requestNavigateTo(new gadgets.views.View("canvas"));

  this.assertEquals("requestNavigateTo", rpcs[0][1]);
  this.assertEquals("canvas", rpcs[0][3]);
};

RequestNavigateToTest.prototype.testKeyValueParams = function() {
  gadgets.views.requestNavigateTo("canvas", {foo:"bar"});

  this.assertEquals("requestNavigateTo", rpcs[0][1]);
  this.assertEquals("canvas", rpcs[0][3]);
  this.assertEquals("bar", rpcs[0][4].foo);
};

RequestNavigateToTest.prototype.testUriParams = function() {
  gadgets.views.requestNavigateTo("canvas", "/foo/bar?blah");

  this.assertEquals("requestNavigateTo", rpcs[0][1]);
  this.assertEquals("canvas", rpcs[0][3]);
  this.assertEquals("/foo/bar?blah", rpcs[0][4]);
};

})();


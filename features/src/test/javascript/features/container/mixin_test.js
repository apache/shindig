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

/**
 * @fileoverview
 *
 * Unittests for container mixin functionality.
 */

function MixinTest(name) {
  TestCase.call(this, name); // super
}
MixinTest.inherits(TestCase);

MixinTest.prototype.setUp = function() {
  this.apiUri = window.__API_URI;
  window.__API_URI = shindig.uri('http://shindig.com');
  this.containerUri = window.__CONTAINER_URI;
  window.__CONTAINER_URI = shindig.uri('http://container.com');
  this.gadgetsRpc = gadgets.rpc;
  gadgets.rpc = {
    register: function() {}
  };
};

MixinTest.prototype.tearDown = function() {
  window.__API_URI = this.apiUri;
  window.__CONTAINER_URI = this.containerUri;
  gadgets.rpc = this.gadgetsRpc;
};

MixinTest.prototype.testMixin = function() {
  var testobj = new osapi.container.Container();
  this.assertUndefined(testobj.test);

  osapi.container.addMixin('test', function(context) {
    return {
      'test1' : function() {
        return true;
      }
    };
  });
  osapi.container.addMixin('test', function(context, base) {
    base.test2 = function() {
      return false;
    };

    return base;
  });

  testobj = new osapi.container.Container();

  this.assertNotUndefined(testobj.test);
  this.assertNotUndefined(testobj.test.test1);
  this.assertNotUndefined(testobj.test.test2);
  this.assertTrue(testobj.test.test1());
  this.assertFalse(testobj.test.test2());
};
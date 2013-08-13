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

function ViewEnhancementsTest(name) {
  TestCase.call(this, name);
}

ViewEnhancementsTest.inherits(TestCase);

(function() {

  var rpcs, oldRpc = gadgets.rpc;

  ViewEnhancementsTest.prototype.setUp = function() {
    rpcs = [];
    gadgets.rpc = {
      call: function() {
        rpcs = [];
        rpcs.push(arguments);
      }
    };
  };

  ViewEnhancementsTest.prototype.tearDown = function() {
    gadgets.rpc.call = oldRpc;
  };

  ViewEnhancementsTest.prototype.testOpenGadget = function() {
    var resultCallback = function() {};
    var navigateCallback = function() {};
    var params = {coordinates: {top: 100, left: 100}};

    gadgets.views.openGadget(resultCallback, navigateCallback);

    this.assertEquals('..', rpcs[0][0]);
    this.assertEquals('gadgets.views.openGadget', rpcs[0][1]);
    this.assertNotNull('Assert not null error', rpcs[0][2]);
    this.assertNotNull('Assert not null error', rpcs[0][3]);
    this.assertUndefined('Assert undefined error', rpcs[0][4]);

    gadgets.views.openGadget(resultCallback, navigateCallback, params);
    this.assertEquals(params, rpcs[0][4]);
  };

  ViewEnhancementsTest.prototype.testOpenEmbeddedExperience = function() {
    var resultCallback = function() {};
    var navigateCallback = function() {};
    var dataModel = {'url': 'http://www.example.com'};
    var params = {coordinates: {top: 100, left: 100}};

    gadgets.views.openEmbeddedExperience(resultCallback, navigateCallback,
        dataModel, params);

    this.assertEquals('..', rpcs[0][0]);
    this.assertEquals('gadgets.views.openEmbeddedExperience', rpcs[0][1]);
    this.assertNotNull('Assert not null error', rpcs[0][2]);
    this.assertNotNull('Assert not null error', rpcs[0][3]);
    this.assertEquals('http://www.example.com', rpcs[0][4]['url']);
    this.assertEquals(params, rpcs[0][5]);
  };

  ViewEnhancementsTest.prototype.testOpenUrl = function() {
    var url = 'www...';
    var navigateCallback = function() {};
    var viewTarget = 'dialog';

    gadgets.views.openUrl(url, navigateCallback);

    this.assertEquals('..', rpcs[0][0]);
    this.assertEquals('gadgets.views.openUrl', rpcs[0][1]);
    this.assertNotNull('Assert not null error', rpcs[0][2]);
    this.assertEquals(url, rpcs[0][3]);
    this.assertUndefined('Assert undefined error', rpcs[0][4]);

    gadgets.views.openUrl(url, navigateCallback, viewTarget);
    this.assertEquals(viewTarget, rpcs[0][4]);

    var coordinates = {coordinates: {top: 100, left: 100}};
    gadgets.views.openUrl(url, navigateCallback, viewTarget, coordinates);
    this.assertEquals(coordinates, rpcs[0][5]);
  };

  ViewEnhancementsTest.prototype.testClose = function() {

    var site = {};

    gadgets.views.close();

    this.assertEquals('..', rpcs[0][0]);
    this.assertEquals('gadgets.views.close', rpcs[0][1]);
    this.assertNull('Assert null error', rpcs[0][2]);
    this.assertUndefined('Assert undefined error', rpcs[0][3]);

    gadgets.views.close(site);
    this.assertEquals(site, rpcs[0][3]);
  };

  ViewEnhancementsTest.prototype.testSetReturnValue = function() {

    var returnValue = {};

    gadgets.views.setReturnValue(returnValue);

    this.assertEquals('..', rpcs[0][0]);
    this.assertEquals('gadgets.views.setReturnValue', rpcs[0][1]);
    this.assertNull('Assert null error', rpcs[0][2]);
    this.assertEquals(returnValue, rpcs[0][3]);
  };

  ViewEnhancementsTest.prototype.testGetContainerDimensions = function() {
    var resultCallback = function() {};

    gadgets.window.getContainerDimensions(resultCallback);

    this.assertEquals('..', rpcs[0][0]);
    this.assertEquals('gadgets.window.getContainerDimensions', rpcs[0][1]);
    this.assertEquals(resultCallback, rpcs[0][2]);
  };

})();

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

/**
 * @fileoverview Tests for selection
 */

function SelectionTest(name) {
  TestCase.call(this, name);
}

SelectionTest.inherits(TestCase);

(function() {

  SelectionTest.prototype.setUp = function() {
    this.apiUri = window.__API_URI;
    window.__API_URI = shindig.uri('http://shindig.com');
    this.containerUri = window.__CONTAINER_URI;
    window.__CONTAINER_URI = shindig.uri('http://container.com');
    this.shindigContainerGadgetSite = osapi.container.GadgetSite;

    this.gadgetsRpc = gadgets.rpc;
    var that = this;
    gadgets.rpc = {};
    gadgets.rpc.register = function() {
    };
    gadgets.rpc.call = function() {
      that.rpcArguments = Array.prototype.slice.call(arguments);
    };
  };

  SelectionTest.prototype.tearDown = function() {
    window.__API_URI = this.apiUri;
    window.__CONTAINER_URI = this.containerUri;
    osapi.container.GadgetSite = this.shindigContainerGadgetSite;
    gadgets.rpc = this.gadgetsRpc;
    this.rpcArguments = undefined;
  };

  SelectionTest.prototype.testContainerSetGetSelection = function() {
    var container = new osapi.container.Container({});
    var _token = "hello";
    container.selection.setSelection(_token);
    var token = container.selection.getSelection();
    this.assertEquals(_token, token);
  };

  SelectionTest.prototype.testGadgetSetGetSelection = function() {
    var container = new osapi.container.Container({});
    var token = "hello";
    gadgets.selection.setSelection(token);
    this.assertRpcCalled("..", "gadgets.selection.set", null, token);
  };

  SelectionTest.prototype.testGadgetGetSelection = function() {
    var container = new osapi.container.Container({});
    var token = "hello";
    gadgets.selection.setSelection(token);
    var _token = gadgets.selection.getSelection();
    this.assertEquals(token, _token);
  };

  SelectionTest.prototype.testGadgetAddSelectionListener = function() {
    var container = new osapi.container.Container({});
    gadgets.selection.addListener(function(){});
    this.assertRpcCalled("..", "gadgets.selection.register", function(){});
    gadgets.selection.addListener(function(){});
    this.assertNoRpcCalled();
  };

  /**
   * Asserts gadgets.rpc.call() is called with the expected arguments given.
   * Note that it resets this.rpcArguments for next RPC call assertion.
   */
  SelectionTest.prototype.assertRpcCalled = function() {
    this.assertNotUndefined("RPC was not called.", this.rpcArguments);
    this.assertEquals("RPC argument list not valid length.", arguments.length,
        this.rpcArguments.length);

    for ( var i = 0; i < arguments.length; i++) {
      this.assertEquals(arguments[i], this.rpcArguments[i]);
    }
    this.resetRpc();
  };

  /**
   * Resets this.rpcArguments.
   */
  SelectionTest.prototype.resetRpc = function() {
    this.rpcArguments = undefined;
  };

  /**
   * Asserts that no gadgets.rpc.call() is called.
   */
  SelectionTest.prototype.assertNoRpcCalled = function() {
    this.assertUndefined("RPC was called.", this.rpcArguments);
  };

})();
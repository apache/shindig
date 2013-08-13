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
 * @fileoverview Tests for actions feature
 */

function DeclarativeActionsTest(name) {
  TestCase.call(this, name);
}

DeclarativeActionsTest.inherits(TestCase);

(function() {

  DeclarativeActionsTest.prototype.setUp = function() {
    this.apiUri = window.__API_URI;
    window.__API_URI = shindig.uri('http://shindig.com');
    this.containerUri = window.__CONTAINER_URI;
    window.__CONTAINER_URI = shindig.uri('http://container.com');

    this.gadgetsRpc = gadgets.rpc;
    var self = this;
    gadgets.rpc = {};
    gadgets.rpc.register = function(service, callback) {
      if (self.captures && self.captures.hasOwnProperty(service)) {
        self.captures[service] = callback;
      }
    };
    gadgets.rpc.call = function() {
      self.rpcArguments = Array.prototype.slice.call(arguments);
    };
  };

  DeclarativeActionsTest.prototype.tearDown = function() {
    window.__API_URI = this.apiUri;
    window.__CONTAINER_URI = this.containerUri;

    gadgets.rpc = this.gadgetsRpc;
    delete this.rpcArguments;
    delete this.captures;
  };

  DeclarativeActionsTest.prototype.testGadgetsAddAction = function() {
    var actionId = "testAction";
    var callbackFn = function(){};
    var _actionObj = {
        id: actionId,
        label:"Test Action",
        path:"container/navigationLinks",
        callback: callbackFn
      };
    gadgets.actions.addAction(_actionObj);
    this.assertRpcCalled('..', 'actions.bindAction', null, _actionObj);
  };

  DeclarativeActionsTest.prototype.testGadgetsRemoveAction = function() {
    var actionId = "testAction";
    gadgets.actions.removeAction(actionId);
    this.assertRpcCalled('..', 'actions.removeAction', null, actionId);
  };

  DeclarativeActionsTest.prototype.testGadgetsRunAction = function() {
    var actionId = "testAction";
    var opt_selection = "testSelection";
    gadgets.actions.runAction(actionId, opt_selection);
    this.assertRpcCalled('..', 'actions.runAction', null,
      actionId, opt_selection
    );
  };


  DeclarativeActionsTest.prototype.testContainerGetAction = function() {
    var container = new osapi.container.Container({});
    var actionId = "testAction";
    var actionObj = container.actions.getAction(actionId);
    // registry is empty
    this.assertUndefined(actionObj);
  };


  DeclarativeActionsTest.prototype.testContainerGetActionsByPath = function() {
    var container = new osapi.container.Container();
    var actionId = "testAction";
    var actionsArray = container.actions
      .getActionsByPath("container/navigationLinks");
    //registry is empty
    this.assertEquals(actionsArray, []);
  };

  DeclarativeActionsTest.prototype.testContainerGetActionsByDataType = function() {
    var container = new osapi.container.Container();
    var actionId = "testAction";
    var actionsArray = container.actions.getActionsByDataType("opensocial.Person");
    // registry is empty
    this.assertEquals(actionsArray, []);
  };

  DeclarativeActionsTest.prototype.testBindInvalidAction = function() {
    var undef, captures = this.captures = {
      'actions.bindAction': undef
    };

    var container = new osapi.container.Container(),
        showActionCalled = false,
        actionId = 'testAction',
        actionObj = {
          id: actionId
        };

    this.assertNotUndefined('RPC endpoint "actions.bindAction" was not registered.', captures['actions.bindAction']);
    container.actions.registerShowActionsHandler(function() {
      showActionCalled = true;
    });
    captures['actions.bindAction'](actionObj);

    this.assertUndefined(container.actions.getAction(actionId));
    this.assertFalse(showActionCalled);
  };

  DeclarativeActionsTest.prototype.testContainerGetAction_Full = function() {
    var undef, captures = this.captures = {
      'actions.bindAction': undef,
      'actions.removeAction': undef
    };

    var container = new osapi.container.Container({}),
        actionId = "testAction",
        actionObj = {
          id: actionId,
          label: "Test Action",
          path: "container/navigationLinks"
        };

    this.assertNotUndefined('RPC endpoint "actions.bindAction" was not registered.', captures['actions.bindAction']);
    this.assertNotUndefined('RPC endpoint "actions.removeAction" was not registered.', captures['actions.removeAction']);
    captures['actions.bindAction'](actionObj);

    this.assertEquals(actionObj, container.actions.getAction(actionId));

    captures['actions.removeAction'](actionId);
    this.assertUndefined(container.actions.getAction(actionId));
  };


  DeclarativeActionsTest.prototype.testContainerGetActions_Full = function() {
    var undef, captures = this.captures = {
      'actions.bindAction': undef,
      'actions.removeAction': undef
    };

    var container = new osapi.container.Container({}),
        actions = [
          { id: "test1", label: "Test Action1", path: "container/navigationLinks" },
          { id: "test2", label: "Test Action2", path: "container/navigationLinks" },
          { id: "test3", label: "Test Action3", dataType: "opensocial.Person" },
          { id: "test4", label: "Test Action4", dataType: "opensocial.Person" }
        ];
    this.assertNotUndefined('RPC endpoint "actions.bindAction" was not registered.', captures['actions.bindAction']);
    this.assertNotUndefined('RPC endpoint "actions.removeAction" was not registered.', captures['actions.removeAction']);

    for (var i = 0; i < actions.length; i++) {
      captures['actions.bindAction'](actions[i]);
    }
    this.assertEquals(actions, container.actions.getAllActions());

    for (var i = 0; i < actions.length; i++) {
      captures['actions.removeAction'](actions[i].id);
    }
    this.assertEquals([], container.actions.getAllActions());

  };


  DeclarativeActionsTest.prototype.testContainerGetActionsByPath_Full = function() {
    var undef, captures = this.captures = {
      'actions.bindAction': undef,
      'actions.removeAction': undef
    };

    var container = new osapi.container.Container(),
        actionObj = {
          id: "testAction",
          label: "Test Action",
          path: "container/navigationLinks"
        };
    this.assertNotUndefined('RPC endpoint "actions.bindAction" was not registered.', captures['actions.bindAction']);
    this.assertNotUndefined('RPC endpoint "actions.removeAction" was not registered.', captures['actions.removeAction']);

    captures['actions.bindAction'](actionObj);
    this.assertEquals([actionObj], container.actions.getActionsByPath("container/navigationLinks"));

    captures['actions.removeAction'](actionObj.id);
    this.assertEquals([], container.actions.getActionsByPath("container/navigationLinks"));
  };

  DeclarativeActionsTest.prototype.testContainerGetActionsByDataType_Full = function() {
    var undef, captures = this.captures = {
      'actions.bindAction': undef,
      'actions.removeAction': undef
    };

    var container = new osapi.container.Container();
        actionObj = {
          id: "testAction",
          label: "Test Action",
          dataType: "opensocial.Person"
        };
    this.assertNotUndefined('RPC endpoint "actions.bindAction" was not registered.', captures['actions.bindAction']);
    this.assertNotUndefined('RPC endpoint "actions.removeAction" was not registered.', captures['actions.removeAction']);

    captures['actions.bindAction'](actionObj);
    this.assertEquals([actionObj], container.actions.getActionsByDataType("opensocial.Person"));

    captures['actions.removeAction'](actionObj.id);
    this.assertEquals([], container.actions.getActionsByDataType("opensocial.Person"));
  };

  /**
   * Asserts gadgets.rpc.call() is called with the expected arguments given.
   */
  DeclarativeActionsTest.prototype.assertRpcCalled = function() {
    this.assertNotUndefined("RPC was not called.", this.rpcArguments);
    this.assertEquals("RPC argument list not valid length.", arguments.length,
        this.rpcArguments.length);

    for ( var i = 0; i < arguments.length; i++) {
      this.assertEquals(arguments[i], this.rpcArguments[i]);
    }
    this.rpcArguments = undefined;
  };
})();
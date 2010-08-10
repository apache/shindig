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
 * @fileoverview
 *
 * Unittests for the setprefs feature.
 */

function SetPrefsTest(name) {
  TestCase.call(this, name);
}

SetPrefsTest.inherits(TestCase);

SetPrefsTest.prototype.setUp = function() {
  var that = this;
  this.savedRpc = gadgets.rpc;
  gadgets.rpc = {};
  gadgets.rpc.call = function() {
    that.rpcArguments = Array.prototype.slice.call(arguments);
  };
};

SetPrefsTest.prototype.tearDown = function() {
  this.rpcArguments = undefined;
  gadgets.rpc = this.savedRpc;
};

SetPrefsTest.prototype.testSet = function() {
  var pref = new gadgets.Prefs();
  gadgets.Prefs.setInternal_('key', 100);

  // Clear the RPC call history before pref.set() is called.
  this.resetRpc();

  // The same group of values, should not invoke a RPC call.
  pref.set('key', 100);
  this.assertNoRpcCalled();

  // Value altered, should invoke a RPC call.
  pref.set('key', 200);
  this.assertEquals(200, pref.getInt('key'));
  this.assertRpcCalled(null, 'set_pref', null, 0, 'key', 200);

  // Set the same value again, should not invoke a RPC call.
  pref.set('key', 200);
  this.assertNoRpcCalled();
};

SetPrefsTest.prototype.testSetArray = function() {
  var pref = new gadgets.Prefs();
  pref.setArray('array', ['foo', 'bar']);
  var array = pref.getArray('array');
  this.assertEquals(2, array.length);
  this.assertEquals('foo', array[0]);
  this.assertEquals('bar', array[1]);

  this.assertRpcCalled(null, 'set_pref', null, 0, 'array', 'foo|bar');
};

SetPrefsTest.prototype.testSetArrayWithPipe = function() {
  var pref = new gadgets.Prefs();
  pref.setArray('array', ['foo', 'b|ar']);
  var array = pref.getArray('array');
  this.assertEquals(2, array.length);
  this.assertEquals('foo', array[0]);
  this.assertEquals('b|ar', array[1]);

  this.assertRpcCalled(null, 'set_pref', null, 0, 'array', 'foo|b%7Car');
};

SetPrefsTest.prototype.testSetArrayWithNumbers = function() {
  var pref = new gadgets.Prefs();
  pref.setArray('array', [1, 2]);
  var array = pref.getArray('array');
  this.assertEquals(2, array.length);
  this.assertEquals('1', array[0]);
  this.assertEquals('2', array[1]);

  this.assertRpcCalled(null, 'set_pref', null, 0, 'array', '1|2');
};

/**
 * Asserts gadgets.rpc.call() is called with the expected arguments given.
 * Note that it resets this.rpcArguments for next RPC call assertion.
 */
SetPrefsTest.prototype.assertRpcCalled = function() {
  this.assertNotUndefined("RPC was not called.", this.rpcArguments);
  this.assertEquals("RPC argument list not valid length.",
      arguments.length, this.rpcArguments.length);
    
  for (var i = 0; i < arguments.length; i++) {
    this.assertEquals(arguments[i], this.rpcArguments[i]);
  }
  this.resetRpc();
};

/**
 * Resets this.rpcArguments.
 */
SetPrefsTest.prototype.resetRpc = function() {
  this.rpcArguments = undefined;
};

/**
 * Asserts that no gadgets.rpc.call() is called.
 */
SetPrefsTest.prototype.assertNoRpcCalled = function() {
  this.assertUndefined("RPC was called.", this.rpcArguments);
};

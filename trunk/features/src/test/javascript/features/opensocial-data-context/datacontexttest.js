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
 * Unittests for the opensocial-data-context feature.
 */
function DataContextTest(name) {
  TestCase.call(this, name);
}

DataContextTest.inherits(TestCase);

DataContextTest.prototype.setUp = function() {
};

DataContextTest.prototype.tearDown = function() {
  var dataSets = opensocial.data.getDataContext().dataSets;
  for (var key in dataSets) {
    if (dataSets.hasOwnProperty(key)) {
      delete dataSets[key];
    }
  }
};

DataContextTest.prototype.testPutDataSet = function() {
  var context = opensocial.data.getDataContext();

  context.putDataSet('key', 'value');
  this.assertEquals('value', context.getDataSet('key'));
  
  // Test that putting null and undefined don't change the value.
  // TODO: this seems wrong;  why not support removing data?
  context.putDataSet('key', null);
  this.assertEquals('value', context.getDataSet('key'));

  context.putDataSet('key', undefined);
  this.assertEquals('value', context.getDataSet('key'));
};

/**
 * Test registerListener()
 */
DataContextTest.prototype.testRegisterListener = function() {
  var context = opensocial.data.getDataContext();
  var listenerCalledWithKey = null;
  var that = this;
  var listener = function(key) {
    listenerCalledWithKey = key;
    that.assertNotNull(key);
  };
  
  context.registerListener('key', listener);
  this.assertNull(listenerCalledWithKey);
  
  context.putDataSet('other', 1);
  this.assertNull(listenerCalledWithKey);

  context.putDataSet('key', 2);
  this.assertEquals('key', listenerCalledWithKey[0]);

  listenerCalledWithKey = null;
  context.putDataSet('key', 3);
  this.assertEquals('key', listenerCalledWithKey[0]);
};

/**
 * Test registerListener()
 */
DataContextTest.prototype.testRegisterListenerWithArray = function() {
  var context = opensocial.data.getDataContext();
  var listenerCalledWithKey = null;
  var that = this;
  var listener = function(key) {
    listenerCalledWithKey = key;
    that.assertNotNull(key);
  };
  
  context.registerListener(['aone', 'atwo'], listener);
  this.assertNull(listenerCalledWithKey);

  context.putDataSet('aone', 1);
  this.assertNull(listenerCalledWithKey);

  context.putDataSet('atwo', 2);
  this.assertEquals('atwo', listenerCalledWithKey[0]);

  context.putDataSet('aone', 3);
  this.assertEquals('aone', listenerCalledWithKey[0]);
};

/**
 * Test registerListener() with '*'
 */
DataContextTest.prototype.testRegisterListenerWithStar = function() {
  var context = opensocial.data.getDataContext();
  var listenerCalledWithKey = null;
  var that = this;
  var listener = function(key) {
    listenerCalledWithKey = key;
    that.assertNotNull(key);
  };
  
  context.registerListener('*', listener);
  this.assertNull(listenerCalledWithKey);
  
  context.putDataSet('one', 1);
  this.assertEquals('one', listenerCalledWithKey[0]);

  context.putDataSet('two', 2);
  this.assertEquals('two', listenerCalledWithKey[0]);
};

/**
 * Test getData()
 */
DataContextTest.prototype.testGetData = function() {
  var context = opensocial.data.getDataContext();
  context.putDataSet('key', 'value');
  this.assertEquals('value', context.getData()['key']);
  context.putDataSet('key', 'value2');
  this.assertEquals('value2', context.getData()['key']);
  
  // Test that altering the result of getData doesn't change the context
  var data = context.getData();
  data['key'] = 'ball';
  this.assertEquals('value2', context.getDataSet('key'));
};

/**
 * Test putDataSets()
 */
DataContextTest.prototype.testPutDataSets = function() {
  var context = opensocial.data.getDataContext();
  var counter = 0;
  var passedKeys = null;
  var listener = function(keys) {
    counter++;
    passedKeys = keys;
  };
  context.registerListener(['sets1', 'sets2'], listener);
  context.putDataSets({ sets1: 'a', sets2: 'b' });
  this.assertEquals('a', context.getDataSet('sets1'));
  this.assertEquals('b', context.getDataSet('sets2'));
  
  // Test that listener was only called once.
  this.assertEquals(1, counter);
  
  // Test that listener was passed both keys.
  this.assertEquals(2, passedKeys.length);
};

/**
 * Test registerOneTimeListener_()
 */
DataContextTest.prototype.testOneTimeListener = function() {
  var context = opensocial.data.getDataContext();
  var counter = 0;
  var listener = function(keys) {
    counter++;
  };
  context.registerOneTimeListener_('oneTime', listener);
  context.putDataSet('oneTime', 'foo');
  this.assertEquals(1, counter);
  context.putDataSet('oneTime', 'bar');
  this.assertEquals(1, counter);
};
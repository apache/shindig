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
  var dataSets = osd.getDataContext().dataSets_;
  for (var key in dataSets) {
    if (dataSets.hasOwnProperty(key)) {
      delete dataSets[key];
    }
  }
};

DataContextTest.prototype.testPutDataSet = function() {
  var context = osd.getDataContext();

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
  var context = osd.getDataContext();
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
  this.assertEquals('key', listenerCalledWithKey);

  listenerCalledWithKey = null;
  context.putDataSet('key', 3);
  this.assertEquals('key', listenerCalledWithKey);
}



/**
 * Test registerListener()
 */
DataContextTest.prototype.testRegisterListenerWithArray = function() {
  var context = osd.getDataContext();
  var listenerCalledWithKey = null;
  var that = this;
  var listener = function(key) {
    listenerCalledWithKey = key;
    that.assertNotNull(key);
  };
  
  context.registerListener(['one', 'two'], listener);
  this.assertNull(listenerCalledWithKey);
  
  context.putDataSet('one', 1);
  this.assertNull(listenerCalledWithKey);

  context.putDataSet('two', 2);
  this.assertEquals('two', listenerCalledWithKey);

  context.putDataSet('one', 3);
  this.assertEquals('one', listenerCalledWithKey);
}


/**
 * Test registerListener() with '*'
 */
DataContextTest.prototype.testRegisterListenerWithStar = function() {
  var context = osd.getDataContext();
  var listenerCalledWithKey = null;
  var that = this;
  var listener = function(key) {
    listenerCalledWithKey = key;
    that.assertNotNull(key);
  };
  
  context.registerListener('*', listener);
  this.assertNull(listenerCalledWithKey);
  
  context.putDataSet('one', 1);
  this.assertEquals('one', listenerCalledWithKey);

  context.putDataSet('two', 2);
  this.assertEquals('two', listenerCalledWithKey);
}
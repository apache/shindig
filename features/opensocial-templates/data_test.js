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
 * Returns a namespace object. Creates one if it does not exist.
 * @param {string} name The namespace name.
 * @return {Object} The namespace object.
 */
function verifyNamespace(name) {
  var ns = os.getNamespace(name);
  if (!ns) {
    ns = os.createNamespace(name);
  }
  return ns;
}

/**
 * Unit test to check full functionality of a request handler.
 */
function testRequestHandler() {
  verifyNamespace('test');
  var results = {};
  os.data.registerRequestHandler('test:request', function(descriptor) {
    results[descriptor.key] = descriptor.getAttribute('data');
  });
  var xmlData =
      '<test:request key="first" data="testData"/>' +
      '<test:request key="second" data="${foo}"/>';
window.console.log(os.data);
  os.data.loadRequests(xmlData);
  assertNotNull(os.data.requests_['first']);
  assertNotNull(os.data.requests_['second']);

  os.data.DataContext.putDataSet("foo", "bar");
  os.data.executeRequests();

  assertEquals('testData', results['first']);
  assertEquals('bar', results['second']);
}

/**
 * Unit test to test data result handlers.
 */
function testPutDataSet() {
  var key = 'test1';
  var value = 'foo';
  os.data.DataContext.putDataSet(key, value);
  assertEquals(value, os.data.DataContext.getDataSet(key));
}

/**
 * Unit test to test listener functionality when a data key is put.
 */
function testListener() {
  var fired = false;
  os.data.DataContext.registerListener('testKey', function() {
    fired = true;
  });
  os.data.DataContext.putDataSet('testKey', {});
  assertEquals(true, fired);
}


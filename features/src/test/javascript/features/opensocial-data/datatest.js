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
 * Unittests for the opensocial-data-context feature.
 */
function DataTest(name) {
  TestCase.call(this, name);
}

DataTest.inherits(TestCase);

DataTest.prototype.setUp = function() {
};

DataTest.prototype.testParseExpression = function() {
  var expressions = [
    [ "Hello world", null ],
    [ "${foo}", "(foo)" ],
    [ "Hello ${foo} world", "'Hello '+(foo)+' world'" ],
    [ "${foo} ${bar}", "(foo)+' '+(bar)" ]
  ];
  for (var i = 0; i < expressions.length; i++) {
    this.assertEquals(
        expressions[i][1],
        opensocial.data.parseExpression_(expressions[i][0])
    );
  }
};

DataTest.prototype.testEvalExpression = function() {
  var data = {
    'foo': 'Hello',
    'bar': 'World'
  };
  data.name = {
    'first': 'John',
    'last': 'Doe'
  };
  opensocial.data.DataContext.putDataSet("test", data);
  var expressions = [
    [ opensocial.data.parseExpression_("Test: ${test.foo}"), "Test: Hello" ],
    [ opensocial.data.parseExpression_("${test.foo} ${test.bar}!"), "Hello World!" ],
    [ opensocial.data.parseExpression_("${test.name.first} ${test.name.last}"), "John Doe" ]
  ];
  for (var i = 0; i < expressions.length; i++) {
    this.assertEquals(
        expressions[i][1],
        opensocial.data.DataContext.evalExpression(expressions[i][0])
    );
  }
};

/**
 * Unit test to test data result handlers.
 */
DataTest.prototype.testPutDataSet = function() {
  var key = 'test1';
  var value = 'foo';
  opensocial.data.DataContext.putDataSet(key, value);
  this.assertEquals(value, opensocial.data.DataContext.getDataSet(key));
};

function registerNS(prefix) {
  opensocial.xmlutil.NSMAP[prefix] = "#" + prefix;
};

/**
 * Unit test to check full functionality of a request handler.
 */
DataTest.prototype.testRequestHandler = function() {
  registerNS("test");
  var results = {};
  
  opensocial.data.registerRequestHandler('test:request', function(descriptor) {
    results[descriptor.key] = descriptor.getAttribute('data');
  });
  
  var xmlData =
      '<test:request key="first" data="testData"/>' +
      '<test:request key="second" data="${foo}"/>';
  
  try {
    opensocial.data.loadRequests(xmlData);
  } catch (e) {
    // TODO: This test breaks because Mavan's JSUnit doesn't implement neither 
    // IE's nor FF's XML parsing interface.
    return;
  }

  this.assertNotNull(opensocial.data.requests_['first']);
  this.assertNotNull(opensocial.data.requests_['second']);

  opensocial.data.DataContext.putDataSet("foo", "bar");
  opensocial.data.executeRequests();

  this.assertEquals('testData', results['first']);
  this.assertEquals('bar', results['second']);
};

/**
 * Unit test to test listener functionality when a data key is put.
 */
DataTest.prototype.testListener = function() {
  var fired = false;
  opensocial.data.DataContext.registerListener('testKey', function() {
    fired = true;
  });
  opensocial.data.DataContext.putDataSet('testKey', {});
  this.assertEquals(true, fired);
};


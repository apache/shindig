/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Verifies members for a single service request function
 * @param fn (Function) Service Request function
 */
TestCase.prototype.assertRequestPropertiesForService = function(fn) {
  this.assertTrue('Should have produced a result', fn);
  this.assertTrue('Should have an execute method', fn.execute);
  this.assertTrue('Should have a json-rpc method', !!fn.method);
  this.assertTrue('Should have a json-rpc', fn.rpc);
};


/**
 * Verify that arguments sent out of system (to non-proxied xhr) are built properly
 * @param argsInCall
 * @param expectedJson
 */
TestCase.prototype.assertArgsToMakeNonProxiedRequest = function(argsInCall, expectedJson) {
  this.assertTrue('url should be passed to makeNonProxiedRequest',
      argsInCall.hasOwnProperty('url'));
  this.assertTrue('callback should be passed to makeNonProxiedRequest',
      argsInCall.callback);
  this.assertTrue('params should be passed to makeNonProxiedRequest',
      argsInCall.params);
  this.assertEquals('Content type should match', 'application/json',
      argsInCall.headers['Content-Type']);
  this.assertEquals('Json for batch should match', expectedJson,
      gadgets.json.parse(argsInCall.params.POST_DATA));

};

/**
 * Verify that arguments sent out of system (to proxied xhr) are built properly
 * @param argsInCall
 * @param expectedJson
 */
TestCase.prototype.assertArgsToMakeRequest = function(argsInCall) {
  this.assertTrue('url should be passed to makeNonProxiedRequest',
      argsInCall.hasOwnProperty('url'));
  this.assertTrue('callback should be passed to makeNonProxiedRequest',
      argsInCall.callback);
  this.assertTrue('options should be passed to makeNonProxiedRequest',
      argsInCall.options);

};

/**
 * Make a callback that can be asserted that it was called, and that still calls
 * a callback.
 * @return (Function) A callback function
 */
var makeInspectableCallback = function(realCallback) {
  var called = false;

  return {
    callback : function(result) {
      called = true;
      if (realCallback != null) {
        if (typeof result !== 'array') {
          result = [result];
        }
        realCallback.apply(this, result);
      }
    },
    wasCalled : function() {
      return called;
    }
  };
};


/**
 * An assert equals that compares correctly and prints useful output for object types, like json.
 * @param msg
 * @param expected
 * @param actual
 */
TestCase.prototype.assertEquals = function(msg, expected, actual) {
  if (arguments.length == 2) {
    actual = expected;
    expected = msg;
    msg = null;
  }

  if (!deepEquals(expected, actual)) {
    if (typeof( expected ) == 'string' && typeof( actual ) == 'string') {
      throw new ComparisonFailure(msg, expected, actual, new CallStack());
    } else {
      this.fail('Expected:<' + getSourceMessage(expected) +
                '>\n, but was:<' + getSourceMessage(actual) + '>'
          , new CallStack(), msg);
    }
  }
};

/**
 * Print an operand to an assert operation.
 * @param expected
 * @param actual
 */
var getSourceMessage = function(operand) {
  if (operand === undefined) {
    return "undefined";
  } else if (operand === null) {
    return "null";
  } else {
    return operand.toSource();
  }
};

/**
 * Implements deep equality for JSON objects;  the two objects
 * are equal if they have the same properties with the same values.
 *
 * @param {Object} a first object
 * @param {Object} b second object
 * @return {boolean} true if the objects are equal
 * @private
 */
function deepEquals(expected, actual) {
  if (expected === actual) {
    return true;
  }

  // Undefined/null can be treated as equal here, I believe
  if (expected == null) {
    return actual == null;
  }

  // If the types are different, the objects are different
  var typeOfExpected = typeof expected;
  if (typeOfExpected != typeof actual) {
    return false;
  }

  // A few types that can handle a straight === check
  if ((typeOfExpected == 'string') || (typeOfExpected == 'boolean') ||
      (typeOfExpected == 'number')) {
    return expected === actual;
  }

  // If it's an array, use deepEquals on each entry
  if (typeOfExpected == 'array') {
    if (expected.length != actual.length) {
      return false;
    }

    for (var i = 0; i < expected.length; i++) {
      if (!deepEquals(expected[i], actual[i])) {
        return false;
      }
    }

    return true;
  }

  // OK, we figure it's just an object.

  // Make sure everything in a matches the value in b
  for (var aKey in expected) {
    if (!expected.hasOwnProperty(aKey)) {
      continue;
    }

    if (!deepEquals(expected[aKey], actual[aKey])) {
      return false;
    }
  }

  // And make sure everything in b is in a
  for (var bKey in actual) {
    if (!actual.hasOwnProperty(bKey)) {
      continue;
    }
    if (!(bKey in expected)) {
      return false;
    }
  }

  return true;
}
;



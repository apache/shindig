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

function findTests() {
  // The following tests fail due to HtmlUnit limitation.
  // If a name exists in this object, it is excluded from testing.
  var excludedTests = {
    testEventHandlers : 1,
    testInjectStyle : 1,
    testLoadContent : 1,
    testInjectJavaScript : 1,
    testRegisterTemplates : 1
  };

  var testSource = typeof RuntimeObject != 'undefined' ?
                   RuntimeObject('test' + '*') : self;
  var tests = {};
  for (i in testSource) {
    if (i.substring(0, 4) == 'test' && typeof(testSource[i]) == 'function'
      && ! (i in excludedTests)) {
      tests[i] = testSource[i];
    }
  }
  return tests;
}

function assertTrue(value) {
  if (!value) {
    throw "assertTrue() failed: ";
  }
}

function assertFalse(value) {
  if (value) {
    throw "assertFalse() failed: ";
  }
}

function assertEquals(a, b) {
  if (a != b) {
    throw "assertEquals() failed: " +
          "\nExpected \"" + a + "\", was \"" + b + "\"";
  }
}

function assertNotNull(value) {
  if (value === null) {
    throw "assertTrue() failed: ";
  }
}

function allTests() {
  var tests = findTests();
  for (var testMethod in tests) {
    alert(testMethod);
    tests[testMethod]();
    alert("FINISHED");
  }
}

gadgets.util.registerOnLoadHandler(allTests);

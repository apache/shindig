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
 * Unittests for gadgets.prefs.
 * TODO cover more gadgets.prefs functions.
 */

function PrefsTest(name) {
  TestCase.call(this, name);
}

PrefsTest.inherits(TestCase);

PrefsTest.prototype.setUp = function() {
  this.params = {myCounter: 100, myString: '15.3', myUndefined: undefined,
      myObject: {}, myFloat: 3.3, myBool: true, myArray: ['one', 'two'],
      boolString: 'true'};
};

PrefsTest.prototype.tearDown = function() {
  this.params = undefined;
};

PrefsTest.prototype.testGetInt = function() {
    var expectedResults = {myCounter: 100, myString: 15, myUndefined: 0,
        myObject: 0, myFloat: 3};

    var pref = new gadgets.Prefs();

    gadgets.Prefs.setInternal_(this.params, 100);

    for (var userPref in expectedResults) {
      this.assertEquals(expectedResults[userPref], pref.getInt(userPref));
    }
};

PrefsTest.prototype.testGetFloat = function() {
    var expectedResults = {myCounter: 100, myString: 15.3, myUndefined: 0,
        myObject: 0, myFloat: 3.3};

    var pref = new gadgets.Prefs();

    gadgets.Prefs.setInternal_(this.params, 100);

    for (var userPref in expectedResults) {
      this.assertEquals(expectedResults[userPref], pref.getFloat(userPref));
    }
};

PrefsTest.prototype.testGetBool = function() {
    var expectedResults = {myCounter: true, myString: true, myUndefined: false,
        myObject: false, myFloat: true, myBool: true, boolString: true,
        myArray: false};

    var pref = new gadgets.Prefs();

    gadgets.Prefs.setInternal_(this.params, 100);

    for (var userPref in expectedResults) {
      this.assertEquals(expectedResults[userPref], pref.getBool(userPref));
    }
};


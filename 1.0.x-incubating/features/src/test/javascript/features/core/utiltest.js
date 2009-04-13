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
 * Unittests for gadgets.util.
 * TODO cover more gadgets.util functions.
 */

function UtilTest(name) {
  TestCase.call(this, name);
}

UtilTest.inherits(TestCase);

UtilTest.prototype.setUp = function() {
};

UtilTest.prototype.tearDown = function() {
};

UtilTest.prototype.testMakeEnum = function() {
  var val = ['Foo', 'BAR', 'baz'];
  var obj = gadgets.util.makeEnum(val);
  this.assertEquals('Foo', obj['Foo']);
  this.assertEquals('BAR', obj['BAR']);
  this.assertEquals('baz', obj['baz']);
};

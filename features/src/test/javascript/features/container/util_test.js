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
 * Unittests for the util library.
 */

function UtilTest(name) {
  TestCase.call(this, name);
}

UtilTest.inherits(TestCase);


UtilTest.prototype.setUp = function() {
  this.container = window.__CONTAINER;
  window.__CONTAINER = 'abc';
};

UtilTest.prototype.tearDown = function() {
  window.__CONTAINER = this.container;
};

UtilTest.prototype.testIsEmptyJson = function() {
  this.assertEquals(true, osapi.container.util.isEmptyJson({}));
  this.assertFalse(osapi.container.util.isEmptyJson({ 'a' : 'b' }));
};

UtilTest.prototype.testNewMetadataRequest = function() {
  var req = osapi.container.util.newMetadataRequest(['a.xml', 'b.xml']);
  this.assertEquals('abc', req.container);
  this.assertEquals(2, req.ids.length);
  this.assertEquals('a.xml', req.ids[0]);
  this.assertEquals('b.xml', req.ids[1]);
};

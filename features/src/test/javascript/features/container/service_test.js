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
 * Unittests for the service library.
 */

function ServiceTest(name) {
  TestCase.call(this, name);
}

ServiceTest.inherits(TestCase);

ServiceTest.prototype.setUp = function() {
  this.apiUri = window.__API_URI;
  window.__API_URI = shindig.uri('http://shindig.com');
};

ServiceTest.prototype.tearDown = function() {
  window.__API_URI = this.apiUri;
};

ServiceTest.prototype.testNew = function() {
  var service = new shindig.container.Service();
  this.assertTrue(service != null);
};

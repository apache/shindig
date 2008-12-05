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
 * Unit test for testing the behavior of the OpenSocial identifier resolver.
 */
function testResolveOpenSocialIdentifier() {
  /**
   * Sample class with a property, property getter, custom getField and a get()
   * method.
   */
  var TestClass = function() {
    this.foo = 'fooData';
    this.bar_ = 'barData';
    this.thumbnailUrl_ = 'thumbnailUrlData';
    this.responseItem_ = {};
    this.responseItem_.getData = function() {
      return 'responseItemData';
    };
  };
  TestClass.prototype.getBar = function() {
    return this.bar_;
  };
  TestClass.prototype.getField = function(field) {
    if (field == 'THUMBNAIL_URL') {
      return this.thumbnailUrl_;
    }
    return null;
  };
  TestClass.prototype.get = function(field) {
    if (field == 'responseItem') {
      return this.responseItem_;
    }
    return null;
  };
  
  var obj = new TestClass(); 
  
  assertEquals('fooData', os.resolveOpenSocialIdentifier(obj, 'foo'));
  assertEquals('barData', os.resolveOpenSocialIdentifier(obj, 'bar'));
  assertEquals('thumbnailUrlData',
      os.resolveOpenSocialIdentifier(obj, 'THUMBNAIL_URL'));
  assertEquals('responseItemData',
      os.resolveOpenSocialIdentifier(obj, 'responseItem'));
}

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

var gadgets = gadgets || {};

function JsonMediaItemTest(name) {
  TestCase.call(this, name);
};
JsonMediaItemTest.inherits(TestCase);

JsonMediaItemTest.prototype.setUp = function() {
  // Prepare for mocks
  this.oldGetField = opensocial.Container.getField;
  opensocial.Container.getField = function(fields, key, opt_params) {
    return fields[key];
  };
};

JsonMediaItemTest.prototype.tearDown = function() {
  // Remove mocks
  opensocial.Container.getField = this.oldGetField;
};

JsonMediaItemTest.prototype.testJsonMediaItemConstructor = function() {
  var mediaItem = new JsonMediaItem({'mimeType' : 'black', 'url' : 'white',
      'type' : 'orange'});

  var fields = opensocial.MediaItem.Field;
  this.assertEquals('black', mediaItem.getField(fields.MIME_TYPE));
  this.assertEquals('white', mediaItem.getField(fields.URL));
  this.assertEquals('orange', mediaItem.getField(fields.TYPE));
};
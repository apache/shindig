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

var JsonAlbum = function(opt_params) {
  opt_params = opt_params || {};

  JsonAlbum.constructObject(opt_params, "location", opensocial.Address);

  opensocial.Album.call(this, opt_params);
};
JsonAlbum.inherits(opensocial.Album);

JsonAlbum.prototype.toJsonObject = function() {
  return JsonAlbum.copyFields(this.fields_);
};

// Converts the fieldName into an instance of the specified object
JsonAlbum.constructObject = function(map, fieldName, className) {
  var fieldValue = map[fieldName];
  if (fieldValue) {
    map[fieldName] = new className(fieldValue);
  }
};

//TODO: Pull into common class as well
JsonAlbum.copyFields = function(oldObject) {
  var newObject = {};
  for (var field in oldObject) {
    newObject[field] = oldObject[field];
  }
  return newObject;
};

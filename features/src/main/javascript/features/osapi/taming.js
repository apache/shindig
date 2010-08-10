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
 * @class
 * Tame and expose core osapi.* API to cajoled gadgets
 */
var tamings___ = tamings___ || [];
tamings___.push(function(imports) {

  ___.tamesTo(osapi.newBatch, ___.markFuncFreeze(function () {
    var result = osapi.newBatch();
    ___.markInnocent(result['add'], 'add');
    ___.markInnocent(result['execute'], 'execute');
    return ___.tame(result);
  }));

  // OSAPI functions are marked as simple funcs as they are registered
  imports.outers.osapi = ___.tame(osapi);
  ___.grantRead(imports.outers, 'osapi');

  // Forced to tame in an onload handler because peoplehelpers does
  // not define some functions till runOnLoadHandlers runs
  var savedImports = imports;
  gadgets.util.registerOnLoadHandler(function() {
    if (osapi && osapi.people && osapi.people.get) {
      caja___.whitelistFuncs([
        [osapi.people, 'getViewer'],
        [osapi.people, 'getViewerFriends'],
        [osapi.people, 'getOwner'],
        [osapi.people, 'getOwnerFriends']
      ]);
      // Careful not to clobber osapi.people which already has tamed functions on it
      savedImports.outers.osapi.people.getViewer = ___.tame(osapi.people.getViewer);
      savedImports.outers.osapi.people.getViewerFriends = ___.tame(osapi.people.getViewerFriends);
      savedImports.outers.osapi.people.getOwner = ___.tame(osapi.people.getOwner);
      savedImports.outers.osapi.people.getOwnerFriends = ___.tame(osapi.people.getOwnerFriends);
    }
  });

});

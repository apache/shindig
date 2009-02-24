/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview Caja is a whitelisting javascript sanitizing
 * rewriter.  This file sets up the container and allows a gadget to
 * access console logging functions.
 */

var valijaMaker = undefined;

(function(){
  var imports = ___.copy(___.sharedImports);
  imports.loader = {
    provide: ___.func(function(v) { valijaMaker = v; })
  };
  ___.grantRead(imports, 'loader');
  ___.grantCall(imports.loader, 'provide');
  ___.getNewModuleHandler().setImports(imports);
  ___.getNewModuleHandler().handleUncaughtException = function(e) {
    throw e;
  };
 })();

(function () {
  ___.sharedImports.console = {};
  for (var k in { log: 0, warn: 0, info: 0, error: 0, trace: 0,
                  group: 0, groupEnd: 0, time: 0, timeEnd: 0, dir: 0,
                  assert: 0, dirxml: 0, profile: 0, profileEnd: 0 }) {
    ___.sharedImports.console[k] = (function (k, f) {
      return ___.func(function () { f.apply(console, arguments); });
    })(k, console[k]);
  }
})();

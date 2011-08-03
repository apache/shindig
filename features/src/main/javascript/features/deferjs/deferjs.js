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
 * @fileoverview This provides a mechanism to defer JS symbols.
 *
 * Works in concert with exportJs() to bind dummy symbol names,
 * provided as namespace + array of method names, which enqueue
 * arguments passed to them. When a payload bundle of JS is loaded
 * that defines the full implementation, exportJs() calls the
 * deferred implementation dequeueing the enqueued arguments with
 * the actual method.
 */
function deferJs(namespace, components) {
  var JSL = '___jsl';
  var DEFER_KEY = 'df';
  var base = window;
  var nsParts = namespace.split('.');
  var sliceFn = [].slice;

  // Set up defer function queue.
  var deferMap = ((window[JSL] = window[JSL] || {})[DEFER_KEY] = window[JSL][DEFER_KEY] || {});

  var part;
  while (part = nsParts.shift()) {
    base[part] = base[part] || {};
    base = base[part];
  }

  var methods = sliceFn.call(components, 0);
  var method;
  while (method = methods.shift()) {
    // Don't overwrite an existing method if present,
    // whether deferred or full/exported.
    if (!base[method]) {
      var fulltok = namespace + '.' + method;
      base[method] = (function() {
        var queue = [];
        var ret = function() {
          queue.push(sliceFn.call(arguments, 0));
        };
        deferMap[fulltok] = function(ctx, method) {
          for (var i = 0, len = queue.length; i < len; ++i) {
            method.apply(ctx, queue[i]);
          }
        };
        return ret;
      })();
    }
  }
}

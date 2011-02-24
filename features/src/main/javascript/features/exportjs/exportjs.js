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
 * @fileoverview This provides a mechanism to export symbols, across all
 * cases below. This implementation works best when used with ClosureJsCompiler.
 *
 * "global" object, ie: feature=globals.
 * feature directive: <exports type="js">gadgets</exports>
 * code: var gadgets = {};
 *
 * "singleton" object, ie: feature=rpc.
 * feature directive: <exports type="js">gadgets.foo.bar</exports>
 * gadgets.foo = function() {
 *   return { bar : function() { ... } };
 * }();
 *
 * "closured" object, ie: feature=shindig.uri.
 * This wraps to a function that exports any resulting properties it returns
 * in an Object. feature directive: <exports type="js">gadgets.foo.bar</exports>
 * gadgets.foo = (function() {
 *   return { bar : function() { ... } };
 * })();
 *
 * "prototype" object, ie: feature=container.
 * feature directive: <exports type="js">gadgets.foo.prototype.bar</exports>
 * gadgets.foo = function() {};
 * gadgets.foo.prototype.bar = function() { ... };
 *
 * "static" object.
 * feature directive: <exports type="js">gadgets.foo.bar</exports>
 * gadgets.foo = {};
 * gadgets.foo.bar = function() { ... };
 */
function exportJs(namespace, components, opt_props) {
  var base = window;
  var prevBase = null;
  var nsParts = namespace.split('.');

  for (var i = 0, part; part = nsParts.shift(); i++) {
    base[part] = components[i] || {};
    prevBase = base;
    base = base[part];
  }

  var exportProps = function(root) {
    var props = opt_props || {};
    for (var prop in props) {
      if (props.hasOwnProperty(prop) && root.hasOwnProperty(prop)) {
        root[props[prop]] = root[prop];
      }
    }
  };

  if (typeof base === 'object') {
    exportProps(base);

  } else if (typeof base === 'function') {
    var exportedFn = function() {
      var result = base.apply(null, arguments);
      if (typeof result === 'object') {
        exportProps(result);
      }
      return result;
    };
    prevBase[part] = exportedFn;
  }
}

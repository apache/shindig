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
 * Get a cloo!
 *
 * Simple utils for CLosure-style Object Orientation.
 * JavaScript inheritance via prototype is a little awkward and too permissive.
 * Method overrides are actually method overwrites, and importantly there's
 * no such thing as private state: all properties attached to "this" are public
 * and accessible.
 *
 * With this library, object-oriented JS APIs are implemented as function
 * closures with private internal variables. Exported APIs are returned from
 * these as Objects themselves.
 *
 * To define a class, simply define a Function that returns an Object whose
 * keys define the class's external API, with that Object wrapped as:
 * cloo.obj(opt_classname, super1, super2, ..., exported);
 *
 * To define an interface/abstract method in your exports, use cloo.interfc():
 * cloo.obj({ foo: cloo.interfc() });
 *
 * Exported symbols that begin with an underbar are *not* inherited.
 *
 * Consequences of this mechanism are:
 * + "True" private variables and functions are supported, by declaring them
 *   in the class's Function body without exporting them.
 * + Quasi-"protected" methods are supported by defining a base class exporting
 *   methods whose name begins with an underbar.
 * + A form of multiple inheritance is implicitly supported, by providing
 *   multiple superclass objects to the cloo.obj() function. Override precedence
 *   is that last-superclass-wins. In practice, this should only be used with
 *   superclasses that don't have state, ie. comprise only of interface methods.
 *   This is not programmatically enforced by the library to keep code lean.
 * + Base-class state must be accessed via getters and setters, which may be
 *   "protected" as described above.
 *
 * Meaningless example exhibiting all features:
 * var MyBase = (function(config, params) {
 *   var self = cloo.me();
 *   var handler = function() { };
 *   var cfg = config;
 *   var color = params["color"] || "red";
 *
 *   function setColor(newColor) {
 *     color = newColor;
 *   }
 *
 *   function callHandler() {
 *     handler(self());
 *   }
 *
 *   return cloo.obj({
 *     setColor: setColor,
 *     _getColor: function() { return color; },
 *     getHeight: cloo.interfc()  // Makes MyBase implicitly abstract
 *   });
 * });
 *
 * var MyClass = (function(config, params) {
 *   var super = MyBase(config, params);
 *   var height = params["height"] || 34;
 *
 *   function getHeight() {
 *     // Blue objects are always 123 in height.
 *     return super._getColor() === "blue" ? 123 : height;
 *   }
 *
 *   return cloo.obj(super, {
 *     getHeight: getHeight
 *   });
 * }
 *
 * var myClassInstance = MyClass({}, { height: 12, color: "red"});
 */

var cloo = (function() {
  var UNKNOWN_NAME = '(n/a)';
  var selfs = [];

  function InterfaceMethod(className, methodName) {
    return function() {
      throw 'Class ' + className + ' missing ' + methodName + '()';
    }
  }

  var INTERFACE_PLACEHOLDER = InterfaceMethod(UNKNOWN_NAME, UNKNOWN_NAME);

  function interfaceCreator() {
    return INTERFACE_PLACEHOLDER;
  }

  function hasOwnFunctionProperty(obj, key) {
    return obj.hasOwnProperty(key) && typeof obj[key] === 'function';
  }

  function objectCreator() {
    var args = arguments;
    var className = UNKNOWN_NAME;
    var ix = 0;
    if (typeof args[0] === 'string') {
      className = args[0];
      ix = 1;
    }

    // Create return Object.
    var out = {};

    for (; ix < (args.length - 1); ++ix) {
      var parent = args[ix];
      // Copy keys over from parent that aren't intended
      // to be "protected" ie starting with an underbar.
      for (var key in parent) {
        if (hasOwnFunctionProperty(parent, key) && !/^_/.test(key)) {
          out[key] = parent[key];
        }
      }
    }

    // Then override with new exports, replacing
    // interface placeholders with properly-named versions.
    var exports = args[ix];
    for (var key in exports) {
      if (hasOwnFunctionProperty(exports, key)) {
        if (exports[key] === INTERFACE_PLACEHOLDER) {
          exports[key] = InterfaceMethod(className, key);
        }
        out[key] = exports[key];
      }
    }

    var lastIx = selfs.length - 1;
    if (lastIx >= 0 && !selfs[lastIx]) {
      selfs[lastIx] = out;
    }

    return out;
  }

  function selfStorage() {
    var ix = selfs.length;
    if (ix && !selfs[ix - 1]) {
      throw 'me() must be followed by obj()';
    }
    selfs.push(null);
    return function() {
      var obj = selfs[ix];
      if (!obj) {
        throw 'me() access before obj creation';
      }
      return obj;
    };
  }

  return {
    interfc: interfaceCreator,
    obj: objectCreator,
    me: selfStorage
  };
})();

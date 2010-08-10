/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * @fileoverview Provides utility methods.
 */

/**
 * @namespace This namespace defines utility methods for use within
 * the Wave Gadgets API.
 */
wave.util = wave.util || {};

wave.util.SPACES_ = '                                                 ';

wave.util.toSpaces_ = function(tabs) {
  return wave.util.SPACES_.substring(0, tabs * 2);
}

wave.util.isArray_ = function(obj) {
  try {
    return obj && typeof(obj.length) == 'number';
  } catch (e) {
    return false;
  }
};

/**
 * Outputs JSON objects in text format. Optionally pretty print.
 *
 * @param {Object} obj The object to print.
 * @param {boolean=} opt_pretty If true, pretty print (optional).
 * @param {number=} opt_tabs Number of tabs to start indent.
 * @return {string} The formatted object in text.
 */
wave.util.printJson = function(obj, opt_pretty, opt_tabs) {
  if (!obj || typeof(obj.valueOf()) != 'object') {
    if (typeof(obj) == 'string') {
      return '\'' + obj + '\'';
    }
    else if (obj instanceof Function) {
      return '[function]';
    }
    return '' + obj;
  }
  var text = [];
  var isArray = wave.util.isArray_(obj);
  var brace = isArray ? '[]' : '{}';
  var newline = opt_pretty ? '\n' : '';
  var spacer = opt_pretty ? ' ' : '';
  var i = 0;
  var tabs = opt_tabs || 1;
  if (!opt_pretty) {
    tabs = 0;
  }
  text.push(brace.charAt(0));
  for (var key in obj) {
    var value = obj[key];
    if (i++ > 0) {
      text.push(', ');
    }
    if (isArray) {
      text.push(wave.util.printJson(value, opt_pretty, tabs + 1));
    } else {
      text.push(newline);
      text.push(wave.util.toSpaces_(tabs));
      text.push(key + ': ');
      text.push(spacer);
      text.push(wave.util.printJson(value, opt_pretty, tabs + 1));
    }
  }
  if (!isArray) {
    text.push(newline);
    text.push(wave.util.toSpaces_(tabs - 1));
  }
  text.push(brace.charAt(1));
  return text.join('');
};

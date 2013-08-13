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

// Defines default validators in a separate file from the rest of the config
// system, to enable its separability from these.

(function() {
  /**
    * Ensures that data is one of a fixed set of items.
    * Also supports argument sytax: EnumValidator("Dog", "Cat", "Fish");
    *
    * @param {Array.<string>} list The list of valid values.
    */
  gadgets.config.EnumValidator = function(list) {
    var listItems = [];
    if (arguments.length > 1) {
      for (var i = 0, arg; (arg = arguments[i]); ++i) {
        listItems.push(arg);
      }
    } else {
      listItems = list;
    }
    return function(data) {
      for (var i = 0, test; (test = listItems[i]); ++i) {
        if (data === listItems[i]) {
          return true;
        }
      }
      return false;
    };
  };

  /**
   * Tests the value against a regular expression.
   * @member gadgets.config
   */
  gadgets.config.RegExValidator = function(re) {
    return function(data) {
      return re.test(data);
    };
  };

  /**
   * Validates that a value was provided.
   * @param {*} data
   */
  gadgets.config.ExistsValidator = function(data) {
    return typeof data !== 'undefined';
  };

  /**
   * Validates that a value is a non-empty string.
   * @param {*} data
   */
  gadgets.config.NonEmptyStringValidator = function(data) {
    return typeof data === 'string' && data.length > 0;
  };

  /**
   * Validates that the value is a boolean.
   * @param {*} data
   */
  gadgets.config.BooleanValidator = function(data) {
    return typeof data === 'boolean';
  };

  /**
   * Similar to the ECMAScript 4 virtual typing system, ensures that
   * whatever object was passed in is "like" the existing object.
   * Doesn't actually do type validation though, but instead relies
   * on other validators.
   *
   * This can be used recursively as well to validate sub-objects.
   *
   * @example
   *
   *  var validator = new gadgets.config.LikeValidator(
   *    "booleanField" : gadgets.config.BooleanValidator,
   *    "regexField" : new gadgets.config.RegExValidator(/foo.+/);
   *  );
   *
   *
   * @param {Object} test The object to test against.
   */
  gadgets.config.LikeValidator = function(test) {
    return function(data) {
      for (var member in test) {
        if (test.hasOwnProperty(member)) {
          var t = test[member];
          if (!t(data[member])) {
            return false;
          }
        }
      }
      return true;
    };
  };
})();

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
 * @fileoverview Provides unified configuration for all features.
 *
 *
 * <p>This is a custom shindig library that has not yet been submitted for
 * standardization. It is designed to make developing of features for the
 * opensocial / gadgets platforms easier and is intended as a supplemental
 * tool to Shindig's standardized feature loading mechanism.
 *
 * <p>Usage:
 * First, you must register a component that needs configuration:
 * <pre>
 *   var config = {
 *     name : gadgets.config.NonEmptyStringValidator,
 *     url : new gadgets.config.RegExValidator(/.+%mySpecialValue%.+/)
 *   };
 *   gadgets.config.register("my-feature", config, myCallback);
 * </pre>
 *
 * <p>This will register a component named "my-feature" that expects input config
 * containing a "name" field with a value that is a non-empty string, and a
 * "url" field with a value that matches the given regular expression.
 *
 * <p>When gadgets.config.init is invoked by the container, it will automatically
 * validate your registered configuration and will throw an exception if
 * the provided configuration does not match what was required.
 *
 * <p>Your callback will be invoked by passing all configuration data passed to
 * gadgets.config.init, which allows you to optionally inspect configuration
 * from other features, if present.
 *
 * <p>Note that the container may optionally bypass configuration validation for
 * performance reasons. This does not mean that you should duplicate validation
 * code, it simply means that validation will likely only be performed in debug
 * builds, and you should assume that production builds always have valid
 * configuration.
 */

gadgets.config = function() {
  var components = {};
  var configuration;

  return {
    'register':
        /**
     * Registers a configurable component and its configuration parameters.
     * Multiple callbacks may be registered for a single component if needed.
     *
     * @param {string} component The name of the component to register. Should
     *     be the same as the fully qualified name of the <Require> feature or
     *     the name of a fully qualified javascript object reference
     *     (e.g. "gadgets.io").
     * @param {Object=} opt_validators Mapping of option name to validation
     *     functions that take the form function(data) {return isValid(data);}.
     * @param {function(Object)=} opt_callback A function to be invoked when a
     *     configuration is registered. If passed, this function will be invoked
     *     immediately after a call to init has been made. Do not assume that
     *     dependent libraries have been configured until after init is
     *     complete. If you rely on this, it is better to defer calling
     *     dependent libraries until you can be sure that configuration is
     *     complete. Takes the form function(config), where config will be
     *     all registered config data for all components. This allows your
     *     component to read configuration from other components.
     * @member gadgets.config
     * @name register
     * @function
     */
        function(component, opt_validators, opt_callback) {
          var registered = components[component];
          if (!registered) {
            registered = [];
            components[component] = registered;
          }

          registered.push({
            validators: opt_validators || {},
            callback: opt_callback
          });
        },

    'get':
        /**
     * Retrieves configuration data on demand.
     *
     * @param {string=} opt_component The component to fetch. If not provided
     *     all configuration will be returned.
     * @return {Object} The requested configuration, or an empty object if no
     *     configuration has been registered for that component.
     * @member gadgets.config
     * @name get
     * @function
     */
        function(opt_component) {
          if (opt_component) {
            return configuration[opt_component] || {};
          }
          return configuration;
        },

    /**
     * Initializes the configuration.
     *
     * @param {Object} config The full set of configuration data.
     * @param {boolean=} opt_noValidation True if you want to skip validation.
     * @throws {Error} If there is a configuration error.
     * @member gadgets.config
     * @name init
     * @function
     */
    'init': function(config, opt_noValidation) {
      configuration = config;
      for (var name in components) {
        if (components.hasOwnProperty(name)) {
          var componentList = components[name],
              conf = config[name];

          for (var i = 0, j = componentList.length; i < j; ++i) {
            var component = componentList[i];
            if (conf && !opt_noValidation) {
              var validators = component.validators;
              for (var v in validators) {
                if (validators.hasOwnProperty(v)) {
                  if (!validators[v](conf[v])) {
                    throw new Error('Invalid config value "' + conf[v] +
                        '" for parameter "' + v + '" in component "' +
                        name + '"');
                  }
                }
              }
            }

            if (component.callback) {
              component.callback(config);
            }
          }
        }
      }
    },

    // Standard validators go here.

    /**
     * Ensures that data is one of a fixed set of items.
     * Also supports argument sytax: EnumValidator("Dog", "Cat", "Fish");
     *
     * @param {Array.<string>} list The list of valid values.
     *
     * @member gadgets.config
     * @name  EnumValidator
     * @function
     */
    'EnumValidator': function(list) {
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
    },

    /**
     * Tests the value against a regular expression.
     * @member gadgets.config
     * @name RegexValidator
     * @function
     */
    'RegExValidator': function(re) {
      return function(data) {
        return re.test(data);
      };
    },

    /**
     * Validates that a value was provided.
     * @param {*} data
     * @member gadgets.config
     * @name ExistsValidator
     * @function
     */
    'ExistsValidator': function(data) {
      return typeof data !== 'undefined';
    },

    /**
     * Validates that a value is a non-empty string.
     * @param {*} data
     * @member gadgets.config
     * @name NonEmptyStringValidator
     * @function
     */
    'NonEmptyStringValidator': function(data) {
      return typeof data === 'string' && data.length > 0;
    },

    /**
     * Validates that the value is a boolean.
     * @param {*} data
     * @member gadgets.config
     * @name BooleanValidator
     * @function
     */
    'BooleanValidator': function(data) {
      return typeof data === 'boolean';
    },

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
     * @member gadgets.config
     * @name BooleanValidator
     * @function
     */
    'LikeValidator' : function(test) {
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
    }
  };
}();

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

if (!window['gadgets']['config']) {
gadgets.config = function() {
  var ___jsl;
  var components = {};
  var configuration = {};

  function foldConfig(origConfig, updConfig) {
    for (var key in updConfig) {
      if (!updConfig.hasOwnProperty(key)) {
        continue;
      }
      if (typeof origConfig[key] === 'object' &&
          typeof updConfig[key] === 'object') {
        // Both have the same key with an object value. Recurse.
        foldConfig(origConfig[key], updConfig[key]);
      } else {
        // If updConfig has a new key, or a value of different type
        // than the original config for the same key, or isn't an object
        // type, then simply replace the value for the key.
        origConfig[key] = updConfig[key];
      }
    }
  }

  function getLoadingScript() {
    // Attempt to retrieve config augmentation from latest script node.
    var scripts = document.scripts || document.getElementsByTagName('script');
    if (!scripts || scripts.length == 0) return null;
    var scriptTag;
    if (___jsl['u']) {
      for (var i = 0; !scriptTag && i < scripts.length; ++i) {
        var candidate = scripts[i];
        if (candidate.src &&
            candidate.src.indexOf(___jsl['u']) == 0) {
          // Do indexOf test to allow for fragment info
          scriptTag = candidate;
        }
      }
    }
    if (!scriptTag) {
      scriptTag = scripts[scripts.length - 1];
    }
    if (!scriptTag.src) return null;
    return scriptTag;
  }

  function getInnerText(scriptNode) {
    var scriptText = '';
    if (scriptNode.nodeType == 3 || scriptNode.nodeType == 4) {
      scriptText = scriptNode.nodeValue;
    } else if (scriptNode.innerText) {
      scriptText = scriptNode.innerText;
    } else if (scriptNode.innerHTML) {
      scriptText = scriptNode.innerHTML;
    } else if (scriptNode.firstChild) {
      var content = [];
      for (var child = scriptNode.firstChild; child; child = child.nextSibling) {
        content.push(getInnerText(child));
      }
      scriptText = content.join('');
    }
    return scriptText;
  }

  function parseConfig(configText) {
    var config;
    try {
      config = (new Function('return (' + configText + '\n)'))();
    } catch (e) { }
    if (typeof config === 'object') {
      return config;
    }
    try {
      config = (new Function('return ({' + configText + '\n})'))();
    } catch (e) { }
    return typeof config === 'object' ? config : {};
  }

  function augmentConfig(baseConfig) {
    var loadScript = getLoadingScript();
    if (!loadScript) {
      return;
    }
    var scriptText = getInnerText(loadScript);
    var configAugment = parseConfig(scriptText);
    if (___jsl['f'] && ___jsl['f'].length == 1) {
      // Single-feature load on current request.
      // Augmentation adds to just this feature's config if
      // "short-form" syntax is used ie. skipping top-level feature key.
      var feature = ___jsl['f'][0];
      if (!configAugment[feature]) {
        var newConfig = {};
        newConfig[___jsl['f'][0]] = configAugment;
        configAugment = newConfig;
      }
    }
    foldConfig(baseConfig, configAugment);

    var globalConfig = window['___cfg'];
    if (globalConfig) {
      foldConfig(baseConfig, globalConfig);
    }
  }

  /**
   * Iterates through all registered components.
   * @param {function(string,Object)} processor The processor method.
   */
  function forAllComponents(processor) {
    for (var name in components) {
      if (components.hasOwnProperty(name)) {
        var componentList = components[name];
        for (var i = 0, j = componentList.length; i < j; ++i) {
          processor(name, componentList[i]);
        }
      }
    }
  }

  return {
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
     * @param {boolean=} opt_callOnUpdate Whether the callback shall be call
     *     on gadgets.config.update() as well.
     * @member gadgets.config
     * @name register
     * @function
     */
    register: function(component, opt_validators, opt_callback,
        opt_callOnUpdate) {
      var registered = components[component];
      if (!registered) {
        registered = [];
        components[component] = registered;
      }

      registered.push({
        validators: opt_validators || {},
        callback: opt_callback,
        callOnUpdate: opt_callOnUpdate
      });
    },

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
    get: function(opt_component) {
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
    init: function(config, opt_noValidation) {
      ___jsl = window['___jsl'] || {};
      foldConfig(configuration, config);
      augmentConfig(configuration);
      var inlineOverride = window['___config'] || {};
      foldConfig(configuration, inlineOverride);
      forAllComponents(function(name, component) {
        var conf = configuration[name];
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
          component.callback(configuration);
        }
      });
    },

    /**
     * Method largely for dev and debugging purposes that
     * replaces or manually updates feature config.
     * @param {Object} updateConfig Config object, with keys for features.
     * @param {boolean} opt_replace true to replace all configuration.
     */
    update: function(updateConfig, opt_replace) {
      // Iterate before changing updateConfig and configuration.
      var callbacks = [];
      forAllComponents(function(name, component) {
        if (updateConfig.hasOwnProperty(name) ||
            (opt_replace && configuration && configuration[name])) {
          if (component.callback && component.callOnUpdate) {
            callbacks.push(component.callback);
          }
        }
      });
      configuration = opt_replace ? {} : configuration || {};
      foldConfig(configuration, updateConfig);
      for (var i = 0, j = callbacks.length; i < j; ++i) {
        callbacks[i](configuration);
      }
    }
  };
}();
} // ! end double inclusion guard

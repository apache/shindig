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
 * @fileoverview Provide mixin capabilities to the CommonContainer
 */

osapi.container = osapi.container || {};

(function() {
  /**
   * Adds the ability for features to extend the container with
   * their own functionality that may be specific to that feature.
   * @type {Object<string,function>}
   * @private
   */
  var mixins = {};

  /**
   * Order of addMixin calls.
   * @type {Array<string>}
   * @private
   */
  var order = [];

  /**
   * Adds a new namespace to be mixed into a constructed container object.
   * The namespace will contain the result of calling the function passed in.
   *
   * @param {string} namespace The namespace to add.
   * @param {function} func Constructor for the namespace. (will be newed)
   */
  osapi.container.addMixin = function(namespace, func) {
    if (mixins[namespace]) {
      var orig = mixins[namespace];
      mixins[namespace] = function(container) {
        var base = orig.call(this, container);
        return func.call(this, container, base); // pass overriding mixins the original.
      };
    } else {
      order.push(namespace);
      mixins[namespace] = func;
    }
  };

  /**
   * Mixes all added mixins into the provided container.
   *
   * @param {osapi.container.Container} container The container to mix in to.
   */
  osapi.container.mixin = function(container) {
    for (var i = 0; i < order.length; i++) {
      var namespace = order[i];
      container[namespace] = new mixins[namespace](container);
    }
  };
})();
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
 * @fileoverview General purpose utilities that gadgets can use.
 */

/**
 * @static
 * @class Provides general-purpose utility functions.
 * @name gadgets.util
 */
gadgets.util = gadgets.util || {};

(function() {  
  
  var features = {};
  var services = {};
  var onLoadHandlers = [];

  function attachAttributes(elem, opt_attribs) {
    var attribs = opt_attribs || {};
    for (var attrib in attribs) {
      if (attribs.hasOwnProperty(attrib)) {
        elem[attrib] = attribs[attrib];
      }
    }
  }

  function stringifyElement(tagName, opt_attribs) {
    var arr = ['<', tagName];
    var attribs = opt_attribs || {};
    for (var attrib in attribs) {
      if (attribs.hasOwnProperty(attrib)) {
        arr.push(' ');
        arr.push(attrib);
        arr.push('="');
        arr.push(gadgets.util.escapeString(attribs[attrib]));
        arr.push('"');
      }
    }
    arr.push('></');
    arr.push(tagName);
    arr.push('>');
    return arr.join('');
  }

  /**
   * Initializes feature parameters.
   */
  function init(config) {
    features = config['core.util'] || {};
  }
  if (gadgets.config) {
    gadgets.config.register('core.util', null, init);
  }

  /**
   * Creates a closure that is suitable for passing as a callback.
   * Any number of arguments
   * may be passed to the callback;
   * they will be received in the order they are passed in.
   *
   * @param {Object} scope The execution scope; may be null if there is no
   *     need to associate a specific instance of an object with this
   *     callback.
   * @param {function(Object,Object)} callback The callback to invoke when this is run;
   *     any arguments passed in will be passed after your initial arguments.
   * @param {Object} var_args Initial arguments to be passed to the callback.
   *
   * @member gadgets.util
   * @private Implementation detail.
   */
  gadgets.util.makeClosure = function(scope, callback, var_args) {
    // arguments isn't a real array, so we copy it into one.
    var baseArgs = [];
    for (var i = 2, j = arguments.length; i < j; ++i) {
      baseArgs.push(arguments[i]);
    }
    return function() {
      // append new arguments.
      var tmpArgs = baseArgs.slice();
      for (var i = 0, j = arguments.length; i < j; ++i) {
        tmpArgs.push(arguments[i]);
      }
      return callback.apply(scope, tmpArgs);
    };
  };

  /**
   * Utility function for generating an "enum" from an array.
   *
   * @param {Array.<string>} values The values to generate.
   * @return {Object.<string,string>} An object with member fields to handle
   *   the enum.
   *
   * @private Implementation detail.
   */
  gadgets.util.makeEnum = function(values) {
    var i, v, obj = {};
    for (i = 0; (v = values[i]); ++i) {
      obj[v] = v;
    }
    return obj;
  };

  /**
   * Gets the feature parameters.
   *
   * @param {string} feature The feature to get parameters for.
   * @return {Object} The parameters for the given feature, or null.
   *
   * @member gadgets.util
   */
  gadgets.util.getFeatureParameters = function(feature) {
    return typeof features[feature] === 'undefined' ? null : features[feature];
  };

  /**
   * Returns whether the current feature is supported.
   *
   * @param {string} feature The feature to test for.
   * @return {boolean} True if the feature is supported.
   *
   * @member gadgets.util
   */
  gadgets.util.hasFeature = function(feature) {
    return typeof features[feature] !== 'undefined';
  };

  /**
   * Returns the list of services supported by the server
   * serving this gadget.
   *
   * @return {Object} List of Services that enumerate their methods.
   *
   * @member gadgets.util
   */
  gadgets.util.getServices = function() {
    return services;
  };

  /**
   * Registers an onload handler.
   * @param {function()} callback The handler to run.
   *
   * @member gadgets.util
   */
  gadgets.util.registerOnLoadHandler = function(callback) {
    onLoadHandlers.push(callback);
  };

  /**
   * Runs all functions registered via registerOnLoadHandler.
   * @private Only to be used by the container, not gadgets.
   */
  gadgets.util.runOnLoadHandlers = function() {
    for (var i = 0, j = onLoadHandlers.length; i < j; ++i) {
      onLoadHandlers[i]();
    }
  };

  /**
   * Attach an event listener to given DOM element (Not a gadget standard)
   *
   * @param {Object} elem  DOM element on which to attach event.
   * @param {string} eventName  Event type to listen for.
   * @param {function()} callback  Invoked when specified event occurs.
   * @param {boolean} useCapture  If true, initiates capture.
   */
  gadgets.util.attachBrowserEvent = function(elem, eventName, callback, useCapture) {
    if (typeof elem.addEventListener != 'undefined') {
      elem.addEventListener(eventName, callback, useCapture);
    } else if (typeof elem.attachEvent != 'undefined') {
      elem.attachEvent('on' + eventName, callback);
    } else {
      gadgets.warn('cannot attachBrowserEvent: ' + eventName);
    }
  };

  /**
   * Remove event listener. (Shindig internal implementation only)
   *
   * @param {Object} elem  DOM element from which to remove event.
   * @param {string} eventName  Event type to remove.
   * @param {function()} callback  Listener to remove.
   * @param {boolean} useCapture  Specifies whether listener being removed was added with
   *                              capture enabled.
   */
  gadgets.util.removeBrowserEvent = function(elem, eventName, callback, useCapture) {
    if (elem.removeEventListener) {
      elem.removeEventListener(eventName, callback, useCapture);
    } else if (elem.detachEvent) {
      elem.detachEvent('on' + eventName, callback);
    } else {
      gadgets.warn('cannot removeBrowserEvent: ' + eventName);
    }
  };

  /**
   * Creates an HTML or XHTML iframe element with attributes.
   * @param {Object=} opt_attribs Optional set of attributes to attach. The
   * only working attributes are spelled the same way in XHTML attribute
   * naming (most strict, all-lower-case), HTML attribute naming (less strict,
   * case-insensitive), and JavaScript property naming (some properties named
   * incompatibly with XHTML/HTML).
   * @return {Element} The DOM node representing body.
   */
  gadgets.util.createIframeElement = function(opt_attribs) {
    // TODO: move to core.util.dom.
    var frame = gadgets.util.createElement('iframe');
    try {
      // TODO: provide automatic mapping to only set the needed
      // and JS-HTML-XHTML compatible subset through stringifyElement (just
      // 'name' and 'id', AFAIK). The values of the attributes will be
      // stringified should the stringifyElement code path be taken (IE)
      var tagString = stringifyElement('iframe', opt_attribs);
      var ieFrame = gadgets.util.createElement(tagString);
      if (ieFrame &&
          ((!frame) ||
           ((ieFrame.tagName == frame.tagName) &&
            (ieFrame.namespaceURI == frame.namespaceURI)))) {
        frame = ieFrame;
      }
    } catch (nonStandardCallFailed) {
    }
    attachAttributes(frame, opt_attribs);
    return frame;
  };

})();

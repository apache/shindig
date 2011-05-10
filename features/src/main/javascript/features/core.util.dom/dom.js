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

  var XHTML_SPEC = 'http://www.w3.org/1999/xhtml';

  /**
   * Creates an HTML or XHTML element.
   * @param {string} tagName The type of element to construct.
   * @return {Element} The newly constructed element.
   */
  gadgets.util.createElement = function(tagName) {
    var element;
    if ((!document.body) || document.body.namespaceURI) {
      try {
        element = document.createElementNS(XHTML_SPEC, tagName);
      } catch (nonXmlDomException) {
      }
    }
    return element || document.createElement(tagName);
  };

  /**
   * Gets the HTML or XHTML body element.
   * @return {Element} The DOM node representing body.
   */
  gadgets.util.getBodyElement = function() {
    if (document.body) {
      return document.body;
    }
    try {
      var xbodies = document.getElementsByTagNameNS(XHTML_SPEC, 'body');
      if (xbodies && (xbodies.length == 1)) {
        return xbodies[0];
      }
    } catch (nonXmlDomException) {
    }
    return document.documentElement || document;
  };

})();
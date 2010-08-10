/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * @fileoverview Implements namespace support for custom tags.
 *
 * TODO(davidbyttow): Refactor this.
 */

/**
 * Map of namespace collections.
 *
 * Each namespace collection is either a map of tag handlers, or an object
 * that has a getTag(tagName) method that will return a tag handler based on 
 * name.
 * 
 * A tag handler function should be have the following signature:
 * function({Element} node, {Object} data, {JSEvalContext} context)
 * where context is the JSEvalContext used to wrap data. 
 *
 * For simpler implementations,
 * function({Element} node, {Object} data)
 * can be used, omitting the third param.
 * 
 * Handler functions can return a string, a DOM Element or an Object with 
 * {Element} root and, optionally, {Function} onAttach properties.
 *
 * @type {Object}
 * @private  
 */
os.nsmap_ = {};

/**
 * Map of namespace prefixes to full urls.
 * @type {Object}
 * @private  
 */
os.nsurls_ = {};

/***
 * Registers the given namespace with a specified URL. Throws an error if it
 * already exists as a different URL.
 * @param {string} ns Namespace tag.
 * @param {string} url URL for namespace.
 * @return {Object} The object map of registered tags.
 */
os.createNamespace = function(ns, url) {
  var tags = os.nsmap_[ns];
  if (!tags) {
    tags = {};
    os.nsmap_[ns] = tags;
    os.nsurls_[ns] = url;
  } else if (os.nsurls_[ns] != url) {
    throw("Namespace " + ns + " already defined with url " + os.nsurls_[ns]);
  }
  return tags;
};

/**
 * Returns the namespace object for a given prefix.
 */
os.getNamespace = function(prefix) {
  return os.nsmap_[prefix];
};

os.addNamespace = function(ns, url, nsObj) {
  if (os.nsmap_[ns]) {
    throw ("Namespace '" + ns + "' already exists!");
  }
  os.nsmap_[ns] = nsObj;
  os.nsurls_[ns] = url;
};

os.getCustomTag = function(ns, tag) {
  var nsObj = os.nsmap_[ns];
  if (!nsObj) {
    return null;
  }
  if (nsObj.getTag) {
    return nsObj.getTag(tag);
  } else {
    return nsObj[tag];
  }
};

/**
 * Returns the XML namespace declarations that need to be injected into a
 * particular template to make it valid XML. Uses the defined namespaces to
 * see which are available, and checks that they are used in the supplied code.
 * An empty string is returned if no injection is needed.
 *
 * @param {string} templateSrc Template source code.
 * @return {string} A string of xmlns delcarations required for this tempalte.
 */
os.getRequiredNamespaces = function(templateSrc) {
  var codeToInject = "";
  for (var ns in os.nsurls_) {
    if (templateSrc.indexOf("<" + ns + ":") >= 0 &&
        templateSrc.indexOf("xmlns:" + ns + ":") < 0) {
      codeToInject += " xmlns:" + ns + "=\"" + os.nsurls_[ns] + "\"";
    }
  }
  return codeToInject;
};

/**
 * Define 'os:renderAll' and 'os:Html' tags and the @onAttach attribute
 */
os.defineBuiltinTags = function() {
  var osn = os.getNamespace("os") || 
      os.createNamespace("os", "http://opensocial.com/#template");
      
  /**
   * <os:Render> custom tag renders the specified child nodes of the current 
   * context.
   */
  osn.Render = function(node, data, context) {
    var parent = context.getVariable(os.VAR_parentnode);
    var exp = node.getAttribute("content") || "*";
    var result = os.getValueFromNode_(parent, exp);
    if (!result) {
       return ""; 
    } else if (typeof(result) == "string") {
      var textNode = document.createTextNode(result);
      result = [];
      result.push(textNode);
    } else if (!isArray(result)) {
      var resultArray = [];
      for (var i = 0; i < result.childNodes.length; i++) {
        resultArray.push(result.childNodes[i]);
      }
      result = resultArray;
    } else if (exp != "*" && result.length == 1 &&
        result[0].nodeType == DOM_ELEMENT_NODE) {
      // When we call <os:renderAll content="tag"/>, render the inner content
      // of the tag returned, not the tag itself.
      var resultArray = [];
      for (var i = 0; i < result[0].childNodes.length; i++) {
        resultArray.push(result[0].childNodes[i]);
      }
      result = resultArray;      
    }

    // Trim away leading and trailing spaces on IE, which interprets them 
    // literally.
    if (os.isIe) {
      for (var i = 0; i < result.length; i++) {
        if (result[i].nodeType == DOM_TEXT_NODE) {
          var trimmed = os.trimWhitespaceForIE_(
              result[i].nodeValue, (i == 0), (i == result.length - 1));
          if (trimmed != result[i].nodeValue) {
            result[i].parentNode.removeChild(result[i]);
            result[i] = document.createTextNode(trimmed);
          }
        }
      }
    }
    
    return result;
  }
  osn.render = osn.RenderAll = osn.renderAll = osn.Render;
  
  /**
   * <os:Html> custom tag renders HTML content (as opposed to HTML code), so
   * <os:Html code="<b>Hello</b>"/> would result in the bold string "Hello", 
   * rather than the text of the markup. 
   */
  osn.Html = function(node) {
    var html = node.code ? "" + node.code : node.getAttribute("code") || "";
    // TODO(levik): Sanitize the HTML here to avoid script injection issues.
    // Perhaps use the gadgets sanitizer if available.
    return html;
  }
  
  function createClosure(object, method) {
    return function() {
      method.apply(object);
    }
  }
  
  function processOnAttach(node, code, data, context) {
    var callbacks = context.getVariable(os.VAR_callbacks);
    var func = new Function(code);
    callbacks.push(createClosure(node, func));
  }
  os.registerAttribute("onAttach", processOnAttach);
};

os.defineBuiltinTags();

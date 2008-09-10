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
 * @fileoverview Prototype for OpenSocial Templates implementation.
 * 
 * Simple usage of templates:
 *   var template = os.compileTemplate("<span>Hello, ${name}</span>");
 *   template.renderInto(document.getElementById("output"), { name: "Bob" });
 * 
 * More complex usecase:
 *   var data = { ... };
 *   var template = os.compileTemplate(document.getElementById("template"));
 *   var context = os.createContext(data);
 *   var output = template.render(data, context);
 *   // ... attach the output node ...
 *   os.fireCallbacks(context);
 * 
 * TODO(levik): Optimization:
 *   - Define all regexps as globals once, not once per function call.
 *   - Use queue-based DOM walker instead of recursion.
 *   - doTag() safeguards node from abuse (no parent access, etc).
 */

var opensocial = opensocial || {};
opensocial.template = opensocial.template || {};
var os = opensocial.template;

/**
 * Sends a log to the console.
 */
os.log = function(msg) {
  log("LOG: " + msg);
};

/**
 * Logs a warning to the console.
 */
os.warn = function(msg) {
  os.log("WARNING: " + msg);
};

/**
 * Constants
 * TODO(davidbyttow): Pull these out of os and make them global (optimization)
 */
os.ATT_customtag = "customtag";

os.VAR_my = "$my";
os.VAR_cur = "$cur";
os.VAR_node = "$node"; 
os.VAR_parentnode = "$parentnode";
os.VAR_uniqueId = "$uniqueId";
os.VAR_identifierresolver = "$_ir";
os.VAR_callbacks = "$callbacks_";

/**
 * Regular expressions
 * TODO(levik): Move all regular expressions here.
 */
os.regExps_ = {
  onlyWhitespace: /^[ \t\n]*$/
};

/**
 * Preprocess the template.
 * @param {Element|string} node DOM node containing the template data, or the
 * string source.
 * @param {string} opt_id An optional ID for the new template. 
 * @return {os.Template} A compiled Template object
 */
os.compileTemplate = function(node, opt_id) {
  // Allow polymorphic behavior.
  if (typeof(node) == "string") {
    return os.compileTemplateString(node, opt_id);
  }
  
  opt_id = opt_id || node.id;
  var src = node.value || node.innerHTML;
  var template = os.compileTemplateString(src, opt_id);
  return template;
};

/**
 * Compile a template without requiring a DOM node.
 * @param {Element} src XML data to be compiled.
 * @param {string} opt_id An optional ID for the new template.
 * @return {os.Template} A compiled Template object.
 */
os.compileTemplateString = function(src, opt_id) {
  var src = os.prepareTemplateXML_(src);
  var doc = os.parseXML_(src);
  return os.compileXMLDoc(doc, opt_id);
};

/**
 * Render one compiled node with a context.
 */
os.renderTemplateNode_ = function(compiledNode, context) {
  var template = domCloneElement(compiledNode);
  if (template.removeAttribute) {
    template.removeAttribute(STRING_id);
  }
  jstProcess(context, template);
  return template;  
};

/**
 * @type {number} A global counter for rendered elements.
 * @private
 */
os.elementIdCounter_ = 0;

/**
 * Creates a custom tag function for rendering a compiled template.
 */
os.createTemplateCustomTag = function(template) {
  return function(node, data, context) {
    context.setVariable(os.VAR_my, node);
    context.setVariable(os.VAR_node, node);
    context.setVariable(os.VAR_uniqueId, os.elementIdCounter_++);
    var ret = template.render(data, context);

    // Prevent reprocessing after attachment.
    os.markNodeToSkip(ret);

    return ret;
  }
};

/**
 * Creates a functor which returns a value from the specified node given a 
 * name.
 * @param {Node} node Node to get the value from.
 * @return {Function} The functor which takes a type {string}.
 * @private
 */
os.createNodeAccessor_ = function(node) {
  return function(name) {
    return os.getValueFromNode_(node, name);
  }         
};

/**
 * Globally disallowed dynamic attributes. These are the attributes where
 * ${} notation will be ignored reguardless of the tag.
 */
os.globalDisallowedAttributes_ = {
  'data': 1
};

/**
 * A map of custom attributes keyed by attribute name.
 * Maps {string} types onto Function({Element|string|Object|JSEvalContext}). 
 * @type {Object}
 * @private
 */
os.customAttributes_ = {};

/**
 * Registers a custom attribute functor. When this attribute is encountered in
 * a DOM node, the specified functor will be called. 
 * @param {string} attrName The name of the custom attribute.
 * @param {Function} functor A function with signature 
 *     function({Element}, {string}, {Object}, {JSEvalContext})
 */
os.registerAttribute = function(attrName, functor) {
  os.customAttributes_[attrName] = functor;
};

/**
 * Calls a pre-registered custom attribute handler.
 */
os.doAttribute = function(node, attrName, data, context) {
  var attrFunctor = os.customAttributes_[attrName];
  if (!attrFunctor) {
    return;
  }
  attrFunctor(node, node.getAttribute(attrName), data, context);
};

/**
 * Processes a custom tag by invoking the appropriate custom tag function.
 * @param {Element} node Parent DOM node.
 * @param {string} ns Namespace.
 * @param {string} tag Tag name.
 * @param {Object} data Current evaluation data.
 * @param {Object} context JSEvalContext object encapsulating data.
 */
os.doTag = function(node, ns, tag, data, context) {
  var tagFunction = os.getCustomTag(ns, tag);
  if (!tagFunction) {
    throw "Custom tag <" + ns + ":" + tag + "> not defined.";
  }
  
  // Process tag's inner content before processing the tag.
  for (var child = node.firstChild; child; child = child.nextSibling) {
    if (child.nodeType == DOM_ELEMENT_NODE) {
      jstProcess(context, child);
      os.markNodeToSkip(child);
    }
  }
  
  var result = tagFunction.call(null, node, data, context);

  if (!result && typeof(result) != "string") {
    throw "Custom tag <" + ns + ":" + tag + "> failed to return anything.";
  }

  if (typeof(result) == "string") {
    node.innerHTML = result ? result : "";    
  } else if (isArray(result) && result.nodeType != DOM_TEXT_NODE) {
    os.removeChildren(node);
    for (var i = 0; i < result.length; i++) {
      if (result[i].nodeType && 
          (result[i].nodeType == DOM_ELEMENT_NODE || 
           result[i].nodeType == DOM_TEXT_NODE)) {
        node.appendChild(result[i]);
        if (result[i].nodeType == DOM_ELEMENT_NODE) {
          os.markNodeToSkip(result[i]);
        }
      }
    }
  } else {
    var callbacks = context.getVariable(os.VAR_callbacks);
    var resultNode = null;
    if (result.nodeType && result.nodeType == DOM_ELEMENT_NODE) {
      resultNode = result;
    } else if (result.root && result.root.nodeType &&
        result.root.nodeType == DOM_ELEMENT_NODE) {
      resultNode = result.root;
    }
    
    // Only attach the result DOM if it's not the same as the container node,
    // or not already attached. In IE, detached nodes can be parented in
    // DocumentFragments, so we check for that as well.
    if (resultNode && resultNode != node && (
        !resultNode.parentNode || 
        result.parentNode.nodeType == DOM_DOCUMENT_FRAGMENT_NODE)) {
      os.removeChildren(node); 
      node.appendChild(resultNode);
      os.markNodeToSkip(resultNode);      
    }
    if (result.onAttach) {
      callbacks.push(result);
    }
  }
};


/**
 * Checks the current context, and if it's an element node, sets it to be used 
 * for future <os:renderAll/> operations. 
 */
os.setContextNode_ = function(data, context) {
  if (data.nodeType == DOM_ELEMENT_NODE) {
    context.setVariable(os.VAR_node, data);
  }
};

/**
 * Mark the node to not be re-processed by continued template processing.
 * Useful if the node contains a template that needs to be processed with a 
 * different context.
 */
os.markNodeToSkip = function(node) {
  node.setAttribute(ATT_skip, "true");

  // Remove the attributes processed when jsskip is true
  node.removeAttribute(ATT_select);
  node.removeAttribute(ATT_eval);
  node.removeAttribute(ATT_values);
  node.removeAttribute(ATT_display);

  // Cause the cache to be re-calculated
  node[PROP_jstcache] = null;
  node.removeAttribute(ATT_jstcache);
};

 

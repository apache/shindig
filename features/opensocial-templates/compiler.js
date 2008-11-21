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
 * @fileoverview Implements compiler functionality for OpenSocial Templates.
 *
 * TODO(davidbyttow): Move into os.Compiler.
 */

/**
 * Literal semcolons have special meaning in JST, so we need to change them to
 * variable references.
 */
os.SEMICOLON = ';';

/**
 * Check if the browser is Internet Explorer.
 * 
 * TODO(levik): Find a better, more general way to do this, esp. if we need
 * to do other browser checks elswhere.
 */
os.isIe = navigator.userAgent.indexOf('Opera') != 0 && 
    navigator.userAgent.indexOf('MSIE') != -1;

/**
 * Takes an XML node containing Template markup and compiles it into a Template.
 * The node itself is not considered part of the markup.
 * @param {Node} node XML node to be compiled.
 * @param {string} opt_id An optional ID for the new template.
 * @return {os.Template} A compiled Template object.
 */
os.compileXMLNode = function(node, opt_id) {
  var nodes = [];
  for (var child = node.firstChild; child; child = child.nextSibling) {
    if (child.nodeType == DOM_ELEMENT_NODE) {
      nodes.push(os.compileNode_(child));
    } else if (child.nodeType == DOM_TEXT_NODE) {
      if (child != node.firstChild || 
          !child.nodeValue.match(os.regExps_.onlyWhitespace)) {
        var compiled = os.breakTextNode_(child);
        for (var i = 0; i < compiled.length; i++) {
          nodes.push(compiled[i]);
        }
      }
    }
  }
  var template = new os.Template(opt_id);
  template.setCompiledNodes_(nodes);
  return template;
};

/**
 * Takes an XML Document and compiles it into a Template object.
 * @param {Node} doc XML document to be compiled.
 * @param {string} opt_id An optional ID for the new template.
 * @return {os.Template} A compiled Template object.
 */
os.compileXMLDoc = function(doc, opt_id) {
  var node = doc.firstChild;
  // Find the <root> node (skip DOCTYPE).
  while (node.nodeType != DOM_ELEMENT_NODE) {
    node = node.nextSibling;
  }
  
  return os.compileXMLNode(node, opt_id);
};

/**
 * Parses an XML string into an XML Document.
 * @param {string} str A string of well-formed XML.
 * @return {Document} XML document.
 */
os.parseXML_ = function(str) {
  if (typeof(DOMParser) != "undefined") {
    os.parser_ = os.parser_ || new DOMParser();
    var doc = os.parser_.parseFromString(str, "text/xml");
    if (doc.firstChild && doc.firstChild.tagName == 'parsererror') {
      throw doc.firstChild.firstChild.nodeValue;
    }
    return doc;
  } else {
    var doc = new ActiveXObject("MSXML2.DomDocument");
    doc.validateOnParse = false;
    doc.loadXML(str);
    if (doc.parseError && doc.parseError.errorCode) {
      throw doc.parseError.reason;
    }
    return doc;
  }
};

/**
 * Map of special operators to be transformed.
 */
os.operatorMap = {
  'and': '&&',
  'eq': '==',
  'lte': '<=',
  'lt': '<',
  'gte': '>=',
  'gt': '>',
  'neq': '!=',
  'or': '||',
  'not': '!'
};

/**
 * Parses operator markup into JS code. See operator map above.
 * 
 * TODO: Simplify this to only work on neccessary operators - binary ones that
 * use "<" or ">". 
 * 
 * @param {string} src The string snippet to parse.
 */
os.remapOperators_ = function(src) {
  // TODO(davidbyttow): This can be further sped up if the operatorMap
  // is sorted (if necessary).
  var out = "";
  var sub = "";
  for (var i = 0; i < src.length; ++i) {
    var c = src.charAt(i);
    if (os.isAlphaNum(c)) {
      sub += c;
    } else {
      if (sub.length > 0) {
        if (sub.length < 4) {
          sub = os.operatorMap[sub.toLowerCase()] || sub;
        }
        out += sub;
        sub = "";
      }
      out += c;
    }
  }
  out += sub;
  return out;
};

/**
 * Remap variable references in the expression.
 * @param {string} expr The expression to transform.
 * @return {string} Transformed exression. 
 */
os.transformVariables_ = function(expr) {
  expr = os.replaceTopLevelVars_(expr);
      
  return expr;
};

/**
 * Map of variables to transform
 */
os.variableMap_ = {
  'my': os.VAR_my,
  'My': os.VAR_my,
  'cur': VAR_this,
  'Cur': VAR_this,
  '$cur': VAR_this,
  'Top': VAR_top,
  'Index': VAR_index,
  'Count': VAR_count
};

/**
 * Replace the top level variables
 * @param {string} expr The expression
 * @return {string} Expression with replacements
 */
os.replaceTopLevelVars_ = function(text) {
	
  // Currently, values specced to be inside the "Context" variable are placed
  // by JSTemplate into the top-level of the data context.
  // To make references like "Context.Index" work, remove the "Context." prefix. 
  text = text.replace(/Context[.]/g, "");
	
  // This line needed because there wasn't an obvious way to match
  // [^.$a-zA-Z0-9] or the start of the line
  text = ' ' + text;
  var regex = /([^.$a-zA-Z0-9])([$a-zA-Z0-9]+)/g;
  
  var results;
  var dest = '';
  var index = 0;
 
  while ((results = regex.exec(text)) != null) {
    dest += text.substring(index, regex.lastIndex - results[0].length);
    dest += results[1];
    if (results[2] in os.variableMap_) {
      dest += os.variableMap_[results[2]];
    } else {
      dest += results[2];
    }
    index = regex.lastIndex;
  }
  dest += text.substring(index, text.length);
  
  return dest.substring(1);
};

/**
 * This function is used to lookup named properties of objects.
 * By default only a simple lookup is performed, but using
 * os.setIdentifierResolver() it's possible to plug in a more complex function,
 * for example one that looks up foo -> getFoo() -> get("foo").
 * 
 * TODO: This should not be in compiler.
 */
os.identifierResolver_ = function(data, name) {
  return data[name];
};

/**
 * Sets the Identifier resolver function. This is global, and must be done 
 * before any compilation of templates takes place. 
 * 
 * TODO: This should possibly not be in compiler?
 */
os.setIdentifierResolver = function(resolver) {
  os.identifierResolver_ = resolver;
};


/**
 * Gets a named property from a JsEvalContext (by checking data_ and vars_) or
 * from a simple JSON object by looking at properties. The IdentifierResolver
 * function is used in either case.
 * 
 * TODO: This should not be in compiler.
 * 
 * @param {JsEvalContext|Object} context Context to get property from
 * @param {String} name Name of the property
 * @return {Object|String}
 */
os.getFromContext = function(context, name, opt_default) {
  var ret;
  // Check if this is a context object.
  if (context.vars_ && context.data_) {
    // Is the context payload a DOM node?              
    if (context.data_.nodeType == DOM_ELEMENT_NODE) {
      ret = os.getValueFromNode_(context.data_, name);
      if (ret == null) {
        var und;
        ret = und;
      }
    } else {
      ret = os.identifierResolver_(context.data_, name);
    }
    if (typeof(ret) == "undefined") {
      ret = os.identifierResolver_(context.vars_, name);
    }
  } else if (context.nodeType == DOM_ELEMENT_NODE) {
    // Is the context a DOM node?
    ret = os.getValueFromNode_(context, name);
  } else {
    ret = os.identifierResolver_(context, name);
  }
  if (typeof(ret) == "undefined" || ret == null) {
    if (typeof(opt_default) != "undefined") {
      ret = opt_default;
    } else {
      ret = "";
    }
  }
  return ret;
};

/**
 * Prepares an expression for JS evaluation.
 * @param {string} expr The expression snippet to parse.
 * @param {string} opt_default An optional default value reference (such as the 
 * literal string 'null').
 */
os.transformExpression_ = function(expr, opt_default) {
  expr = os.remapOperators_(expr);
  expr = os.transformVariables_(expr);
  if (os.identifierResolver_) {
    expr = os.wrapIdentifiersInExpression(expr, opt_default);
  }
  return expr;  
};

/**
 * A Map of special attribute names to change while copying attributes during
 * compilation.
 */
os.attributeMap_ = {
  'if': 'jsdisplay',
  'repeat': 'jsselect',
  'context': 'jsselect'
};

/**
 * Appends a JSTemplate attribute value while maintaining previous values.
 */
os.appendJSTAttribute_ = function(node, attrName, value) {
  var previousValue = node.getAttribute(attrName);
  if (previousValue) {
    value = previousValue + ";" + value;
  }
  node.setAttribute(attrName, value);
};

/**
 * Copies attributes from one node (xml or html) to another (html),. 
 * Special OpenSocial attributes are substituted for their JStemplate 
 * counterparts.
 * @param {Element} from An XML or HTML node to copy attributes from.
 * @param {Element} to An HTML node to copy attributes to.
 * @param {String} opt_customTag The name of the custom tag, being processed if 
 * any.
 * 
 * TODO(levik): On IE, some properties/attributes might be case sensitive when
 * set through script (such as "colSpan") - since they're not case sensitive
 * when defined in HTML, we need to support this type of use.
 */ 
os.copyAttributes_ = function(from, to, opt_customTag) {

  var dynamicAttributes = null;

  for (var i = 0; i < from.attributes.length; i++) {
    var name = from.attributes[i].nodeName;
    var value = from.getAttribute(name);
    if (name && value) {
      if (name == 'var') {
        os.appendJSTAttribute_(to, ATT_vars, from.getAttribute(name) +
            ': $this'); 
      } else if (name == 'index') {
        os.appendJSTAttribute_(to, ATT_vars, from.getAttribute(name) +
            ': $index');        
      } else if (name.length < 7 || name.substring(0, 6) != 'xmlns:') {
        if (os.customAttributes_[name]) {
          os.appendJSTAttribute_(to, ATT_eval, "os.doAttribute(this, '" + name +
              "', $this, $context)");
        } else if (name == 'repeat') {
          os.appendJSTAttribute_(to, ATT_eval,
              "os.setContextNode_($this, $context)");
        }
        var outName = os.attributeMap_[name] || name;
        var substitution = 
            (os.attributeMap_[name] ||
                opt_customTag && os.globalDisallowedAttributes_[outName]) ? 
            null : os.parseAttribute_(value);

        if (substitution) {
          if (outName == 'class') {
            // Dynamically setting the @class attribute gets ignored by the 
            // browser. We need to set the .className property instead.
            outName = '.className';
          } else if (outName == 'style') {
            // Similarly, on IE, setting the @style attribute has no effect.
            // The cssText property of the style object must be set instead.
            outName = '.style.cssText';
          } else if (to.getAttribute(os.ATT_customtag)) {
            // For custom tags, it is more useful to put values into properties
            // where they can be accessed as objects, rather than placing them
            // into attributes where they need to be serialized.
            outName = '.' + outName;
          } else if (os.isIe && !os.customAttributes_[outName] &&
              outName.substring(0, 2).toLowerCase() == 'on') {
            // For event handlers on IE, setAttribute doesn't work, so we need
            // to create a function to set as a property.
            outName = '.' + outName;
            substitution = 'new Function(' + substitution + ')';
          }

          // TODO: reuse static array (IE6 perf).          
          if (!dynamicAttributes) {
            dynamicAttributes = [];
          }
          dynamicAttributes.push(outName + ':' + substitution);
        } else {
          // For special attributes, do variable transformation.
          if (os.attributeMap_[name]) {
            // If the attribute value looks like "${expr}", just use the "expr".
            if (value.length > 3 && 
              value.substring(0, 2) == '${' && 
              value.charAt(value.length - 1) == '}') {
              value = value.substring(2, value.length - 1);
            }
            // In special attributes, default value is null.
            value = os.transformExpression_(value, 'null');
          } else if (outName == 'class') {
            // In IE, we must set className instead of class.
            to.setAttribute('className', value);
          } else if (outName == 'style') {
            // Similarly, on IE, setting the @style attribute has no effect.
            // The cssText property of the style object must be set instead.
            to.style.cssText = value;
          } 
          if (os.isIe && !os.customAttributes_[outName] &&
              outName.substring(0, 2).toLowerCase() == 'on') {
            // In IE, setAttribute doesn't create event handlers, so we must
            // use attachEvent in order to create handlers that are preserved
            // by calls to cloneNode().
            to.attachEvent(outName, new Function(value));
          } else {
            to.setAttribute(outName, value);
          }
        }
      }
    }
  }
  
  if (dynamicAttributes) {
    os.appendJSTAttribute_(to, ATT_values, dynamicAttributes.join(';'));
  }
};

/**
 * Recursively compiles an individual node from XML to DOM (for JSTemplate)
 * Special os.* tags and tags for which custom functions are defined
 * are converted into markup recognizable by JSTemplate.
 *
 * TODO: process text nodes and attributes  with ${} notation here
 */
os.compileNode_ = function(node) {
  if (node.nodeType == DOM_TEXT_NODE) {
    var textNode = node.cloneNode(false);
    return os.breakTextNode_(textNode);
  } else if (node.nodeType == DOM_ELEMENT_NODE) {
    var output;
    if (node.tagName.indexOf(":") > 0) {
      output = document.createElement("span");
      output.setAttribute(os.ATT_customtag, node.tagName);

      var custom = node.tagName.split(":");
      os.appendJSTAttribute_(output, ATT_eval, "os.doTag(this, \"" 
          + custom[0] + "\", \"" + custom[1] + "\", $this, $context)");
      var context = node.getAttribute("context") || "$this||true";
      output.setAttribute(ATT_select, context);

      // For os:Render, create a parent node reference.
      if (node.tagName == "os:render" || node.tagName == "os:Render" ||
          node.tagName == "os:renderAll" || node.tagName == "os:RenderAll") {
        os.appendJSTAttribute_(output, ATT_values, os.VAR_parentnode + ":" +
            os.VAR_node);
      }
      
      os.copyAttributes_(node, output, node.tagName);
    } else {
      output = os.xmlToHtml_(node);
    }
    if (output && !os.processTextContent_(node, output)) {
      for (var child = node.firstChild; child; child = child.nextSibling) {
        var compiledChild = os.compileNode_(child);
        if (compiledChild) {
          if (!compiledChild.tagName && 
              typeof(compiledChild.length) == 'number') {
            for (var i = 0; i < compiledChild.length; i++) {
              output.appendChild(compiledChild[i]);
            }
          } else {
            // If inserting a TR into a TABLE, inject a TBODY element. 
            if (compiledChild.tagName == 'TR' && output.tagName == 'TABLE') {
              var lastEl = output.lastChild;
              while (lastEl && lastEl.nodeType != DOM_ELEMENT_NODE && 
                  lastEl.previousSibling) {
                lastEl = lastEl.previousSibling;
              } 
              if (!lastEl || lastEl.tagName != 'TBODY') {
                lastEl = document.createElement('tbody');
                output.appendChild(lastEl);
              }
              lastEl.appendChild(compiledChild);
            } else {
              output.appendChild(compiledChild);
            }
          }
        }
      }
    }
    return output;
  }
  return null;      
};

/**
 * XHTML Entities we need to support in XML, definted in DOCTYPE format.
 * 
 * TODO(levik): A better way to do this. 
 */
os.ENTITIES = "<!ENTITY nbsp \"&#160;\">";

/**
 * Prepares an XML string to be compiled as a template. Injects a DOCTYPE 
 * with entities and a top-level <root> element to encapsulate the template.
 * @param {string} templateSrc XML string to be prepared.
 * @return {string} XML string prepared for template compilation.
 */
os.prepareTemplateXML_ = function(templateSrc) {
  var namespaces = os.getRequiredNamespaces(templateSrc);
  return "<!DOCTYPE root [" + os.ENTITIES + "]><root xml:space=\"preserve\"" + 
      namespaces + ">" + templateSrc + "</root>";;
};

/**
 * Creates an HTML node that's a shallow copy of an XML node 
 * (includes attributes).
 */
os.xmlToHtml_ = function(xmlNode) {
  var htmlNode = document.createElement(xmlNode.tagName);
  os.copyAttributes_(xmlNode, htmlNode);
  return htmlNode;
};

/**
 * Creates a context object out of a json data object. 
 */
os.createContext = function(data, opt_globals) {
  var context = JsEvalContext.create(data);
  context.setVariable(os.VAR_callbacks, []);
  context.setVariable(os.VAR_identifierresolver, os.getFromContext);
  if (opt_globals) {
    for (var global in opt_globals) {
      context.setVariable(global, opt_globals[global]);
    }
  }
  return context;            
};

/**
 * Fires callbacks on a context object 
 */
os.fireCallbacks = function(context) {
  var callbacks = context.getVariable(os.VAR_callbacks);
  while (callbacks.length > 0) {
    var callback = callbacks.pop();
    if (callback.onAttach) {
      callback.onAttach();
    // TODO(levik): Remove no-context handlers?
    } else {
      callback();
    }
  }            
};

/**
 * Checks for an processes an optimized case where a node only has text content.
 * In this instance, any variable substitutions happen without creating 
 * intermediary spans.
 * 
 * This will work when node content looks like:
 *   - "Plain text"
 *   - "${var}"
 *   - "Plain text with ${var} inside"
 * But not when it is
 *   - "Text <b>With HTML content</b> (with or without a ${var})
 *   - Custom tags are also exempt from this optimization.
 * 
 * @return {boolean} true if node only had text data and needs no further 
 * processing, false otherwise.
 */
os.processTextContent_ = function(fromNode, toNode) {
  if (fromNode.childNodes.length == 1 && 
      !toNode.getAttribute(os.ATT_customtag) && 
      fromNode.firstChild.nodeType == DOM_TEXT_NODE) {
    var substitution = os.parseAttribute_(fromNode.firstChild.data);
    if (substitution) {
      toNode.setAttribute(ATT_content, substitution);
    } else {
      toNode.appendChild(document.createTextNode(
          os.trimWhitespaceForIE_(fromNode.firstChild.data, true, true)));
    }
    return true;
  }
  return false;
};

/**
 * IE does not hide extra whitespace if it is manually injected into a text 
 * node, so we need to remove extra whitespace by hand.
 * @param {String} text A string to be create a text node.
 * @return {TextNode} A TextNode constructed with the specified text.
 */
os.pushTextNode = function(array, text) {
  if (text.length > 0) {
    array.push(document.createTextNode(text));
  }
};

/**
 * Removes extra whitespace and newline characters for IE - to be used for
 * transforming strings that are destined for textNode content.
 * @param {String} string The string to trim spaces from.
 * @param {boolean} opt_trimStart Trim the start of the string. 
 * @param {boolean} opt_trimEnd Trim the end of the string.
 * @return {String} The string with extra spaces removed on IE, original 
 * string on other browsers.
 */
os.trimWhitespaceForIE_ = function(string, opt_trimStart, opt_trimEnd) {
  if (os.isIe) {
    // Replace newlines with spaces, then multiple spaces with single ones.
    // Then remove leading and trailing spaces.
    var ret = string.replace(/\n/g, ' ').replace(/\s+/g, ' ');
    if (opt_trimStart) {
      ret = ret.replace(/^\s/, '');
    }
    if (opt_trimEnd) {
      ret = ret.replace(/\s$/, '');
    }
    return ret;
  }
  return string;
};

/**
 * Breaks up a text node with special ${var} markup into a series of text nodes
 * and spans with appropriate jscontent attribute.
 * 
 * @return {Array.<Node>} An array of textNodes and Span Elements if variable 
 * substitutions were found, or an empty array if none were.  
 */
os.breakTextNode_ = function(textNode) {
  var substRex = /^([^$]*)(\$\{[^\}]*\})([\w\W]*)$/;
  var text = textNode.data;
  var nodes = [];
  var match = text.match(substRex);
  while (match) {
    if (match[1].length > 0) {
      os.pushTextNode(nodes, os.trimWhitespaceForIE_(match[1]));
    }
    var token = match[2].substring(2, match[2].length - 1);
    if (!token) {
      token = '$this';
    }
    var tokenSpan = document.createElement("span");
    tokenSpan.setAttribute(ATT_content, os.transformExpression_(token));
    nodes.push(tokenSpan);
    match = text.match(substRex);
    text = match[3];
    match = text.match(substRex);
  }
  if (text.length > 0) {
    os.pushTextNode(nodes, os.trimWhitespaceForIE_(text));
  }
  return nodes; 
};

/**
 * Transforms a literal string for inclusion into a variable evaluation:
 *   - Escapes single quotes.
 *   - Replaces newlines with spaces.
 *   - Substitutes variable references for literal semicolons.
 *   - Addes single quotes around the string.
 */
os.transformLiteral_ = function(string) {
  return "'" + string.replace(/'/g, "\\'").
      replace(/\n/g, " ").replace(/;/g, "'+os.SEMICOLON+'") + "'";
};

/**
 * Parses an attribute value into a JS expression. "Hello, ${user}!" becomes
 * "Hello, " + user + "!".
 * 
 * TODO: Rename to parseExpression()
 */
os.parseAttribute_ = function(value) {
  if (!value.length) {
    return null;
  }
  var substRex = /^([^$]*)(\$\{[^\}]*\})([\w\W]*)$/;
  var text = value;
  var parts = [];
  var match = text.match(substRex);
  if (!match) {
    return null;
  }
  while (match) {
    if (match[1].length > 0) {
      parts.push(os.transformLiteral_(
          os.trimWhitespaceForIE_(match[1], parts.length == 0)));
    }
    var expr = match[2].substring(2, match[2].length - 1);
    parts.push('(' + os.transformExpression_(expr) + ')');
    text = match[3];
    match = text.match(substRex);
  }
  if (text.length > 0) {
    parts.push(os.transformLiteral_(
        os.trimWhitespaceForIE_(text, false, true)));
  }
  return parts.join('+');
};

/**
 * Returns a named value of a given object. If the object happens to be a DOM
 * node, os.getValueFromNode_() is used, otherwise, we look for a property.
 * @param {Object} object The Object to inspect.
 * @param {string} name The name of the value to get
 * @return {string|Element|Object|Array.<Element>} The value as a String,
 * Object, Element or array of Elements.
 */
os.getValueFromObject_ = function(object, name) {
  if (!name) {
    return object;
  } else if (object.nodeType == DOM_ELEMENT_NODE) {
    return os.getValueFromNode_(object, name);
  } else {
    return object[name];
  }
};

/**
 * Returns a named value of a given node. First looks for an attribute, then
 * a child node (or nodes).
 * @param {Element} node The DOM node to inspect
 * @param {string} name The name of the property/attribute/child node(s) to get.
 * The special value "*" means return all child Elemens
 * @return {string|Element|Object|Array.<Element>} The value as a String,
 * Object, Element or array of Elements.
 */
os.getValueFromNode_ = function(node, name) {
  var ret = node[name];
  if (typeof(ret) == "undefined" || ret == null) {
    ret = node.getAttribute(name);
  }
  
  if (typeof(ret) == "undefined" || ret == null) {
    if (name) {
      name = name.toLowerCase();
    }

    // A special character meaning "get all child nodes".  
    var allChildren = ("*" == name);
    
    ret = [];
    for (var child = node.firstChild; child; child = child.nextSibling) {
      if (allChildren) {
        ret.push(child);
        continue;
      }
      if (child.nodeType != DOM_ELEMENT_NODE) {
        continue;
      }
      var tagName = child.getAttribute(os.ATT_customtag);
      if (!tagName) {
        tagName = child.tagName;
      }
      tagName = tagName.toLowerCase();
      if (tagName == name) {
        ret.push(child);
      }    
    }
    if (ret.length == 0) {
      ret = null;
    }
    
    // Check for "optimized" dynamic content. A tag like: 
    // <div>${a}</div> would have compiled to <div jscontent="a"/>, and we
    // need to reconstruct its content here into a span.
    var content;
    if (ret == null && allChildren &&
        (content = node.getAttribute(ATT_content))) {
      var span = document.createElement("span");
      span.setAttribute(ATT_content, content);
      ret = [ span ];
    } 
  }
  
  // Process special cases where ret would be wrongly evaluated as "true"
  if (ret == "false") {
    ret = false;
  } else if (ret == "0") {
    ret = 0;
  }
  return ret;
};

//------------------------------------------------------------------------------
// The functions below are for parsing JS expressions to wrap identifiers.
// They should be move into a separate file/js-namespace.
//------------------------------------------------------------------------------

/**
 * A map of identifiers that should not be wrapped 
 * (such as JS built-ins and special method names). 
 */
os.identifiersNotToWrap_ = {};
os.identifiersNotToWrap_['true'] = true;
os.identifiersNotToWrap_['false'] = true;
os.identifiersNotToWrap_['null'] = true;
os.identifiersNotToWrap_['var'] = true;
os.identifiersNotToWrap_[os.VAR_my] = true;
os.identifiersNotToWrap_[VAR_this] = true;
os.identifiersNotToWrap_[VAR_context] = true;
os.identifiersNotToWrap_[VAR_top] = true;
os.identifiersNotToWrap_[VAR_index] = true;
os.identifiersNotToWrap_[VAR_count] = true;

/**
 * Checks if a character can begin an legal JS identifier name.
 * @param {string} ch Character to check.
 * @return {boolean} This character can start an identifier.
 */
os.canStartIdentifier= function(ch) {
  return (ch >= 'a' && ch <= 'z') || 
      (ch >= 'A' && ch <= 'Z') || 
      ch == '_' || ch == '$'; 
};

/**
 * Checks if a character can be contained in a legal JS identifier name.
 * @param {string} ch Character to check.
 * @return {string} This is a valid identifier character.
 */
os.canBeInIdentifier = function(ch) {
  return os.canStartIdentifier(ch) || (ch >= '0' && ch <= '9') ||
      ch == '-' ||
      // The colon char cannot be in a real JS identifier, but we allow it,
      // so that namespaced tag names are treated as whole identifiers.
      ch == ':';
};

/**
 * Checks if a character can be contained in a legal JS identifier name.
 * @param {string} ch Character to check.
 * @return {string} This is a valid identifier character.
 */
os.canBeInToken = function(ch) {
  return os.canBeInIdentifier(ch) || ch == '(' || ch == ')' ||
      ch == '[' || ch == ']' || ch == '.';
};

/**
 * Wraps an identifier for Identifier Resolution with respect to the context.
 * os.VAR_idenfitierresolver ("$_ir") is used as the function name.
 * So, "foo.bar" becomes "$_ir($_ir($context, 'foo'), 'bar')"
 * @param {string} iden A string representing an identifier.
 * @param {string} opt_context A string expression to use for context. 
 * @param {string} opt_default An optional default value reference (such as the 
 * literal string 'null'). 
 */
os.wrapSingleIdentifier = function(iden, opt_context, opt_default) {
  if (os.identifiersNotToWrap_[iden]) {
    return iden; 
  }
  return os.VAR_identifierresolver + '(' + 
      (opt_context || VAR_context) + ', \'' + iden + '\'' + 
      (opt_default ? ', ' + opt_default : '') + 
      ')';  
};

/**
 * Wraps identifiers in a single token of JS.
 */
os.wrapIdentifiersInToken = function(token, opt_default) {
  if (!os.canStartIdentifier(token.charAt(0))) {
    return token;
  }

  // If the identifier is accessing a message 
  // (and gadget messages are obtainable), inline it here.
  // TODO: This is inefficient for times when the message contains no markup - 
  // such cases should be optimized.  
  if (token.substring(0, os.VAR_msg.length + 1) == (os.VAR_msg + '.') && 
      os.gadgetPrefs_) {
    var key = token.split(".")[1];
    var msg = os.getPrefMessage(key) || '';
    return os.parseAttribute_(msg) || os.transformLiteral_(msg);
  }
  
  var identifiers = os.tokenToIdentifiers(token);
  var parts = false;
  var buffer = [];
  var output = null;
  for (var i = 0; i < identifiers.length; i++) {
    var iden = identifiers[i];
    parts = os.breakUpParens(iden);
    if (!parts) {
      output = os.wrapSingleIdentifier(iden, output, opt_default);
    } else {         
      buffer.length = 0;
      buffer.push(os.wrapSingleIdentifier(parts[0], output));
      for (var j = 1; j < parts.length; j+= 3) {
        buffer.push(parts[j]);
        if (parts[j + 1]) {
          buffer.push(os.wrapIdentifiersInExpression(parts[j + 1]));
        }
        buffer.push(parts[j + 2]);
      }
      output = buffer.join('');
    }
  }
 return output;
};

/**
 * Wraps all identifiers in a JS expression. The expression is tokenized, then
 * each token is wrapped individually.
 * @param {string} expr The expression to wrap.
 * @param {string} opt_default An optional default value reference (such as the 
 * literal string 'null'). 
 */
os.wrapIdentifiersInExpression = function(expr, opt_default) {
  var out = [];
  var tokens = os.expressionToTokens(expr);
  for (var i = 0; i < tokens.length; i++) {
    out.push(os.wrapIdentifiersInToken(tokens[i], opt_default));
  }
  return out.join('');
};

/**
 * Tokenizes a JS expression. Each token is either an operator, a literal 
 * string, an identifier, or a function call.
 * For example, 
 *   "foo||bar" is tokenized as ["foo", "||", "bar"], but
 *   "bing(foo||bar)" becomes   ["bing(foo||bar)"]. 
 */
os.expressionToTokens = function(expr) {
  var tokens = [];
  var inquotes = false;
  var inidentifier = false;
  var inparens = 0;
  var escaped = false;
  var quotestart = null;
  var buffer = [];
  for (var i = 0; i < expr.length; i++) {
    var ch = expr.charAt(i);
    if (inquotes) {
      if (!escaped && ch == quotestart) {
        inquotes = false;
      } else if (ch == '\\') {
        escaped = true;
      } else {
        escaped = false;
      } 
      buffer.push(ch);      
    } else {
      if (ch == "'" || ch == '"') {
        inquotes = true;
        quotestart = ch;
        buffer.push(ch);
        continue;
      }
      if (!inquotes && ch == '(') {
        inparens++;
      } else if (!inquotes && ch == ')' && inparens > 0) {
        inparens--;
      }
      if (inparens > 0) {
        buffer.push(ch);
        continue;
      }
      if (!inidentifier && os.canStartIdentifier(ch)) {
        if (buffer.length > 0) {
          tokens.push(buffer.join(''));
          buffer.length = 0;
        }          
        inidentifier = true;
        buffer.push(ch);
        continue;
      }
      if (inidentifier) {
        if (os.canBeInToken(ch)) {
          buffer.push(ch);
        } else {
          tokens.push(buffer.join(''));
          buffer.length = 0;
          inidentifier = false;
          buffer.push(ch);
        }
      } else {
        buffer.push(ch);        
      }
    }
  }
  tokens.push(buffer.join(''));
  return tokens;            
}; 

/**
 * Breaks up a JS token into identifiers, separated by '.'
 * "foo.bar" becomes ["foo", "bar"].
 */
os.tokenToIdentifiers = function(token) {
  var inquotes = false;
  var quotestart = null;    
  var escaped = false;
  var buffer = [];
  var identifiers = [];  
  for (var i = 0; i < token.length; i++) {
    var ch = token.charAt(i);
    if (inquotes) {
      if (!escaped && ch == quotestart) {
        inquotes = false;
      } else if (ch == '\\') {
        escaped = true;
      } else {
        escaped = false;
      } 
      buffer.push(ch);
      continue;      
    } else {
      if (ch == "'" || ch == '"') {
        buffer.push(ch);
        inquotes = true;
        quotestart = ch;
        continue;
      }
    }
    if (ch == '.' && !inquotes) {
      identifiers.push(buffer.join(''));
      buffer.length = 0;
      continue;
    } 
    buffer.push(ch);
  }
  identifiers.push(buffer.join(''));
  return identifiers;
};


/**
 * Checks if a JS identifier has parenthesis and bracket parts. If no such
 * parts are found, return false. Otherwise, the expression is returned as
 * an array of components:
 *   "foo(bar)"       -> ["foo", "(", "bar", ")"]
 *   "foo[bar](baz)"  -> ["foo", "[", "bar", "]", "(", "baz", ")"]
 */
os.breakUpParens = function(identifier) {
  var parenIndex = identifier.indexOf('(');
  var bracketIndex = identifier.indexOf('[');
  if (parenIndex < 0 && bracketIndex < 0) {
    return false;
  }
  var parts = [];
  if (parenIndex < 0 || (bracketIndex >= 0 && bracketIndex < parenIndex)) {
    parenIndex = 0;
    parts.push(identifier.substring(0, bracketIndex));
  } else {
    bracketIndex = 0;
    parts.push(identifier.substring(0, parenIndex));      
  }
  var parenstart = null;
  var inquotes = false;
  var quotestart = null;
  var parenlevel = 0;
  var escaped = false;
  var buffer = [];
  for (var i = bracketIndex + parenIndex; i < identifier.length; i++) {
    var ch = identifier.charAt(i);
    if (inquotes) {
      if (!escaped && ch == quotestart) {
        inquotes = false;
      } else if (ch == '\\') {
        escaped = true;
      } else {
        escaped = false;
      } 
      buffer.push(ch);      
    } else {
      if (ch == "'" || ch == '"') {
        inquotes = true;
        quotestart = ch;
        buffer.push(ch);
        continue;
      }
      if (parenlevel == 0) {
        if (ch == '(' || ch == '[') {
          parenstart = ch;
          parenlevel++;
          parts.push(ch);
          buffer.length = 0;
        }
      } else {
        if ((parenstart == '(' && ch == ')') || 
          (parenstart == '[' && ch == ']')) {
          parenlevel--;
          if (parenlevel == 0) {
            parts.push(buffer.join(''));
            parts.push(ch);
          } else {
            buffer.push(ch);
          }
        } else {
          if (ch == parenstart) {
            parenlevel++;
          }
          buffer.push(ch);
        }
      }
    }
  }
  return parts;
};

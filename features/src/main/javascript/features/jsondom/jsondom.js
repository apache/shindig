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
// Based on <http://www.w3.org/TR/2000/ REC-DOM-Level-2-Core-20001113/
// core.html#ID-1950641247>.
var DOM_ELEMENT_NODE = 1;
var DOM_ATTRIBUTE_NODE = 2;
var DOM_TEXT_NODE = 3;
var DOM_CDATA_SECTION_NODE = 4;
var DOM_ENTITY_REFERENCE_NODE = 5;
var DOM_ENTITY_NODE = 6;
var DOM_PROCESSING_INSTRUCTION_NODE = 7;
var DOM_COMMENT_NODE = 8;
var DOM_DOCUMENT_NODE = 9;
var DOM_DOCUMENT_TYPE_NODE = 10;
var DOM_DOCUMENT_FRAGMENT_NODE = 11;
var DOM_NOTATION_NODE = 12;

gadgets.jsondom = (function() {
  var domCache = {};

  function Node(data, opt_nextSibling) {
    if (typeof data === 'string') {
      return Text(data, opt_nextSibling);
    } else if (typeof data === 'object') {
      if (data.e) {
        throw new Error(data.e);
      }
      return Element(data, opt_nextSibling);
    }
    return null;
  }

  function Element(json, opt_nextSibling) {
    var nodeType = DOM_ELEMENT_NODE;
    var tagName = json.n;
    var attributes = [];
    var children = [];
    var nextSibling = opt_nextSibling;

    // Set up attributes.
    // They are passed as an array named "a", with
    // each value having "n" = name and "v" = value.
    for (var i = 0; i < json.a.length; ++i) {
      attributes.push(Attr(json.a[i].n, json.a[i].v));
    }

    // Set up children. Do so from the back of the list to
    // properly set up nextSibling references.
    var reverseChildren = [];
    var backChild = (json.c.length > 0 ? Node(json.c[json.c.length - 1]) : null);
    for (var i = json.c.length - 2; i >= 0; --i) {
      var next = Node(json.c[i], backChild);
      reverseChildren.push(next);
      backChild = next;
    }

    // children is the reverse of reverseChildren
    for (var i = reverseChildren.length - 1; i >= 0; --i) {
      children.push(reverseChildren[i]);
    }

    return {
      nodeType: nodeType,
      tagName: tagName,
      children: children,
      attributes: attributes,
      firstChild: children[0],
      nextSibling: nextSibling,
      getAttribute: function(key) {
        for (var i = 0; i < attributes.length; ++i) {
          if (attributes[i].nodeName == key) {
            return attributes[i];
          }
        }
        return null;
      }
    };
  }

  function Text(value, opt_nextSibling, opt_name, opt_type) {
    var nodeType = opt_type || DOM_TEXT_NODE;
    var nodeName = opt_name || '#text';
    var nodeValue = value;
    var nextSibling = opt_nextSibling;

    return {
      nodeType: nodeType,
      nodeName: nodeName,
      nodeValue: nodeValue,
      data: nodeValue,
      nextSibling: nextSibling,
      cloneNode: function() {
        return Text(nodeValue, nodeName);
      }
    };
  }

  function Attr(name, value) {
    return Text(value, null, name, DOM_ATTR_NODE);
  }

  function preload(id, json) {
    domCache[id] = Node(json);
  }

  function parse(str, opt_id) {
    // Unique ID per parseable String.
    if (opt_id && domCache[opt_id]) {
      return domCache[opt_id];
    }

    // Parse using browser primitives.
    var doc = opensocial.xmlutil.parseXML(str);

    if (opt_id) {
      domCache[opt_id] = doc;
    }

    return doc;
  }

  return {
    parse: parse,
    preload_: preload
  };
})();

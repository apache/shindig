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

var opensocial = opensocial || {};

opensocial.xmlutil = opensocial.xmlutil || {};

/**
 * Cached DOMParser objects on browsers that support it.
 */
opensocial.xmlutil.parser_ = null;


/**
 * Parses an XML string into an XML Document.
 * @param {string} str A string of well-formed XML.
 * @return {Document} XML document.
 */
opensocial.xmlutil.parseXML = function(str) {
  if (typeof(DOMParser) != "undefined") {
    opensocial.xmlutil.parser_ = opensocial.xmlutil.parser_ || new DOMParser();
    var doc = opensocial.xmlutil.parser_.parseFromString(str, "text/xml");
    if (doc.firstChild && doc.firstChild.tagName == 'parsererror') {
      throw doc.firstChild.firstChild.nodeValue;
    }
    return doc;
  } else if (typeof(ActiveXObject) != "undefined") {
    var doc = new ActiveXObject("MSXML2.DomDocument");
    doc.validateOnParse = false;
    doc.loadXML(str);
    if (doc.parseError && doc.parseError.errorCode) {
      throw doc.parseError.reason;
    }
    return doc;
  }
  throw "No XML parser found in this browser.";
};


/**
 * Map of Namespace prefixes to their respective URLs.
 * @type Map<string, string>
 */
opensocial.xmlutil.NSMAP = {
  "os": "http://opensocial.org/"
};


/**
 * Returns the XML namespace declarations that need to be injected into a
 * particular XML-like snippet to make it valid XML. Uses the defined
 * namespaces to see which are available, and checks that they are used in
 * the supplied code. An empty string is returned if no injection is needed.
 *
 * @param {string} xml XML-like source code.
 * @return {string} A string of xmlns delcarations required for this XML.
 */
opensocial.xmlutil.getRequiredNamespaces = function(xml) {
  var codeToInject = [];
  for (var ns in opensocial.xmlutil.NSMAP) {
    if (xml.indexOf("<" + ns + ":") >= 0 &&
        xml.indexOf("xmlns:" + ns + ":") < 0) {
      codeToInject.push(" xmlns:");
      codeToInject.push(ns);
      codeToInject.push("=\"");
      codeToInject.push(opensocial.xmlutil.NSMAP[ns]);
      codeToInject.push("\"");
    }
  }
  return codeToInject.join("");
};


/**
 * XHTML Entities we need to support in XML, definted in DOCTYPE declaration.
 *
 * TODO: A better way to do this.
 */
opensocial.xmlutil.ENTITIES = "<!ENTITY nbsp \"&#160;\">";


/**
 * Prepares an XML-like string to be parsed by browser parser. Injects a DOCTYPE
 * with entities and a top-level <root> element to encapsulate the code.
 * @param {string} xml XML string to be prepared.
 * @return {string} XML string prepared for client-side parsing.
 */
opensocial.xmlutil.prepareXML = function(xml) {
  var namespaces = opensocial.xmlutil.getRequiredNamespaces(xml);
  return "<!DOCTYPE root [" + opensocial.xmlutil.ENTITIES +
      "]><root xml:space=\"preserve\"" +
      namespaces + ">" + xml + "</root>";
};
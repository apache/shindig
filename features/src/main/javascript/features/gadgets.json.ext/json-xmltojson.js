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
 * @fileoverview
 * Extends the gadgets.json namespace with code that translates arbitrary XML to JSON.
 */

/**
 * @static
 * @class Translates arbitrary XML to JSON
 * @name gadgets.json.convertXmlToJson
 */
gadgets.json.xml = (function() {

  //Integer which represents a text node
  var TEXT_NODE = 3;

    /**
     * Parses all the child nodes of a specific DOM element and adds them to the JSON object
     * passed in.
     *
     * @param {Array} childNodes an array of DOM nodes.
     * @param {Object} json The JSON object to use for the conversion.  The DOM nodes will be added to
     * this JSON object.
     */
  function parseChildNodes(childNodes, json) {
    for (var index = 0; index < childNodes.length; index++) {
      var node = childNodes[index];
      if (node.nodeType == TEXT_NODE) {
        setTextNodeValue(json, node.nodeName, node);
      }
      else {

        if (node.childNodes.length == 0) {
          if (node.attributes != null && node.attributes.length != 0) {
            /*
             * If there are no children but there are attributes set the value for
             * this node in the JSON object to the JSON for the attributes.  There is nothing
             * left to do since there are no children.
             */
            setAttributes(node, json);
          }
          else {
            /*
             * If there are no children and no attributes set the value to null.
             */
            json[node.nodeName] = null;
          }
        }
        else {
          if (node.childNodes.length == 1 && node.firstChild.nodeType == TEXT_NODE && (node.attributes == null || node.attributes.length == 0)) {
            /*
             * There is only one child node and it is a text node AND we have no attributes so
             * just extract the text value from the text node and set it in the JSON object.
             */
            setTextNodeValue(json, node.nodeName, node.firstChild);
          }
          else {
            /*
             * There are both children and attributes, so recursively call this method until we have
             * reached the end.
             */
            setChildrenValues(json, node);
          }
        }
      }
    }
    };

    /**
     * Sets the JSON values for the children of a specified DOM element.
     * @param {Object} json the JSON object to set the values in.
     * @param node the DOM node containing children.
     */
    function setChildrenValues(json, node) {
      var currentValue = json[node.nodeName];
      if (currentValue == null) {
        /*
         * If there is no value for this property (node name) than
         * add the attributes and parse the children.
         */
        json[node.nodeName] = {};
        if (node.attributes != null && node.attributes.length != 0) {
          setAttributesValues(node.attributes, json[node.nodeName]);
        }
        parseChildNodes(node.childNodes, json[node.nodeName]);
      }
      else {
        /*
         * There is a value already for this property (node name) so
         * we need to create an array for the values of this property.
         * First add all the attributes then parse the children and create
         * an array from the result.
         */
        var temp = {};
        if (node.attributes != null && node.attributes.length != 0) {
          setAttributesValues(node.attributes, temp);
        }
        parseChildNodes(node.childNodes, temp);
        json[node.nodeName] = createValue(currentValue, temp);
      }
    };

    /**
     * Sets the JSON value for a text node.
     * @param {Object} json the JSON object to set the values in.
     * @param {string} nodeName the node name to set the value to.
     * @param textNode the text node containing the value to set.
     */
    function setTextNodeValue(json, nodeName, textNode) {
      var currentValue = json[nodeName];
      if (currentValue != null) {
        json[nodeName] = createValue(currentValue, textNode.nodeValue);
      }
      else {
        json[nodeName] = textNode.nodeValue;
      }
    };

    /**
     * Handles creating the text node value.  In some cases you may want to
     * create an array for the value if the node already has a value in the
     * JSON object.
     * @param currentValue the current value from the JSON object.
     * @param node the text node containing the value.
     */
    function createValue(currentValue, value) {
      if (currentValue instanceof Array) {
        currentValue[currentValue.length] = value;
        return currentValue;
      }
      else {
        return new Array(currentValue, value);
      }
    };


    /**
     * Sets the attributes from a DOM node in a JSON object.
     * @param node the node to add the attributes are on.
     * @param json the json object to set the attributes in.
     */
    function setAttributes(node, json) {
      var currentValue = json[node.nodeName];
      if (currentValue == null) {
        json[node.nodeName] = {};
        setAttributesValues(node.attributes, json[node.nodeName]);
      }
      else {
        var temp = {};
        setAttributesValues(node.attributes, temp);
        json[node.nodeName] = createValue(currentValue, temp);
      }
    };

    /**
     * Sets the values from attributes from a DOM node in a JSON object.
     * @param attributes the DOM node's attributes.
     * @param {Object} json the JSON object to set the values in.
     */
    function setAttributesValues(attributes, json) {
      var attribute = null;
      for (var attrIndex = 0; attrIndex < attributes.length; attrIndex++) {
        attribute = attributes[attrIndex];
        json['@' + attribute.nodeName] = attribute.nodeValue;
      }
    };

    return {
      convertXmlToJson: function(xmlDoc) {
        var childNodes = xmlDoc.childNodes;
        var result = {};
        parseChildNodes(childNodes, result);
        return result;
      }
    };

})();

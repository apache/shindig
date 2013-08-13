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

function JsonXmlToJsonTest(name) {
  TestCase.call(this, name); // super
}
JsonXmlToJsonTest.inherits(TestCase);
JsonXmlToJsonTest.prototype.setUp = function() {};
JsonXmlToJsonTest.prototype.tearDown = function() {};

JsonXmlToJsonTest.prototype.testConvertXmlToJson = function() {
  var fakeDom = {
    nodeName: '#document',
    nodeType: 9,
    attributes: [],
    childNodes: [{
      nodeName: 'embed',
      nodeType: 1,
      attributes: [],
      childNodes: [{
        nodeName: 'url',
        nodeType: 1,
        attributes: [],
        childNodes: [{
          nodeName: '#text',
          attributes: [],
          nodeType: 3,
          nodeValue: 'http://www.example.com'
        }]
      }]
    }]
  };
  fakeDom.firstChild = fakeDom.childNodes[0];
  fakeDom.firstChild.firstChild = fakeDom.firstChild.childNodes[0];
  fakeDom.firstChild.firstChild.firstChild = fakeDom.firstChild.firstChild.childNodes[0];

  var result = gadgets.json.xml.convertXmlToJson(fakeDom),
      expected = {embed:{url:'http://www.example.com'}};
  this.assertEquals(expected, result);
};
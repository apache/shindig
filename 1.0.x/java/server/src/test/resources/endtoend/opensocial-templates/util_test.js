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
 * Unit test for various DOM utils.
 */
function testDomUtils() {
  var sourceNode = document.getElementById('domSource');
  var targetNode = document.getElementById('domTarget');
  var html = sourceNode.innerHTML;
  targetNode.innerHTML = '';

  // test appendChildren
  os.appendChildren(sourceNode, targetNode);
  assertEquals(html, targetNode.innerHTML);

  // test removeChildren
  os.removeChildren(targetNode);
  assertEquals(0, targetNode.childNodes.length);

  // test replaceNode
  var child = document.createElement('p');
  sourceNode.appendChild(child);
  os.replaceNode(child, document.createElement('div'));
  assertEquals('DIV', sourceNode.firstChild.tagName);
}

/**
 * Unit test for createPropertyGetter
 */
function testGetPropertyGetterName() {
  assertEquals('getFoo', os.getPropertyGetterName('foo'));
  assertEquals('getFooBar', os.getPropertyGetterName('fooBar'));
}

/**
 * Unit test for convertConstantToCamelCase.
 */
function testConvertToCamelCase() {
  assertEquals('foo', os.convertToCamelCase('FOO'));
  assertEquals('fooBar', os.convertToCamelCase('FOO_BAR'));
  assertEquals('fooBarBaz', os.convertToCamelCase('FOO_BAR__BAZ'));
}

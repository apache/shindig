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
 * Unit test for compiler identifier wrapping.
 * TODO(kjin): test all of the following:
 *    "'a'",
 *    "foo",
 *    "foo + bar",
 *    "foo||bar",
 *    "foo.bar",
 *    "foo().bar",
 *    "foo.bar(baz)",
 *    "foo.bar().baz",
 *    "foo('a').bar",
 *    "foo[bar].baz",
 *    "foo.bar.baz",
 *    "$my('foo').bar",
 *    "$cur($context, 'person').ProfileName",
 *    "foo(bar)[baz]"
 */
function testWrapIdentifiers() {
  assertEquals("$_ir($_ir($context, 'foo'), 'bar')", 
      os.wrapIdentifiersInExpression("foo.bar"));

  assertEquals("$_ir($_ir($context, 'data'), 'array')()",
      os.wrapIdentifiersInExpression("data.array()"));

  assertEquals("$_ir($_ir($context, 'data')(), 'array')", 
      os.wrapIdentifiersInExpression('data().array'));

  // Check that namespaced tags are treated as single identifiers.      
  assertEquals("$_ir($context, 'os:Item')", 
      os.wrapIdentifiersInExpression("os:Item"));
      
  // Check that a colon surrounded by spaces is not treated as 
  // part of identifier 
  assertEquals("$_ir($context, 'foo') ? $_ir($context, 'bar') : " + 
      "$_ir($context, 'baz')",
      os.wrapIdentifiersInExpression("foo ? bar : baz"));
}

function testTransformVariables() {
  assertEquals("$this.foo", os.transformVariables_('$cur.foo'));
}

/**
 * Unit test for JSP operator support.
 */
function testOperators() {
  var data = {A:42, B:101};

  var testData = [
    { template:"${A lt B}", expected:"true" },
    { template:"${A gt B}", expected:"false" },
    { template:"${A eq A}", expected:"true" },
    { template:"${A neq A}", expected:"false" },
    { template:"${A lte A}", expected:"true" },
    { template:"${A lte B}", expected:"true" },
    { template:"${A gte B}", expected:"false" },
    { template:"${A gte A}", expected:"true" },
    { template:"${A eq " + data.A + "}", expected:"true" },
    { template:"${(A eq A) ? 'PASS' : 'FAIL'}", expected:"PASS" },
    { template:"${not true}", expected:"false" },
    { template:"${A eq A and B eq B}", expected:"true" },
    { template:"${A eq A and false}", expected:"false" },
    { template:"${false or A eq A}", expected:"true" },
    { template:"${false or false}", expected:"false" }
    //TODO: precedence, parenthesis
  ];

  for (var i = 0; i < testData.length; i++) {
    var testEntry = testData[i];
    var template = os.compileTemplateString(testEntry.template);
    var resultNode = template.render(data);
    var resultStr = resultNode.firstChild.innerHTML;
    assertEquals(resultStr, testEntry.expected);
  }
}

function testCopyAttributes() {
  var src = document.createElement('div');
  var dst = document.createElement('div');
  src.setAttribute('attr', 'test');
  src.setAttribute('class', 'foo');
  os.copyAttributes_(src, dst);
  assertEquals('test', dst.getAttribute('attr'));
  assertEquals('foo', dst.getAttribute('className'));
  assertEquals('foo', dst.className);
};

/**
 * Tests TBODY injection.
 */
function testTbodyInjection() {
  var src, check, template, output;
  
  // One row.
  src = "<table><tr><td>foo</td></tr></table>";
  check = "<table><tbody><tr><td>foo</td></tr></tbody></table>";
  template = os.compileTemplateString(src);
  output = template.templateRoot_.innerHTML;
  output = output.toLowerCase();
  output = output.replace(/\s/g, '');
  assertEquals(check, output);
  
  // Two rows.
  src = "<table><tr><td>foo</td></tr><tr><td>bar</td></tr></table>";
  check = "<table><tbody><tr><td>foo</td></tr>" + 
      "<tr><td>bar</td></tr></tbody></table>";
  template = os.compileTemplateString(src);
  output = template.templateRoot_.innerHTML;
  output = output.toLowerCase();
  output = output.replace(/\s/g, '');
  assertEquals(check, output);  
};

function testEventHandlers() {
  var src, template, output;
  
  window['testEvent'] = function(value) {
    window['testValue'] = value;
  };
  
  // Static handler
  src = "<button onclick=\"testEvent(true)\">Foo</button>";
  template = os.compileTemplateString(src);
  output = template.render();
  // Append to document to enable events
  document.body.appendChild(output);
  window['testValue'] = false;
  output.firstChild.click();
  document.body.removeChild(output);
  assertEquals(true, window['testValue']);
  
  // Dynamic handler
  src = "<button onclick=\"testEvent('${title}')\">Foo</button>";
  template = os.compileTemplateString(src);
  output = template.render({ title: 'foo' });
  // Append to document to enable events
  document.body.appendChild(output);
  window['testValue'] = false;
  output.firstChild.click();
  document.body.removeChild(output);
  assertEquals('foo', window['testValue']); 
};

function testNestedIndex() {
  var src, template, output;
  
  src = '<table><tr repeat="list" var="row" index="x">' + 
      '<td repeat="row" index="y">${x},${y}</td></tr></table>';
  template = os.compileTemplateString(src);
  output = template.render({ list: [ ['a', 'b'], ['c', 'd'] ] });
  //                           table  /  tbody  /   tr    /   td
  assertEquals('1,1', output.lastChild.lastChild.lastChild.lastChild.innerHTML);
};

function testLoopNullDefaultValue() {
  var src = '<div repeat="foo">a</div>';
  var template = os.compileTemplateString(src);
  var select = template.templateRoot_.firstChild.getAttribute("jsselect");
  assertEquals("$_ir($context, 'foo', null)", select); 
};
/*
function testEmbed() {
  var src, template, output;
  
  src = '<embed sRc="http://www.youtube.com/v/${$my.movie}&amp;hl=en" type="application/x-shockwave-flash" wmode="transparent" height="${$my.height}" width="${$my.width}"/>';
  template = os.compileTemplateString(src);
  src = '<img sRc="http://www.youtube.com/v/${$my.movie}&amp;hl=en" type="application/x-shockwave-flash" wmode="transparent" height="${$my.height}" width="${$my.width}"/>';
  var template2 = os.compileTemplateString(src);
  output = template.render();
}
*/

function testGetFromContext() {
  // JSON context
  var context = { foo: 'bar' };
  assertEquals('bar', os.getFromContext(context, 'foo'));
  
  // JsEvalContext  
  context = os.createContext(context);
  assertEquals('bar', os.getFromContext(context, 'foo'));
  
  // Variable from context
  context.setVariable('baz', 'bing');
  assertEquals('bing', os.getFromContext(context, 'baz'));
  
  // Non-existent value
  assertEquals('', os.getFromContext(context, 'title'));
  
  // Non-existent value with default
  assertEquals(null, os.getFromContext(context, 'title', null));
};

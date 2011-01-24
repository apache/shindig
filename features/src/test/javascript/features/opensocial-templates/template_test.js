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
 * Helper functions.
 * @param {string} templateId The id of template definition node.
 * @param {Object} opt_context The context data.
 * @return {Element} The rendered HTML element.
 * @private
 */
function compileAndRender_(templateId, opt_context) {
  var template = os.compileTemplate(document.getElementById(templateId));

  // Process the template and output the result
  var outputNode = document.createElement("div");
  template.renderInto(outputNode, opt_context);
  return outputNode;
}

/**
 * Takes a string representing a the markup of single DOM node, and returns
 * the corresponding DOM node.
 *
 * @param {string} markup Markup text of the DOM node
 * @return {Node} The DOM node
 */
function markupToNode(markup) {
  var node = document.createElement('div');
  node.innerHTML = markup;
  return node.firstChild;
}

/**
 * Finds if an Attr object is real (specified and not inserted by JST).
 * The JST attributes start with 'js', but IE also has a special '__jstcache'
 * attribute.
 * @param {Node} attr The Attr node object.
 */
function isRealAttribute(attr) {
  return attr.specified &&
         attr.name.indexOf('js') != 0 &&
         attr.name != '__jstcache';
};

/**
 * Normalize a node to the corresponding markup text, ignoring
 *     JsTemplate artifacts:
 * - Removes attributes starting with "js"
 * - Removes the SPAN tag if it has a "customtag" attribute or has no attributes
 *   that don't start with "js". This leaves the contents of the tag
 * - Removes nodes with style="display: none;", which is what JsTemplate does
 *   to nodes that aren't officially output.
 *
 * @param {Node} node The DOM node
 * @return {string} The normalized markup
 */
function nodeToNormalizedMarkup(node) {
  if (node.nodeType == 3) {
    return node.nodeValue;
  } else if (node.nodeType == 1) {
    var hasRealAttributes = false;

    for (var i = 0; i < node.attributes.length; i++) {
      if (isRealAttribute(node.attributes[i])) {
        hasRealAttributes = true;
      }
    }

    if (node.getAttribute('customtag') != null) {
      hasRealAttributes = false;
    }

    if (node.nodeName == 'SPAN' && !hasRealAttributes) {
      var text = '';
      for (var i = 0; i < node.childNodes.length; i++) {
        text += nodeToNormalizedMarkup(node.childNodes[i]);
      }
      return text;
    }
    if (node.style.display == 'none') {
      return '';
    }

    var text = '<' + node.nodeName;
    for (var i = 0; i < node.attributes.length; i++) {
      var att = node.attributes[i];
      if (isRealAttribute(att)) {
        text += ' ' + att.name + '="' + att.value + '"';
      }
    }
    text += '>';
    for (var i = 0; i < node.childNodes.length; i++) {
      text += nodeToNormalizedMarkup(node.childNodes[i]);
    }
    text += '</' + node.nodeName + '>';
    return text;
  }
  return '';
}

/**
 * Normalizes a node or text string to normalized text of the DOM node
 *
 * @param {Node|string} nodeOrText The DOM node or text of the DOM node
 * @return {string} The normalized markup text
 *
 */
function normalizeNodeOrMarkup(nodeOrText) {
  var node = typeof nodeOrText == 'string'
    ? markupToNode(nodeOrText) : nodeOrText;
  return nodeToNormalizedMarkup(node);
}


/*
 * Checks if two DOM node are equal, ingoring template artefacts.
 *
 * @param {Node|string} lhs First DOM node or string of markup contents
 * @param {Node|string} rhs Second DOM node or string of markup contents
 */
function assertTemplateDomEquals(lhs, rhs) {
  lhs = normalizeNodeOrMarkup(lhs);
  rhs = normalizeNodeOrMarkup(rhs);

  assertEquals(lhs, rhs);
}


/**
 * Allow testing of templates passed in a strings. Allows for calling
 * a template, passing in a map of named templates, and passing in the data
 * context.
 *
 * @param {string} templateText The text of the inline template to evaluate
 * @param {string} output The expected output
 * @param {Object=} context The data context
 * @param {Array<String>=} namedTemplates Array of text of namedTemplates
 */
function assertTemplateOutput(templateText, output, context,
    namedTemplates) {

  // Parse and register named templates
  if (namedTemplates instanceof Array) {
    for (var i = 0; i < namedTemplates.length; i++) {
      var text = '<Templates xmlns:os="uri:unused">' + namedTemplates[i] +
          '</Templates>';
      var dom = opensocial.xmlutil.parseXML(text);
      os.Loader.processTemplatesNode(dom);
    }
  }

  var template = os.compileTemplateString(templateText);

  // Process the template and output the result
  var outputNode = document.createElement("div");
  template.renderInto(outputNode, context);
  assertTemplateDomEquals(output, outputNode.firstChild);
}

/**
 * Tests Namespace.
 */
function testNamespace() {
  // Create the "custom" namespace
  var custom = os.createNamespace("custom", "http://google.com/#custom");
  assertEquals(custom, os.getNamespace("custom"));

  var custom_sameUrl =
    os.createNamespace("custom", "http://google.com/#custom");
  assertEquals(custom_sameUrl, os.getNamespace("custom"));

  try {
    var custom_newUrl =
      os.createNamespace("custom", "http://google.com/#custom_new");
    fail("no exception thrown with new URL for the same namespace");
  }
  catch (e) {
    // We expect os to throw an exception for namespace conflict.
    // But if e is a JsUnitException (thrown from fail), throw it again.
    if (e.isJsUnitException) {
      throw e;
    }
  }
}

/**
 * Tests Substitution.
 */
function testSubstitution_text() {
  var data = {
    title: "count",
    value: 0
  };
  assertTemplateOutput('<div>${title}:${value}</div>',
    '<div>' + data.title + ":" + data.value + '</div>',
    data);
}

function testSubstitution_attribute() {
  var data = {
    id: "varInAttr",
    color: "red",
    A1: 111,
    text: "click me"
  };
  var outputNode = compileAndRender_("_T_Substitution_attribute", data);
  var contentNode = outputNode.firstChild;

  assertEquals(data.id, contentNode.id);
  assertEquals(data.color, contentNode.style.color);
  assertEquals("value " + data.A1, contentNode.getAttribute("a1"));
  assertEquals(data.text, contentNode.innerHTML);
}

function testSubstitution_nested() {
  var data = {
    title: "Users",
    users: [
      { title: "President", color: 'red',
        user: { name: "Bob", id: "101", url: "http://www.bob.com" }},
      { title: "Manager", color: 'green',
        user: { name: "Rob", id: "102", url: "http://www.rob.com" }},
      { title: "Peon", color: 'blue',
        user: { name: "Jeb", id: "103", url: "http://www.jeb.com" }}
    ]
  };

  os.createNamespace("my", "www.google.com/#my");
  os.Container.registerTag('my:user');
  os.Container.registerTag('my:record');

  var outputNode = compileAndRender_("_T_Substitution_nested", data);

  assertEquals(data.users.length, outputNode.childNodes.length);
  for (var i = 0; i < data.users.length; i++) {
    var user = data.users[i];
    var divNode = outputNode.childNodes[i];
    assertEquals("DIV", divNode.tagName);

    // Find first Element child. FF creates an empty #text node, IE does not,
    // so we need to look.
    var spanNode = divNode.firstChild;
    while (!spanNode.tagName) {
      spanNode = spanNode.nextSibling;
    }

    assertEquals(user.color, spanNode.color);
    assertEquals(user.user.id, spanNode.foo);

    var recordNode = spanNode.childNodes[0];
    assertEquals(user.color, recordNode.firstChild.style.color);
    assertEquals(user.title, recordNode.firstChild.innerHTML);

    var userNode = recordNode.lastChild;
    assertEquals(user.user.id, userNode.foo);
    var anchorNode = userNode.firstChild.childNodes[0];
    assertEquals(user.user.name, anchorNode.innerHTML);
    assertContains(user.user.url, anchorNode.href);

    var fooNode = userNode.firstChild.childNodes[2];
    assertEquals(user.user.id, fooNode.innerHTML);
  }
}

/**
 * Tests if attribute.
 */
function testConditional_Number() {
  var outputNode = os.compileTemplateString(
      '<span if="42==42">TRUE</span><span if="!(42==42)">FALSE</span>'
      ).render();
  assertEquals("TRUE", domutil.getVisibleTextTrim(outputNode));
}

function testConditional_String() {
  var outputNode = os.compileTemplateString(
      "<span if=\"'101'=='101'\">TRUE</span><span if=\"'101'!='101'\">FALSE</span>"
      ).render();
  assertEquals("TRUE", domutil.getVisibleTextTrim(outputNode));
}

function testConditional_Mixed() {
  var outputNode = os.compileTemplateString(
      "<span if=\"'101' gt 42\">TRUE</span><span if=\"'101' lt 42\">FALSE</span>"
      ).render();
  assertEquals("TRUE", domutil.getVisibleTextTrim(outputNode));
}

/**
 * Tests repeat attribute.
 */
function testRepeat() {
  var data = {
    entries : [
      { data: "This" },
      { data: "is" },
      { data: "an" },
      { data: "array" },
      { data: "of" },
      { data: "data." }
    ]
  };
  var outputNode = compileAndRender_("_T_Repeat", data);

  assertEquals(data.entries.length, outputNode.childNodes.length);
  for (var i = 0; i < data.entries.length; i++) {
    var entry = data.entries[i];
    assertEquals("DIV", outputNode.childNodes[i].tagName);
    assertEquals(entry.data,
      domutil.getVisibleTextTrim(outputNode.childNodes[i]));
  }
}

/**
 * Tests select elements.
 */
function testSelect() {
  var data = {
    options : [
      { value: "one" },
      { value: "two" },
      { value: "three" }
    ]
  };
  var outputNode = compileAndRender_("_T_Options", data);
  var selectNode = outputNode.firstChild;

  assertEquals(data.options.length, selectNode.options.length);
  for (var i = 0; i < data.options.length; i++) {
    var entry = data.options[i];
    var optionNode = selectNode.options[i];
    assertEquals("OPTION", optionNode.tagName);
    assertEquals(entry.value, optionNode.getAttribute('value'));
  }
}

function testList() {
  os.Container.registerTag('custom:list');
  var output = compileAndRender_('_T_List');
  assertEquals('helloworld', domutil.getVisibleText(output));
}

/**
 * Tests JS custom tags.
 */
function testTag_input() {
  var custom = os.createNamespace("custom", "http://google.com/#custom");
  /**
   * Custom controller that uses the value of any input fields as a key to
   * replace itself with in the context data.
   */
  custom.input = function(node, context) { // return HTML;
    var inputNode = document.createElement('input');

    // Use the "value" attribute from the tag to index the context data
    inputNode.value = context[node.getAttribute('value')];
    return inputNode;
  };

  var data = {
    data: "Some default data"
  };
  var template = os.compileTemplateString("<custom:input value=\"data\" cur=\"Cur\"/>");
  var output = template.render(data);

  // extract contentNode
  var contentNode = output.getElementsByTagName("input")[0];

  assertEquals(contentNode.value, data.data);
}

function testHelloWorld() {
  assertTemplateOutput(
    '<div>Hello world!</div>',
    '<div>Hello world!</div>');
}

function testSimpleExpression() {
  assertTemplateOutput(
    '<div>${HelloWorld}</div>',
    '<div>Hello world!</div>',
    {HelloWorld: 'Hello world!'});
}

function testNamedTemplate() {
  assertTemplateOutput(
    '<div><os:HelloWorld/></div>',
    '<div>Hello world!</div>',
    null,
    ['<Template tag="os:HelloWorld">Hello world!</Template>']);
}

function testParameter() {
  var tryTemplateContent = function(content) {
    assertTemplateOutput(
      '<div><os:HelloWorldWithParam text="Hello world!"/></div>',
      '<div>Hello world!</div>',
      null,
      ['<Template tag="os:HelloWorldWithParam">' + content + '</Template>']);
  };

  tryTemplateContent('${$my.text}');
  tryTemplateContent('${My.text}');
  tryTemplateContent('${my.text}');

  // Not working yet:
  /*
  tryTemplateContent('${text}');
  */
}

function testContent() {
  var tryTemplateContent = function(content) {
    assertTemplateOutput(
      '<div><os:HelloWorldWithContent>Hello world!' +
      '</os:HelloWorldWithContent></div>',
      '<div>Hello world!</div>',
      null,
      ['<Template tag="os:HelloWorldWithContent">' + content + '</Template>']);
  };

  tryTemplateContent('<os:Render/>');
}

function testNamedContent() {
  var tryTemplateContent = function(content) {
    assertTemplateOutput(
      '<div>' +
      '<os:HelloWorldWithNamedContent>' +
        '<os:DontShowThis>Don\'t show this</os:DontShowThis>' +
        '<os:Foo>Hello <b>world!</b></os:Foo>' +
        '<Content>Hello <b>world!</b></Content>' +
      '</os:HelloWorldWithNamedContent>' +
      '</div>',

      '<div>Hello <b>world!</b></div>',
      null,
      ['<Template tag="os:HelloWorldWithNamedContent">' + content + '</Template>']);
  };
  tryTemplateContent('<os:Render content="os:Foo"/>');
  tryTemplateContent('<os:Render content="Content"/>');

  // Not working yet:
  /*
  tryTemplateContent('<os:Render content="${my.os:Content}"/>');
  tryTemplateContent('<os:Render content="my.os:Content"/>');
  tryTemplateContent('<os:Render content="${My.os:Content}"/>');
  tryTemplateContent('<os:Render content="${my.Content}"/>');
  tryTemplateContent('<os:Render content="my.Content"/>');
  tryTemplateContent('<os:Render content="${My.Content}"/>');
  tryTemplateContent('<os:Render content="${Content}"/>');
  */
}

function testRepeatedContent() {
  var tryTemplateContent = function(content) {
    assertTemplateOutput(
      '<os:HelloWorldRepeatedContent>' +
        '<Word>Hello</Word>' +
        '<os:Word>world!</os:Word>' +
      '</os:HelloWorldRepeatedContent>',

      '<div>Helloworld!</div>',
      null,
      ['<Template tag="os:HelloWorldRepeatedContent">' + content + '</Template>']);
  };

  tryTemplateContent('<div><span repeat="${My.Word}"><os:Render/></span></div>');
  tryTemplateContent('<div><span repeat="${Word}"><os:Render/></span></div>');

  // Not working yet because $my must be explicit:
  /*
    tryTemplateContent('<div><span repeat="${Word}"><os:Render/></span></div>');
    tryTemplateContent('<div><span repeat="${os:Word}"><os:Render/></span></div>');
    tryTemplateContent('<div><span repeat="Word"><os:Render/></span></div>');
    tryTemplateContent('<div><span repeat="os:Word"><os:Render/></span></div>');
  */
};

/**
 * Bug when calling a repeat twice - 2nd time fails
 *
 * This is because <os:Render> moves the child tags out to destination, so
 * the second loop is empty.
 */
function testRepeatedContentTwice() {
  /*
  assertTemplateOutput(
    '<os:HelloWorldRepeatedContent>' +
      '<os:Word>Hello</os:Word>' +
      '<os:Word>world!</os:Word>' +
    '</os:HelloWorldRepeatedContent>',

    '<div><div>Helloworld!</div><div>Helloworld!</div></div>',
    null,
    ['<Template tag="os:HelloWorldRepeatedContent">
      '<div>' +
      '<div><span repeat="$my.os:Word"><os:Render/></span></div>' +
      '<div><span repeat="$my.os:Word"><os:Render/></span></div>' +
      '</Template>']
    );
  */
};

/**
 * Currently, expression inside "content" attribute is equiv of no attribute.
 * Probably should just include no data.
 *
 * I.e.
 * <os:Render content="${os:NoMatch}"/> currently has same output as
 * <os:Render/>
 */
function testRenderAllBadExprInContent() {
  /*
  assertTemplateOutput(
    '<div>' +
    '<os:HelloWorldBadExpr>' +
      '<os:Content>Hello world!</os:Content>' +
    '</os:HelloWorldBadExpr>' +
    '</div>',

    '<div></div>',
    null,
    ['<Template tag="os:HelloWorldBadExpr">' +
     '<os:Render content="${os:NoMatch}"/>' +
     '</Template>']);
  */
}


function testBooleanTrue() {
  assertTemplateOutput(
    '<span if="${BooleanTrue}">Hello world!</span>',
    '<span>Hello world!</span>',
    {BooleanTrue: true});

  assertTemplateOutput(
    '<span if="BooleanTrue">Hello world!</span>',
    '<span>Hello world!</span>',
    {BooleanTrue: true});

  assertTemplateOutput(
    '<span if="!BooleanTrue">Hello world!</span>',
    '<span></span>',
    {BooleanTrue: true});

  assertTemplateOutput(
    '<span if="${!BooleanTrue}">Hello world!</span>',
    '<span></span>',
    {BooleanTrue: true});
}


function testBooleanFalse() {
  assertTemplateOutput(
    '<span if="BooleanFalse">Hello world!</span>',
    '<span></span>',
    {BooleanFalse: false});

  assertTemplateOutput(
    '<span if="!BooleanFalse">Hello world!</span>',
    '<span>Hello world!</span>',
    {BooleanFalse: false});

  assertTemplateOutput(
    '<span if="${!BooleanFalse}">Hello world!</span>',
    '<span>Hello world!</span>',
    {BooleanFalse: false});

  assertTemplateOutput(
    '<span if="${BooleanFalse}">Hello world!</span>',
    '<span></span>',
    {BooleanFalse: false});
}


function testRepeatedNode() {
  var tryTemplateContent = function(content) {
    assertTemplateOutput(
      content,
      '<div>Helloworld!</div>',
      {
        Words: ['Hello', 'world!'],
        WordObjects: [{value: 'Hello'}, {value: 'world!'}]
      });
  };

  tryTemplateContent('<div><span repeat="WordObjects">${$cur.value}</span></div>');
  tryTemplateContent('<div><span repeat="WordObjects">${value}</span></div>');
  tryTemplateContent('<div><span repeat="WordObjects">${cur.value}</span></div>');
  tryTemplateContent('<div><span repeat="Words">${cur}</span></div>');
  tryTemplateContent('<div><span repeat="Words">${$cur}</span></div>');
  tryTemplateContent('<div><span repeat="Words">${Cur}</span></div>');

  // Do we want to continue to support this?
  tryTemplateContent('<div><span repeat="Words">${$this}</span></div>');
};

function testDynamicRepeatedContent() {

  assertTemplateOutput(
    '<os:DynamicRepeat>' +
      '<Word repeat="WordObjects">${Cur.value}</Word>' +
    '</os:DynamicRepeat>',
    '<div>Helloworld!</div>',
    {WordObjects: [{value: 'Hello'}, {value: 'world!'}]},

    ['<Template tag="os:DynamicRepeat">' +
     '<div><span repeat="My.Word"><os:Render/></span></div>' +
     '</Template>']);

};

function testReplaceTopLevelVars() {
  function test(src, dest) {
    assertEquals(dest, os.replaceTopLevelVars_(src));
  }

  // Basic substitution for each replacement
  test('my.man', '$my.man');
  test('my', '$my');
  test('My.man', '$my.man');
  test('My', '$my');
  test('cur.man', '$this.man');
  test('cur', '$this');
  test('Cur.man', '$this.man');
  test('Cur', '$this');

  // Basic no sustitution
  test('$my.man', '$my.man');
  test('$my', '$my');
  test('ns.My', 'ns.My');
  test('Cur/2', '$this/2');
  test('Cur*2', '$this*2');
  test('Cur[My.name]', '$this[$my.name]');
  test('Cur||\'Nothing\'', '$this||\'Nothing\'');

  // Single operator, both fist and last expression
  test('My.man+your.man', '$my.man+your.man');
  test('your.man>My.man', 'your.man>$my.man');

  // Tests a specific operator
  function testOperator(operator) {
    test('My.man' + operator + 'your.man',
        '$my.man' + operator + 'your.man');

    test('your.man' + operator + 'My.man',
        'your.man' + operator + '$my.man');

    test('My' + operator + 'My',
        '$my' + operator + '$my');
  }

  // All operators
  testOperator('+');
  testOperator(' + ');
  testOperator('-');
  testOperator('<');
  testOperator(' lt ');
  testOperator('>');
  testOperator(' gt ');
  testOperator('=');
  testOperator('!=');
  testOperator('==');
  testOperator('&&');
  testOperator(' and ');
  testOperator('||');
  testOperator(' or ');
  testOperator(' and !');
  testOperator('/');
  testOperator('*');
  testOperator('|');
  testOperator('(');
  testOperator('[');
};

function testHtmlTag() {
  var template = os.compileTemplateString('<os:Html code="${foo}"/>');
  var output = template.render({foo: 'Hello <b>world</b>!'});
  var boldNodes = output.getElementsByTagName("b");
  assertEquals(1, boldNodes.length);
};

function testOnAttachAttribute() {
  var template = os.compileTemplateString(
      '<div onAttach="this.title=\'bar\'"/>');
  var output = document.createElement('div');
  template.renderInto(output);
  assertEquals('bar', output.firstChild.title);
};

function testSpacesAmongTags() {
  var tryTemplateContent = function(templateText) {
   var output = os.compileTemplateString(templateText).render();
    assertEquals('Hello world!', domutil.getVisibleTextTrim(output));
  };

  os.Loader.loadContent('<Templates xmlns:os="uri:unused">' +
    '<Template tag="os:msg">${My.text}</Template></Templates>');

  tryTemplateContent('<div><os:msg text="Hello"/>\n' +
      ' <os:msg text="world!"/></div>');
  tryTemplateContent('<div><os:msg text="Hello"/>  ' +
      '<os:msg text="world!"/></div>');
  tryTemplateContent('<div> <os:msg text="Hello"/>  ' +
      '<os:msg text="world!"/>\n</div>');

  os.Loader.loadContent('<Templates xmlns:os="uri:unused">' +
    '<Template tag="os:msg"><os:Render/></Template></Templates>');

  tryTemplateContent('<div><os:msg>Hello</os:msg>\n' +
      ' <os:msg>world!</os:msg>\n</div>');
  tryTemplateContent('<div><os:msg>Hello</os:msg>' +
      '  <os:msg>world!</os:msg></div>');
  tryTemplateContent('<div>\n  <os:msg>Hello</os:msg>' +
      '  <os:msg>world!</os:msg>\n</div>');
};

function testVariablePrecedence() { 
  // TODO: Update tests to reflect @cur not propagating into custom tags  
  // Precedence should be ${Cur} -> ${My} -> ${Top}  
  var tryTemplateContent = function(templateText, data) {
    var output = os.compileTemplateString(templateText).render(data);
    assertEquals('Right', domutil.getVisibleTextTrim(output));
  };
  os.Loader.loadContent('<Templates xmlns:os="uri:unused">' + 
    '<Template tag="os:msg">${Value}</Template></Templates>');
  
  tryTemplateContent('<os:msg/>', { Value: 'Right' } );
  tryTemplateContent('<os:msg Value="Right"/>', { } );
  tryTemplateContent('<os:msg Value="Right"/>', { Value: 'Wrong' } );
  tryTemplateContent('<os:msg Value="Right" cur="${Top}"/>', { Value: 'Wrong' } );
  tryTemplateContent('<os:msg Value="${Value}"/>', { Value: 'Right' } );
};

function testOsRepeat() {
  var data = { list : [ { name: 'a' }, { name: 'b' }, { name: 'c' } ] };
  var output = os.compileTemplateString('<os:Repeat expression="${list}"><b>${name}</b> <b>${name}</b> </os:Repeat>').render(data);
  assertEquals("a a b b c c", domutil.getVisibleTextTrim(output));
  
  output = os.compileTemplateString('<select><os:Repeat expression="${list}"><option>${name}</option><option>${name} again</option></os:Repeat></select>').render(data);
  assertEquals(6, output.firstChild.options.length);
  
  output = os.compileTemplateString('<table><os:Repeat expression="${list}"><tr><td>${name}</td></tr><tr><td>${name} again</td></tr></os:Repeat></table>').render(data);
  assertEquals(6, output.firstChild.rows.length);
};

function testOsIf() {
  var data = { list : [ { name: 'a' }, { name: 'b' }, { name: 'c' } ] };
  var output = os.compileTemplateString('<os:Repeat expression="${list}"><os:If condition="${name != \'b\'}"><b>${name}</b> <b>${name}</b> </os:If></os:Repeat>').render(data);
  assertEquals("a a c c", domutil.getVisibleTextTrim(output));
};

function testOsVar() {

  assertTemplateOutput(
  '<div><os:Var key="counter" value="1" />${counter}</div>',
  '<div>1</div>',
  {});

  assertTemplateOutput(
  '<div><os:Var key="counter" value="1" /><os:Var key="counter" value="${counter + 1}" />${counter}</div>',
  '<div>2</div>',
  {});

  assertTemplateOutput(
  '<div><os:Var key="counter" value="[1,3,5,7]" />${counter[1]}</div>',
  '<div>3</div>',
  {});

  assertTemplateOutput(
  '<div><os:Var key="counter">{"key" : "value"}</os:Var>${counter.key}</div>',
  '<div>value</div>',
  {});
}

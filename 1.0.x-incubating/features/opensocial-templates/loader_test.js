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
 * Unit test for injecting JavaScript into the global scope with the 
 * os.Loader.
 */
function testInjectJavaScript() {
  var jsCode = "function testFunction() { return 'foo'; }";
  os.Loader.injectJavaScript(jsCode);
  assertTrue(window.testFunction instanceof Function);
  assertEquals(window.testFunction(), 'foo');
}

/**
 * Unit test for injecting CSS through the os.Loader.
 */
function testInjectStyle() {
  var cssCode = '.testCSS { width: 100px; height: 200px; }';
  os.Loader.injectStyle(cssCode);
  var rule = getStyleRule('.testCSS');
  assertNotNull(rule);
  assertEquals(rule.style.width, '100px');
  assertEquals(rule.style.height, '200px');
}

/**
 * @type {String} Template XML data for testLoadContent.
 */
var testContentXML =
    '<Templates xmlns:test="http://www.google.com/#test">' +
    '  <Namespace prefix="test" url="http://www.google.com/#test"/>' +
    '  <Template tag="test:tag">' +
    '    <div id="tag"></div>' +
    '  </Template>' +
    '  <JavaScript>' +
    '    function testJavaScript() {' +
    '      return "testJavaScript";' +
    '    }' +
    '  </JavaScript>' +
    '  <Style>' +
    '    .testStyle {' +
    '      width: 24px;' +
    '    }' +
    '  </Style>' +
    '  <TemplateDef tag="test:tagDef">' +
    '    <Template>' +
    '      <div id="tagDef"></div>' +
    '    </Template>' +
    '    <JavaScript>' +
    '      function testJavaScriptDef() {' +
    '        return "testJavaScriptDef";' +
    '      }' +
    '    </JavaScript>' +
    '    <Style>' +
    '      .testStyleDef {' +
    '        height: 42px;' +
    '      }' +
    '    </Style>' +
    '  </TemplateDef>' +
    '</Templates>';

/**
 * System test for os.loadContent functionality. This tests
 * all functionality except for XHR.
 */
function testLoadContent() {
  os.Loader.loadContent(testContentXML);

  // Verify registered tags.
  var ns = os.nsmap_['test'];
  assertNotNull(ns);
  assertTrue(ns['tag'] instanceof Function);
  assertTrue(ns['tagDef'] instanceof Function);

  // Verify JavaScript functions.
  assertTrue(window['testJavaScript'] instanceof Function);
  assertEquals(window.testJavaScript(), 'testJavaScript');
  assertTrue(window['testJavaScriptDef'] instanceof Function);
  assertEquals(window.testJavaScriptDef(), 'testJavaScriptDef');

  // Verify styles.
  var rule = getStyleRule('.testStyle');
  assertNotNull(rule);
  assertEquals(rule.style.width, '24px');
  var ruleDef = getStyleRule('.testStyleDef');
  assertNotNull(ruleDef);
  assertEquals(ruleDef.style.height, '42px');
}

/**
 * Utility function for retrieving a style rule by selector text
 * if its available.
 * @param {string} name Selector text name.
 * @return {Object} CSSRule object.
 */
function getStyleRule(name) {
  var sheets = document.styleSheets;
  for (var i = 0; i < sheets.length; ++i) {
    var rules = sheets[i].cssRules || sheets[i].rules;
    if (rules) {
      for (var j = 0; j < rules.length; ++j) {
        if (rules[j].selectorText == name
            //hack for WebKit Quirks mode
            || rules[j].selectorText == name.toLowerCase()) {
          return rules[j];
        }
      }
    }
  }
  return null;
}

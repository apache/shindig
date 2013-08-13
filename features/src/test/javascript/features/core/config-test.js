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

function ConfigTest(name) {
  TestCase.call(this, name);
}

ConfigTest.inherits(TestCase);

ConfigTest.prototype.setUp = function() {
  gadgets.config.clear();
  this.script1 = { nodeType: 3, src: "http://www.1.com/js/1.js?c=0" };
  this.script2 = { nodeType: 3, src: "http://www.2.com/js/2.js?c=1" };
  this.script3 = { nodeType: 3, src: "http://www.3.com/js/3.js?blah&c=0" };
  document.scripts = [ this.script1, this.script2, this.script3 ];
  this.defaultConfig = {
    'core.io': {
      jsPath: '/js',
      proxyUrl: '',
      jsonProxyUrl: 'a',
      unparseableCruft: ''
    },
    testBasic: { data: "Hello, World!", untouched: "Goodbye" },
    testSecond: { foo: "Bar" }
  };
};

ConfigTest.prototype.tearDown = function() {
  gadgets.config.update({}, true);  // "reset" gadgets lib
  window["___jsl"] = undefined;
  window["___config"] = undefined;
};

ConfigTest.prototype.testBasic = function() {
  var testBasicConfig;
  gadgets.config.register("testBasic", null, function(config) {
    testBasicConfig = config.testBasic;
  });

  gadgets.config.init(this.defaultConfig);

  this.assertEquals("Hello, World!", testBasicConfig.data);
};

ConfigTest.prototype.testMultiple = function() {
  var testMultiple0;
  gadgets.config.register("testMultiple", null, function(config) {
    testMultiple0 = config.testMultiple;
  });

  var testMultiple1;
  gadgets.config.register("testMultiple", null, function(config) {
    testMultiple1 = config.testMultiple;
  });

  gadgets.config.init({
    'core.io': {
      jsPath: '/js',
      proxyUrl: '',
      jsonProxyUrl: 'a',
      unparseableCruft: ''
    },
    testMultiple: {data: "Hello, World!"}
  });

  this.assertEquals("Hello, World!", testMultiple0.data);
  this.assertEquals("Hello, World!", testMultiple1.data);
};

ConfigTest.prototype.testFindScriptLastNoHint = function() {
  var testListen;
  gadgets.config.register("testBasic", null, function(config) {
    testListen = config;
  });
  this.script3.nodeValue = "{ testBasic: { data: 'News' } }";

  gadgets.config.init(this.defaultConfig);

  this.assertEquals("News", testListen.testBasic.data);
  this.assertEquals("Goodbye", testListen.testBasic.untouched);
};

ConfigTest.prototype.testFindScriptLastHintMismatch = function() {
  var testListen;
  gadgets.config.register("testBasic", null, function(config) {
    testListen = config;
  });
  this.script3.nodeValue = " testBasic: { data: 'AgainNew' } ";

  window["___jsl"] = { u: "http://nomatch.com/foo.js" };
  gadgets.config.init(this.defaultConfig);

  this.assertEquals("AgainNew", testListen.testBasic.data);
  this.assertEquals("Goodbye", testListen.testBasic.untouched);
};

ConfigTest.prototype.testFindScriptHintExact = function() {
  var testListen;
  gadgets.config.register("testBasic", null, function(config) {
    testListen = config;
  });
  this.script2.nodeValue = "data: 'Override'";
  window["___jsl"] = {f: [ "testBasic" ]};
  gadgets.config.init(this.defaultConfig);
  this.assertEquals("Override", testListen.testBasic.data);
  this.assertEquals("Goodbye", testListen.testBasic.untouched);
};

ConfigTest.prototype.testFindScriptHintPrefixMatch = function() {
  var testListen;
  gadgets.config.register("testBasic", null, function(config) {
    testListen = config;
  });
  this.script2.src += "#hash=1";
  this.script2.nodeValue = "testBasic: { data: 'Override' }, testSecond: [ 'difftype' ]";
  window["___jsl"] = {f: [ "testBasic", "testSecond" ]};
  gadgets.config.init(this.defaultConfig);
  this.assertEquals("Override", testListen.testBasic.data);
  this.assertEquals("Goodbye", testListen.testBasic.untouched);
  this.assertEquals("difftype", testListen.testSecond[0]);
};

ConfigTest.prototype.testInitMergesNotOverwrites = function() {
  var testListen;
  gadgets.config.register("testBasic", null, function(config) {
    testListen = config;
  });
  gadgets.config.init(this.defaultConfig);
  this.assertEquals("Hello, World!", testListen.testBasic.data);
  this.assertEquals("Goodbye", testListen.testBasic.untouched);
  this.assertEquals("Bar", testListen.testSecond.foo);

  gadgets.config.init({ testBasic: { data: "Override" } });
  this.assertEquals("Override", testListen.testBasic.data);
  this.assertEquals("Goodbye", testListen.testBasic.untouched);
  this.assertEquals("Bar", testListen.testSecond.foo);
};

ConfigTest.prototype.testUpdateMerge = function() {
  var testListen;
  gadgets.config.register("one", null, function(config) {
    testListen = config;
  });
  gadgets.config.init({
    'core.io': {
      jsPath: '/js',
      proxyUrl: '',
      jsonProxyUrl: 'a',
      unparseableCruft: ''
    },
    one: { oneKey1: { oneSubkey1: "oneVal1" }, oneKey2: "data" },
    two: "twoVal1"
  });
  this.assertEquals("oneVal1", testListen.one.oneKey1.oneSubkey1);
  this.assertEquals("data", testListen.one.oneKey2);
  this.assertEquals("twoVal1", testListen.two);
  gadgets.config.update({
    one: { oneKey1: { oneSubkey1: "updated", oneSubkey2: "newpair" } },
    two: [ "newtype" ],
    three: { foo: 123 }
  });
  testListen = gadgets.config.get();
  this.assertEquals("updated", testListen.one.oneKey1.oneSubkey1);
  this.assertEquals("newpair", testListen.one.oneKey1.oneSubkey2);
  this.assertEquals("data", testListen.one.oneKey2);
  this.assertEquals("newtype", testListen.two[0]);
  this.assertEquals(123, testListen.three.foo);
};

ConfigTest.prototype.testUpdateBeforeInit = function() {
  var testListen = null;
  gadgets.config.register("one", null, function(config) {
    testListen = config;
  });
  gadgets.config.update({
    one: { oneKey1: { oneSubkey1: "oneVal1", sticks: "stones" }, breaks: "bones" }
  });
  this.assertTrue(testListen === null);
  gadgets.config.init({
    'core.io': {
      jsPath: '/js',
      proxyUrl: '',
      jsonProxyUrl: 'a',
      unparseableCruft: ''
    },
    one: { oneKey1: { oneSubkey1: "overwrite" } }
  });
  this.assertEquals("overwrite", testListen.one.oneKey1.oneSubkey1);
  this.assertEquals("stones", testListen.one.oneKey1.sticks);
  this.assertEquals("bones", testListen.one.breaks);
};

ConfigTest.prototype.testMergeFromInlineConfig = function() {
  var testListen;
  gadgets.config.register("one", null, function(config) {
    testListen = config;
  });
  window["___config"] = { one: { oneKey1: { oneSubkey1: "override" } } };
  gadgets.config.init({
    'core.io': {
      jsPath: '/js',
      proxyUrl: '',
      jsonProxyUrl: 'a',
      unparseableCruft: ''
    },
    one: { oneKey1: { oneSubkey1: "oneVal1" }, oneKey2: "data" },
    two: "twoVal1"
  });
  this.assertEquals("override", testListen.one.oneKey1.oneSubkey1);
  this.assertEquals("data", testListen.one.oneKey2);
  this.assertEquals("twoVal1", testListen.two);
};

ConfigTest.prototype.testValidator = function() {
  var validatorValue;
  gadgets.config.register("testValidator", {data: function(value) {
    validatorValue = value;
    return true;
  }});

  gadgets.config.init({
    'core.io': {
      jsPath: '/js',
      proxyUrl: '',
      jsonProxyUrl: 'a',
      unparseableCruft: ''
    },
    testValidator: {data: "Hello, World!"}
  });

  this.assertEquals("Hello, World!", validatorValue);
};

ConfigTest.prototype.testValidatorMultiple = function() {
  var validatorValue0;
  gadgets.config.register("testValidator", {key0: function(value) {
    validatorValue0 = value;
    return true;
  }});

  var validatorValue1;
  gadgets.config.register("testValidator", {key1: function(value) {
    validatorValue1 = value;
    return true;
  }});

  gadgets.config.init({
    'core.io': {
      jsPath: '/js',
      proxyUrl: '',
      jsonProxyUrl: 'a',
      unparseableCruft: ''
    },
    testValidator: {key0: "Hello, World!", key1: "Goodbye, World!"}
  });

  this.assertEquals("Hello, World!", validatorValue0);
  this.assertEquals("Goodbye, World!", validatorValue1);
};

ConfigTest.prototype.testValidatorRejection = function() {
  gadgets.config.register("testValidatorRejection", {data: function(value) {
    return false;
  }});

  try {
    gadgets.config.init({testValidatorRejection: {data: "Hello, World!"}});
    this.fail("Did not throw an exception when validation failed.");
  } catch (e) {
    // Expected.
  }
};

ConfigTest.prototype.testValidatorDisabled = function() {
  var testValidatorDisabledConfig;
  gadgets.config.register("testValidatorDisabled", {data: function(value) {
    return false;
  }},
  function(config) {
    testValidatorDisabledConfig = config.testValidatorDisabled;
  });

  gadgets.config.init({
    'core.io': {
      jsPath: '/js',
      proxyUrl: '',
      jsonProxyUrl: 'a',
      unparseableCruft: ''
    },
    testValidatorDisabled: {data: "Hello, World!"}
  }, true);

  this.assertEquals("Hello, World!", testValidatorDisabledConfig.data);
};

ConfigTest.prototype.testEnumValidator = function() {
  var validator = gadgets.config.EnumValidator("foo", "bar", "baz");

  this.assertTrue(validator("foo"));
  this.assertTrue(validator("bar"));
  this.assertTrue(validator("baz"));
  this.assertFalse(validator("junk"));
};

ConfigTest.prototype.testRegExValidator = function() {
  var validator = gadgets.config.RegExValidator(/^hello.*$/);

  this.assertTrue(validator("hello"));
  this.assertTrue(validator("hello, world"));
  this.assertTrue(validator("hellothere"));
  this.assertFalse(validator("not hello"));
};

ConfigTest.prototype.testExistsValidator = function() {
  var validator = gadgets.config.ExistsValidator;

  this.assertTrue(validator("hello"));
  this.assertTrue(validator(0));
  this.assertTrue(validator(false));
  this.assertTrue(validator(null));
  this.assertTrue(validator(""));

  this.assertFalse(validator({}.foo));
};

ConfigTest.prototype.testNonEmptyStringValidator = function() {
  var validator = gadgets.config.NonEmptyStringValidator;

  this.assertTrue(validator("hello"));

  this.assertFalse(validator(0));
  this.assertFalse(validator(false));
  this.assertFalse(validator(null));
  this.assertFalse(validator(""));
  this.assertFalse(validator(undefined));
};

ConfigTest.prototype.testBooleanValidator = function() {
  var validator = gadgets.config.BooleanValidator;

  this.assertTrue(validator(true));
  this.assertTrue(validator(false));

  this.assertFalse(validator("hello"));
  this.assertFalse(validator(0));
  this.assertFalse(validator(null));
  this.assertFalse(validator(undefined));
};

ConfigTest.prototype.testLikeValidator = function() {
  var key0value, key1value;

  var validator = gadgets.config.LikeValidator({
    key0: function(data) {
      key0value = data;
      return true;
    },
    key1: function(data) {
      key1value = data;
      return true;
    }
  });

  this.assertTrue(validator({key0:"Key0", key1: "Key1"}));
  this.assertEquals("Key0", key0value);
  this.assertEquals("Key1", key1value);
};

ConfigTest.prototype.testLikeValidatorWithFailure = function() {
  var key0value, key1value;

  var validator = gadgets.config.LikeValidator({
    key0: function(data) {
      key0value = data;
      return false;
    },
    key1: function(data) {
      key1value = data;
      return true;
    }
  });

  this.assertFalse(validator({key0:"Key0", key1: "Key1"}));
  this.assertEquals("Key0", key0value);
  this.assertEquals(null, key1value);
};

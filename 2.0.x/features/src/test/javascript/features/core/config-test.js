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

ConfigTest.prototype.testBasic = function() {
  var testBasicConfig;
  gadgets.config.register("testBasic", null, function(config) {
    testBasicConfig = config.testBasic;
  });

  gadgets.config.init({testBasic: {data: "Hello, World!"}});

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

  gadgets.config.init({testMultiple: {data: "Hello, World!"}});

  this.assertEquals("Hello, World!", testMultiple0.data);
  this.assertEquals("Hello, World!", testMultiple1.data);
};

ConfigTest.prototype.testValidator = function() {
  var validatorValue;
  gadgets.config.register("testValidator", {data: function(value) {
    validatorValue = value;
    return true;
  }});

  gadgets.config.init({testValidator: {data: "Hello, World!"}});

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

  gadgets.config.init({testValidator: {key0: "Hello, World!", key1: "Goodbye, World!"}});

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

  gadgets.config.init({testValidatorDisabled: {data: "Hello, World!"}}, true);

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

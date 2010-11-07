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

var gadgets = gadgets || {};

function AuthTest(name) {
  TestCase.call(this, name);
};
AuthTest.inherits(TestCase);

AuthTest.prototype.setUp = function() {
  // Prepare for mocks
  gadgets.util = gadgets.util || {};
  gadgets.config = gadgets.config || {};
  this.oldGetUrlParameters = gadgets.util.getUrlParameters;
  this.oldConfigRegister = gadgets.config.register;
};

AuthTest.prototype.tearDown = function() {
  // Remove mocks
  gadgets.util.getUrlParameters = this.oldGetUrlParameters;
  gadgets.config.register = this.oldConfigRegister;
};

AuthTest.prototype.testTokenOnFragment = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({});
  };
  gadgets.util.getUrlParameters = function() {
    return { 'st' : 'authtoken' };
  };
  var auth = new shindig.Auth();
  this.assertEquals('authtoken', auth.getSecurityToken());
  this.assertNull(auth.getTrustedData());
  auth.updateSecurityToken('newtoken');
  this.assertEquals('newtoken', auth.getSecurityToken());
};

AuthTest.prototype.testTokenInConfig = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({ 'shindig.auth' : { 'authToken' : 'configAuthToken' }});
  };
  gadgets.util.getUrlParameters = function() {
    return { 'st' : 'fragmentAuthToken' };
  };
  var auth = new shindig.Auth();
  this.assertEquals('configAuthToken', auth.getSecurityToken());
  this.assertNull(auth.getTrustedData());
  auth.updateSecurityToken('newtoken');
  this.assertEquals('newtoken', auth.getSecurityToken());
};

AuthTest.prototype.testNoToken = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({ 'shindig.auth' : null });
  };
  gadgets.util.getUrlParameters = function() {
    return {};
  };
  var auth = new shindig.Auth();
  this.assertEquals(null, auth.getSecurityToken());
  this.assertNull(auth.getTrustedData());
  auth.updateSecurityToken('newtoken');
  this.assertEquals('newtoken', auth.getSecurityToken());
};

AuthTest.prototype.testAddParamsToToken_normal = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({});
  };
  gadgets.util.getUrlParameters = function() {
    return { 
      'st' : 't=abcd&url=$',
      'url' : 'http://www.example.com/gadget.xml'
    };
  };
  var auth = new shindig.Auth();
  this.assertEquals(
      't=abcd&url=http%3a%2f%2fwww.example.com%2fgadget.xml',
      auth.getSecurityToken());
  auth.updateSecurityToken('newtoken');
  this.assertEquals('newtoken', auth.getSecurityToken());
};

AuthTest.prototype.testAddParamsToToken_blankvalue = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({});
  };
  gadgets.util.getUrlParameters = function() {
    return { 
      'st' : 't=abcd&url=$&url=',
      'url' : 'http://www.example.com/gadget.xml'
    };
  };
  var auth = new shindig.Auth();
  this.assertEquals(
      't=abcd&url=http%3a%2f%2fwww.example.com%2fgadget.xml&url=',
      auth.getSecurityToken());
};

AuthTest.prototype.testAddParamsToToken_dupname = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({});
  };
  gadgets.util.getUrlParameters = function() {
    return { 
      'st' : 't=abcd&url=$&url=$',
      'url' : 'http://www.example.com/gadget.xml'
    };
  };
  var auth = new shindig.Auth();
  this.assertEquals(
      't=abcd&url=http%3a%2f%2fwww.example.com%2fgadget.xml&url=' + 
          'http%3a%2f%2fwww.example.com%2fgadget.xml',
      auth.getSecurityToken());
};

AuthTest.prototype.testAddParamsToToken_blankname = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({});
  };
  gadgets.util.getUrlParameters = function() {
    return { 
      'st' : 't=abcd&=&url=$',
      'url' : 'http://www.example.com/gadget.xml'
    };
  };
  var auth = new shindig.Auth();
  this.assertEquals(
      't=abcd&=&url=http%3a%2f%2fwww.example.com%2fgadget.xml',
      auth.getSecurityToken());
};

AuthTest.prototype.testAddParamsToToken_nonpaired = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({});
  };
  gadgets.util.getUrlParameters = function() {
    return { 
      'st' : 't=abcd&foo&url=$',
      'url' : 'http://www.example.com/gadget.xml'
    };
  };
  var auth = new shindig.Auth();
  this.assertEquals(
      't=abcd&foo&url=http%3a%2f%2fwww.example.com%2fgadget.xml',
      auth.getSecurityToken());
};

AuthTest.prototype.testAddParamsToToken_extraequals = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({});
  };
  gadgets.util.getUrlParameters = function() {
    return { 
      'st' : 't=abcd&foo=$bar$=$baz$&url=$',
      'url' : 'http://www.example.com/gadget.xml'
    };
  };
  var auth = new shindig.Auth();
  this.assertEquals(
      't=abcd&foo=$bar$=$baz$&url=http%3a%2f%2fwww.example.com%2fgadget.xml',
      auth.getSecurityToken());
};

AuthTest.prototype.testTrustedJson = function() {
  gadgets.config.register = function(name, validator, callback) {
    callback({ 'shindig.auth' : { 'trustedJson' : '{ "foo" : "bar" }' }});
  };
  gadgets.util.getUrlParameters = function() {
    return { 
      'st' : 't=abcd&foo=$bar$=$baz$&url=$',
      'url' : 'http://www.example.com/gadget.xml'
    };
  };
  var auth = new shindig.Auth();
  this.assertEquals('bar', auth.getTrustedData().foo);
};

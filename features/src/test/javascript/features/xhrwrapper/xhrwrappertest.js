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
 *
 * Unittests for the xhrwrapper feature.
 */

var shindig = shindig || {};

function XhrWrapperTest(name) {
  TestCase.call(this, name);
}
XhrWrapperTest.inherits(TestCase);

XhrWrapperTest.prototype.setUp = function() {
  // prepare mocks
  gadgets.io = gadgets.io || {};
  window.location = window.location || {};
  opensocial.xmlutil = opensocial.xmlutil || {};
  this.oldMakeRequest = gadgets.io.makeRequest;
  this.oldWindowLocation = window.location;
  this.oldParseXML = opensocial.xmlutil.parseXML;
  this.madeRequest = {};
  gadgets.io.makeRequest = this.mockMakeRequest(this.madeRequest);
  window.location = { 'href': 'http://shindig/gadgets/ifr?url=blah' };
  opensocial.xmlutil.parseXML = XhrWrapperTest.mockParseXML;
  document.scripts = [];
  gadgets.config.init(
    {"shindig.xhrwrapper": {"contentUrl": "http://foo.bar/baz/bax.html"}});
};

XhrWrapperTest.prototype.tearDown = function() {
  // remove mocks
  gadgets.io.makeRequest = this.oldMakeRequest;
  window.location = this.oldWindowLocation;
  opensocial.xmlutil.parseXML = this.oldParseXML;
};

XhrWrapperTest.prototype.testBasicWorking = function() {
  var that = this;
  var calledCallback = false;
  var xhr = new shindig.xhrwrapper.XhrWrapper();
  xhr.open('GET', 'http://foo.bar');
  xhr.onreadystatechange = function(e) {
    that.assertEquals('readystatechange', e.type);
    that.assertEquals(xhr, e.target);
    calledCallback = true;
  };
  xhr.send();

  this.madeRequest.doCallback();

  this.checkRequest('GET', 'http://foo.bar');
  this.assertTrue(calledCallback);
  this.assertEquals(4, xhr.readyState);
  this.assertEquals('some text', xhr.responseText);
  this.assertEquals('this would normally be XML', xhr.responseXML);
  this.assertEquals(200, xhr.status);
  this.assertEquals('no error', xhr.statusText);
  this.assertEquals('v1', xhr.getResponseHeader('h1'));
  this.assertEquals('v2', xhr.getResponseHeader('h2'));
  this.assertEquals('h1: v1\nh2: v2\n', xhr.getAllResponseHeaders());
};

XhrWrapperTest.prototype.testAddRequestHeaders = function() {
  var xhr = new shindig.xhrwrapper.XhrWrapper();
  xhr.open('GET', 'http://foo.bar');
  xhr.setRequestHeader('header', 'value');
  xhr.send();

  this.assertEquals('value', this.madeRequest.params.HEADERS['header']);
};

XhrWrapperTest.prototype.testSameOriginViolation = function() {
  var thrown;
  var xhr;

  // Different schema
  gadgets.config.init(
    {"shindig.xhrwrapper": {"contentUrl": "https://foo.bar/baz/bax.html"}});  
  xhr = new shindig.xhrwrapper.XhrWrapper();
  try {
    xhr.open('GET', 'http://foo.bar/thing');
    thrown = false;
  } catch (x) {
    thrown = true;
  }
  this.assertTrue('Should have thrown an error.', thrown);

  // Different authority
  gadgets.config.init(
    {"shindig.xhrwrapper": {"contentUrl": "http://baw.net/bax.html"}});  
  xhr = new shindig.xhrwrapper.XhrWrapper();
  try {
    xhr.open('GET', 'http://foo.bar/thing');
    thrown = false;
  } catch (x) {
    thrown = true;
  }
  this.assertTrue('Should have thrown an error.', thrown);

  // Same schema and authority
  gadgets.config.init(
    {"shindig.xhrwrapper": {"contentUrl": "http://foo.bar/some/bax.html"}});
  xhr = new shindig.xhrwrapper.XhrWrapper();
  try {
    xhr.open('GET', 'http://foo.bar/thing');
    thrown = false;
  } catch (x) {
    thrown = true;
  }
  this.assertFalse('Should not have thrown an error.', thrown);
};

XhrWrapperTest.prototype.testResolveRelativeUrl = function() {
  var xhr;

  // Only path provided
  xhr = new shindig.xhrwrapper.XhrWrapper();
  xhr.open('GET', '/foo/bar/baz.xml');
  xhr.send();
  this.checkRequest('GET', 'http://foo.bar/foo/bar/baz.xml');

  // Schema missing
  xhr = new shindig.xhrwrapper.XhrWrapper();
  xhr.open('GET', '//foo.bar/baz.xml');
  xhr.send();
  this.checkRequest('GET', 'http://foo.bar/baz.xml');
};

XhrWrapperTest.prototype.testRepointWrongUrls = function() {
  var xhr;

  // Only schema and hostname match
  xhr = new shindig.xhrwrapper.XhrWrapper();
  xhr.open('GET', 'http://shindig/foo/bar/baz.xml');
  xhr.send();
  this.checkRequest('GET', 'http://foo.bar/foo/bar/baz.xml');

  // Schema, hostname and first part of path match
  xhr = new shindig.xhrwrapper.XhrWrapper();
  xhr.open('GET', 'http://shindig/gadgets/foo/bar/baz.xml');
  xhr.send();
  this.checkRequest('GET', 'http://foo.bar/baz/foo/bar/baz.xml');
};

XhrWrapperTest.prototype.testOauthParamsUsed = function() {
  gadgets.config.init({
      'shindig.xhrwrapper': {
          'contentUrl': 'http://foo.bar/baz/bax.html',
          'authorization': 'oauth',
          'oauthService': 'testOauthService'
    	}
  });
  xhr = new shindig.xhrwrapper.XhrWrapper();
  xhr.open('GET', '/foo/bar/baz.xml');
  xhr.send();
  this.checkOAuth('testOauthService');

  gadgets.config.init({
      'shindig.xhrwrapper': {
          'contentUrl': 'http://foo.bar/baz/bax.html',
          'authorization': 'oauth',
          'oauthService': 'testOauthService',
          'oauthTokenName': 'testOauthToken'
    	}
  });
  xhr = new shindig.xhrwrapper.XhrWrapper();
  xhr.open('GET', '/foo/bar/baz.xml');
  xhr.send();
  this.checkOAuth('testOauthService', 'testOauthToken');
};

XhrWrapperTest.prototype.testSignedAuthParamsUsed = function() {
	  gadgets.config.init({
	      'shindig.xhrwrapper': {
	          'contentUrl': 'http://foo.bar/baz/bax.html',
	          'authorization': 'signed'
	    	}
	  });
	  xhr = new shindig.xhrwrapper.XhrWrapper();
	  xhr.open('GET', '/foo/bar/baz.xml');
	  xhr.send();

	  this.assertEquals('SIGNED', this.madeRequest.params['AUTHORIZATION']);
	};

XhrWrapperTest.prototype.mockMakeRequest = function(info) {
  var that = this;
  return function(url, callback, opt_params) {
    info.url = url;
    info.callback = callback;
    info.params = opt_params;
    info.doCallback = function() {
      var response = {
        data: 'some data',
        errors: [ 'no error' ],
        headers: { h1: 'v1', h2: 'v2' },
        rc: 200,
        text: 'some text'
      };
      info.callback.call(null, response);
    };
  };
};

XhrWrapperTest.mockParseXML = function(t) {
  return 'this would normally be XML';
};

XhrWrapperTest.prototype.checkRequest = function(method, url) {
  this.assertEquals(method, this.madeRequest.params['METHOD']);
  this.assertEquals(url, this.madeRequest.url);
};

XhrWrapperTest.prototype.checkOAuth = function(service, opt_token) {
  this.assertEquals('OAUTH', this.madeRequest.params['AUTHORIZATION']);
  this.assertEquals(service, this.madeRequest.params['OAUTH_SERVICE_NAME']);
  this.assertEquals(opt_token, this.madeRequest.params['OAUTH_TOKEN_NAME']);
};

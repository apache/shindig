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
 * Provides a simulated XMLHttpRequest object.
 *
 * To use, create a FakeXhrFactory, then populate the factory
 * with FakeXhrExpectation and FakeXhrResponse objects.
 */

var fakeXhr = fakeXhr || {};

/**
 * @class
 * What XHR to expect.  Requests are matched based on method, url, headers
 * query string parameters, and body parameters.  Parameter ordering does
 * not matter.
 *
 * Parameters are assumed to be HTML form encoded.
 *
 * @name fakeXhr.Expectation
 */

/**
 * Create a request.
 *
 * @constructor
 */
fakeXhr.Expectation = function(method, url) {
  this.method = method;
  this.url = url;
  this.queryArgs = {};
  this.bodyArgs = {};
  this.headers = {};
};

fakeXhr.Expectation.prototype.setMethod = function(method) {
  this.method = method;
};

fakeXhr.Expectation.prototype.setUrl = function(url) {
  this.url = url;
  var query = url.indexOf("?");
  if (query !== -1) {
    this.queryArgs = this.parseForm(url.substr(query+1));
    this.url = url.substr(0, query);
  }
};

fakeXhr.Expectation.prototype.setBodyArg = function(name, value) {
  if (value !== null) {
    this.bodyArgs[name] = value;
  } else {
    delete this.bodyArgs[name];
  }
};

fakeXhr.Expectation.prototype.setQueryArg = function(name, value) {
  if (value !== null) {
    this.queryArgs[name] = value;
  } else {
    delete this.queryArgs[name];
  }
};

fakeXhr.Expectation.prototype.setHeader = function(name, value) {
  if (value !== null) {
    this.headers[name] = value;
  } else {
    delete this.headers[name];
  }
};

fakeXhr.Expectation.prototype.parseForm = function(form) {
  var result = {};
  if (form) {
    var pairs = form.split("&");
    for (var i=0; i < pairs.length; ++i) {
      var arg = pairs[i].split("=");
      // We use unescape here instead of decodeURIComponent because of a bug
      // in Rhino: https://bugzilla.mozilla.org/show_bug.cgi?id=217257.
      // Rhino fixed this ages ago, but there hasn't been a new release of
      // the Berlios jsunit package that incorporates the fix.
      var name = unescape(arg[0]);
      var value = unescape(arg[1]);
      result[name] = value;
    }
  }
  return result;
};

fakeXhr.Expectation.prototype.toString = function() {
  return gadgets.json.stringify(this);
};

fakeXhr.Expectation.prototype.checkMatch = function(testcase, other) {
  testcase.assertEquals(this.method, other.method);
  testcase.assertEquals(this.url, other.url);
  this.checkTableEquals(testcase, "query", this.queryArgs, other.queryArgs);
  this.checkTableEquals(testcase, "body", this.bodyArgs, other.bodyArgs);
  this.checkTableEquals(testcase, "header", this.headers, other.headers);
};

fakeXhr.Expectation.prototype.checkTableEquals = function(testcase, type, x, y) {
  // Check for things in x that aren't in y
  var member;
  for (member in x) if (x.hasOwnProperty(member)) {
    testcase.assertEquals(
        "wrong value for " + type + " parameter " + member,
        x[member], y[member]);
  }
  // Check for things in y that aren't in x
  for (member in y) if (y.hasOwnProperty(member)) {
    testcase.assertEquals(
        "extra value for " + type + " parameter " + member,
        x[member], y[member]);
  }
};


/**
 * @class
 * What data to return for an XMLHttpRequest.
 *
 * @name fakeXhr.Response
 */

/**
 * Create a response.
 *
 * @constructor
 */
fakeXhr.Response = function(responseText, status) {
  this.responseText = responseText;
  this.status = status || 200;
};

fakeXhr.Response.prototype.getResponseText = function() {
  return this.responseText;
};

fakeXhr.Response.prototype.getStatus = function() {
  return this.status;
};


/**
 * @class
 * Holds a list of expected XMLHTTPRequests and responses.
 *
 * @name fakeXhr.Factory
 */

/**
 * Create a factory to collect requests and responses.
 *
 * @constructor
 */
fakeXhr.Factory = function(testcase) {
  this.testcase = testcase;
  this.expectations = [];
};

/**
 * Expect a certain request and return the specified response.
 */
fakeXhr.Factory.prototype.expect = function(expect, response) {
  this.expectations.push({ "expect" : expect, "response" : response});
};

/**
 * Finds the response matching a particular request.
 */
fakeXhr.Factory.prototype.find = function(req) {
  this.testcase.assertTrue(this.expectations.length > 0);
  var next = this.expectations.shift();
  next.expect.checkMatch(this.testcase, req);
  return next.response;
};

/**
 * Create a new XMLHttpRequest that will be fed from the expectations
 * associated with this factory.
 */
fakeXhr.Factory.prototype.getXhrConstructor = function() {
  var factory = this;
  return function() {
    return new fakeXhr.Request(factory);
  };
};


/**
 * @class
 * An XMLHTTPRequest object
 *
 * @name fakeXhr.Factory
 */

/**
 * Create a new XMLHTTPRequest, with response data returned from the
 * specified factory.
 *
 * @constructor
 */
fakeXhr.Request = function(factory) {
  this.factory = factory;
  this.actual = new fakeXhr.Expectation(null, null);
  this.response = null;
  this.onreadystatechange = null;
};

fakeXhr.Request.prototype.open = function(method, url, async) {
  this.actual.setMethod(method);
  this.actual.setUrl(url);
};

fakeXhr.Request.prototype.setRequestHeader = function(name, value) {
  this.actual.setHeader(name, value);
};

fakeXhr.Request.prototype.send = function(body) {
  this.actual.bodyArgs = this.actual.parseForm(body);
  var response = this.factory.find(this.actual);
  this.readyState = 4;
  this.status = response.status;
  this.responseText = response.responseText;
  this.onreadystatechange();
};

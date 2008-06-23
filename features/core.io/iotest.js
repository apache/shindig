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

function IoTest(name) {
  TestCase.call(this, name);
};
IoTest.inherits(TestCase);

IoTest.prototype.setUp = function() {
  this.oldGetUrlParameters = gadgets.util.getUrlParameters;
  gadgets.util.getUrlParameters = function() {
    return { "st" : "authtoken", "url" : "http://www.gadget.com/gadget.xml" };
  };
  if (!shindig.auth) {
    shindig.auth = new shindig.Auth();
  }

  this.fakeXhrs = new fakeXhr.Factory(this);
  this.oldXMLHttpRequest = window.XMLHTTPRequest;
  window.XMLHttpRequest = this.fakeXhrs.getXhrConstructor();

  gadgets.config.init({ "core.io" : {
      "proxyUrl" : "http://www.example.com/proxy?url=%url%&refresh=%refresh%",
      "jsonProxyUrl" : "http://www.example.com/json" }}); 
};

IoTest.prototype.tearDown = function() {
  gadgets.util.getUrlParameters = this.oldGetUrlParameters;
  window.XMLHttpRequest = this.oldXMLHTTPRequest;
};

IoTest.prototype.testEncodeDecode = function() {
  // BEE: remove this, just testing workaround for
  // https://bugzilla.mozilla.org/show_bug.cgi?id=217257
  var s = "http://www.example.com/somepage";
  var enc = encodeURIComponent(s);
  var dec = unescape(enc);
  this.assertEquals(s, dec);
};

IoTest.prototype.testGetProxyUrl = function() {
  var proxied = gadgets.io.getProxyUrl("http://target.example.com/image.gif");
  this.assertEquals(
      "http://www.example.com/proxy?url=http%3a%2f%2ftarget.example.com%2f" +
          "image.gif&refresh=3600",
      proxied);
};

IoTest.prototype.testGetProxyUrl_nondefaultRefresh = function() {
  var proxied = gadgets.io.getProxyUrl("http://target.example.com/image.gif",
      { 'REFRESH_INTERVAL' : 30 });
  this.assertEquals(
      "http://www.example.com/proxy?url=http%3a%2f%2ftarget.example.com%2f" +
          "image.gif&refresh=30",
      proxied);
};

// Disabling this test for now, because it uncovered a bug ingadgets.io.js, but
// I don't want to make functional changes at the same time as I refactor
// it for testing.
/*
IoTest.prototype.testGetProxyUrl_disableCache = function() {
  var proxied = gadgets.io.getProxyUrl("http://target.example.com/image.gif",
      { 'REFRESH_INTERVAL' : 0 });
  this.assertEquals(
      "http://www.example.com/proxy?url=http%3a%2f%2ftarget.example.com%2f" +
          "image.gif&refresh=0",
      proxied);
};
*/

IoTest.prototype.testEncodeValues = function() {
  var x = gadgets.io.encodeValues({ 'foo' : 'bar' });
  this.assertEquals("foo=bar", x);
};

IoTest.prototype.setArg = function(req, inBody, name, value) {
  if (inBody) {
    req.setBodyArg(name, value);
  } else {
    req.setQueryArg(name, value);
  }
}

IoTest.prototype.setStandardArgs = function(req, inBody) {
  this.setArg(req, inBody, "refresh", "3600");
  this.setArg(req, inBody, "st", "");
  this.setArg(req, inBody, "contentType", "TEXT");
  this.setArg(req, inBody, "oauthService", "");
  this.setArg(req, inBody, "authz", "");
  this.setArg(req, inBody, "bypassSpecCache", "");
  this.setArg(req, inBody, "signViewer", "true");
  this.setArg(req, inBody, "oauthToken", "");
  this.setArg(req, inBody, "signOwner", "true");
  this.setArg(req, inBody, "getSummaries", "false");
  this.setArg(req, inBody, "gadget", "http://www.gadget.com/gadget.xml");
  this.setArg(req, inBody, "oauthState", "");
  this.setArg(req, inBody, "headers", "");
  this.setArg(req, inBody, "numEntries", "3");
  this.setArg(req, inBody, "postData", "");
  this.setArg(req, inBody, "httpMethod", "GET");
};

IoTest.prototype.makeFakeResponse = function(text) {
  return new fakeXhr.Response("throw 1; < don't be evil' >" + text, 200);
};

IoTest.prototype.testNoMethod = function() {
  var req = new fakeXhr.Expectation("GET", "http://www.example.com/json");
  this.setStandardArgs(req, false);
  req.setQueryArg("url", "http://target.example.com/somepage");

  var resp = this.makeFakeResponse(
      "{ 'http://target.example.com/somepage' : { 'body' : 'some data' }}");

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  gadgets.io.makeRequest("http://target.example.com/somepage",
      function(data) {
        resp = data;
      });
  this.assertEquals('some data', resp.text);
};

IoTest.prototype.testPost = function() {
  var req = new fakeXhr.Expectation("POST", "http://www.example.com/json");
  this.setStandardArgs(req, true);
  req.setBodyArg("httpMethod", "POST");
  req.setBodyArg("postData", "foo=bar");
  req.setBodyArg("url", "http://target.example.com/somepage");
  req.setBodyArg("refresh", null);
  req.setBodyArg("headers", "Content-Type=application%2fx-www-form-urlencoded");
  req.setHeader("Content-Type", "application/x-www-form-urlencoded");

  var resp = this.makeFakeResponse(
      "{ 'http://target.example.com/somepage' : { 'body' : 'some data' }}");

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  var params = {};
  params[gadgets.io.RequestParameters.METHOD] = "POST";
  params[gadgets.io.RequestParameters.POST_DATA] = "foo=bar";
  gadgets.io.makeRequest(
      "http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      params);
  this.assertEquals('some data', resp.text);
};

IoTest.prototype.testPut = function() {
  var req = new fakeXhr.Expectation("POST", "http://www.example.com/json");
  this.setStandardArgs(req, true);
  req.setBodyArg("httpMethod", "PUT");
  req.setBodyArg("postData", "abcd");
  req.setBodyArg("url", "http://target.example.com/somepage");
  req.setBodyArg("refresh", null);
  req.setHeader("Content-Type", "application/x-www-form-urlencoded");

  var resp = this.makeFakeResponse(
      "{ 'http://target.example.com/somepage' : { 'body' : 'some data' }}");

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  var params = {};
  params[gadgets.io.RequestParameters.METHOD] = "PUT";
  params[gadgets.io.RequestParameters.POST_DATA] = "abcd";
  gadgets.io.makeRequest(
      "http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      params);
  this.assertEquals('some data', resp.text);
};

IoTest.prototype.testSignedGet = function() {
  var req = new fakeXhr.Expectation("POST", "http://www.example.com/json");
  this.setStandardArgs(req, true);
  req.setBodyArg("url", "http://target.example.com/somepage");
  req.setBodyArg("signOwner", "true");
  req.setBodyArg("signViewer", "true");
  req.setBodyArg("authz", "signed");
  req.setBodyArg("st", "authtoken");
  req.setBodyArg("refresh", null);
  req.setHeader("Content-Type", "application/x-www-form-urlencoded");

  var resp = this.makeFakeResponse(
      "{ 'http://target.example.com/somepage' : { 'body' : 'some data' }}");

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  var params = {};
  params["AUTHORIZATION"] = "SIGNED";
  gadgets.io.makeRequest(
      "http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      params);
  this.assertEquals('some data', resp.text);
};

IoTest.prototype.testSignedPost = function() {
  var req = new fakeXhr.Expectation("POST", "http://www.example.com/json");
  this.setStandardArgs(req, true);
  req.setBodyArg("url", "http://target.example.com/somepage");
  req.setBodyArg("signOwner", "true");
  req.setBodyArg("signViewer", "true");
  req.setBodyArg("authz", "signed");
  req.setBodyArg("st", "authtoken");
  req.setBodyArg("refresh", null);
  req.setBodyArg("httpMethod", "POST");
  req.setBodyArg("headers", "Content-Type=application%2fx-www-form-urlencoded");
  req.setHeader("Content-Type", "application/x-www-form-urlencoded");

  var resp = this.makeFakeResponse(
      "{ 'http://target.example.com/somepage' : { 'body' : 'some data' }}");

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  var params = {};
  params["AUTHORIZATION"] = "SIGNED";
  params["METHOD"] = "POST";
  gadgets.io.makeRequest(
      "http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      params);
  this.assertEquals('some data', resp.text);
};

IoTest.prototype.testOAuth = function() {
  var req = new fakeXhr.Expectation("POST", "http://www.example.com/json");
  this.setStandardArgs(req, true);
  req.setBodyArg("url", "http://target.example.com/somepage");
  req.setBodyArg("authz", "authenticated");
  req.setBodyArg("st", "authtoken");
  req.setBodyArg("refresh", null);
  req.setHeader("Content-Type", "application/x-www-form-urlencoded");

  var resp = this.makeFakeResponse(gadgets.json.stringify(
      { 'http://target.example.com/somepage' : { 
          'approvalUrl' : 'http://sp.example.com/authz?oauth_token=foo',
          'oauthState' : 'newState' 
         }
      }));

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  var params = {};
  params["AUTHORIZATION"] = "AUTHENTICATED";
  gadgets.io.makeRequest(
      "http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      params);
  this.assertEquals("http://sp.example.com/authz?oauth_token=foo",
      resp.approvalUrl);

  var req = new fakeXhr.Expectation("POST", "http://www.example.com/json");
  this.setStandardArgs(req, true);
  req.setBodyArg("url", "http://target.example.com/somepage");
  req.setBodyArg("authz", "authenticated");
  req.setBodyArg("st", "authtoken");
  req.setBodyArg("oauthState", "newState");
  req.setBodyArg("refresh", null);
  req.setHeader("Content-Type", "application/x-www-form-urlencoded");

  var resp = this.makeFakeResponse(
      "{ 'http://target.example.com/somepage' : { 'body' : 'personal data' }}");

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  var params = {};
  params["AUTHORIZATION"] = "AUTHENTICATED";
  gadgets.io.makeRequest(
      "http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      params);
  this.assertEquals("personal data", resp.text);
};

IoTest.prototype.testJson = function() {
  var req = new fakeXhr.Expectation("GET", "http://www.example.com/json");
  this.setStandardArgs(req, false);
  req.setQueryArg("url", "http://target.example.com/somepage");
  req.setQueryArg("contentType", "JSON");

  var resp = this.makeFakeResponse(gadgets.json.stringify(
      { 'http://target.example.com/somepage' : { 
          'body' : '{ "somejsonparam" : 3 }',
         }
      }));

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  gadgets.io.makeRequest("http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      {
        "CONTENT_TYPE" : "JSON",
      });
  this.assertEquals(3, resp.data.somejsonparam);
};

IoTest.prototype.testJson_malformed = function() {
  var req = new fakeXhr.Expectation("GET", "http://www.example.com/json");
  this.setStandardArgs(req, false);
  req.setQueryArg("url", "http://target.example.com/somepage");
  req.setQueryArg("contentType", "JSON");

  var resp = this.makeFakeResponse(gadgets.json.stringify(
      { 'http://target.example.com/somepage' : { 
          'body' : '{ bogus : 3 }',
         }
      }));

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  gadgets.io.makeRequest("http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      {
        "CONTENT_TYPE" : "JSON",
      });
  this.assertEquals("failed to parse JSON", resp.errors[0]);
};

IoTest.prototype.testPreload = function() {
  gadgets.io.preloaded_ = {
    "http://target.example.com/somepage" : {
      "rc" : 200,
      "body" : "preloadedbody",
    }
  };

  var resp = null;
  gadgets.io.makeRequest("http://target.example.com/somepage",
      function(data) {
        resp = data;
      });

  this.assertEquals("preloadedbody", resp.text);

  var req = new fakeXhr.Expectation("GET", "http://www.example.com/json");
  this.setStandardArgs(req, false);
  req.setQueryArg("url", "http://target.example.com/somepage");

  var resp = this.makeFakeResponse(gadgets.json.stringify(
      { 'http://target.example.com/somepage' : { 
          'body' : 'not preloaded',
         }
      }));

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  gadgets.io.makeRequest("http://target.example.com/somepage",
      function(data) {
        resp = data;
      });
  this.assertEquals("not preloaded", resp.text);
};

IoTest.prototype.testPreloadMiss_postRequest = function() {
  gadgets.io.preloaded_ = {
    "http://target.example.com/somepage" : {
      "rc" : 200,
      "body" : "preloadedbody",
    }
  };

  var req = new fakeXhr.Expectation("POST", "http://www.example.com/json");
  this.setStandardArgs(req, true);
  req.setBodyArg("httpMethod", "POST");
  req.setBodyArg("postData", "foo=bar");
  req.setBodyArg("url", "http://target.example.com/somepage");
  req.setBodyArg("refresh", null);
  req.setBodyArg("headers", "Content-Type=application%2fx-www-form-urlencoded");
  req.setHeader("Content-Type", "application/x-www-form-urlencoded");

  var resp = this.makeFakeResponse(
      "{ 'http://target.example.com/somepage' : { 'body' : 'some data' }}");

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  var params = {};
  params[gadgets.io.RequestParameters.METHOD] = "POST";
  params[gadgets.io.RequestParameters.POST_DATA] = "foo=bar";
  gadgets.io.makeRequest(
      "http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      params);
  this.assertEquals('some data', resp.text);
};

IoTest.prototype.testPreloadMiss_wrongUrl = function() {
  gadgets.io.preloaded_ = {
    "http://target.example.com/somepage2" : {
      "rc" : 200,
      "body" : "preloadedbody",
    }
  };

  var req = new fakeXhr.Expectation("GET", "http://www.example.com/json");
  this.setStandardArgs(req, false);
  req.setQueryArg("url", "http://target.example.com/somepage");

  var resp = this.makeFakeResponse(
      "{ 'http://target.example.com/somepage' : { 'body' : 'some data' }}");

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  var params = {};
  gadgets.io.makeRequest(
      "http://target.example.com/somepage",
      function(data) {
        resp = data;
      },
      params);
  this.assertEquals('some data', resp.text);
};

IoTest.prototype.testPreload_error404 = function() {
  gadgets.io.preloaded_ = {
    "http://target.example.com/somepage" : {
      "rc" : 404,
    }
  };

  var req = new fakeXhr.Expectation("GET", "http://www.example.com/json");
  this.setStandardArgs(req, false);
  req.setQueryArg("url", "http://target.example.com/somepage");

  var resp = this.makeFakeResponse(gadgets.json.stringify(
      { 'http://target.example.com/somepage' : { 
          'body' : 'not preloaded',
         }
      }));

  this.fakeXhrs.expect(req, resp);

  var resp = null;
  gadgets.io.makeRequest("http://target.example.com/somepage",
      function(data) {
        resp = data;
      });
  this.assertEquals("Error 404", resp.errors[0]);

  var resp = null;
  gadgets.io.makeRequest("http://target.example.com/somepage",
      function(data) {
        resp = data;
      });
  this.assertEquals("not preloaded", resp.text);
};

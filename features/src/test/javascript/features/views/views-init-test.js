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

function ViewsInitTest(name) {
  TestCase.call(this, name);
}

ViewsInitTest.inherits(TestCase);

(function() {

var callback;

ViewsInitTest.prototype.setUp = function() {
  this.document = document;
  this.getUrlParameters = gadgets.util.getUrlParameters;
};
ViewsInitTest.prototype.tearDown = function() {
  document = this.document;
  gadgets.util.getUrlParameters = this.getUrlParameters;
};

ViewsInitTest.prototype.testObjectParams = function() {
  gadgets.util.getUrlParameters = function() {
    return {"view-params": gadgets.json.stringify({foo: "bar"})};
  };

  document.scripts = [];
  gadgets.config.init({views:{}});

  this.assertEquals("bar", gadgets.views.getParams().foo);
};

ViewsInitTest.prototype.testStringParams = function() {
  // In practice, containers should actually be passing this as the 'path' query string param
  // to the gadget renderer, but we want to be sure that we can handle it just in case.
  var path = "/foo/bar/baz.html?blah=blah&foo=bar";
  gadgets.util.getUrlParameters = function() {
    return {"view-params": gadgets.json.stringify(path)};
  };

  document.scripts = [];
  gadgets.config.init({views:{}});

  this.assertEquals(path, gadgets.views.getParams());
};


ViewsInitTest.prototype.testRewriteLinksStandards = function() {
  var name, func, bubble;
  document = {
    addEventListener: function() {
      name = arguments[0];
      func = arguments[1];
      bubble = arguments[2];
    },
    getElementsByTagName: function (name) { return []; }
  };

  document.scripts = [];
  gadgets.config.init({views:{rewriteLinks: true}});

  this.assertEquals("click", name);
  this.assertTrue(typeof func === "function");
  this.assertFalse(bubble);
  this.assertTrue(typeof gadgets.views.getSupportedViews().rewriteLinks === "undefined");
};

ViewsInitTest.prototype.testRewriteLinksIe = function() {
  var name, func, self = this;
  document = {
    attachEvent: function() {
      name = arguments[0];
      func = arguments[1];

    },
    addEventListener: undefined,
    getElementsByTagName: function (name) { return []; }
  };

  document.scripts = [];
  gadgets.config.init({views:{rewriteLinks: true}});

  this.assertEquals("onclick", name);
  this.assertTrue(typeof func === "function");
  this.assertTrue(typeof gadgets.views.getSupportedViews().rewriteLinks === "undefined");
};

ViewsInitTest.prototype.testCurrentSubView = function() {
  var viewName = "home.default";
  gadgets.util.getUrlParameters = function() {
    return {"view": viewName};
  };

  document.scripts = [];
  gadgets.config.init({"views":{"home":{"isOnlyVisible": false},"default":{"isOnlyVisible": false}}});

  this.assertTrue(gadgets.views.getCurrentView());
  this.assertEquals(viewName, gadgets.views.getCurrentView().getName());
};

// TODO: Verify behavior of onclick.

})();

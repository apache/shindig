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

var oldDocument = document;
var callback;

ViewsInitTest.prototype.tearDown = function() {
  document = oldDocument;
};

ViewsInitTest.prototype.testObjectParams = function() {
  gadgets.util.getUrlParameters = function() {
    return {"view-params": gadgets.json.stringify({foo: "bar"})};
  };

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

  gadgets.config.init({views:{}});

  this.assertEquals(path, gadgets.views.getParams());
};

function createAnchors(input) {
  var anchors = [];
  for (var i = 0, j = input.length; i < j; ++i) {
    anchors[i] = {
      href: input[i],
      eventName: null,
      addEventListener: function(name, func) {
        this.invokedAddEventListener = true;
        this.eventName = name;
      }
    };
  }
  return anchors;
}

ViewsInitTest.prototype.testRewriteLinks = function() {
  var input = [
    "http://example.org/absolute",
    "/relative/path?arg=foo",
    "#fragment",
    "/relative/path?arg=foo",
    null
  ];

  var anchors = createAnchors(input);

  // Make the last one pretend to be IE.
  anchors[3].attachEvent = function(name, func) {
    this.invokedAttachEvent = true;
    this.eventName = name;
  };

  document = {
    getElementsByTagName: function(tag) {
      if (tag === "a") {
        return anchors;
      }
      return [];
    }
  };

  gadgets.config.init({views:{rewriteLinks: true}});

  this.assertEquals(null, anchors[0].eventName);
  this.assertEquals("click", anchors[1].eventName);
  this.assertEquals(null, anchors[2].eventName);
  this.assertEquals("onclick", anchors[3].eventName);
  this.assertEquals(null, anchors[4].eventName);

  this.assertTrue(anchors[1].invokedAddEventListener);
  this.assertTrue(anchors[3].invokedAttachEvent);

  this.assertTrue(typeof gadgets.views.getSupportedViews().rewriteLinks === "undefined");
};

})();

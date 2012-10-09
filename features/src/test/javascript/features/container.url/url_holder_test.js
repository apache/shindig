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
 * @fileoverview Tests for the containers URL holder.
 */

function UrlHolderTest(name) {
  TestCase.call(this, name);
}

UrlHolderTest.inherits(TestCase);
UrlHolderTest.prototype.setUp = function() {

};

UrlHolderTest.prototype.tearDown = function() {

};

UrlHolderTest.prototype.testNew = function() {
  var element = {};
  var holder = new osapi.container.UrlHolder({getId: function(){return 123;}}, element);
  this.assertEquals(element, holder.getElement());
  this.assertUndefined(holder.getIframeId());
  this.assertUndefined(holder.getUrl());
};

UrlHolderTest.prototype.testRenderWithoutParams = function() {
  var element = {};
  var url = "http://example.com";
  var holder = new osapi.container.UrlHolder({getId: function(){return 123;}, getTitle: function(){return "default title"}}, element);
  this.assertUndefined(holder.getUrl());
  this.assertUndefined(holder.getIframeId());
  holder.render(url, {});
  this.assertEquals('<iframe' + ' marginwidth="0"' + ' hspace="0"' + ' title="default title"' + ' frameborder="0"'
          + ' scrolling="auto"' + ' marginheight="0"' + ' vspace="0"' + ' id="__url_123"'
          + ' name="__url_123"' + ' src="http://example.com"' + ' ></iframe>', element.innerHTML);
  this.assertEquals(url, holder.getUrl());
  this.assertEquals("__url_123", holder.getIframeId());
};

UrlHolderTest.prototype.testRenderWithParams = function() {
  var element = {};
  var url = "http://example.com";
  var holder = new osapi.container.UrlHolder({getId: function(){return 123;}, getTitle: function(){return "default title"}}, element);
  this.assertUndefined(holder.getUrl());
  this.assertUndefined(holder.getIframeId());
  holder.render(url, {
          "class" : "myClass",
          "width" : 54,
          "height" : 104
  });
  this.assertEquals('<iframe' + ' marginwidth="0"' + ' hspace="0"' + ' height="104"'
          + ' title="default title"' + ' frameborder="0"' + ' scrolling="auto"' + ' class="myClass"' + ' marginheight="0"'
          + ' vspace="0"' + ' id="__url_123"' + ' width="54"' + ' name="__url_123"'
          + ' src="http://example.com"' + ' ></iframe>', element.innerHTML);
  this.assertEquals(url, holder.getUrl());
  this.assertEquals("__url_123", holder.getIframeId());
};
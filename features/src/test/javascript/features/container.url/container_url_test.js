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
 * @fileoverview Tests for container APIs for URL sites.
 */

function UrlContainerTest(name) {
  TestCase.call(this, name);
}

UrlContainerTest.inherits(TestCase);
UrlContainerTest.prototype.setUp = function() {
  this.apiUri = window.__API_URI;
  window.__API_URI = shindig.uri('http://shindig.com');
  this.containerUri = window.__CONTAINER_URI;
  window.__CONTAINER_URI = shindig.uri('http://container.com');
  this.shindigContainerGadgetSite = osapi.container.GadgetSite;
  this.shindigContainerUrlSite = osapi.container.UrlSite;
  this.gadgetsRpc = gadgets.rpc;
};

UrlContainerTest.prototype.tearDown = function() {
  window.__API_URI = this.apiUri;
  window.__CONTAINER_URI = this.containerUri;
  osapi.container.GadgetSite = this.shindigContainerGadgetSite;
  osapi.container.UrlSite = this.shindigContainerUrlSite;
  gadgets.rpc = this.gadgetsRpc;
};

UrlContainerTest.prototype.testNewUrlSite = function() {
  this.setupGadgetsRpcRegister();
  this.setupUrlSite(5, "http://example.com", null);
  var container = new osapi.container.Container();
  var site = container.newUrlSite({});
  this.assertEquals(5, site.getId());
};

UrlContainerTest.prototype.testNavigateUrl = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container({
    'allowDefaultView' : true,
    'renderCajole' : true,
    'renderDebug' : true,
    'renderTest' : true
  });

  this.setupUrlSite(2, "http://example.com", null);
  container.navigateUrl(osapi.container.UrlSite(), "http://example.com/index.html", {});
  this.assertEquals("http://example.com/index.html", this.urlsite_render_url);
};

UrlContainerTest.prototype.setupGadgetsRpcRegister = function() {
  gadgets.rpc = {
    register : function() {}
  };
};

UrlContainerTest.prototype.setupUrlSite = function(id, url, urlHolder) {
  var self = this;
  osapi.container.UrlSite = function() {
    return {
      "getId" : function() {
        return id;
      },
      "render" : function(url, renderParams) {
        self.urlsite_render_url = url;
        self.urlsite_render_renderParams = renderParams;
      }
    };
  };
};
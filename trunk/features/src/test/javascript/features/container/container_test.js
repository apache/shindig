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
 * Unittests for the container library.
 */

function ContainerTest(name) {
  TestCase.call(this, name);
}

ContainerTest.inherits(TestCase);

ContainerTest.prototype.setUp = function() {
  this.apiUri = window.__API_URI;
  window.__API_URI = shindig.uri('http://shindig.com');
  this.containerUri = window.__CONTAINER_URI;
  window.__CONTAINER_URI = shindig.uri('http://container.com');
  this.shindigContainerGadgetSite = osapi.container.GadgetSite;
  this.gadgetsRpc = gadgets.rpc;
  window._setTimeout = window.setTimeout;
  window.setTimeout = function(fn, time) {};
  setTimeout = window.setTimeout;
};

ContainerTest.prototype.tearDown = function() {
  window.__API_URI = this.apiUri;
  window.__CONTAINER_URI = this.containerUri;
  osapi.container.GadgetSite = this.shindigContainerGadgetSite;
  gadgets.rpc = this.gadgetsRpc;
  window.setTimeout = window._setTimeout;
};

ContainerTest.prototype.testUnloadGadget = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container();
  container.preloadedGadgetUrls_ = {
    'preloaded1.xml' : {},
    'preloaded2.xml' : {}
  };
  container.unloadGadget('preloaded1.xml');
  this.assertTrue('1', container.preloadedGadgetUrls_['preloaded1.xml'] == null);
  this.assertTrue('2', container.preloadedGadgetUrls_['preloaded2.xml'] != null);
};

ContainerTest.prototype.testUnloadGadgets = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container();
  container.preloadedGadgetUrls_ = {
    'preloaded1.xml' : {},
    'preloaded2.xml' : {},
    'preloaded3.xml' : {}
  };
  container.unloadGadgets(['preloaded1.xml', 'preloaded2.xml']);
  this.assertTrue('1', container.preloadedGadgetUrls_['preloaded1.xml'] == null);
  this.assertTrue('2', container.preloadedGadgetUrls_['preloaded2.xml'] == null);
  this.assertTrue('3', container.preloadedGadgetUrls_['preloaded3.xml'] != null);
};

ContainerTest.prototype.testPreloadConfigGadgets = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container( 
    { 'preloadMetadatas' : { 'preloaded1.xml' : {}}});
  var test = null;
  this.assertTrue('1', 'preloaded1.xml' in container.preloadedGadgetUrls_);
  this.assertFalse('2', 'preloaded2.xml' in container.preloadedGadgetUrls_);
};

ContainerTest.prototype.testPreloadCaches = function() {
  var self = this;
  this.setupGadgetsRpcRegister();
  var mockMetadata = {'preloaded1.xml' : {}};
  var container = new osapi.container.Container();
  container.service_.addGadgetMetadatas = function(gadgets, refTime) {
    self.assertEquals(mockMetadata, gadgets);
    self.assertNull(refTime);
  };
  container.service_.addGadgetTokens = function(tokens, refTime) {
    self.assertEquals(mockMetadata, tokens);
    self.assertNull(refTime);
  };
  container.addPreloadGadgets_ = function(gadgets) {
    self.assertEquals(mockMetadata, gadgets);
  };
  container.applyLifecycleCallbacks_ = function() {};
  container.preloadCaches({
    'preloadMetadatas' : mockMetadata,
    'preloadTokens' : mockMetadata
  });
};

ContainerTest.prototype.testPreloadConfigTokens = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container(
          { 'preloadMetadatas' : { 'preloaded1.xml' : {'tokenTTL' : 200, 'needsTokenRefresh' : true}},
            'preloadTokens' : { 'preloaded1.xml' : {'tokenTTL' : 100}}});
  this.assertEquals(100*1000*0.8, container.tokenRefreshInterval_);

  container = new osapi.container.Container(
          { 'preloadMetadatas' : { 'preloaded1.xml' : {'tokenTTL' : 200, 'needsTokenRefresh' : true}}});
  this.assertEquals(200*1000*0.8, container.tokenRefreshInterval_);

  container = new osapi.container.Container(
          { 'preloadMetadatas' : { 'preloaded1.xml' : {'tokenTTL' : 200, 'needsTokenRefresh' : true}},
            'preloadTokens' : { 'preloaded1.xml' : {'tokenTTL' : 300}}});
  this.assertEquals(300*1000*0.8, container.tokenRefreshInterval_);
};

ContainerTest.prototype.testNavigateGadget = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container({
    'allowDefaultView' : true,
    'renderCajole' : true,
    'renderDebug' : true,
    'renderTest' : true
  });

  this.setupGadgetSite(1, {}, null);
  var site = container.newGadgetSite(null);
  container.navigateGadget(site, 'gadget.xml', {}, {});
  this.assertEquals('gadget.xml', this.site_navigateTo_gadgetUrl);
  this.assertTrue(this.site_navigateTo_renderParams['allowDefaultView']);
  this.assertTrue(this.site_navigateTo_renderParams['cajole']);
  this.assertTrue(this.site_navigateTo_renderParams['debug']);
  this.assertTrue(this.site_navigateTo_renderParams['nocache']);
  this.assertTrue(this.site_navigateTo_renderParams['testmode']);
};

ContainerTest.prototype.testNewGadgetSite = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container();
  this.setupGadgetSite(1, {}, null);
  var site1 = container.newGadgetSite(null);
  this.setupGadgetSite(2, {}, null);
  var site2 = container.newGadgetSite(null);
  this.assertTrue(container.sites_[1] != null);
  this.assertTrue(container.sites_[2] != null);
};

ContainerTest.prototype.testMixinViaPrototype = function() {
  this.setupGadgetsRpcRegister();
  osapi.container.Container.prototype.mixins_['test'] = function(context) {
    return {
      'getSitesLength' : function() {
        return context.sites_.length;
      }
    };
  };
  osapi.container.Container.prototype.mixinsOrder_.push('test');
  var container = new osapi.container.Container();
  this.setupGadgetSite(1, {}, null);
  container.newGadgetSite(null);
  this.assertTrue(container.sites_[1] != null);
  this.assertEquals(container.sites_.length, container.test.getSitesLength());
};

ContainerTest.prototype.testMixinViaAdd = function() {
  this.setupGadgetsRpcRegister();
  osapi.container.Container.addMixin('test2', function(context) {
    return {
      'getSitesLength' : function() {
        return context.sites_.length;
      }
    };
  });
  var container = new osapi.container.Container();
  this.setupGadgetSite(1, {}, null);
  container.newGadgetSite(null);
  this.assertTrue(container.sites_[1] != null);
  this.assertEquals(container.sites_.length, container.test2.getSitesLength());
};

ContainerTest.prototype.setupGadgetSite = function(id, gadgetInfo, gadgetHolder) {
  var self = this;
  osapi.container.GadgetSite = function() {
    return {
      'getId' : function() {
        return id;
      },
      'navigateTo' : function(gadgetUrl, viewParams, renderParams, func) {
        self.site_navigateTo_gadgetUrl = gadgetUrl;
        self.site_navigateTo_viewParams = viewParams;
        self.site_navigateTo_renderParams = renderParams;
        func(gadgetInfo);
      },
      'getActiveSiteHolder' : function() {
        return gadgetHolder;
      }
    };
  };
};

ContainerTest.prototype.setupGadgetsRpcRegister = function() {
  gadgets.rpc = {
    register: function() {
    }
  };
};

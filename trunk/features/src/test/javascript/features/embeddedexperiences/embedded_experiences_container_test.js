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
 * @fileoverview Tests for container APIs for embedded experiences.
 */

function EEContainerTest(name) {
  TestCase.call(this, name);
}

EEContainerTest.inherits(TestCase);
EEContainerTest.prototype.setUp = function() {
      this.apiUri = window.__API_URI;
      window.__API_URI = shindig.uri('http://shindig.com');
      this.containerUri = window.__CONTAINER_URI;
      window.__CONTAINER_URI = shindig.uri('http://container.com');
      this.shindigContainerGadgetSite = osapi.container.GadgetSite;
      this.shindigContainerUrlSite = osapi.container.UrlSite;
      this.shindigContainerPreload = osapi.container.Container.preloadGadget;
      this.gadgetsRpc = gadgets.rpc;
};

EEContainerTest.prototype.tearDown = function() {
      window.__API_URI = this.apiUri;
      window.__CONTAINER_URI = this.containerUri;
      osapi.container.GadgetSite = this.shindigContainerGadgetSite;
      osapi.container.UrlSite = this.shindigContainerUrlSite;
      osapi.container.Container.preloadGadget = this.shindigContainerPreload;
      gadgets.rpc = this.gadgetsRpc;
};

EEContainerTest.prototype.testNavigateGadget = function() {
      this.setupGadgetsRpcRegister();
      var container = new osapi.container.Container({
        'allowDefaultView' : true,
        'renderCajole' : true,
        'renderDebug' : true,
        'renderTest' : true
      });

      var eeDataModel = {'gadget' : 'http://example.com/gadget.xml', 'context' : '123'};

      this.setupGadgetSite(1, {}, null);
      this.setupPreload();
      container.ee.navigate({}, eeDataModel, {});
      var renderParamDataModel = this.site_navigateTo_renderParams['eeDataModel'];
      this.assertEquals('http://example.com/gadget.xml', renderParamDataModel.gadget);
      this.assertEquals('123', renderParamDataModel.context);
      this.assertEquals('embedded', this.site_navigateTo_renderParams['view']);
      this.assertEquals('http://example.com/gadget.xml', this.site_navigateTo_gadgetUrl);
      this.assertTrue(this.site_navigateTo_renderParams['allowDefaultView']);
      this.assertTrue(this.site_navigateTo_renderParams['cajole']);
      this.assertTrue(this.site_navigateTo_renderParams['debug']);
      this.assertTrue(this.site_navigateTo_renderParams['nocache']);
      this.assertTrue(this.site_navigateTo_renderParams['testmode']);

};

EEContainerTest.prototype.testNavigateGadgetWithAssociatedContext = function() {
      this.setupGadgetsRpcRegister();
      var container = new osapi.container.Container({});

      var eeDataModel =
        {'gadget' : 'http://example.com/gadget.xml', 'context' : {'label' : '123'}};

      var container_context = {'associatedContext' : {'id': "123abc",
                                                      'type': 'opensocial.ActivityEntry',
                                                      'objectReference' : {}}};

      this.setupGadgetSite(1, {}, null);
      this.setupPreload();
      container.ee.navigate({}, eeDataModel, {}, function() {}, container_context);

      // verify
      var renderParamDataModel = this.site_navigateTo_renderParams['eeDataModel'];
      this.assertEquals('http://example.com/gadget.xml', renderParamDataModel.gadget);
      var expectedContext = {"openSocial": {"associatedContext":{"id":"123abc","type":"opensocial.ActivityEntry","objectReference":{}}}, "label":"123"};
      this.assertEquals(expectedContext, renderParamDataModel.context);
};


EEContainerTest.prototype.testNavigateGadgetWithPreferredExperience = function() {
      this.setupGadgetsRpcRegister();
      var container = new osapi.container.Container({});
      var eeDataModel =
        {'gadget' : 'http://example.com/gadget.xml', 'context' : {'objectid' : '123'},
          'preferredExperience' : {'target' : {'type' : 'gadget','view' : 'my-ee-view', 'viewTarget' : 'DIALOG'},
          'display' : {'type' : 'text', 'label' : 'Click me to say Hello World'}}};

      this.setupGadgetSite(1, {}, null);
      this.setupPreload();
      container.ee.navigate({}, eeDataModel, {});

      // verify
      var renderParamDataModel = this.site_navigateTo_renderParams['eeDataModel'];
      this.assertEquals({"objectid":"123"}, renderParamDataModel.context);
      this.assertEquals('my-ee-view', this.site_navigateTo_renderParams['view']);

};

EEContainerTest.prototype.testNavigateGadgetWithBadGadgetTarget = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container({});
  var eeDataModel =
    {'url' : 'http://example.com/myee.html',
      'preferredExperience' : {'target' : {'type' : 'gadget','view' : 'my-ee-view', 'viewTarget' : 'DIALOG'},
      'display' : {'type' : 'text', 'label' : 'Click me to say Hello World'}}};

  this.setupUrlSite(1, null, null);
  this.setupPreload();
  container.ee.navigate({}, eeDataModel, {});

  // verify
  this.assertEquals('http://example.com/myee.html', this.urlsite_render_url);

};

EEContainerTest.prototype.testNavigateGadgetWithBadUrlTarget = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container({});
  var eeDataModel =
    {'gadget' : 'http://example.com/myee.xml', 'context' : {'objectid' : '123'},
      'preferredExperience' : {'target' : {'type' : 'url', 'viewTarget' : 'DIALOG'},
      'display' : {'type' : 'text', 'label' : 'Click me to say Hello World'}}};

  this.setupGadgetSite(1, {}, null);
  this.setupPreload();
  container.ee.navigate({}, eeDataModel, {});

  // verify
  var renderParamDataModel = this.site_navigateTo_renderParams['eeDataModel'];
  this.assertEquals('http://example.com/myee.xml', renderParamDataModel.gadget);
  this.assertEquals({"objectid":"123"}, renderParamDataModel.context);
  this.assertEquals('embedded', this.site_navigateTo_renderParams['view']);

};

EEContainerTest.prototype.testGadgetNavigateWithCustomEENavigation = function() {
      this.setupGadgetsRpcRegister();
      var customEENavigate = function(dataModel) {
        customNavigate = true;
        if(dataModel.gadget)
          return 'gadget';
        else
          return 'url';
      };
      var customNavigate = false;

      var container = new osapi.container.Container({'GET_EE_NAVIGATION_TYPE' : customEENavigate});
      var eeDataModel = {'gadget' : 'http://example.com/gadget.xml', 'context' : '123'};

      this.setupGadgetSite(1, {}, null);
      this.setupPreload();
      container.ee.navigate({}, eeDataModel, {});

      // verify
      this.assertTrue(customNavigate);
      var renderParamDataModel = this.site_navigateTo_renderParams['eeDataModel'];
      this.assertEquals('http://example.com/gadget.xml', renderParamDataModel.gadget);
      this.assertEquals('123', renderParamDataModel.context);
      this.assertEquals('embedded', this.site_navigateTo_renderParams['view']);
      this.assertEquals('http://example.com/gadget.xml', this.site_navigateTo_gadgetUrl);
};

EEContainerTest.prototype.setupGadgetsRpcRegister = function() {
      gadgets.rpc = {
        register: function() {
        }
      };
};

EEContainerTest.prototype.setupGadgetSite = function(id, gadgetInfo, gadgetHolder) {
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

EEContainerTest.prototype.setupUrlSite = function(id, url, urlHolder) {
    var self = this;
    osapi.container.UrlSite = function() {
        return {
            'getId' : function() {
                return id;
            },
            'render' : function(url, renderParams) {
                self.urlsite_render_url = url;
                self.urlsite_render_renderParams = renderParams;
            }
        };
    };
};

EEContainerTest.prototype.setupPreload = function() {
  osapi.container.Container.prototype.preloadGadget = function(gadgetUrl, func) {
    var ret = [];
    ret[gadgetUrl] = {};
    func(ret);
  };
};

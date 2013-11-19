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
 * Unittests for the gadget_holder library.
 */

function GadgetHolderTest(name) {
  TestCase.call(this, name);
}

GadgetHolderTest.inherits(TestCase);

GadgetHolderTest.prototype.setUp = function() {
  this.containerUri = window.__CONTAINER_URI;
  window.__CONTAINER_URI = shindig.uri('http://container.com');
  this.gadgetsRpc = gadgets.rpc;
  this.pubsub2router = gadgets.pubsub2router;
};

GadgetHolderTest.prototype.tearDown = function() {
  window.__CONTAINER_URI = this.containerUri;
  gadgets.rpc = this.gadgetsRpc;
  gadgets.pubsub2router = this.pubsub2router;
};

GadgetHolderTest.prototype.testNew = function() {
  var element = {
    getAttribute: function() {
      return '0';
    },
    id: '123'
  };
  var site = new osapi.container.GadgetSite(null, null, {gadgetEl: element});
  var holder = new osapi.container.GadgetHolder(site, element);
  this.assertEquals(element, holder.getElement());
  this.assertUndefined(holder.getIframeId());
  this.assertUndefined(holder.getGadgetInfo());
  this.assertUndefined(holder.getUrl());
};

GadgetHolderTest.prototype.testRenderWithoutRenderParams = function() {
  var element = {};
  var gadgetInfo = {
      'iframeUrls' : {'default' : 'http://shindig/gadgets/ifr?url=gadget.xml&lang=en&country=US#rpctoken=1234'},
      'url' : 'gadget.xml'
  };
  this.setupGadgetsRpcSetupReceiver();
  var element = {
    id: '123'
  };
  var service = {};
  service.getCountry = function(){return "ZH";};
  service.getLanguage = function(){return "cn"};
  var site = new osapi.container.GadgetSite(null, service, {gadgetEl: element});
  var holder = new osapi.container.GadgetHolder(site, element, '__gadgetOnLoad');
  holder.render(gadgetInfo, {}, {'view' : 'default'});
  this.assertEquals('<iframe' +
      ' marginwidth="0"' +
      ' hspace="0"' +
      ' title="default title"' +
      ' frameborder="0"' +
      ' scrolling="no"' +
      ' onload="window.__gadgetOnLoad(\'gadget.xml\', \'123\');"' +
      ' marginheight="0"' +
      ' vspace="0"' +
      ' id="__gadget_123"' +
      ' name="__gadget_123"' +
      ' src="http://shindig/gadgets/ifr?url=gadget.xml&lang=en&country=US&debug=0&nocache=0&testmode=0' +
          '&view=default&parent=http%3A//container.com&mid=0#rpctoken=1234"' +
      ' ></iframe>',
      element.innerHTML);
};

GadgetHolderTest.prototype.testRenderWithRenderRequests = function() {
  var gadgetInfo = {
      'iframeUrls' : {'default' : 'http://shindig/gadgets/ifr?url=gadget.xml&lang=%lang%&country=%country%#rpctoken=1234'},
      'url' : 'gadget.xml'
  };
  var renderParams = {
      'cajole' : true,
      'class' : 'xyz',
      'debug' : true,
      'height' : 111,
      'nocache' : true,
      'testmode' : true,
      'width' : 222,
      'view' : 'default'
  };
  this.setupGadgetsRpcSetupReceiver();
  var element = {
    id: '123'
  };
  var service = {};
  service.getCountry = function(){return "US";};
  service.getLanguage = function(){return "en"};
  var site = new osapi.container.GadgetSite(null, service, {gadgetEl: element, moduleId: 123});
  var holder = new osapi.container.GadgetHolder(site, element, '__gadgetOnLoad');
  holder.render(gadgetInfo, {}, renderParams);
  this.assertEquals('<iframe' +
      ' marginwidth="0"' +
      ' hspace="0"' +
      ' height="111"' +
      ' title="default title"' +
      ' frameborder="0"' +
      ' scrolling="no"' +
      ' onload="window.__gadgetOnLoad(\'gadget.xml\', \'123\');"' +
      ' class="xyz"' +
      ' marginheight="0"' +
      ' vspace="0"' +
      ' id="__gadget_123"' +
      ' width="222"' +
      ' name="__gadget_123"' +
      ' src="http://shindig/gadgets/ifr?url=gadget.xml&lang=en&country=US&debug=1&nocache=1&testmode=1' +
          '&view=default&libs=caja&caja=1&parent=http%3A//container.com&mid=0#rpctoken=1234"' +
      ' ></iframe>',
      element.innerHTML);
};

GadgetHolderTest.prototype.testRemoveOaContainer_exisiting = function() {
    var hub = this.setupMockPubsub2router(true);
    var holder = new osapi.container.GadgetHolder();
    var answer = 42;
    holder.removeOaaContainer_(answer);
    this.assertEquals(answer, hub.getCallArgs().g.id);
    this.assertEquals(answer, hub.getCallArgs().r.container.passedId);
};

GadgetHolderTest.prototype.testRemoveOaContainer_nonexisting = function() {
    var hub = this.setupMockPubsub2router(false);
    var holder = new osapi.container.GadgetHolder();
    var answer = 42;
    holder.removeOaaContainer_(answer);
    this.assertEquals(answer, hub.getCallArgs().g.id);
    this.assertEquals("undefined", typeof hub.getCallArgs().r.container);
};

GadgetHolderTest.prototype.testDisposeOaContainer = function() {
  osapi.container.GadgetHolder.prototype.relayPath_ = '/gadgets/files/container/rpc_relay.html';
  var gadgetInfo = {
          'iframeUrls' : {'default' : 'http://shindig/gadgets/ifr?url=gadget.xml&lang=en&country=US#rpctoken=1234'},
          'url' : 'gadget.xml',
          'modulePrefs' : {
            'features' : {
              'pubsub-2' : {}
            }
          }
      };
  this.setupMockOAHub();
  var hub = this.setupMockPubsub2router(true);
  var element = {
          id: '123'
  };
  var service = {};
  service.getCountry = function(){return "US";};
  service.getLanguage = function(){return "en"};
  var site = new osapi.container.GadgetSite(null, service, {gadgetEl: element});
  site.id_ = 42;
  var holder = new osapi.container.GadgetHolder(site, element, '__gadgetOnLoad');
  // I would like to call holder.render, but I can't get the setup to work, so I "mock it"
  holder.iframeId_ = site.id_;
  holder.isOaaIframe_ = true;
  holder.dispose();

  this.assertEquals(site.id_, hub.getCallArgs().g.id);
  this.assertEquals(site.id_, hub.getCallArgs().r.container.passedId);
};

GadgetHolderTest.prototype.setupGadgetsRpcSetupReceiver = function() {
  gadgets.rpc = {
    setupReceiver: function(iframeId, relayUri, rpcToken) {
    }
  };
};

GadgetHolderTest.prototype.setupMockPubsub2router = function(existing) {
    gadgets.pubsub2router = {
        hub:(function () {
            var getArgs = {}, removeArgs = {};
            return{
                getContainer:function (id) {
                    getArgs.id = id;
                    return existing ? {passedId:id} : null;
                },
                removeContainer:function (container) {
                    removeArgs.container = container;
                },
                getCallArgs:function () {
                    return {g:getArgs, r:removeArgs}
                }
            }
        })()
    };
    return gadgets.pubsub2router.hub;
};

GadgetHolderTest.prototype.setupMockOAHub = function() {
  OpenAjax = {};
  OpenAjax.hub = {};
  OpenAjax.hub.IframeContainer = function() {
    // Do nothing
  };
};

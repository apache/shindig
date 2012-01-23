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
};

GadgetHolderTest.prototype.tearDown = function() {
  window.__CONTAINER_URI = this.containerUri;
  gadgets.rpc = this.gadgetsRpc;
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
      'iframeUrl' : 'http://shindig/gadgets/ifr?url=gadget.xml',
      'url' : 'gadget.xml'
  };
  this.setupGadgetsRpcSetupReceiver();
  var element = {
    id: '123'
  };
  var site = new osapi.container.GadgetSite(null, null, {gadgetEl: element});
  var holder = new osapi.container.GadgetHolder(site, element, '__gadgetOnLoad');
  holder.render(gadgetInfo, {}, {});
  this.assertEquals('<iframe' +
      ' marginwidth="0"' +
      ' hspace="0"' +
      ' frameborder="0"' +
      ' scrolling="no"' +
      ' onload="window.__gadgetOnLoad(\'gadget.xml\');"' +
      ' marginheight="0"' +
      ' vspace="0"' +
      ' id="__gadget_123"' +
      ' name="__gadget_123"' +
      ' src="http://shindig/gadgets/ifr?url=gadget.xml&debug=0&nocache=0&testmode=0' +
          '&parent=http%3A//container.com&mid=0"' +
      ' ></iframe>',
      element.innerHTML);
};

GadgetHolderTest.prototype.testRenderWithRenderRequests = function() {
  var gadgetInfo = {
      'iframeUrl' : 'http://shindig/gadgets/ifr?url=gadget.xml',
      'url' : 'gadget.xml'
  };
  var renderParams = {
      'cajole' : true,
      'class' : 'xyz',
      'debug' : true,
      'height' : 111,
      'nocache' : true,
      'testmode' : true,
      'width' : 222
  };
  this.setupGadgetsRpcSetupReceiver();
  var element = {
    id: '123'
  };
  var site = new osapi.container.GadgetSite(null, null, {gadgetEl: element, moduleId: 123});
  var holder = new osapi.container.GadgetHolder(site, element, '__gadgetOnLoad');
  holder.render(gadgetInfo, {}, renderParams);
  this.assertEquals('<iframe' +
      ' marginwidth="0"' +
      ' hspace="0"' +
      ' height="111"' +
      ' frameborder="0"' +
      ' scrolling="no"' +
      ' onload="window.__gadgetOnLoad(\'gadget.xml\');"' +
      ' class="xyz"' +
      ' marginheight="0"' +
      ' vspace="0"' +
      ' id="__gadget_123"' +
      ' width="222"' +
      ' name="__gadget_123"' +
      ' src="http://shindig/gadgets/ifr?url=gadget.xml&debug=1&nocache=1&testmode=1' +
          '&libs=caja&caja=1&parent=http%3A//container.com&mid=0"' +
      ' ></iframe>',
      element.innerHTML);
};

GadgetHolderTest.prototype.setupGadgetsRpcSetupReceiver = function() {
  gadgets.rpc = {
    setupReceiver: function(iframeId, relayUri, rpcToken) {
    }
  };
};

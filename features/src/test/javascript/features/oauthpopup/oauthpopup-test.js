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

function PopupTest(name) {
  TestCase.call(this, name);
};
PopupTest.inherits(TestCase);

PopupTest.prototype.setUp = function() {
  this.oldWindow = window;
  window = new mocks.FakeWindow();
  window.__API_URI = shindig.uri('http://shindig.com');
  window.__CONTAINER_URI = shindig.uri('http://container.com');

  this.gadgetsRpc = gadgets.rpc;
  gadgets.rpc = {};
  var self = this;
  gadgets.rpc.register = function(service, callback) {
    if (self.captures && self.captures.hasOwnProperty(service)) {
      self.captures[service] = callback;
    }
  };
  gadgets.rpc.call = function() {
    self.rpcArguments = Array.prototype.slice.call(arguments);
  };
};

PopupTest.prototype.tearDown = function() {
  window = this.oldWindow;
  gadgets.rpc = this.gadgetsRpc;
  delete this.rpcArguments;
  delete this.captures;
};

PopupTest.prototype.testPopup = function() {
  var undef, captures = this.captures = {
    'oauth.open': undef
  };
  var container = new osapi.container.Container();
  this.assertNotUndefined('RPC endpoint "oauth.open" was not registered.', captures['oauth.open']);

  delete this.rpcArguments;

  var cbid, popup, opened = false;
  window.open = function(url, target, options) {
    return popup = mocks.FakeWindow.prototype.open.call(this, url, target, options);
  };
  this.assertUndefined('Window opened prematurely.', popup);

  captures['oauth.open'].call({f: 'from', callback: function(id) {
    opened = true;
    cbid = id;
  }}, 'destination', 'options');
  this.assertNotUndefined('Window not opened.', popup);
  this.assertTrue('Opened callback not fired.', opened);
  this.assertEquals('Url incorrect.', 'destination', popup.url_);
  this.assertEquals('Target incorrect.', '_blank', popup.target_);
  this.assertEquals('Options incorrect.', 'options', popup.options_);

  // Wait a bit for our events to run
  window.incrementTime(1000);
  this.assertUndefined('close callback called early.', this.rpcArguments);

  // User or site closes window
  popup.close();
  window.incrementTime(100);
  this.assertEquals('Closer callback not called.', ['from', 'oauth.close', null, cbid, undef], this.rpcArguments);

  delete this.rpcArguments;
  window.incrementTime(1000);
  this.assertUndefined('Timer not cancelled.', this.rpcArguments);
};

PopupTest.prototype.testPopup_userClick = function() {
  var opened = false, closed = false;
  // Create the popup
  var popup = new gadgets.oauth.Popup('destination', 'options', function() {
    opened = true;
  }, function() {
    closed = true;
  });
  var openerOnClick = popup.createOpenerOnClick();
  var closerOnClick = popup.createApprovedOnClick();

  // Open the window
  openerOnClick();

  // Wait a bit for our events to run
  window.incrementTime(1000);
  this.assertFalse('closer callback called early', closed);

  // User clicks link
  var ranDefaultAction = closerOnClick();
  this.assertFalse(ranDefaultAction);
  this.assertTrue('Closer callback not called', closed);
};

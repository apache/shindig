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
};

PopupTest.prototype.tearDown = function() {
  window = this.oldWindow;
};

PopupTest.prototype.testPopup = function() {
  var opened = false;
  var open = function() {
    opened = true;
  };
  var closed = false;
  var close = function() {
    closed = true;
  };
  // Create the popup
  var popup = new gadgets.oauth.Popup('destination', 'options', open, close);
  var openerOnClick = popup.createOpenerOnClick();
  var closerOnClick = popup.createApprovedOnClick();
  this.assertNull('Window opened prematurely', popup.win_);
  this.assertFalse('Opener callback was called', opened);

  // Open the window
  var ranDefaultAction = openerOnClick();
  this.assertTrue('Window not opened', opened);
  this.assertFalse('Ran browser default action on open', ranDefaultAction);
  this.assertNotNull('Window was null', popup.win_);
  this.assertEquals('Url incorrect', 'destination', popup.win_.url_);
  this.assertEquals('Target incorrect', '_blank', popup.win_.target_);
  this.assertEquals('Options incorrect', 'options', popup.win_.options_);

  // Wait a bit for our events to run
  window.incrementTime(1000);
  this.assertFalse('closer callback called early', closed);

  // User or site closes window
  popup.win_.close();
  window.incrementTime(100);
  this.assertTrue('Closer callback not called', closed);
};

PopupTest.prototype.testPopup_userClick = function() {
  var opened = false;
  var open = function() {
    opened = true;
  };
  var closed = false;
  var close = function() {
    closed = true;
  };
  // Create the popup
  var popup = new gadgets.oauth.Popup('destination', 'options', open, close);
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

PopupTest.prototype.testTimerCancelled = function() {
  var open = function() {};
  var closeCount = 0;
  var close = function() {
    ++closeCount;
  };

  // Create the popup
  var popup = new gadgets.oauth.Popup('destination', 'options', open, close);
  var openerOnClick = popup.createOpenerOnClick();
  var closerOnClick = popup.createApprovedOnClick();

  // Open the window
  openerOnClick();

  // Close the window
  popup.win_.close();

  // Wait a bit for our events to run
  window.incrementTime(1000);
  this.assertEquals('Wrong number of calls to close', 1, closeCount);
  window.incrementTime(1000);
  this.assertEquals('timer not cancelled', 1, closeCount);
};

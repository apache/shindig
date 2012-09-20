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
 * Provides a simulated window object.  Recommended usage is to reset
 * the global window object in each test case that uses the window.
 *
 * Example:
 * 
 * ExampleTest.prototype.testSomething = function() {
 *   window = new mocks.FakeWindow();
 *   // Test things
 * };
 */

var mocks = mocks || {};

/**
 * @constructor
 * @description creates a new fake window object.
 * @param {string} url Destination url.
 * @param {string} target Name of window
 * @param {string} options Options for window, such as size.
 */
mocks.FakeWindow = function(url, target, options) {
  // Properties passed to window.open
  this.url_ = url;
  this.target_ = target;
  this.options_ = options;

  // Whether the window has been closed.
  this.closed = false;

  // Event handling.  Events array is always sorted in order of ascending
  // execution time.
  this.now_ = 1000000;
  this.events_ = [];
  this.nextEventId_ = 1000;
};

/**
 * Replacement for window.open.
 */
mocks.FakeWindow.prototype.open = function(url, target, options) {
  return new mocks.FakeWindow(url, target, options);
};

/**
 * Replacement for window.close.
 */
mocks.FakeWindow.prototype.close = function() {
  this.closed = true;
};

/**
 * Replacement for window.setInterval
 */
mocks.FakeWindow.prototype.setInterval = function(callback, millis) {
  var event = {
    id: this.nextEventId_,
    when: this.now_ += millis,
    interval: millis,
    callback: callback
  };
  this.events_.push(event);
  this.sortEvents_();
  ++this.nextEventId_;
  return event.id;
};

mocks.FakeWindow.prototype.sortEvents_ = function(event) {
  this.events_.sort(function (a, b) {
    return a.when - b.when;
  });
};

/**
 * Replacement for window.clearInterval
 */
mocks.FakeWindow.prototype.clearInterval = function(id) {
  // Removes a single event by copying everything but that event.
  var remaining = [];
  for (var i = 0; i < this.events_.length; ++i) {
    e = this.events_[i];
    if (e.id !== id) {
      remaining.push(e);
    }
  }
  if (this.events_.length === remaining.length) {
    throw 'window.clearInterval failed, no event with id ' + id;
  }
  this.events_ = remaining;
};

/**
 * Moves the clock forward, running any associated events.
 */
mocks.FakeWindow.prototype.incrementTime = function(millis) {
  if (this.active) {
    throw 'recursive invocation of window.incrementTime.  Cut that out';
  }
  this.active = true;

  // Each iteration bumps the time just enough to run a single event, or
  // else ends the loop.
  var finish = this.now_ + millis;
  do {
    var ranEvent = false;
    if (this.events_.length > 0) {
      var e = this.events_[0];
      if (e.when <= finish) {
        this.now_ = e.when;
        e.when += e.interval;
        this.sortEvents_();
        // Deliberately let exceptions propagate, it's probably a bug if
        // a timer throws an exception.
        e.callback();
        ranEvent = true;
      }
    }
  } while (ranEvent);
  this.now_ = finish;
  this.active = false;
};

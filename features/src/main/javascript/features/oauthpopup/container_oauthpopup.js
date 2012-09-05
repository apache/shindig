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
 * @fileoverview API to assist with management of the OAuth popup window.
 */

(function() {
  var timers = {},
      cbid = 1;  // start at 1 so they are always truthy

  function checkClosed(win, callback, key) {
    if (this.isClosed(win)) {
      // setInterval, when missed, can run multiple times.
      if (typeof(timers[key]) != 'undefined') {
        window.clearInterval(timers[key]);
        delete timers[key];
        callback();
      }
    }
  }

  // scope
  var oauth = {
    /**
     * Handles the rpc endpoint for gadgets wanting to open an oauth popup.
     *
     * @param {string} location Location to open
     * @param {string} options Window open options
     * @param {string} from Unique identifier of gadget requesting oauth
     * @param {function(stirng)} onOpen Callback that takes the id of the close callback
     */
    open: function(location, options, from, onOpen) {
      // If a popup blocker blocks the window, we do nothing.  The user will
      // need to approve the popup, then click again to open the window.
      // Note that because we don't call window.open until the user has clicked
      // something the popup blockers *should* let us through.
      var key = location + ':' + from + ":" + options,
          win = this.getWindow(location, options);

      if (win) {
        var id = cbid++,
            callback = gadgets.util.makeClosure(this, function(id) {
              this.closeWindow(win); // make sure it's closed
              gadgets.rpc.call(from, 'oauth.close', null, id);
            }, id);

        // Poll every 100ms to check if the window has been closed
        timers[key] = window.setInterval(gadgets.util.makeClosure(this, checkClosed, win, callback, key), 100);

        onOpen(id);
      } else {
        // handle error that might be able to resume.
        this.error(Array.prototype.slice.call(arguments));
      }
    },

    /**
     * Opens a window at a specific location.
     *
     * @param {string} location Location to open
     * @param {string} options Window open options
     * @param {string} from Unique identifier of gadget requesting oauth
     * @return {Object} The opened window
     */
    getWindow: function(location, options, from) {
      return window.open(location, '_blank', options);
    },

    /**
     * Closes a window opened by
     *
     * @param {Object} win A window opened by getWindow()
     */
    closeWindow: function(win) {
      win && win.close && win.close();
    },

    /**
     * Check to see if window opened by getWindow() is closed.
     *
     * @param {Object} win A window opened by getWindow()
     * @return {boolean} If win is closed.
     */
    isClosed: function(win) {
      return !win || win.closed;
    },

    error: function(openargs) {
      gadgets.warn('OAuth popup window was not opened.');
      // Try again?
      // this.open.apply(this, openargs);
    }
  };

  // Support mixing into the container which makes js rpc tests super easy.
  // To override this mixin, you'll need to setTimeout, because we need to do the same here.
  // This feature doesn't depend on `container` so it probably won't get defined before us.
  setTimeout(function() {
    if (osapi && osapi.container && osapi.container.Container && osapi.container.Container.addMixin) {
      osapi.container.Container.addMixin('oauth', function(container) {
        gadgets.rpc.register('oauth.open', function(location, options) {
          container.oauth.open(location, options, this.f, this.callback);
        });
  
        return oauth;
      });
    } else {
      gadgets.rpc.register('oauth.open', function(location, options) {
        oauth.open(location, options, this.f, this.callback);
      });
    }
  }, 0);
})();
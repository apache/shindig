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

/**
 * @constructor
 */
gadgets.oauth = gadgets.oauth || {};

/**
 * @class OAuth popup window manager.
 *
 * <p>
 * Expected usage:
 * </p>
 *
 * <ol>
 * <li>
 * <p>
 * Gadget attempts to fetch OAuth data for the user and discovers that
 * approval is needed.  The gadget creates two new UI elements:
 * </p>
 * <ul>
 *   <li>
 *      a "personalize this gadget" button or link.
 *   </li>
 *   <li>
 *      a "personalization done" button or link, which is initially hidden.
 *   </li>
 * </ul>
 * <p>
 * The "personalization done" button may be unnecessary.  The popup window
 * manager will attempt to detect when the window closes.  However, the
 * "personalization done" button should still be displayed to handle cases
 * where the popup manager is unable to detect that a window has closed.  This
 * allows the user to signal approval manually.
 * </p>
 * </li>
 *
 * <li>
 * Gadget creates a popup object and associates event handlers with the UI
 * elements:
 *
 * <pre>
 *    // Called when the user opens the popup window.
 *    var onOpen = function() {
 *      $("personalizeDone").style.display = "block"
 *    }
 *    // Called when the user closes the popup window.
 *    var onClose = function() {
 *      $("personalizeDone").style.display = "none"
 *      fetchData();
 *    }
 *    var popup = new gadgets.oauth.Popup(
 *        response.oauthApprovalUrl,
 *        "height=300,width=200",
 *        onOpen,
 *        onClose
 *    );
 *
 *    personalizeButton.onclick = popup.createOpenerOnClick();
 *    personalizeDoneButton.onclick = popup.createApprovedOnClick();
 * </pre>
 * </li>
 *
 * <li>
 * <p>
 * When the user clicks the personalization button/link, a window is opened
 * to the approval URL.  The onOpen function is called to notify the gadget
 * that the window was opened.
 * </p>
 * </li>
 *
 * <li>
 * <p>
 * When the window is closed, the popup manager calls the onClose function
 * and the gadget attempts to fetch the user's data.
 * </p>
 * </li>
 * </ol>
 *
 * @constructor
 *
 * @description used to create a new OAuth popup window manager.
 *
 * @param {string} destination Target URL for the popup window.
 * @param {string} windowOptions Options for window.open, used to specify
 *     look and feel of the window.
 * @param {function()} onOpen Function to call when the window is opened.
 * @param {function()} onClose Function to call when the window is closed.
 */
gadgets.oauth.Popup = function(destination, windowOptions, onOpen, onClose) {
  this.destination_ = destination;
  this.windowOptions_ = windowOptions;
  this.openCallback_ = onOpen;
  this.closeCallback_ = onClose;
};

/**
 * @return {function()} an onclick handler for the "open the approval window" link.
 */


(function() {
  var callbacks = {};

  gadgets.util.registerOnLoadHandler(function() {
    gadgets.rpc.register('oauth.close', function(cbid, cburl) {
      if (this.f == '..') {
        gadgets.io.oauthReceivedCallbackUrl_ = cburl; // gadgets.io completion.
        callbacks[cbid] && callbacks[cbid]();
      }
    });
  });

  function onOpen(cbid) {
    this.cbid = cbid;
    callbacks[cbid] = this.createApprovedOnClick();
    this.openCallback_();
  }

  /**
   * Called when the user clicks to open the popup window.
   *
   * @return {boolean} false to prevent the default action for the click.
   * @private
   */
  gadgets.oauth.Popup.prototype.createOpenerOnClick = function() {
    return gadgets.util.makeClosure(this, function() {
      gadgets.rpc.call('..', 'oauth.open', gadgets.util.makeClosure(this, onOpen),
              this.destination_, this.windowOptions_
      );
      return false;
    });
  };

  /**
   * @return {function()} an onclick handler for the "I've approved" link.  This may not
   * ever be called.  If we successfully detect that the window was closed,
   * this link is unnecessary.
   */
  gadgets.oauth.Popup.prototype.createApprovedOnClick = function() {
    return gadgets.util.makeClosure(this, function() {
      if (this.cbid) {
        delete callbacks[this.cbid];
      }
      this.closeCallback_();
    });
  };
})();
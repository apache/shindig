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
 * API to assist with management of the OAuth popup window.
 *
 * Please MAKE A COPY of this file.  Do not hot link to it.
 *
 * Expected usage:
 *
 * 1) Gadget attempts to fetch OAuth data for the user and discovers that
 * approval is needed.  The gadget creates two new UI elements:
 *
 *   - a "personalize this gadget" button or link
 *   - a "personalization done" button or link, which is initially hidden.
 *
 * With any luck, the user will never need to click the "personalization done"
 * button, but it should be created and displayed in case we can't
 * automatically detect when the user has approved access to their gadget.
 *
 * 2) Gadget creates an oauthPopup object and associates event handlers with
 * the UI elements:
 *  
 *    var popup = oauthPopup();
 *    popup.setDestination(response.oauthApprovalUrl);
 *    popup.setOnOpen(function() {
 *      // Called when the popup window is opened.
 *      $("personalizeDone").style.display = "block";
 *    });
 *    popup.setOnClose(function() {
 *      // Called when the window is closed or the user indicates they've
 *      // approved access to the gadget
 *      $("personalizeButton").style.display = "none";
 *      $("personalizeDoneButton").style.display = "none";
 *      fetchDataForUser();
 *    });
 *    // Optional: popup.setWindowOptions(parameters for window.open)
 *
 *    personalizeButton.onclick = popup.createOpenerOnClick();
 *    personalizeDoneButton.onclick = popup.createApprovedOnClick();
 *
 * 3) When the user clicks the personalization button/link, a window is opened
 *    to the approval URL.
 *
 * 4) When the window is closed, the oauth popup calls the onClose function
 *    and the gadget attempts to fetch the user's data.
 */

var oauthPopup = function() {
  var destination = null;
  var windowOptions = null;
  var onOpen = function() { throw "someone forgot to call setOnOpen"; };
  var onClose = function() { throw "someone forgot to call setOnClose"; };

  // created window
  var win = null;
  // setInterval timer
  var timer = null;

  /*
   * Set the destination for the popup window.
   */
  function setDestination(dest) {
    destination = dest;
  }

  /*
   * Set the options to use for the window.open call.  By default no options
   * are specified, so a full-fledged window with status bar, menu bar, and
   * location bar is opened.
   */
  function setWindowOptions(options) {
    windowOptions = options;
  }

  /*
   * Set the function to call when the popup window is opened.
   */
  function setOnOpen(func) {
    onOpen = func;
  }

  /*
   * Set the function to call when the popup window has closed (which usually
   * indicates that the user has granted permission).
   */
  function setOnClose(func) {
    onClose = func;
  }

  function handleApproval() {
    if (timer) {
      window.clearInterval(timer);
      timer = null;
    }
    if (win) {
      win.close();
      win = null;
    }
    onClose();
    return false;
  }

  // Called at intervals to check whether the window has closed.  If it has,
  // we act as if the user had clicked the "I've approved" link.
  function checkClosed() {
    if ((!win) || win.closed) {
      win = null;
      handleApproval();
    }
  }

  /*
   * Returns an onclick handler for the "open the approval window" link.
   */
  function createOpenerOnClick() {
    return function() {
      // If a popup blocker blocks the window, we do nothing.  The user will
      // need to approve the popup, then click again to open the window.
      // Note that because we don't call window.open until the user has clicked
      // something the popup blockers *should* let us through.
      win = window.open(destination, "_blank", windowOptions);
      if (win) {
        // Poll every 100ms to check if the window has been closed
        timer = window.setInterval(checkClosed, 100);
        onOpen();
      }
      return false;
    };
  }

  /*
   * Returns an onclick handler for the "I've approved" link.  This may not
   * ever be called.  If we successfully detect that the window was closed,
   * this link is unnecessary.
   */
  function createApprovedOnClick() {
    return handleApproval;
  }

  return {
    setDestination: setDestination,
    setWindowOptions: setWindowOptions,
    setOnOpen: setOnOpen,
    setOnClose: setOnClose,
    createOpenerOnClick: createOpenerOnClick,
    createApprovedOnClick: createApprovedOnClick
  };
};

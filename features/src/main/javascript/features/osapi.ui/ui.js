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
var osapi = osapi || {};

var gadgets = gadgets || {};
gadgets.rpc = gadgets.rpc || {
  register : function(topic, callback) {}
};


/**
 * Colleciton of functions that require user interaction, via the container.
 */
osapi.ui = function() {

  var callbackIds = {};

  gadgets.rpc.register('osapi.requestShareApp_callback',
      requestShareAppCallback);

  gadgets.rpc.register('osapi.requestCreateActivity_callback',
      requestCreateActivityCallback);

  gadgets.rpc.register('osapi.requestPermission_callback',
      requestPermissionCallback);

  gadgets.rpc.register('osapi.requestSendMessage_callback',
      requestSendMessageCallback);

  var getBody = function(reason) {
    var body = reason.body;

    if (!body || body.length === 0) {
      var bodyMsgKey = reason.bodyId;
      body = gadgets.Prefs.getMsg(bodyMsgKey);
    }
    return body;
  };

  var makeResponseBody = function(success, errorCode, recipientIds) {
    var data = { success : success };
    if (recipientIds) {
      data['recipientIds'] = recipientIds;
    }
    if (errorCode) {
      data['error'] = errorCode;
    }
    return data;
  };

  var callCallback = function(callbackId, success, errorCode, recipientIds) {
    var callback = callbackIds[callbackId];
    if (callback) {
      callbackIds[callbackId] = null;
      callback(makeResponseBody(success, errorCode, recipientIds));
    }
  };

  /**
   * Function to ask the user to share the app with friends.
   * This call is container mediated.
   * @param {Array.<string>?} recipientIds recipients of request
   * @param {string} reason The message to be shown to the user
   * @param {Function} callback Gadget callback function.
   *
   */
  var requestShareApp = function(recipientIds, reason, callback) {
    var callbackId = "cId_" + Math.random();
    callbackIds[callbackId] = callback;

    var body = getBody(reason);

    gadgets.rpc.call('..', 'osapi.requestShareApp',
        null,
        callbackId,
        recipientIds,
        body);
  };

  /**
   * Receives the returned results from the parent container.
   *
   * @param {string} callbackId callback to call
   * @param {boolean} success if false, the message will not be sent
   * @param {string} errorCode an error code if success is false
   * @param {Array.<string>?} recipientIds an array of recipient IDs,
   * @private
   */
  var requestShareAppCallback = function(callbackId, success, errorCode, recipientIds) {
    callCallback(callbackId, success, errorCode, recipientIds);
  }

  /**
   * Function to ask the user for permission to create an Activity.
   * This call is container mediated.
   * @param {string} reason The message to be shown to the user
   * @param {Function} callback Gadget callback function.
   *
   */
  var requestCreateActivity = function(reason, callback) {
    var callbackId = "cId_" + Math.random();
    callbackIds[callbackId] = callback;

    var body = getBody(reason);

    gadgets.rpc.call('..', 'osapi.requestCreateActivity',
        null,
        callbackId,
        body);
  };

  /**
   * Receives the returned results from the parent container.
   *
   * @param {string} callbackId callback to call
   * @param {boolean} success if false, the message will not be sent
   * @param {string} errorCode an error code if success is false
   * @private
   */
  var requestCreateActivityCallback = function(callbackId, success, errorCode) {
    callCallback(callbackId, success, errorCode);
  };

  /**
   * Function to ask the user to share the app with friends.
   * This call is container mediated.
   * @param {string} permission Permission requested.
   * @param {string} reason The message to be shown to the user
   * @param {Function} callback Gadget callback function.
   *
   */
  var requestPermission = function(permission, reason, callback) {
    var callbackId = "cId_" + Math.random();
    callbackIds[callbackId] = callback;

    var body = getBody(reason);

    gadgets.rpc.call('..', 'osapi.requestPermission',
        null,
        callbackId,
        permission,
        body);
  };

  /**
   * Receives the returned results from the parent container.
   *
   * @param {string} callbackId callback to call
   * @param {boolean} success if false, the message will not be sent
   * @param {string} errorCode an error code if success is false
   * @private
   */
  var requestPermissionCallback = function(callbackId, success, errorCode) {
    callCallback(callbackId, success, errorCode);
  };  

  /**
   * Function to ask the user for permission to send a message to friends.
   * This call is container mediated.
   * @param {Array.<string>?} recipientIds recipients of request
   * @param {string} reason The message to be shown to the user
   * @param {Function} callback Gadget callback function.
   *
   */
  var requestSendMessage = function(recipientIds, message, callback) {
    var callbackId = "cId_" + Math.random();
    callbackIds[callbackId] = callback;

    var body = getBody(message);

    gadgets.rpc.call('..', 'osapi.requestSendMessage',
        null,
        callbackId,
        recipientIds,
        body);
  };

  /**
   * Receives the returned results from the parent container.
   *
   * @param {string} callbackId callback to call
   * @param {boolean} success if false, the message will not be sent
   * @param {string} errorCode an error code if success is false
   * @param {Array.<string>?} recipientIds recipients of request
   * @private
   */
   var requestSendMessageCallback = function(callbackId, success, errorCode, recipientIds) {
     callCallback(callbackId, success, errorCode, recipientIds);   
  }

  return {
    requestCreateActivity : requestCreateActivity,
    requestShareApp : requestShareApp,
    requestPermission : requestPermission,
    requestSendMessage : requestSendMessage
  };
}();

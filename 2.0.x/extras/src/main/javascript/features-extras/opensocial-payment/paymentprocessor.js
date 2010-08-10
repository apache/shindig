/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview Container-side codes as a processor logic for the virtual 
 * currency payment functionality.
 */

var shindig = shindig || {};


/**
 * @static
 * @class Provides the virtual currency payment processor features on 
          container side. Handles the payment request from app, prompts the 
          container processor page for user to confirm the payment, and 
          passes the response back to the app. The container need to implement 
          the open/close event functions to fulfill the functionality.
 * @name shindig.paymentprocessor
 */
shindig.paymentprocessor = (function() {
  /**
   * The state indicating if the processor panel is on or off.
   * @type {boolean}
   */
  var isOpened_ = false;

  /**
   * A set of params for the procedure that holds necessary data needed for container 
   * processor panel page. In the implementation of the processor panel page, you can use 
   * <code>getParam</code> function and <code>setParam</code> to access the values in this set. Here
   * <paymentJson> is the pure json format of an opensocial.Payment object defined on gadget side.
   * E.g.  getParam('payment.orderId') returns the orderId field in paymentJson.
   *
   * Here lists the preset data of params set:
   * 
   * {
   *   frameId : <string>,
   *   appTitle : <string>,
   *   appSpec : <string>,
   *   stoken : <string>,
   *   callbackId : <string>,
   *   
   *   payment : <paymentJson>,      // Only for requestPayment process
   *
   *   records : {                   // Only for requestPaymentRecords process
   *     responseCode : <string>,
   *     responseMessage : <string>,
   *     payments : {
   *       <orderId> : <paymentJson>,
   *       <orderId> : <paymentJson>,
   *       ...
   *     }
   *   },
   *   reqParams : <object>          // Only for requestPaymentRecords process
   * }
   *
   * @type {Object.<string, Object>}
   */
  var processorParams_ = null;

  /**
   * A set of event functions which allow customizing the UI and actions of the
   * processor panel by container. They are passed in and registered in the
   * init functions.
   * @type {Object.<string, function()>}
   */
  var events_ = {};

  /**
   * Initiates the gadget parameters for the current processor. It uses a frameId
   * which indicates which gadget is requesting payment. 
   *
   * NOTE: The 'shindig-container' feature is required.
   * @see /features/shindig-container/
   *
   * @return {Object.<string, string>} The gadget meta data.
   */
  function initGadgetParams(frameId) {
    var params = null;
    if (shindig.container && shindig.container.gadgetService) {
      params = {};
      params['frameId'] = frameId;

      // By default, will set the title and spec with default value.
      params['appTitle'] = 'Unknown Title';
      params['appSpec'] = 'Unknown SpecUrl';
      // This part need the shindig.container service support or customized by 
      // container page.
      var thisGadget = shindig.container.getGadget(
          shindig.container.gadgetService.getGadgetIdFromModuleId(frameId));
      if (thisGadget) {
        params['appTitle'] = thisGadget['title'];
        params['appSpec'] = thisGadget['specUrl'];
        params['stoken'] = thisGadget['securityToken'];
      }
    }
    return params;
  };


  /**
   * Handles the request called via rpc from opensocial.requestPayment on the
   * app side. Turns on the processor panel.
   * <p>
   * The 'this' in this function is the rpc object, thus contains
   * some information of the app.
   * </p>
   * <p>
   * See the definition of processorParams_ for the structure of the underlying
   * object.
   * </p>
   *
   * @param {Object.<string, Object>} paymentJson The json object holding the
   *     payment parameters from the app with ITEMS, AMOUNT, MESSAGE and
   *     PARAMETERS fields set. Note that this object is serialized and passed
   *     through RPC channel so all functions are lost.
   */
  function openPayment_(callbackId, paymentJson) {
    // Checks if the processor panel should be opened.
    if (isOpened_) {
      // Shouldn't continue if the processor is already opened.
      paymentJson['responseCode'] = 'PAYMENT_PROCESSOR_ALREADY_OPENED';
    }

    if (!paymentJson['amount'] || paymentJson['amount'] <= 0) {
      // TODO: Need more check on the AMOUNT value and other values.
      paymentJson['responseCode'] = 'MALFORMED_REQUEST';
    }

    if (!events_['paymentOpen']) {
      // If the open event handle is not registered, return not-implemented.
      paymentJson['responseCode'] = 'NOT_IMPLEMENTED';
    }

    // Initialize the processor parameters.
    processorParams_ = initGadgetParams(this.f);
    if (processorParams_ == null) {
      paymentJson['responseCode'] = 'NOT_IMPLEMENTED';
    }
    
    if (paymentJson['responseCode'] && paymentJson['responseCode'] != 'OK') {
      // callback immediately if any errorcode exists here.
      try {
        gadgets.rpc.call(this.f, 'shindig.requestPayment_callback', null,
                         callbackId, paymentJson);
      } finally {
        return;
      }
    }

    isOpened_ = true;

    // Fill the payment fields before the payment process.
    paymentJson['orderedTime'] = new Date().getTime();
    paymentJson['message'] = gadgets.util.escapeString(paymentJson['message']);

    processorParams_['callbackId'] = callbackId;
    processorParams_['payment'] = paymentJson;

    // Call the container's open event to display the processor panel UI.
    events_.paymentOpen();
  };


  /**
   * Invoked by button click event in processor panel on container side to 
   * close the processor panel. Will calls the rpc callback in app.
   */
  function closePayment_() {
    if (!isOpened_) {
      return;
    }

    // Call the container's close event to hide the processor panel.
    // The close event is optional. If not set, do nothing.
    // (NOTE that the panel is still visible if do nothing...)
    if (events_.paymentClose) {
      events_.paymentClose();
    }

    // Return to the app via rpc.
    try {
      gadgets.rpc.call(processorParams_['frameId'], 
                       'shindig.requestPayment_callback',
                       null,
                       processorParams_['callbackId'],
                       processorParams_['payment']);
    } catch(e) {
      // TODO
    } finally {
      // Reset the underlying data.
      isOpened_ = false;
      processorParams_ = null;
    }
  };


  /**
   * Handles the request called via rpc from opensocial.requestPaymentRecords
   * on the app side. Turns on the processor panel.
   * <p>
   * The 'this' in this function is the rpc object, thus contains
   * some information of the app.
   * </p>
   * <p>
   * See the definition of processorParams_ for the structure of the underlying object.
   * </p>
   *
   * @param {Object.<opensocial.Payment.RecordsRequestFields, Object>} reqParams
   *     Additional parameters to pass to the request. 
   */
  function openPaymentRecords_(callbackId, reqParams) {
    // This object is for response.
    var paymentRecordsJson = {'payments' : {}};

    // Checks if the processor panel should be opened.
    if (isOpened_) {
      // Shouldn't continue if the processor is already opened.
      paymentRecordsJson['responseCode'] = 'PAYMENT_PROCESSOR_ALREADY_OPENED';
    }

    if (!events_['paymentRecordsOpen']) {
      // If the open event handler is not registered, return not-implemented.
      paymentRecordsJson['responseCode'] = 'NOT_IMPLEMENTED';
    }

    if (paymentRecordsJson['responseCode'] && 
        paymentRecordsJson['responseCode'] != 'OK') {
      // callback immediately if any errorcode exists here.
      try {
        gadgets.rpc.call(this.f, 'shindig.requestPaymentRecords_callback', null,
                         callbackId, paymentRecordsJson);
      } finally {
        return;
      }
    }

    isOpened_ = true;

    // Initialize the processor parameters.
    processorParams_ = initGadgetParams(this.f);
    processorParams_['callbackId'] = callbackId;
    processorParams_['records'] = paymentRecordsJson;
    
    processorParams_['reqParams'] = reqParams;

    // Call the container's open event to display the processor panel UI.
    events_['paymentRecordsOpen']();
  };


  /**
   * Invoked by button click event in processor panel on container side to 
   * close the processor panel. Will calls the rpc callback in app.
   */
  function closePaymentRecords_() {
    if (!isOpened_) {
      return;
    }

    // Call the container's cancel event to do some UI change if needed.
    if (events_['paymentRecordsClose']) {
      events_['paymentRecordsClose']();
    }

    try {
      // Return to the app via rpc.
      gadgets.rpc.call(processorParams_['frameId'], 
                     'shindig.requestPaymentRecords_callback',
                     null,
                     processorParams_['callbackId'],
                     processorParams_['records']);
    } finally {
      // Reset the underlying data.
      isOpened_ = false;
      processorParams_ = null;
    }

  };

  /**
   * Accessor to get the parameter value by a key. The key can be chained
   * using dot symbol. E.g.  getParam('foo.bar') will return 
   * processParams_.foo.bar .
   *
   * @param {string} key The access key.
   * @return {Object} The value stored in processParams_ on the given key. 
   */
  function getParam_(key) {
    if (!key) return null;
    var value = null;
    try {
      var arr = key.split('.');
      if (arr.length > 0) {
        var prop = processorParams_;
        for (var i = 0; i < arr.length; i++) {
          prop = prop[arr[i]];
        }
        value = prop;
      }
    } catch(e) {
      value = null;
    }
    return value;
  };

  /**
   * Accessor to set the parameter value by a key. The key can be chained
   * using dot symbol. E.g. setParam('foo.bar', value) will set the value on
   * processParams_.foo.bar .
   *
   * @param {string} key The access key.
   * @param {Object} value The value to be set.
   */
  function setParam_(key, value) {
    if (!key) return;
    try {
      var arr = key.split('.');
      if (arr.length > 1) {
        var prop = processorParams_;
        for (var i = 0; i < arr.length - 1; i++) {
          prop = prop[arr[i]];
        }
        prop[arr[arr.length - 1]] = value;
      }
    } finally {
      return;
    }
  };

  return /** @scope shindig.paymentprocessor */ {
    /**
     * Initializes the 'requestPayment' rpc. It's called by container page in 
     * onload function.
     */
    initPayment : function(openEvent, closeEvent) {
      events_.paymentOpen = openEvent;
      events_.paymentClose = closeEvent;
      gadgets.rpc.register('shindig.requestPayment', openPayment_);
    },

    /**
     * Initializes the 'requestPaymentRecords' rpc. It's called by container 
     * processor page in onload function.
     */
    initPaymentRecords : function(openEvent, closeEvent) {
      events_.paymentRecordsOpen = openEvent;
      events_.paymentRecordsClose = closeEvent;
      gadgets.rpc.register('shindig.requestPaymentRecords',
                           openPaymentRecords_);
    },

    /**
     * The open function for 'requestPayment' pannel. Invoked by rpc from app.
     */
    openPayment : openPayment_,

    /**
     * The close function for 'requestPayment' pannel. Invoked by button click
     * event in processor panel on container side. 
     */
    closePayment : closePayment_,

    /**
     * The open function for 'requestPaymentRecords' pannel. Invoked by rpc 
     * from app.
     */
    openPaymentRecords : openPaymentRecords_,

    /**
     * The close function for 'requestPaymentRecords' pannel. Invoked by button 
     * click event in processor panel on container side. 
     */
    closePaymentRecords : closePaymentRecords_,

    /**
     * Exposes the processor parameters to processor panel.
     */
    getParam : getParam_,

    /**
     * Updates the processor parameters from processor panel.
     */
    setParam : setParam_
  };

})();

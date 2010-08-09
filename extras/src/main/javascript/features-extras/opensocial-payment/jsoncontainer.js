//TODO - originally done during construction
// For opensocial virtual currency extension.
gadgets.rpc.register('shindig.requestPayment_callback',
    JsonRpcContainer.requestPaymentCallback_);
// For opensocial virtual currency extension.
gadgets.rpc.register('shindig.requestPaymentRecords_callback',
    JsonRpcContainer.requestPaymentRecordsCallback_);

/**
 * For OpenSocial VirtualCurrency Ext.
 * The function invokes the whole process of a payment request. It calls the
 * payment processor open function in parent container.
 *
 * @param {opensocial.Payment} payment The Payment object.
 * @param {function(opensocial.ResponseItem)=} opt_callback The finishing
 *     callback function.
 * @private
 */
JsonRpcContainer.prototype.requestPayment = function(payment, opt_callback) {
  if (!payment) {
    if (opt_callback) {
      opt_callback(new opensocial.ResponseItem(null, payment, 
        opensocial.Payment.ResponseCode.MALFORMED_REQUEST, 
        'Payment object is undefined.'));
    }
    return;
  }
 
  var callbackId = "cId_" + Math.random();
  callbackIdStore[callbackId] = opt_callback;
  // The rpc target is registered in container payment processor page.
  gadgets.rpc.call('..', 'shindig.requestPayment',
      null,
      callbackId,
      payment.toJsonObject());
};

/**
 * For OpenSocial VirtualCurrency Ext. The callback function of receives the
 * returned results from the parent container.
 *
 * @param {Object.<string, Object>} paymentJson A jsonpayment object with
 *     parameters filled. 
 * @private
 */
JsonRpcContainer.requestPaymentCallback_ = function(callbackId, paymentJson) {
  callback = callbackIdStore[callbackId];
  if (callback) {
    var errorCode = opensocial.Payment.ResponseCode[
        paymentJson[opensocial.Payment.Field.RESPONSE_CODE]];
    var message = paymentJson[opensocial.Payment.Field.RESPONSE_MESSAGE];

    paymentJson[opensocial.Payment.Field.RESPONSE_CODE] = errorCode;
    var payment = new JsonPayment(paymentJson, false);
    var responseItem = new opensocial.ResponseItem(
        null,
        payment,
        (errorCode == opensocial.Payment.ResponseCode.OK ? null : errorCode),
        message);
    callback(responseItem);
  }
};

/**
 * For OpenSocial VirtualCurrency Ext.
 * The function invokes the payment records panel in parent container.
 *
 * @param {function(opensocial.ResponseItem)=} opt_callback The finishing
 *     callback function.
 * @param {Object.<pensocial.Payment.RecordsRequestFields, Object>=}
 *     opt_params Additional parameters to pass to the request. 
 * @private
 */
JsonRpcContainer.prototype.requestPaymentRecords = function(opt_callback, opt_params) {
  var callbackId = "cId_" + Math.random();
  callbackIdStore[callbackId] = opt_callback;

  // The rpc target is registered in container payment records page.  
  gadgets.rpc.call('..', 'shindig.requestPaymentRecords',
      null, callbackId, opt_params);
};

/**
 * For OpenSocial VirtualCurrency Ext. The callback function of receives the
 * returned results from the parent container.
 *
 * @param {Object.<string, Object>} opt_resultParams The fields set with
 *     result parameters.
 * @private
 */
JsonRpcContainer.requestPaymentRecordsCallback_ = function(callbackId, recordsJson) {
  callback = callbackIdStore[callbackId];
  if (callback) {
    var errorCode = opensocial.Payment.ResponseCode[
        recordsJson[opensocial.Payment.Field.RESPONSE_CODE]];
    var message = recordsJson[opensocial.Payment.Field.RESPONSE_MESSAGE];
    var records = [];
    var payments = recordsJson['payments'];
    for (var orderId in payments) {
      records.push(new JsonPayment(payments[orderId], false));
    }
    
    var responseItem = new opensocial.ResponseItem(
        null,
        records, 
        (errorCode == opensocial.Payment.ResponseCode.OK ? null : errorCode), message);
    callback(responseItem);
  }
};
 
 
JsonRpcContainer.prototype.newPayment = function(opt_params) {
  return new JsonPayment(opt_params, true);
};

JsonRpcContainer.prototype.newBillingItem = function(opt_params) {
  return new JsonBillingItem(opt_params);
};

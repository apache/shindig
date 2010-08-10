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
 * @class
 * Representation of a payment.
 ?
 * @name opensocial.Payment
 */


/**
 * Base interface for all payment objects.
 *
 * @param {Object.<opensocial.Payment.Field, Object>} params
 *    Parameters defining the payment.
 * @private
 * @constructor
 */
opensocial.Payment = function(params) {
  this.fields_ = params || {};
  this.fields_[opensocial.Payment.Field.PAYMENT_TYPE] = 
      this.fields_[opensocial.Payment.Field.PAYMENT_TYPE] || 
      opensocial.Payment.PaymentType.PAYMENT;
};


opensocial.Payment.prototype.isPayment = function() {
  return this.fields_[opensocial.Payment.Field.PAYMENT_TYPE] == 
      opensocial.Payment.PaymentType.PAYMENT;
};

opensocial.Payment.prototype.isCredit = function() {
  return this.fields_[opensocial.Payment.Field.PAYMENT_TYPE] == 
      opensocial.Payment.PaymentType.CREDIT;
};

opensocial.Payment.prototype.isComplete = function() {
  return !!this.fields_[opensocial.Payment.Field.PAYMENT_COMPLETE];
};


/**
 * @static
 * @class
 * All of the fields that a payment object can have.
 *
 * <p>The ITEMS, AMOUNT, MESSAGE, PARAMETERS are required for the request. </p>
 *
 * <p>And the ORDER_ID, ORDERED_TIME, SUBMITTED_TIME, EXECUTED_TIME fields 
 * will be filled during the procedure and return to the app. </p>
 * <p>
 * <b>See also:</b>
 * <a
 * href="opensocial.Payment.html#getField">opensocial.Payment.getField()</a>
 * </p>
 *
 * @name opensocial.Payment.Field
 */
opensocial.Payment.Field = {
  /**
   * @member opensocial.Payment.Field
   */
  SANDBOX : 'sandbox',

  /**
   * @member opensocial.Payment.Field
   */
  ITEMS : 'items',

  /**
   * @member opensocial.Payment.Field
   */
  AMOUNT : 'amount',

  /**
   * @member opensocial.Payment.Field
   */
  MESSAGE : 'message',

  /**
   * @member opensocial.Payment.Field
   */
  PARAMETERS : 'parameters',

  /**
   * @member opensocial.Payment.Field
   */
  PAYMENT_TYPE : 'paymentType',

  /**
   * @member opensocial.Payment.Field
   */
  ORDER_ID : 'orderId',

  /**
   * @member opensocial.Payment.Field
   */
  ORDERED_TIME : 'orderedTime',

  /**
   * @member opensocial.Payment.Field
   */
  SUBMITTED_TIME : 'submittedTime',

  /**
   * @member opensocial.Payment.Field
   */
  EXECUTED_TIME : 'executedTime',

  /**
   * @member opensocial.Payment.Field
   */
  RESPONSE_CODE : 'responseCode',

  /**
   * @member opensocial.Payment.Field
   */
  RESPONSE_MESSAGE : 'responseMessage',

  /**
   * @member opensocial.Payment.Field
   */
  PAYMENT_COMPLETE : 'paymentComplete'

};


/**
 * Gets the payment field data that's associated with the specified key.
 *
 * @param {string} key The key to get data for;
 *   see the <a href="opensocial.Payment.Field.html">Field</a> class
 * for possible values
 * @param {Object.<opensocial.DataRequest.DataRequestFields, Object>=}
 *  opt_params Additional
 *    <a href="opensocial.DataRequest.DataRequestFields.html">params</a>
 *    to pass to the request.
 * @return {string} The data
 * @member opensocial.Payment
 */
opensocial.Payment.prototype.getField = function(key, opt_params) {
  return opensocial.Container.getField(this.fields_, key, opt_params);
};


/**
 * Sets data for this payment associated with the given key.
 *
 * @param {string} key The key to set data for
 * @param {string} data The data to set
 */
opensocial.Payment.prototype.setField = function(key, data) {
  return this.fields_[key] = data;
};


/**
 * @static
 * @class
 * Types for a payment.
 *
 * @name opensocial.Payment.PaymentType
 */
opensocial.Payment.PaymentType = {
  /**
   * @member opensocial.Payment.PaymentType
   */
  PAYMENT : 'payment',

  /**
   * @member opensocial.Payment.PaymentType
   */
  CREDIT : 'credit'
};


/**
 * @static
 * @class
 * Possible response codes for the whole payment process.
 *
 * @name opensocial.Payment.ResponseCode
 */
opensocial.Payment.ResponseCode = {
  /**
   * @member opensocial.Payment.ResponseCode
   */
  APP_LOGIC_ERROR : 'appLogicError',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  APP_NETWORK_FAILURE : 'appNetworkFailure',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  INSUFFICIENT_MONEY : 'insufficientMoney',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  INVALID_TOKEN : 'invalidToken',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  MALFORMED_REQUEST : 'malformedRequest',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  NOT_IMPLEMENTED : 'notImplemented',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  OK : 'ok',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  PAYMENT_ERROR : 'paymentError',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  PAYMENT_PROCESSOR_ALREADY_OPENED : 'paymentProcessorAlreadyOpened',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  UNKNOWN_ERROR : 'unknownError',

  /**
   * @member opensocial.Payment.ResponseCode
   */
  USER_CANCELLED : 'userCancelled'
};


/**
 * @static
 * @class
 * Request fields for requesting payment records.
 *
 * @name opensocial.Payment.RecordsRequestFields
 */
opensocial.Payment.RecordsRequestFields = {

  /**
   * @member opensocial.Payment.RecordsRequestFields
   */
  SANDBOX : 'sandbox',

  /**
   * @member opensocial.Payment.RecordsRequestFields
   */
  MAX : 'max',

  /**
   * @member opensocial.Payment.RecordsRequestFields
   */
  INCOMPLETE_ONLY : 'incompleteOnly'

};





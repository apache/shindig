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
 * Requests the container to open a payment processor panel to show and submit
 * user's order. If the container does not support this method the callback 
 * will be called with a opensocial.ResponseItem. The response item will have 
 * its error code set to NOT_IMPLEMENTED.
 *
 * @param {opensocial.Payment} payment The Payment object.
 * @param {function(opensocial.ResponseItem)=} opt_callback The finishing
 *     callback function.
 */
opensocial.Container.prototype.requestPayment = function(payment,
    opt_callback) {
  if (opt_callback) {
    window.setTimeout(function() {
      opt_callback(new opensocial.ResponseItem(
          null, payment, opensocial.Payment.ResponseCode.NOT_IMPLEMENTED, 
          null));
    }, 0);
  }
};

/**
 * Requests the container to open a payment records processor panel to list all
 * completed or incomplete payments of the user on current app and allowing 
 * users to fix the incomplete payments. If the container does not support 
 * this method the callback will be called with a opensocial.ResponseItem. 
 * The response item will have its error code set to NOT_IMPLEMENTED.
 *
 * @param {function(opensocial.ResponseItem)=} opt_callback The finishing
 *     callback function.
 * @param {Object.<opensocial.Payment.RecordsRequestFields, Object>=}
 *     opt_params Additional parameters to pass to the request. 
 */
opensocial.Container.prototype.requestPaymentRecords = function(opt_callback, 
    opt_params) {
  if (opt_callback) {
    window.setTimeout(function() {
      opt_callback(new opensocial.ResponseItem(
          null, payment, opensocial.Payment.ResponseCode.NOT_IMPLEMENTED, 
          null));
    }, 0);
  }
};


/**
 * Creates a payment object.
 * Creates a payment object.
 * @param {Map.&lt;opensocial.Payment.Field, Object&gt;} params
 *     Parameters defining the payment object.
 * @return {opensocial.Payment} The new
 *     <a href="opensocial.Payment.html">Payment</a> object
 * @private
 */
opensocial.Container.prototype.newPayment = function(params) {
  return new opensocial.Payment(params);
};


/**
 * Creates a billing item object.
 * @param {Map.&lt;opensocial.BillingItem.Field, Object&gt;} params
 *     Parameters defining the billing item object.
 * @return {opensocial.BillingItem} The new
 *     <a href="opensocial.BillingItem.html">BillingItem</a> object
 * @private
 */
opensocial.Container.prototype.newBillingItem = function(params) {
  return new opensocial.BillingItem(params);
};


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
 * Representation of a billing item.
 ?
 * @name opensocial.BillingItem
 */


/**
 * Base interface for billing item objects.
 *
 * @param {Map.&lt;opensocial.BillingItem.Field, Object&gt;} params
 *    Parameters defining the billing item.
 * @private
 * @constructor
 */
opensocial.BillingItem = function(params) {
  this.fields_ = params || {};
  this.fields_[opensocial.BillingItem.Field.COUNT] = 
      this.fields_[opensocial.BillingItem.Field.COUNT] || 1;
};

/**
 * @static
 * @class
 * All of the fields that a billing item object can have.
 *
 * <p>The SKU_ID and PRINE are required for the request. </p>
 *
 * <p>
 * <b>See also:</b>
 * <a href="opensocial.BillingItem.html#getField">
 *    opensocial.BillingItem.getField()</a>
 * </p>
 *
 * @name opensocial.Payment.Field
 */
opensocial.BillingItem.Field = {
  /**
   * @member opensocial.BillingItem.Field
   */
  SKU_ID : 'skuId',

  /**
   * @member opensocial.BillingItem.Field
   */
  PRICE : 'price',

  /**
   * @member opensocial.BillingItem.Field
   */
  COUNT : 'count',

  /**
   * @member opensocial.BillingItem.Field
   */
  DESCRIPTION : 'description'

};


/**
 * Gets the billing item field data that's associated with the specified key.
 *
 * @param {String} key The key to get data for;
 *   see the <a href="opensocial.BillingItem.Field.html">Field</a> class
 * for possible values
 * @param {Map.&lt;opensocial.DataRequest.DataRequestFields, Object&gt;}
 *  opt_params Additional
 *    <a href="opensocial.DataRequest.DataRequestFields.html">params</a>
 *    to pass to the request.
 * @return {String} The data
 * @member opensocial.BillingItem
 */
opensocial.BillingItem.prototype.getField = function(key, opt_params) {
  return opensocial.Container.getField(this.fields_, key, opt_params);
};


/**
 * Sets data for this billing item associated with the given key.
 *
 * @param {String} key The key to set data for
 * @param {String} data The data to set
 */
opensocial.BillingItem.prototype.setField = function(key, data) {
  return this.fields_[key] = data;
};



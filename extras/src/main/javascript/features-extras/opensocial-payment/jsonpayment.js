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

/*global opensocial */

/**
 * Base interface for json based payment objects.
 * NOTE: This class is mainly copied from jsonactivity.js
 *
 * @private
 * @constructor
 */
var JsonPayment = function(opt_params, opt_skipConversions) {
  opt_params = opt_params || {};
  if (!opt_skipConversions) {
    JsonPayment.constructArrayObject(opt_params, 'items', JsonBillingItem);
  }
  opensocial.Payment.call(this, opt_params);
};
JsonPayment.inherits(opensocial.Payment);

JsonPayment.prototype.toJsonObject = function() {
  var jsonObject = JsonPayment.copyFields(this.fields_);

  var oldBillingItems = jsonObject['items'] || [];
  var newBillingItems = [];
  for (var i = 0; i < oldBillingItems.length; i++) {
    newBillingItems[i] = oldBillingItems[i].toJsonObject();
  }
  jsonObject['items'] = newBillingItems;

  return jsonObject;
};


// TODO: Split into separate class
var JsonBillingItem = function(opt_params) {
  opensocial.BillingItem.call(this, opt_params);
};
JsonBillingItem.inherits(opensocial.BillingItem);

JsonBillingItem.prototype.toJsonObject = function() {
  return JsonPayment.copyFields(this.fields_);
};


// TODO: Pull this method into a common class, it is from jsonperson.js
JsonPayment.constructArrayObject = function(map, fieldName, className) {
  var fieldValue = map[fieldName];
  if (fieldValue) {
    for (var i = 0; i < fieldValue.length; i++) {
      fieldValue[i] = new className(fieldValue[i]);
    }
  }
};

// TODO: Pull into common class as well
JsonPayment.copyFields = function(oldObject) {
  var newObject = {};
  for (var field in oldObject) {
    newObject[field] = oldObject[field];
  }
  return newObject;
};


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
 * @fileoverview Provides stubs and fakes for the gadgets namespace
 * that is used in the API. This is used by jsunit tests.
 */
var gadgets = gadgets || {};

gadgets.util = {};

gadgets.util.registerOnLoadHandler = function(callback) {
  callback.call();
};

gadgets.util.getUrlParameters = function() {
  var params = {};
  var waveParamName = 'wave';
  params[waveParamName] = true;
  params.hasOwnProperty = function(key) {
    return !!this[key];
  };
  return params;
};

gadgets.json = {};

gadgets.json.parse = function(data) {
  return data;
};

gadgets.rpc = {};

gadgets.rpc.register = function() {};

gadgets.rpc.call = function() {};

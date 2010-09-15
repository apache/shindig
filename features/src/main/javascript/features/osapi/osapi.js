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

(function() {
  /**
   * Called by the transports for each service method that they expose
   * @param {string} method  The method to expose e.g. "people.get".
   * @param {Object.<string,Object>} transport The transport used to execute a call for the method.
   */
  osapi._registerMethod = function(method, transport) {
    var has___ = typeof ___ !== 'undefined';

    // Skip registration of local newBatch implementation.
    if (method == 'newBatch') {
      return;
    }

    // Lookup last method value.
    var parts = method.split('.');
    var last = osapi;
    for (var i = 0; i < parts.length - 1; i++) {
      last[parts[i]] = last[parts[i]] || {};
      last = last[parts[i]];
    }

    // Use the batch as the actual executor of calls.
    var apiMethod = function(rpc) {
      var batch = osapi.newBatch();
      var boundCall = {};
      boundCall.execute = function(callback) {
        var feralCallback = has___ ? ___.untame(callback) : callback;
        var that = has___ ? ___.USELESS : this;
        batch.add(method, this);
        batch.execute(function(batchResult) {
          if (batchResult.error) {
            feralCallback.call(that, batchResult.error);
          } else {
            feralCallback.call(that, batchResult[method]);
          }
        });
      }
      if (has___) {
        ___.markInnocent(boundCall.execute, 'execute');
      }
      // TODO: This shouldnt really be necessary. The spec should be clear enough about
      // defaults that we dont have to populate this.
      rpc = rpc || {};
      rpc.userId = rpc.userId || '@viewer';
      rpc.groupId = rpc.groupId || '@self';

      // Decorate the execute method with the information necessary for batching
      boundCall.method = method;
      boundCall.transport = transport;
      boundCall.rpc = rpc;

      return boundCall;
    };
    if (has___ && typeof ___.markInnocent !== 'undefined') {
      ___.markInnocent(apiMethod, method);
    }

    if (last[parts[parts.length - 1]]) {
      gadgets.warn('Skipping duplicate osapi method definition ' + method + ' on transport ' + transport.name);
    } else {
      last[parts[parts.length - 1]] = apiMethod;
    }
  };
})();

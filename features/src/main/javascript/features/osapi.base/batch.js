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

/**
 * It is common to batch requests together to make them more efficient.
 *
 * Currently, since makeRequest is proxied, these requests are handled separately to
 * create more parallelism.
 */
osapi.newBatch = function() {

  /*
   * Latch that waits for the batch of json requests and each makeRequest call to finish before
   * calling the user callback with the responseMap.
   * Note, this makes the makeRequest calls happen in parallel, but changes the order of those calls
   * luckily everything in a batch is keyed instead of requiring ordering.
   */
  var newCountdownLatch = function(count, userCallback) {
    var responseMap = {};

    var leftTodo = function() {
      var done = false;
      return {
        down : function() {
          if (!done) {
            count--;
            if (count === 0) {
              done = true;
              userCallback(responseMap);
            }
          }
        }
      };
    }();

    var finishNonProxiedRequest = function(results) {
      for (var result in results) if (results.hasOwnProperty(result)) {
        responseMap[result] = results[result];
        // TODO does this copy errors over as well.
      }
      leftTodo.down();
    };

    var finishProxiedRequest = function(key, result, error) {
      responseMap[key] = result;
      if (error) {
        osapi.base.setGlobalError(responseMap, error.error);
      }
      leftTodo.down();
    };

    return {
      finishedJsonRequest : finishNonProxiedRequest,
      finishProxiedRequest : finishProxiedRequest
    };
  };


  var callbackAsyncAndEmpty = function(userCallback) {
    window.setTimeout(function() {
        userCallback({ data : {}});
      },
      0);
  };

  var each = function(arr, fn) {
    for (var i=0;i<arr.length;i++) {
      fn.apply(null, [arr[i]]);
    }
  };

  var executeJsonRequests = function(json, getDataFromResult, countDownLatch) {
    osapi.newBatchJsonRequest(json, getDataFromResult).execute(function(results) {
      countDownLatch.finishedJsonRequest(results);
    });
  };

  var executeProxiedRequests = function(proxiedRequests, countDownLatch) {
    if (proxiedRequests.length > 0) {
      var makeRequestExecutor = function(keyRequestPair) {
        keyRequestPair.request.execute(function(makeRequestResult) {
          if (makeRequestResult.error) {
            countDownLatch.finishProxiedRequest(keyRequestPair.key, {
              error : { code : osapi.base.translateHttpError("Error " +
                                                              makeRequestResult.error['code']),
                  message :makeRequestResult.error.message }},
            true);
          } else {
            if (makeRequestResult.data) {
              countDownLatch.finishProxiedRequest(keyRequestPair.key, makeRequestResult.data);
            }
          }
        });
      };
      each(proxiedRequests, makeRequestExecutor);
    }
  };

  return function() {
    var that = {};
    
    var jsonRequests = [];
    var proxiedRequests = [];

    /**
     * Create a new request in the batch
     * @param {string} key id for the request
     * @param {object} request the opensocial request object
     */
    var add = function(key, request) {
      if (request.isMakeRequest) {
        proxiedRequests.push({key : key, request : request});
      } else {
        jsonRequests.push({key : key, request : request});
      }
      return that;
    };

    var json = function() {
      var jsonParams = [];
      for (var i = 0; i < jsonRequests.length; i++) {
        var request = jsonRequests[i];
        var requestJson = request.request.json()[0]; // single requests make a json array by default
        requestJson.id = request.key;
        jsonParams.push(requestJson);
      }
      return jsonParams;
    };

    var getDataFromResult = function(result) {      
      var responseMap = {};

      var data = result.data; // the json array
      for (var k = 0; k < jsonRequests.length; k++) {
        var response = data[k];
        if (response.error) {
          var error = { error : { code : osapi.base.translateHttpError("Error "
              + response.error['code']),
            message : response.error.message }};
          responseMap[response.id] = error;
          osapi.base.setGlobalError(responseMap, error.error);
        } else {
          if (response.id !== jsonRequests[k].key) {
            throw "Response Id doesn't match request key";
          } else {
            if (response.data.list) { // array result
              responseMap[response.id] = response.data.list;
            } else {  //single result
              responseMap[response.id] = response.data;
            }
          }
        }
      }
      return responseMap;
    };

    var executeRequests = function(userCallback) {
      var count =  ((jsonRequests.length > 0) ? 1 : 0 ) + proxiedRequests.length;
      var countDownLatch = newCountdownLatch(count, userCallback);
      executeJsonRequests(json, getDataFromResult, countDownLatch);
      executeProxiedRequests(proxiedRequests, countDownLatch);
    };

    /**
     * Call to make a batch execute its requests.
     * @param {Function} userCallback the callback to the gadget where results are passed.
     */
    var execute =  function(userCallback) {
      if (jsonRequests.length == 0 && proxiedRequests.length == 0) {
        callbackAsyncAndEmpty(userCallback);
      } else {                            
        executeRequests(userCallback);
      }
    };

    that.execute = execute;
    that.add = add;
    return that;
  };
}();


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
 * Note: the container config specified endpoints at which services are to be found.
 * When creating a batch, the calls are split out into separate requests based on the 
 * endpoint, as it may get sent to a different server on the backend.
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

    /**
     * Countdown call when a request is done.
     */
    var finishNonProxiedRequest = function(results) {
      for (var result in results) if (results.hasOwnProperty(result)) {
        responseMap[result] = results[result];
        // TODO does this copy errors over as well.
      }
      leftTodo.down();
    };


    return {
      finishedJsonRequest : finishNonProxiedRequest
    };
  };

  /**
   * Handle empty calls properly (asynchronously) to prevent browser hiccups
   */
  var callbackAsyncAndEmpty = function(userCallback) {
    window.setTimeout(function() {
        userCallback({ data : {}});
      },
      0);
  };

  var each = function(arr, fn) {
    for (var i in arr) if (arr.hasOwnProperty(i)) {
      fn.apply(null, [arr[i]]);
    }
  };
  
  var length = function(arr) {
    var len = 0;
    each(arr, function() { len++;});
    return len;
  }

  /**
   * takes the list of requests to execute in this call,
   * @param {object} jsonRequests the collection of requests, by endpoint
   * @param {function} json the json generator function for a batch 
   * @param {function} getDataFromResult the result processor, getDataFromResult
   * @param {object} The countdown latch which the batch is synchronized on before calling the 
   *                 user callback
   */
  var executeJsonRequests = function(jsonRequests, json, getDataFromResult, countDownLatch) {
    for (var endpoint in jsonRequests) if (jsonRequests.hasOwnProperty(endpoint)) {
      var jsonGenerator = function() {
        var ep = endpoint;
        return json(ep);
      };
      var getDataFromResultForEndpoint = function(response) {
        var ep = endpoint;
        return getDataFromResult(ep, response);
      }
      osapi.newBatchJsonRequest(jsonGenerator, getDataFromResultForEndpoint, endpoint).execute(
    	function(results) {
          countDownLatch.finishedJsonRequest(results);
        });
    }
  };

  return function() {
    var that = {};
    
    var jsonRequests = {};

    /**
     * Create a new request in the batch
     * @param {string} key id for the request
     * @param {object} request the opensocial request object
     */
    var add = function(key, request) {
      var endpoint = request.endpoint;
      var existingRequestsAtEndpoint = jsonRequests[endpoint] || [];
      existingRequestsAtEndpoint.push({key : key, request : request});
      jsonRequests[endpoint] = existingRequestsAtEndpoint;
      return that;
    };

    /**
     * Json generator function that generates the batch's post body.
     * @param {string} endpoint Server-specified endpoint for rpc calls
     */
    var json = function(endpoint) {
      var jsonParams = [];
      for (var i = 0; i < jsonRequests[endpoint].length; i++) {
        var request = jsonRequests[endpoint][i];
        var requestJson = request.request.json()[0]; // single requests make a json array by default
        requestJson.id = request.key;
        jsonParams.push(requestJson);
      }
      return jsonParams;
    };

    /**
     * Post processor for the batch call.
     * Essentially, this function just makes error handling 
     * work as expected, but also puts items back into the response
     * according to the key by which they were added.
     * @param {string} endpoint Server specified endpoint used to retrieve result
     * @param { object} result the response from the server
     * 
     */
    var getDataFromResult = function(endpoint, result) {      
      var responseMap = {};
      var jsonRequestsForEndpoint = jsonRequests[endpoint];
      var data = result.data; // the json array
      for (var k = 0; k < jsonRequestsForEndpoint.length; k++) {
        var response = data[k];
        if (response.error) {
          var error = { error : { code : osapi.translateHttpError("Error "
              + response.error['code']),
            message : response.error.message }};
          responseMap[response.id] = error;
          osapi.setGlobalError(responseMap, error.error);
        } else {
          if (response.id !== jsonRequestsForEndpoint[k].key) {
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

    /**
     * Creates a countdown latch that will ultimately call the usercallback,
     * and then executes the jsonRequests.
     * @param {function} userCallback function to call when batch is done
     */
    var executeRequests = function(userCallback) {
      var jsonRequestLength = length(jsonRequests);
      var countDownLatch = newCountdownLatch(jsonRequestLength, userCallback);
      if (jsonRequestLength > 0) {
        executeJsonRequests(jsonRequests, json, getDataFromResult, countDownLatch);
      }
    };

    /**
     * Call to make a batch execute its requests.
     * @param {Function} userCallback the callback to the gadget where results are passed.
     */
    var execute =  function(userCallback) {
      if (length(jsonRequests) == 0) {
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


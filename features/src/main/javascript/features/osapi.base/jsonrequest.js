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
/*
<!--
Required config:

rpcUrl: The url of the opensocial data endpoint.

-->
*/
var osapi = osapi || {};


/**
 * The bare mechanism for creating json opensocial requests.
 * The various known services, activities, appdata, people, and batch, are
 * wrappers over this call.
 */
(function() {

  var config = {};
    /**
   * @param {Object} configuration Configuration settings
   * @private
   */
  function init (configuration) {
    config = configuration["osapi.base"];
  }

  var requiredConfig = {
    rpcUrl : gadgets.config.NonEmptyStringValidator
  };

  gadgets.config.register("osapi.base", requiredConfig, init);

  var JSON_CONTENT_TYPE = "application/json";
  var JSON_REQUEST_CONTENT_TYPE = "JSON";
  
  var requestBody = function(json) {
    return {
      "CONTENT_TYPE" : JSON_REQUEST_CONTENT_TYPE,
      "METHOD" : "POST",
      "AUTHORIZATION" : "SIGNED",
      "POST_DATA" : gadgets.json.stringify(json)
    };
  };

  var url = function() {
    if (config.rpcUrl) {
      var baseUrl = config.rpcUrl.replace("%host%", document.location.host);
    }
    var url = [baseUrl, "/rpc"];
    var token = shindig.auth.getSecurityToken();
    if (token) {
      url.push("?st=", encodeURIComponent(token));
    }

    return url.join('');
  };

  var generateErrorResponse = function(result) {
    var globalErrorCode = osapi.base.translateHttpError(result.errors[0]
        || result.data.error) || 'internalError';

    // Populate each response(request?) item with the GlobalError.
    var errorResponseMap = {};
    osapi.base.setGlobalError(errorResponseMap, { code : globalErrorCode});
//    for (var i = 0; i < result.data.length; i++) {
//      errorResponseMap[result.data[i].id] = { error : globalErrorCode };
//    }
    return errorResponseMap;
  };

  var makeResponseProcessor = function(resultProcessor, userCallback) {
    return function(result) {
      if (result.errors[0]) {
        userCallback(generateErrorResponse(result));
      } else {
        userCallback(resultProcessor(result));
      }
    };
  };

  var defaultUserId = '@viewer';
  var defaultGroupId = '@self';
  var defaultCount = 20;

  var getDataFromResult = function(result) {
    var jsonArray = result.data;
    if (jsonArray[0].error) {
      return { error : { code : osapi.base.translateHttpError("Error "+ jsonArray[0].error.code),
        message : jsonArray[0].error.message}};
    } else if (jsonArray[0].data.list) {
      return jsonArray[0].data.list;
    } else {
      return jsonArray[0].data;
    }
  };

  var renamePropertyInObject = function(obj, originalName, newName, defaultValue) {
    if (obj[originalName]) {
      obj[newName] = obj[originalName];
      delete obj[originalName];
    } else if (defaultValue) {
      obj[newName] = defaultValue;
    }
  };

  var ensureUserId = function(options) {
    if (options.userId) {
      options.userId = (options.userId.constructor === Array) ? 
          options.userId : [options.userId];
    } else {
      options.userId = [defaultUserId];
    }
  };

  var ensureGroupId = function(options) {
    options.groupId = options.groupId || defaultGroupId;
  };

  var makeJsonRequest = function(method, options) {
    ensureUserId(options);
    ensureGroupId(options);
    var jsonParams = { method : method};
    jsonParams['params'] = options;
    return [jsonParams];
  };

  /**
   * Generates the execute method for the batch or specific json rpc request
   * @returns {Function} returns a function that takes a userCallback function
   *              where results will be returned from the call.
   */
  var makeExecuteFn = function(jsonGenerator, responseProcessor) {
    return function(userCallback) {
      userCallback = userCallback || function() {};
      var json = jsonGenerator();
      if (json.length == 0) {
        window.setTimeout(function() {
              makeResponseProcessor(responseProcessor, userCallback)({errors : []});
            },
            0);
      } else {
        gadgets.io.makeNonProxiedRequest(url(),
            makeResponseProcessor(responseProcessor, userCallback),
            requestBody(json),
            JSON_CONTENT_TYPE);
      }
    };
  };

  /**
  * Call to execute a jsonrpc request.
  * @param {Function} jsonGenerator the function to produce the specific request's json params
  * @param {object.<JSON>} The JSON object of parameters for the specific request
  */
  var singleRequest = function(jsonRpcServiceMethod, options) {
    options = options || {};
    var jsonGenerator = function() { return makeJsonRequest(jsonRpcServiceMethod, options); };
    var execute = makeExecuteFn(jsonGenerator, getDataFromResult);

    return {
      json : jsonGenerator,
      execute : execute
    };
  };

  /**
  * Call to execute a jsonrpc request.
  * @param {Function} jsonGenerator the function to produce the specific request's json params
  * @param {Function} resultDataProcessor the function to process the results for the
  *               particular json request.
  */
  var batchRequest = function(jsonGenerator, resultDataProcessor) {
    var execute = makeExecuteFn(jsonGenerator, resultDataProcessor);

    return {
      execute : execute
    };
  };

  osapi.newJsonRequest = singleRequest;
  osapi.newBatchJsonRequest = batchRequest;
})();

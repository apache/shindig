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
 * Set an error property on the responseMap object
 * @param responseMap results returned to a gadget
 * @param error the particular error object to set
 */
osapi.setGlobalError = function(responseMap, error) {
  if (!responseMap['error']) {
    if (error === undefined) {
      responseMap['error'] = "Some requests in the batch had errors";
    } else {
      responseMap['error'] = error;
    }
  }
};

/**
 * Turns an http error code into an opensocial error
 * @param httpError
 */
osapi.translateHttpError = function(httpError) {
  if (httpError == "Error 501") {
    return 'notImplemented';
  } else if (httpError == "Error 401") {
    return 'unauthorized';
  } else if (httpError == "Error 403") {
    return 'forbidden';
  } else if (httpError == "Error 400") {
    return 'badRequest';
  } else if (httpError == "Error 500") {
    return 'internalError';
  } else if (httpError == "Error 404") {
    return 'badRequest';
  } else if (httpError == "Error 417") {
    return 'limitExceeded';
  }
};





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

/**
 * @fileoverview JSON-RPC based opensocial container.
 */

var JsonRpcContainer = function(baseUrl, domain, supportedFieldsArray) {
  opensocial.Container.call(this);

  var supportedFieldsMap = {};
  for (var objectType in supportedFieldsArray) {
    if (supportedFieldsArray.hasOwnProperty(objectType)) {
      supportedFieldsMap[objectType] = {};
      for (var i = 0; i < supportedFieldsArray[objectType].length; i++) {
        var supportedField = supportedFieldsArray[objectType][i];
        supportedFieldsMap[objectType][supportedField] = true;
      }
    }
  }

  this.environment_ = new opensocial.Environment(domain, supportedFieldsMap);
  this.baseUrl_ = baseUrl;

  this.securityToken_ = shindig.auth.getSecurityToken();
};
JsonRpcContainer.inherits(opensocial.Container);

JsonRpcContainer.prototype.getEnvironment = function() {
  return this.environment_;
};

JsonRpcContainer.prototype.requestCreateActivity = function(activity, priority,
    opt_callback) {
  opt_callback = opt_callback || function(){};

  var req = opensocial.newDataRequest();
  var viewer = new opensocial.IdSpec({'userId' : 'VIEWER'});
  req.add(this.newCreateActivityRequest(viewer, activity), 'key');
  req.send(function(response) {
    opt_callback(response.get('key'));
  });
};

JsonRpcContainer.prototype.requestData = function(dataRequest, callback) {
  callback = callback || function(){};

  var requestObjects = dataRequest.getRequestObjects();
  var totalRequests = requestObjects.length;

  if (totalRequests == 0) {
    window.setTimeout(function() {
      callback(new opensocial.DataResponse({}, true));
    }, 0);
    return;
  }

  var jsonBatchData = new Array(totalRequests);

  for (var j = 0; j < totalRequests; j++) {
    var requestObject = requestObjects[j];

    jsonBatchData[j] = requestObject.request.rpc;
    if (requestObject.key) {
      jsonBatchData[j].id = requestObject.key;
    }
  }

  var sendResponse = function(result) {
    if (result.errors[0]) {
      JsonRpcContainer.generateErrorResponse(result, requestObjects, callback);
      return;
    }

    result = result.data;

    var globalError = false;
    var responseMap = {};

    // Map from indices to ids.
    for (var i = 0; i < result.length; i++) {
      result[result[i].id] = result[i];
    }

    for (var k = 0; k < requestObjects.length; k++) {
      var request = requestObjects[k];
      var response = result[k];

      if (request.key && response.id != request.key) {
        throw "Request key(" + request.key +
            ") and response id(" + response.id + ") do not match";
      }

      var rawData = response.data;
      var error = response.error;
      var errorMessage = "";

      if (error) {
        errorMessage = error.message;
      }

      var processedData = request.request.processResponse(
          request.request, rawData, error, errorMessage);
      globalError = globalError || processedData.hadError();
      if (request.key) {
        responseMap[request.key] = processedData;
      }
    }

    var dataResponse = new opensocial.DataResponse(responseMap, globalError);
    callback(dataResponse);
  };

  // TODO: get the jsonbatch url from the container config
  var makeRequestParams = {
    "CONTENT_TYPE" : "JSON",
    "METHOD" : "POST",
    "AUTHORIZATION" : "SIGNED",
    "POST_DATA" : gadgets.json.stringify(jsonBatchData)
  };

  var url = [this.baseUrl_, "/rpc"];
  var token = shindig.auth.getSecurityToken();
  if (token) {
    url.push("?st=", encodeURIComponent(token));
  }

  this.sendRequest(url.join(''), sendResponse, makeRequestParams,
      "application/json");
};

JsonRpcContainer.prototype.sendRequest = function(relativeUrl, callback, params, contentType) {
  gadgets.io.makeNonProxiedRequest(relativeUrl, callback, params, contentType);
}

JsonRpcContainer.generateErrorResponse = function(result, requestObjects,
    callback) {
  var globalErrorCode =
          JsonRpcContainer.translateHttpError(result.errors[0]
                  || result.data.error)
                  || opensocial.ResponseItem.Error.INTERNAL_ERROR;

  var errorResponseMap = {};
  for (var i = 0; i < requestObjects.length; i++) {
    errorResponseMap[requestObjects[i].key] = new opensocial.ResponseItem(
        requestObjects[i].request, null, globalErrorCode);
  }
  callback(new opensocial.DataResponse(errorResponseMap, true));
};

JsonRpcContainer.translateHttpError = function(httpError) {
  if (httpError == "Error 501") {
    return opensocial.ResponseItem.Error.NOT_IMPLEMENTED;
  } else if (httpError == "Error 401") {
    return opensocial.ResponseItem.Error.UNAUTHORIZED;
  } else if (httpError == "Error 403") {
    return opensocial.ResponseItem.Error.FORBIDDEN;
  } else if (httpError == "Error 400") {
    return opensocial.ResponseItem.Error.BAD_REQUEST;
  } else if (httpError == "Error 500") {
    return opensocial.ResponseItem.Error.INTERNAL_ERROR;
  } else if (httpError == "Error 404") {
    return opensocial.ResponseItem.Error.BAD_REQUEST;
  } else if (httpError == "Error 417") {
    return opensocial.ResponseItem.Error.LIMIT_EXCEEDED;
  }
};

JsonRpcContainer.prototype.makeIdSpec = function(id) {
  return new opensocial.IdSpec({'userId' : id});
};

JsonRpcContainer.prototype.translateIdSpec = function(newIdSpec) {
  var userIds = newIdSpec.getField('userId');
  var groupId = newIdSpec.getField('groupId');

  // Upconvert to array for convenience
  if (!opensocial.Container.isArray(userIds)) {
    userIds = [userIds];
  }

  for (var i = 0; i < userIds.length; i++) {
    if (userIds[i] == 'OWNER') {
      userIds[i] = '@owner';
    } else if (userIds[i] == 'VIEWER') {
      userIds[i] = '@viewer';
    }
  }

  if (groupId == 'FRIENDS') {
    groupId = "@friends";
  } else if (groupId == 'SELF' || !groupId) {
    groupId = "@self";
  }

  return { userId : userIds, groupId : groupId};
};

JsonRpcContainer.prototype.newFetchPersonRequest = function(id, opt_params) {
  var peopleRequest = this.newFetchPeopleRequest(
      this.makeIdSpec(id), opt_params);

  var me = this;
  return new JsonRpcRequestItem(peopleRequest.rpc,
          function(rawJson) {
            return me.createPersonFromJson(rawJson);
          });
};

JsonRpcContainer.prototype.newFetchPeopleRequest = function(idSpec,
    opt_params) {
  var rpc = { method : "people.get" };
  rpc.params = this.translateIdSpec(idSpec);
  if (opt_params['profileDetail']) {
    FieldTranslations.translateJsPersonFieldsToServerFields(opt_params['profileDetail']);
    rpc.params.fields = opt_params['profileDetail'];
  }
  if (opt_params['first']) {
    rpc.params.startIndex = opt_params['first'];
  }
  if (opt_params['max']) {
    rpc.params.count = opt_params['max'];
  }
  if (opt_params['sortOrder']) {
    rpc.params.sortBy = opt_params['sortOrder'];
  }
  if (opt_params['filter']) {
    rpc.params.filterBy = opt_params['filter'];
  }
  if (idSpec.getField('networkDistance')) {
    rpc.params.networkDistance = idSpec.getField('networkDistance');
  }

  var me = this;
  return new JsonRpcRequestItem(rpc,
      function(rawJson) {
        var jsonPeople;
        if (rawJson['list']) {
          // For the array of people response
          jsonPeople = rawJson['list'];
        } else {
          // For the single person response
          jsonPeople = [rawJson];
        }

        var people = [];
        for (var i = 0; i < jsonPeople.length; i++) {
          people.push(me.createPersonFromJson(jsonPeople[i]));
        }
        return new opensocial.Collection(people,
            rawJson['startIndex'], rawJson['totalResults']);
      });
};

JsonRpcContainer.prototype.createPersonFromJson = function(serverJson) {
  FieldTranslations.translateServerPersonToJsPerson(serverJson);
  return new JsonPerson(serverJson);
};

JsonRpcContainer.prototype.getFieldsList = function(keys) {
  // datarequest.js guarantees that keys is an array
  if (this.hasNoKeys(keys) || this.isWildcardKey(keys[0])) {
    return [];
  } else {
    return keys;
  }
};

JsonRpcContainer.prototype.hasNoKeys = function(keys) {
  return !keys || keys.length == 0;
};

JsonRpcContainer.prototype.isWildcardKey = function(key) {
  // Some containers support * to mean all keys in the js apis.
  // This allows the RESTful apis to be compatible with them.
  return key == "*";
};

JsonRpcContainer.prototype.newFetchPersonAppDataRequest = function(idSpec, keys,
    opt_params) {
  var rpc = { method : "appdata.get" };
  rpc.params = this.translateIdSpec(idSpec);
  rpc.params.appId = "@app";
  rpc.params.fields = this.getFieldsList(keys);
  if (idSpec.getField('networkDistance')) {
    rpc.params.networkDistance = idSpec.getField('networkDistance');
  }

  return new JsonRpcRequestItem(rpc,
      function (appData) {
        return opensocial.Container.escape(appData, opt_params, true);
      });
};

JsonRpcContainer.prototype.newUpdatePersonAppDataRequest = function(id, key,
    value) {
  var rpc = { method : "appdata.update" };
  rpc.params = this.translateIdSpec(this.makeIdSpec(id));
  rpc.params.appId = "@app";
  rpc.params.data = {};
  rpc.params.data[key] = value;
  rpc.params.fields = key;
  return new JsonRpcRequestItem(rpc);
};

JsonRpcContainer.prototype.newRemovePersonAppDataRequest = function(id, keys) {
  var rpc = { method : "appdata.delete" };
  rpc.params = this.translateIdSpec(this.makeIdSpec(id));
  rpc.params.appId = "@app";
  rpc.params.fields = this.getFieldsList(keys);

  return new JsonRpcRequestItem(rpc);
};

JsonRpcContainer.prototype.newFetchActivitiesRequest = function(idSpec,
    opt_params) {
  var rpc = { method : "activities.get" };
  rpc.params = this.translateIdSpec(idSpec);
  rpc.params.appId = "@app";
  if (idSpec.getField('networkDistance')) {
    rpc.params.networkDistance = idSpec.getField('networkDistance');
  }

  return new JsonRpcRequestItem(rpc,
      function(rawJson) {
        rawJson = rawJson['list'];
        var activities = [];
        for (var i = 0; i < rawJson.length; i++) {
          activities.push(new JsonActivity(rawJson[i]));
        }
        return new opensocial.Collection(activities);
      });
};

JsonRpcContainer.prototype.newActivity = function(opt_params) {
  return new JsonActivity(opt_params, true);
};

JsonRpcContainer.prototype.newMediaItem = function(mimeType, url, opt_params) {
  opt_params = opt_params || {};
  opt_params['mimeType'] = mimeType;
  opt_params['url'] = url;
  return new JsonMediaItem(opt_params);
};

JsonRpcContainer.prototype.newCreateActivityRequest = function(idSpec,
    activity) {
  var rpc = { method : "activities.create" };
  rpc.params = this.translateIdSpec(idSpec);
  rpc.params.appId = "@app";
  if (idSpec.getField('networkDistance')) {
    rpc.params.networkDistance = idSpec.getField('networkDistance');
  }
  rpc.params.activity = activity.toJsonObject();

  return new JsonRpcRequestItem(rpc);
};

var JsonRpcRequestItem = function(rpc, opt_processData) {
  this.rpc = rpc;
  this.processData = opt_processData ||
                     function (rawJson) {
                       return rawJson;
                     };

  this.processResponse = function(originalDataRequest, rawJson, error,
      errorMessage) {
    var errorCode = error
      ? JsonRpcContainer.translateHttpError("Error " + error['code'])
      : null;
    return new opensocial.ResponseItem(originalDataRequest,
        error ? null : this.processData(rawJson), errorCode, errorMessage);
  }
};

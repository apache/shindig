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

JsonRpcContainer.prototype.requestCreateActivity = function(activity,
                                                            priority, opt_callback) {
  opt_callback = opt_callback || {};

  var req = opensocial.newDataRequest();
  var viewer = new opensocial.IdSpec({'userId' : 'VIEWER'});
  req.add(this.newCreateActivityRequest(viewer, activity), 'key');
  req.send(function(response) {
    opt_callback(response.get('key'));
  });
};

JsonRpcContainer.prototype.requestData = function(dataRequest, callback) {
  callback = callback || {};

  var requestObjects = dataRequest.getRequestObjects();
  var totalRequests = requestObjects.length;

  if (totalRequests == 0) {
    callback(new opensocial.DataResponse({}, true));
    return;
  }

  var jsonBatchData = new Array(totalRequests);
  var systemKeyIndex = 0;

  for (var j = 0; j < totalRequests; j++) {
    var requestObject = requestObjects[j];

    if (!requestObject.key) {
      requestObject.key = "systemKey" + systemKeyIndex;
      while (jsonBatchData[requestObject.key]) {
        // If the key exists, choose another and try again
        systemKeyIndex++;
        requestObject.key = "systemKey" + systemKeyIndex;
      }
    }

    jsonBatchData[j] = requestObject.request.rpc;
    jsonBatchData[j].id = requestObject.key;

    if (requestObject.request.postData) {
      jsonBatchData[j].postData = requestObject.request.postData;
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
      var response = result[request.key];

      var rawData = response.data;
      var error = response.error;
      var errorMessage = "";

      if (error) {
        errorMessage = error.message;
      }

      var processedData = request.request.processResponse(
          request.request, rawData, error, errorMessage);
      globalError = globalError || processedData.hadError();
      responseMap[request.key] = processedData;
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

  gadgets.io.makeNonProxiedRequest(
      this.baseUrl_ + "/rpc?st=" + encodeURIComponent(shindig.auth.getSecurityToken()),
      sendResponse, makeRequestParams, "application/json");
};

JsonRpcContainer.generateErrorResponse = function(result, requestObjects, callback) {
  var globalErrorCode = JsonRpcContainer.translateHttpError(result.errors[0] || result.data.error)
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
  // TODO: Which one should the limit exceeded error be?
    // } else if (httpError == "Error ???") {
    //   return opensocial.ResponseItem.Error.LIMIT_EXCEEDED;
  }
};

JsonRpcContainer.prototype.makeIdSpec = function(id) {
  return new opensocial.IdSpec({'userId' : id});
};

JsonRpcContainer.prototype.translateIdSpec = function(newIdSpec) {
  var userId = newIdSpec.getField('userId');
  var groupId = newIdSpec.getField('groupId');

  if (userId == 'OWNER') {
    userId = '@owner';
  } else if (userId == 'VIEWER') {
    userId = '@viewer';
  } else if (opensocial.Container.isArray(newIdSpec)) {
    for (var i = 0; i < newIdSpec.length; i++) {
      // TODO: We will need multiple urls here....don't want to think about
      // that yet
    }
  }

  if (groupId == 'FRIENDS') {
    groupId = "@friends";
  } else if (!groupId) {
    groupId = "@self";
  }

  return { userId : userId, groupId : groupId};
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
    rpc.params.fields = opt_params['profileDetail'];
  }
  if (opt_params['first']) {
    rpc.params.startIndex = opt_params['first'];
  }
  if (opt_params['max']) {
    rpc.params.count = opt_params['max'];
  }
  if (opt_params['sortOrder']) {
    rpc.params.orderBy = opt_params['sortOrder'];
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

JsonRpcContainer.prototype.newFetchPersonAppDataRequest = function(idSpec,
                                                                   keys, opt_params) {
  var rpc = { method : "appdata.get" };
  rpc.params = this.translateIdSpec(idSpec);
  rpc.params.app = "@app";
  rpc.params.fields = this.getFieldsList(keys);
  if (idSpec.getField('networkDistance')) {
    rpc.params.networkDistance = idSpec.getField('networkDistance');
  }

  return new JsonRpcRequestItem(rpc,
      function (appData) {
        return  opensocial.Container.escape(appData['data'], opt_params, true);
      });
};

JsonRpcContainer.prototype.newUpdatePersonAppDataRequest = function(id, key,
                                                                    value) {
  var rpc = { method : "appdata.update" };
  rpc.params = this.translateIdSpec(this.makeIdSpec(id));
  rpc.params.app = "@app";
  rpc.params.data = {};
  rpc.params.data[key] = value;
  return new JsonRpcRequestItem(rpc);
};

JsonRpcContainer.prototype.newRemovePersonAppDataRequest = function(id, keys) {
  var rpc = { method : "appdata.delete" };
  rpc.params = this.translateIdSpec(this.makeIdSpec(id));
  rpc.params.app = "@app";
  rpc.params.keys = keys;

  return new JsonRpcRequestItem(rpc);
};

JsonRpcContainer.prototype.newFetchActivitiesRequest = function(idSpec,
                                                                opt_params) {
  var rpc = { method : "activities.get" };
  rpc.params = this.translateIdSpec(idSpec);
  rpc.params.app = "@app";
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
  rpc.params.app = "@app";
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
    return new opensocial.ResponseItem(originalDataRequest,
        error ? null : this.processData(rawJson), error, errorMessage);
  }
};

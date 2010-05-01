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
 * @fileoverview RESTful based opensocial container.
 *
 * TODO: THIS IS NOW DEPRECATED. Remove once PHP implements rpc support
 */

var RestfulContainer = function(baseUrl, domain, supportedFieldsArray) {
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
RestfulContainer.inherits(opensocial.Container);

RestfulContainer.prototype.getEnvironment = function() {
  return this.environment_;
};

RestfulContainer.prototype.requestCreateActivity = function(activity,
    priority, opt_callback) {
  opt_callback = opt_callback || function(){};

  var req = opensocial.newDataRequest();
  var viewer = new opensocial.IdSpec({'userId' : 'VIEWER'});
  req.add(this.newCreateActivityRequest(viewer, activity), 'key');
  req.send(function(response) {
    opt_callback(response.get('key'));
  });
};

RestfulContainer.prototype.requestData = function(dataRequest, callback) {
  callback = callback || function(){};

  var requestObjects = dataRequest.getRequestObjects();
  var totalRequests = requestObjects.length;

  if (totalRequests == 0) {
    window.setTimeout(function () {
      callback(new opensocial.DataResponse({}, true));
    }, 0);
    return;
  }

  var jsonBatchData = {};
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

    jsonBatchData[requestObject.key] = {url : requestObject.request.url,
      method : requestObject.request.method};
    if (requestObject.request.postData) {
      jsonBatchData[requestObject.key].postData = requestObject.request.postData;
    }
  }

  var sendResponse = function(result) {
    if (result.errors[0] || result.data.error) {
      RestfulContainer.generateErrorResponse(result, requestObjects, callback);
      return;
    }

    result = result.data;

    var responses = result['responses'] || [];
    var globalError = false;

    var responseMap = {};

    for (var k = 0; k < requestObjects.length; k++) {
      var request = requestObjects[k];
      var response = responses[request.key];

      var rawData = response['response'];
      var error = response['error'];
      var errorMessage = response['errorMessage'];

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
      this.baseUrl_ + "/rest/jsonBatch?st=" + encodeURIComponent(shindig.auth.getSecurityToken()),
      sendResponse, makeRequestParams, "application/json");
};

RestfulContainer.generateErrorResponse = function(result, requestObjects, callback) {
  var globalErrorCode = RestfulContainer.translateHttpError(result.errors[0] || result.data.error)
      || opensocial.ResponseItem.Error.INTERNAL_ERROR;

  var errorResponseMap = {};
  for (var i = 0; i < requestObjects.length; i++) {
    errorResponseMap[requestObjects[i].key] = new opensocial.ResponseItem(
        requestObjects[i].request, null, globalErrorCode);
  }
  callback(new opensocial.DataResponse(errorResponseMap, true));
};

RestfulContainer.translateHttpError = function(httpError) {
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

RestfulContainer.prototype.makeIdSpec = function(id) {
  return new opensocial.IdSpec({'userId' : id});
};

RestfulContainer.prototype.translateIdSpec = function(newIdSpec) {
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
  } else if (groupId == 'SELF' || !groupId) {
    groupId = "@self";
  }

  return userId + "/" + groupId;
};

RestfulContainer.prototype.getNetworkDistance = function(idSpec) {
  var networkDistance = idSpec.getField('networkDistance') || '';
  return "networkDistance=" + networkDistance;
}

RestfulContainer.prototype.newFetchPersonRequest = function(id, opt_params) {
  var peopleRequest = this.newFetchPeopleRequest(
      this.makeIdSpec(id), opt_params);

  var me = this;
  return new RestfulRequestItem(peopleRequest.url, peopleRequest.method, null,
      function(rawJson) {
        return me.createPersonFromJson(rawJson['entry']);
      });
};

RestfulContainer.prototype.newFetchPeopleRequest = function(idSpec,
    opt_params) {
  var url = "/people/" + this.translateIdSpec(idSpec);

  FieldTranslations.translateJsPersonFieldsToServerFields(opt_params['profileDetail']);
  url += "?fields=" + (opt_params['profileDetail'].join(','));
  url += "&startIndex=" + (opt_params['first'] || 0);
  url += "&count=" + (opt_params['max'] || 20);
  url += "&orderBy=" + (opt_params['sortOrder'] || 'topFriends');
  // TODO: This filterBy isn't in the spec
  url += "&filterBy=" + (opt_params['filter'] || 'all');
  url += "&" + this.getNetworkDistance(idSpec);

  var me = this;
  return new RestfulRequestItem(url, "GET", null,
      function(rawJson) {
        var jsonPeople;
        if (rawJson['entry']) {
          // For the array of people response
          jsonPeople = rawJson['entry'];
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

RestfulContainer.prototype.createPersonFromJson = function(serverJson) {
  FieldTranslations.translateServerPersonToJsPerson(serverJson);
  return new JsonPerson(serverJson);
};

RestfulContainer.prototype.getFieldsList = function(keys) {
  // datarequest.js guarantees that keys is an array
  if (this.hasNoKeys(keys) || this.isWildcardKey(keys[0])) {
    return '';
  } else {
    return 'fields=' + keys.join(',');
  }
};

RestfulContainer.prototype.hasNoKeys = function(keys) {
  return !keys || keys.length == 0;
};

RestfulContainer.prototype.isWildcardKey = function(key) {
  // Some containers support * to mean all keys in the js apis.
  // This allows the RESTful apis to be compatible with them.
  return key == "*";
};

RestfulContainer.prototype.newFetchPersonAppDataRequest = function(idSpec,
    keys, opt_params) {
  var url = "/appdata/" + this.translateIdSpec(idSpec) + "/@app"
      + "?" + this.getNetworkDistance(idSpec) + "&" + this.getFieldsList(keys);
  return new RestfulRequestItem(url, "GET", null,
      function (appData) {
        return opensocial.Container.escape(appData['entry'], opt_params, true);
      });
};

RestfulContainer.prototype.newUpdatePersonAppDataRequest = function(id, key,
    value) {
  var url = "/appdata/" + this.translateIdSpec(this.makeIdSpec(id))
      + "/@app?fields=" + key;
  var data = {};
  data[key] = value;
  return new RestfulRequestItem(url, "POST", data);
};

RestfulContainer.prototype.newRemovePersonAppDataRequest = function(id, keys) {
  var url = "/appdata/" + this.translateIdSpec(this.makeIdSpec(id))
      + "/@app?" + this.getFieldsList(keys);
  return new RestfulRequestItem(url, "DELETE");
};

RestfulContainer.prototype.newFetchActivitiesRequest = function(idSpec,
    opt_params) {
  var url = "/activities/" + this.translateIdSpec(idSpec)
      + "?appId=@app&" + this.getNetworkDistance(idSpec); // TODO: Handle appId correctly
  return new RestfulRequestItem(url, "GET", null,
      function(rawJson) {
        rawJson = rawJson['entry'];
        var activities = [];
        for (var i = 0; i < rawJson.length; i++) {
          activities.push(new JsonActivity(rawJson[i]));
        }
        return new opensocial.Collection(activities);
      });
};

RestfulContainer.prototype.newActivity = function(opt_params) {
  return new JsonActivity(opt_params, true);
};

RestfulContainer.prototype.newMediaItem = function(mimeType, url, opt_params) {
  opt_params = opt_params || {};
  opt_params['mimeType'] = mimeType;
  opt_params['url'] = url;
  return new JsonMediaItem(opt_params);
};

RestfulContainer.prototype.newCreateActivityRequest = function(idSpec,
    activity) {
  var url = "/activities/" + this.translateIdSpec(idSpec)
      + "/@app?" + this.getNetworkDistance(idSpec); // TODO: Handle appId correctly
  return new RestfulRequestItem(url, "POST", activity.toJsonObject());
};

var RestfulRequestItem = function(url, method, opt_postData, opt_processData) {
  this.url = url;
  this.method = method;
  this.postData = opt_postData;
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

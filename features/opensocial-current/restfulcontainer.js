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
 */

RestfulContainer = function(baseUrl, domain, supportedFieldsArray) {
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
  opt_callback = opt_callback || {};

  var req = opensocial.newDataRequest();
  var viewer = new opensocial.IdSpec({'userId' : 'VIEWER'});
  req.add(this.newCreateActivityRequest(viewer, activity), 'key');
  req.send(function(response) {
    opt_callback(response.get('key'));
  });
};

RestfulContainer.prototype.requestData = function(dataRequest, callback) {
  callback = callback || {};

  var requestObjects = dataRequest.getRequestObjects();
  var totalRequests = requestObjects.length;

  if (totalRequests == 0) {
    callback(new opensocial.DataResponse({}, true));
    return;
  }

  var responseMap = {};
  var globalError = false;
  var responsesReceived = 0;

  var checkIfFinished = function() {
    responsesReceived++;
    if (responsesReceived == totalRequests) {
      var dataResponse = new opensocial.DataResponse(responseMap, globalError);
      callback(dataResponse);
    }
  }

  var makeProxiedRequest = function(requestObject, baseUrl, st) {
    var makeRequestParams = {
      "CONTENT_TYPE" : "JSON",
      "METHOD" : requestObject.request.method
    };

    if (requestObject.request.postData) {
      makeRequestParams["POST_DATA"]
          = gadgets.io.encodeValues(requestObject.request.postData);
    }

    var url = requestObject.request.url;
    var separator = url.indexOf("?") != -1 ? "&" : "?";

    // TODO: Use batching instead of doing this one by one
    gadgets.io.makeNonProxiedRequest(
        baseUrl + url + separator + "st=" + st,
        function(result) {
          result = result.data;

          // TODO: handle errors
          var processedData = requestObject.request.processResponse(
              requestObject.request, result, null, null);
          globalError = globalError || processedData.hadError();
          responseMap[requestObject.key] = processedData;

          checkIfFinished();
        },
        makeRequestParams);
  }

  // may need multiple urls for one response but lets ignore that for now
  for (var i = 0; i < totalRequests; i++) {
    makeProxiedRequest(requestObjects[i], this.baseUrl_, this.securityToken_);
  }

};

RestfulContainer.prototype.makeIdSpec = function(id) {
  return new opensocial.IdSpec({'userId' : id});
}

RestfulContainer.prototype.translateIdSpec = function(newIdSpec) {
  var userId = newIdSpec.getField('userId');
  var groupId = newIdSpec.getField('groupId');

  if (userId == 'OWNER') {
    userId = '@owner';
  } else if (userId == 'VIEWER') {
    userId = '@viewer';
  } else if (opensocial.Container.isArray(idSpec)) {
    for (var i = 0; i < idSpec.length; i++) {
      // TODO: We will need multiple urls here....don't want to think about
      // that yet
    }
  }

  if (groupId == 'FRIENDS') {
    groupId = "@friends";
  } else if (!groupId) {
    groupId = "@self";
  }

  // TODO: Network distance

  return userId + "/" + groupId;
};

RestfulContainer.prototype.newFetchPersonRequest = function(id, opt_params) {
  var peopleRequest = this.newFetchPeopleRequest(
      this.makeIdSpec(id), opt_params);

  var me = this;
  return new RestfulRequestItem(peopleRequest.url, peopleRequest.method, null,
      function(rawJson) {
        return me.createPersonFromJson(rawJson);
      });
};

RestfulContainer.prototype.newFetchPeopleRequest = function(idSpec,
    opt_params) {
  var url = "/people/" + this.translateIdSpec(idSpec);

  url += "?fields=" + (opt_params['profileDetail'].join(','));
  url += "&startIndex=" + (opt_params['first'] || 0);
  url += "&count=" + (opt_params['max'] || 20);
  url += "&orderBy=" + (opt_params['sortOrder'] || 'topFriends');
  // TODO: This filterBy isn't in the spec
  url += "&filterBy=" + (opt_params['filter'] || 'all');

  var me = this;
  return new RestfulRequestItem(url, "GET", null,
      function(rawJson) {
        var jsonPeople = rawJson['entry'];
        var people = [];
        for (var i = 0; i < jsonPeople.length; i++) {
          people.push(me.createPersonFromJson(jsonPeople[i]));
        }
        return new opensocial.Collection(people,
            rawJson['startIndex'], rawJson['totalResults']);
      });
};

RestfulContainer.prototype.createPersonFromJson = function(serverJson) {
  return new JsonPerson(serverJson);
}

RestfulContainer.prototype.getFieldsList = function(keys) {
  if (opensocial.Container.isArray(keys)) {
    return keys.join(',');
  } else {
    return keys;
  }
}

RestfulContainer.prototype.newFetchPersonAppDataRequest = function(idSpec,
    keys) {
  var url = "/appdata/" + this.translateIdSpec(idSpec) + "/@app"
      + "?fields=" + this.getFieldsList(keys);
  return new RestfulRequestItem(url, "GET", null,
      function (appData) {
        return gadgets.util.escape(appData['entry'], true);
      });
};

RestfulContainer.prototype.newUpdatePersonAppDataRequest = function(id, key,
    value) {
  var url = "/appdata/" + this.translateIdSpec(this.makeIdSpec(id))
      + "/@app?fields=" + key;
  var data = {};
  data[key] = value;
  var postData = {};
  postData['entry'] = gadgets.json.stringify(data);
  return new RestfulRequestItem(url, "POST", postData);
};

RestfulContainer.prototype.newRemovePersonAppDataRequest = function(id, keys) {
  var url = "/appdata/" + this.translateIdSpec(this.makeIdSpec(id))
      + "/@app?fields=" + this.getFieldsList(keys);
  return new RestfulRequestItem(url, "DELETE");
};

RestfulContainer.prototype.newFetchActivitiesRequest = function(idSpec,
    opt_params) {
  var url = "/activities/" + this.translateIdSpec(idSpec)
      + "?app=@app"; // TODO: Handle appId correctly
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
      + "/@app"; // TODO: Handle appId correctly
  var postData = {};
  postData['entry'] = gadgets.json.stringify(activity.toJsonObject());
  return new RestfulRequestItem(url, "POST", postData);
};

RestfulRequestItem = function(url, method, postData, processData) {
  this.url = url;
  this.method = method;
  this.postData = postData;
  this.processData = processData ||
    function (rawJson) {
      return rawJson;
    };

  this.processResponse = function(originalDataRequest, rawJson, error,
      errorMessage) {

    return new opensocial.ResponseItem(originalDataRequest,
        error ? null : this.processData(rawJson), error, errorMessage);
  }
};

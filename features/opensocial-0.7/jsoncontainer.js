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
 * @fileoverview Json based opensocial container.
 */


JsonContainer = function(baseUrl, domain, supportedFieldsArray) {
  opensocial.Container.call(this);

  var supportedFieldsMap = {};
  for (var objectType in supportedFieldsArray) {
    if (supportedFieldsArray.hasOwnProperty(objectType)){
      supportedFieldsMap[objectType] = {};
      for (var i = 0; i < supportedFieldsArray[objectType].length; i++) {
        var supportedField = supportedFieldsArray[objectType][i];
        supportedFieldsMap[objectType][supportedField] = true;
      }
    }
  }

  this.environment_ = new opensocial.Environment(domain, supportedFieldsMap);
  this.baseUrl_ = baseUrl;
};
JsonContainer.inherits(opensocial.Container);

JsonContainer.prototype.getEnvironment = function() {
  return this.environment_;
};

JsonContainer.prototype.createJson = function(requestObjects) {
  var jsonObjects = [];
  for (var i = 0; i < requestObjects.length; i++) {
    jsonObjects[i] = requestObjects[i].request.jsonParams;
  }
  return gadgets.json.stringify(jsonObjects);
};

JsonContainer.prototype.requestData = function(dataRequest, callback) {
  callback = callback || {};

  var requestObjects = dataRequest.getRequestObjects();
  if (requestObjects.length == 0) {
    callback(new opensocial.DataResponse({}, true));
    return;
  }

  var jsonText = this.createJson(requestObjects);

  var sendResponse = function(result) {
    result = result.data;

    if (!result || result['globalError']) {
      callback(new opensocial.DataResponse({}, true));
      return;
    }

    var responses = result['responses'] || [];
    var globalError = false;

    var responseMap = {};
    for (var i = 0; i < responses.length; i++) {
      var response = responses[i];
      var rawData = response['response'];

      var processedData = requestObjects[i].request.processResponse(rawData);
      globalError = globalError || processedData.hadError();
      responseMap[requestObjects[i].key] = processedData;
    }

    var dataResponse = new opensocial.DataResponse(responseMap, globalError);
    callback(dataResponse);
  };

  new BatchRequest(jsonText, sendResponse, this.baseUrl_).send();
};

JsonContainer.prototype.newFetchPersonRequest = function(id,
    opt_params) {
  var me = this;
  var peopleRequest = this.newFetchPeopleRequest(id, opt_params);
  peopleRequest.processResponse = function(rawJson) {
    return new opensocial.ResponseItem(null,
        me.createPersonFromJson(rawJson[0]));
  };
  return peopleRequest;
};

JsonContainer.prototype.newFetchPeopleRequest = function(idSpec,
    opt_params) {
  var me = this;
  return new RequestItem(
      {'type' : 'FETCH_PEOPLE', 'idSpec' : idSpec, 'params': opt_params},
      function(rawJson) {
        var people = [];
        for (var i = 0; i < rawJson.length; i++) {
          people.push(me.createPersonFromJson(rawJson[i]));
          // TODO: isOwner, isViewer
        }
        return new opensocial.ResponseItem(null, // TODO: Original request
            new opensocial.Collection(people));
      });
};

JsonContainer.prototype.createPersonFromJson = function(serverJson) {
  // TODO: Probably move this into a subclass of person and make it cover
  // all fields
  var name = new opensocial.Name(serverJson["name"]);
  return new opensocial.Person({"id" : serverJson["id"], "name" : name});
}

JsonContainer.prototype.newFetchPersonAppDataRequest = function(
    idSpec, keys) {
  return new RequestItem({'type' : 'FETCH_PERSON_APP_DATA', 'idSpec' : idSpec,
    'keys' : keys});
};

JsonContainer.prototype.newUpdatePersonAppDataRequest = function(
    id, key, value) {
  return new RequestItem({'type' : 'UPDATE_PERSON_APP_DATA', 'id' : id,
    'key' : key, 'value' : value});
};

JsonContainer.prototype.newFetchActivitiesRequest = function(
    idSpec, opt_params) {
  return new RequestItem({'type' : 'FETCH_ACTIVITIES', 'idSpec' : idSpec},
      function(rawJson) {
        var activities = [];
        for (var i = 0; i < rawJson.length; i++) {
          activities.push(new opensocial.Activity(rawJson[i]));
        }
        return new opensocial.ResponseItem(null, // TODO: Original request
            {'activities' : new opensocial.Collection(activities)});
      });
};

RequestItem = function(jsonParams, processResponse) {
  this.jsonParams = jsonParams;
  this.processResponse = processResponse ||
    function (rawJson) {
      return new opensocial.ResponseItem(null, rawJson); // TODO: Original request
    };
};
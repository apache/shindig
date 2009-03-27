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
 * @fileoverview Helper class used to translate from the 0.8 server apis to the 0.8 js apis
 * (which are unfortunately not the same)
 */

var FieldTranslations = {};

FieldTranslations.translateServerPersonToJsPerson = function(serverJson) {
  if (serverJson.emails) {
    for (var i = 0; i < serverJson.emails.length; i++) {
      serverJson.emails[i].address = serverJson.emails[i].value;
    }
  }

  if (serverJson.phoneNumbers) {
    for (var p = 0; p < serverJson.phoneNumbers.length; p++) {
      serverJson.phoneNumbers[p].number = serverJson.phoneNumbers[p].value;
    }
  }

  if (serverJson.birthday) {
    serverJson.dateOfBirth = serverJson.birthday;
  }

  if (serverJson.utcOffset) {
    serverJson.timeZone = serverJson.utcOffset;
  }

  if (serverJson.addresses) {
    for (var j = 0; j < serverJson.addresses.length; j++) {
      serverJson.addresses[j].unstructuredAddress = serverJson.addresses[j].formatted;
    }
  }

  if (serverJson.gender) {
    var key = serverJson.gender == 'male' ? 'MALE' :
              (serverJson.gender == 'female') ? 'FEMALE' :
              null;
    serverJson.gender = {key : key, displayValue : serverJson.gender};
  }

  FieldTranslations.translateUrlJson(serverJson.profileSong);
  FieldTranslations.translateUrlJson(serverJson.profileVideo);

  if (serverJson.urls) {
    for (var u = 0; u < serverJson.urls.length; u++) {
      FieldTranslations.translateUrlJson(serverJson.urls[u]);
    }
  }

  FieldTranslations.translateEnumJson(serverJson.drinker);
  FieldTranslations.translateEnumJson(serverJson.lookingFor);
  FieldTranslations.translateEnumJson(serverJson.networkPresence);
  FieldTranslations.translateEnumJson(serverJson.smoker);

  if (serverJson.organizations) {
    serverJson.jobs = [];
    serverJson.schools = [];

    for (var o = 0; o < serverJson.organizations.length; o++) {
      var org = serverJson.organizations[o];
      if (org.type == 'job') {
        serverJson.jobs.push(org);
      } else if (org.type == 'school') {
        serverJson.schools.push(org);
      }
    }
  }

  if (serverJson.name) {
    serverJson.name.unstructured = serverJson.name.formatted;
  }

};

FieldTranslations.translateEnumJson = function(enumJson) {
  if (enumJson) {
    enumJson.key = enumJson.value;
  }
};

FieldTranslations.translateUrlJson = function(urlJson) {
  if (urlJson) {
    urlJson.address = urlJson.value;
  }
};


FieldTranslations.translateJsPersonFieldsToServerFields = function(fields) {
  for (var i = 0; i < fields.length; i++) {
    if (fields[i] == 'dateOfBirth') {
      fields[i] = 'birthday';
    } else if (fields[i] == 'timeZone') {
      fields[i] = 'utcOffset';
    } else if (fields[i] == 'jobs') {
      fields[i] = 'organizations';
    } else if (fields[i] == 'schools') {
      fields[i] = 'organizations';
    }
  }

  // displayName and id always need to be requested
  fields.push('id');
  fields.push('displayName');
};

FieldTranslations.translateIsoStringToDate = function(isoString) {
  // Date parsing code from http://delete.me.uk/2005/03/iso8601.html
  var regexp = '([0-9]{4})(-([0-9]{2})(-([0-9]{2})' +
      '(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\.([0-9]+))?)?' +
      '(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?';
  var d = isoString.match(new RegExp(regexp));

  var offset = 0;
  var date = new Date(d[1], 0, 1);

  if (d[3]) { date.setMonth(d[3] - 1); }
  if (d[5]) { date.setDate(d[5]); }
  if (d[7]) { date.setHours(d[7]); }
  if (d[8]) { date.setMinutes(d[8]); }
  if (d[10]) { date.setSeconds(d[10]); }
  if (d[12]) { date.setMilliseconds(Number("0." + d[12]) * 1000); }
  if (d[14]) {
    offset = (Number(d[16]) * 60) + Number(d[17]);
    offset *= ((d[15] == '-') ? 1 : -1);
  }

  offset -= date.getTimezoneOffset();
  time = (Number(date) + (offset * 60 * 1000));
  
  return new Date(Number(time));
}

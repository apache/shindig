/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @fileoverview
 *
 * Unittests for the service library.
 */

function ServiceTest(name) {
  TestCase.call(this, name);
}

ServiceTest.inherits(TestCase);

ServiceTest.prototype.setUp = function() {
  this.apiUri = window.__API_URI;
  window.__API_URI = shindig.uri('http://shindig.com');
  this.containerUri = window.__CONTAINER_URI;
  window.__CONTAINER_URI = shindig.uri('http://container.com');
  this.osapiGadgets = osapi.gadgets;
  this.shindigContainerGadgetSite = osapi.container.GadgetSite;
  this.gadgetsRpc = gadgets.rpc;
  gadgets.rpc = {
    register: function() {
    }
  };
  
  this.self = {};
  var response = {};
  response.error = {};
};

ServiceTest.prototype.tearDown = function() {
  window.__API_URI = this.apiUri;
  window.__CONTAINER_URI = this.containerUri;
  osapi.container.GadgetSite = this.shindigContainerGadgetSite;
  gadgets.rpc = this.gadgetsRpc;
  osapi.gadgets = this.osapiGadgets;
};

ServiceTest.prototype.setupOsapiGadgetsMetadata = function(response) {
  osapi.gadgets = {};
  osapi.gadgets.metadata = function(request) {
    return {
      execute: function(func) {
        func(response);
      }
    };
  };
};

ServiceTest.prototype.setupUtilCurrentTimeMs = function(time) {
  osapi.container.util.getCurrentTimeMs = function() {
    return time;
  };
};

ServiceTest.prototype.testGetGadgetMetadata = function() {
  var service = new osapi.container.Service(new osapi.container.Container({
    GET_LANGUAGE: function() {
      return 'pt'; 
    },
    GET_COUNTRY: function() {
      return 'BR';
    }
  }));
  service.cachedMetadatas_ = {
    'cached1.xml' : {
      'url' : 'cached1.xml',
      'responseTimeMs' : 80,
      'expireTimeMs' : 85,
      'localExpireTimeMs' : 100
    }
  };

  var request = osapi.container.util.newMetadataRequest([
      'cached1.xml', 'resp1.xml', 'resp2.xml', 'resp3.xml'
  ]);

  var response = {
    'resp1.xml' : {
      'responseTimeMs' : 90,
      'expireTimeMs' : 91
    },
    'resp2.xml' : {
      'responseTimeMs' : 110,
      'expireTimeMs' : 112
    },
    'resp3.xml' : {
      'responseTimeMs' : 97,
      'expireTimeMs' : 103
    }
  };

  var self = this;
  var callback = function(response) {
    self.response = response;
  };
  
  this.setupUtilCurrentTimeMs(100);
  this.setupOsapiGadgetsMetadata(response);
  var metadata = service.getGadgetMetadata(request, callback);
  var response = self.response;
  
  this.assertEquals('cached1.xml', response['cached1.xml'].url);
  this.assertEquals(80, response['cached1.xml'].responseTimeMs);
  this.assertEquals(85, response['cached1.xml'].expireTimeMs);
  this.assertEquals(100, response['cached1.xml'].localExpireTimeMs);

  this.assertEquals('resp1.xml', response['resp1.xml'].url);
  this.assertEquals(90, response['resp1.xml'].responseTimeMs);
  this.assertEquals(91, response['resp1.xml'].expireTimeMs);
  this.assertEquals(101, response['resp1.xml'].localExpireTimeMs);

  this.assertEquals('resp2.xml', response['resp2.xml'].url);
  this.assertEquals(110, response['resp2.xml'].responseTimeMs);
  this.assertEquals(112, response['resp2.xml'].expireTimeMs);
  this.assertEquals(102, response['resp2.xml'].localExpireTimeMs);

  this.assertEquals('resp3.xml', response['resp3.xml'].url);
  this.assertEquals(97, response['resp3.xml'].responseTimeMs);
  this.assertEquals(103, response['resp3.xml'].expireTimeMs);
  this.assertEquals(106, response['resp3.xml'].localExpireTimeMs);
  
  this.assertTrue(service.cachedMetadatas_['cached1.xml'] != null);
  this.assertTrue(service.cachedMetadatas_['resp1.xml'] != null);
  this.assertTrue(service.cachedMetadatas_['resp2.xml'] != null);
  this.assertTrue(service.cachedMetadatas_['resp3.xml'] != null);
  
  this.assertEquals('pt', service.getLanguage());
  this.assertEquals('BR', service.getCountry());
  this.assertEquals('pt', request.language);
  this.assertEquals('BR', request.country);
};

ServiceTest.prototype.testUncacheStaleGadgetMetadataExcept = function() {
  var service = new osapi.container.Service(new osapi.container.Container());
  service.cachedMetadatas_ = {
      'cached1.xml' : { 'localExpireTimeMs' : 100 },
      'cached2.xml' : { 'localExpireTimeMs' : 200 },
      'except1.xml' : { 'localExpireTimeMs' : 100 },
      'except2.xml' : { 'localExpireTimeMs' : 200 }
  };
  this.setupUtilCurrentTimeMs(150);
  service.uncacheStaleGadgetMetadataExcept({
      'except1.xml' : null,
      'except2.xml' : null
  });
  this.assertTrue(service.cachedMetadatas_['cached1.xml'] == null);
  this.assertTrue(service.cachedMetadatas_['cached2.xml'] != null);
  this.assertTrue(service.cachedMetadatas_['except1.xml'] != null);
  this.assertTrue(service.cachedMetadatas_['except2.xml'] != null);
};

ServiceTest.prototype.testUpdateResponse = function() {
  var service = new osapi.container.Service(new osapi.container.Container());
  this.setupUtilCurrentTimeMs(120);

  var data = {responseTimeMs : 100, expireTimeMs : 105};
  service.addGadgetMetadatas({'id' : data});
  this.assertEquals("id", data.url);
  this.assertEquals(125, data.localExpireTimeMs);
  
  data = {responseTimeMs : 100, expireTimeMs : 105};
  service.addGadgetMetadatas({'id' : data}, 104);
  this.assertEquals("id", data.url);
  this.assertEquals(121, data.localExpireTimeMs);
};

ServiceTest.prototype.testAddToCache = function() {
  var service = new osapi.container.Service(new osapi.container.Container());
  this.setupUtilCurrentTimeMs(120);

  var cache = {};
  service.addToCache_(
    { "id1": { responseTimeMs : 100, expireTimeMs : 105 },
      "id2": { responseTimeMs : 100, expireTimeMs : 135 }},
    103, cache);
  
  this.assertTrue(122, cache["id1"].localExpireTimeMs);
  this.assertTrue(152, cache["id2"].localExpireTimeMs);
};
  


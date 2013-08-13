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
 * Unittests for the gadget_site library.
 */

function GadgetSiteTest(name) {
  TestCase.call(this, name);
}

GadgetSiteTest.inherits(TestCase);

GadgetSiteTest.prototype.NAVIGATE_CALLBACK = 'nc';
GadgetSiteTest.prototype.GADGET_URL = 'http://gadget/abc.xml';

GadgetSiteTest.prototype.setUp = function() {
  var self = this;

  this.util_getCurrentTimeMs_value = 100;
  this.util_getCurrentTimeMs_func = osapi.container.util.getCurrentTimeMs;
  osapi.container.util.getCurrentTimeMs = function() {
    return self.util_getCurrentTimeMs_value++;
  };

  this.util_warn_value = null;
  this.util_warn_func = gadgets.warn;
  gadgets.warn = function(value) {
    self.util_warn_value = value;
  };

  window[this.NAVIGATE_CALLBACK] = function(timingInfo) {
    self.window_navigateCallback_timingInfo = timingInfo;
  };
};

GadgetSiteTest.prototype.tearDown = function() {
  osapi.container.util.getCurrentTimeMs = this.util_getCurrentTimeMs_func;
  gadgets.warn = this.util_warn_func;
  delete window[this.NAVIGATE_CALLBACK];
};

GadgetSiteTest.prototype.testGetId = function() {
  var site = new osapi.container.GadgetSite(null, null, {});
  this.assertEquals(osapi.container.Site.prototype.nextUniqueSiteId_ - 1, site.getId());
  site = new osapi.container.GadgetSite(null, null, {});
  this.assertEquals(osapi.container.Site.prototype.nextUniqueSiteId_ - 1, site.getId());
};

GadgetSiteTest.prototype.testNavigateToWithUncachedError = function() {
  var self = this;
  var service = {
    getCachedGadgetMetadata : function(url) {
      self.service_getCachedGadgetMetadata_url = url;
      return null; // not cached
    },
    getGadgetMetadata : function(request, callback) {
      self.service_getGadgetMetadata_request = request;
      self.service_getGadgetMetadata_callback = callback;
      callback(self.newMetadataError(self.GADGET_URL, 'na'));
    }
  };
  var navigateToCallback = function(gadgetInfo) {
    self.site_navigateToCallback_gadgetInfo = gadgetInfo;
  };
  var site = this.newGadgetSite(service, 'nc');
  site.navigateTo(this.GADGET_URL, {}, {}, navigateToCallback);
  this.assertEquals('Failed to navigate for gadget ' + this.GADGET_URL + '.',
      this.util_warn_value);
  this.assertTrue(this.window_navigateCallback_timingInfo['id'] >= 0);
  this.assertEquals(this.GADGET_URL, this.window_navigateCallback_timingInfo['url']);
  this.assertEquals(100, this.window_navigateCallback_timingInfo['start']);
  this.assertEquals(1, this.window_navigateCallback_timingInfo['xrt']); // not cached
  this.assertEquals(this.GADGET_URL, this.service_getCachedGadgetMetadata_url);
  this.assertTrue(this.site_navigateToCallback_gadgetInfo['error'] != null);
};

GadgetSiteTest.prototype.testNavigateToWithCachedError = function() {
  var self = this;
  var service = {
    getCachedGadgetMetadata : function(url) {
      self.service_getCachedGadgetMetadata_url = url;
      return self.newMetadataError(self.GADGET_URL, 'na'); // cached
    },
    getGadgetMetadata : function(request, callback) {
      self.service_getGadgetMetadata_request = request;
      self.service_getGadgetMetadata_callback = callback;
      callback(self.newMetadataError(self.GADGET_URL, 'na'));
    }
  };
  var navigateToCallback = function(gadgetInfo) {
    self.site_navigateToCallback_gadgetInfo = gadgetInfo;
  };
  var site = this.newGadgetSite(service, this.NAVIGATE_CALLBACK);
  site.navigateTo(this.GADGET_URL, {}, {}, navigateToCallback);
  this.assertEquals('Failed to navigate for gadget ' + this.GADGET_URL + '.',
      this.util_warn_value);
  this.assertTrue(this.window_navigateCallback_timingInfo['id'] >= 0);
  this.assertEquals(this.GADGET_URL, this.window_navigateCallback_timingInfo['url']);
  this.assertEquals(100, this.window_navigateCallback_timingInfo['start']);
  this.assertEquals(0, this.window_navigateCallback_timingInfo['xrt']); // cached
  this.assertEquals(this.GADGET_URL, this.service_getCachedGadgetMetadata_url);
  this.assertTrue(this.site_navigateToCallback_gadgetInfo['error'] != null);
};

GadgetSiteTest.prototype.newMetadataError = function(url, message) {
  var response = {};
  response[url] = {};
  response[url]['error'] = message;
  return response;
};

GadgetSiteTest.prototype.newGadgetSite = function(service, navigateCallback) {
  return new osapi.container.GadgetSite(null, service, {
    'navigateCallback' : navigateCallback
  });
};

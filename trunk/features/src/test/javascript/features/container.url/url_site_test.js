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
 * @fileoverview Tests for the containers URL site.
 */

function UrlSiteTest(name) {
  TestCase.call(this, name);
}

UrlSiteTest.inherits(TestCase);

UrlSiteTest.prototype.setUp = function() {

};

UrlSiteTest.prototype.tearDown = function() {
  
};

UrlSiteTest.prototype.testNew = function() {
  var args = {
    "urlEl" : {}
  };
  var site = new osapi.container.UrlSite(null, null, args);
  this.assertEquals(osapi.container.Site.prototype.nextUniqueSiteId_ - 1, site.getId());
  this.assertTrue(!site.getActiveSiteHolder());
  var site2 = new osapi.container.UrlSite(null, null, args);
  this.assertEquals(osapi.container.Site.prototype.nextUniqueSiteId_ - 1, site2.getId());
  this.assertTrue(!site.getActiveSiteHolder());
};

UrlSiteTest.prototype.testRenderNoParams = function() {
  var el = {};
  var args = {
    "urlEl" : el
  };
  var url = "http://example.com";
  var site = new osapi.container.UrlSite(null, null, args);
  site.render(url, {});
  this.assertNotNull(site.getActiveSiteHolder());
  this.assertEquals('<iframe' + ' marginwidth="0"' + ' hspace="0"' + ' title="default title"' + ' frameborder="0"'
          + ' scrolling="auto"' + ' marginheight="0"' + ' vspace="0"' + ' id="__url_' + site.getId() + '"'
          + ' name="__url_' + site.getId() + '"' + ' src="http://example.com"' + ' ></iframe>', el.innerHTML);
};

UrlSiteTest.prototype.testRenderWithParams = function() {
  var el = {};
  var args = {
    "urlEl" : el
  };
  var url = "http://example.com";
  var site = new osapi.container.UrlSite(null, null, args);
  site.render(url, {
          "class" : "myClass",
          "width" : 54,
          "height" : 104
  });
  this.assertNotNull(site.getActiveSiteHolder());
  this.assertEquals('<iframe' + ' marginwidth="0"' + ' hspace="0"' + ' height="104"'
          + ' title="default title"' + ' frameborder="0"' + ' scrolling="auto"' + ' class="myClass"' + ' marginheight="0"'
          + ' vspace="0"' + ' id="__url_' + site.getId() + '"' + ' width="54"' + ' name="__url_' + site.getId() + '"'
          + ' src="http://example.com"' + ' ></iframe>', el.innerHTML);
};

UrlSiteTest.prototype.testClose = function() {
  var el = {
          "firstChild" : "firstChild",
          "removeChild" : function(child) {
            this.firstChild = "removedFirstChild"
          }
  };
  var args = {
    "urlEl" : el
  };
  var site = new osapi.container.UrlSite(null, null, args);
  site.render("http://example.com", {});
  site.close();
  this.assertEquals("removedFirstChild", el.firstChild);
}

UrlSiteTest.prototype.testParentId = function() {
  var el = {};
  var args = {
    "urlEl" : el
  };
  var site = new osapi.container.UrlSite(null, null, args);
  site.setParentId(1);
  this.assertEquals(1, site.getParentId());
}

UrlSiteTest.prototype.testSetWidth = function() {
  var el = {};
  var args = {
    "urlEl" : el
  };
  var site = new osapi.container.UrlSite(null, null, args);
  this.assertEquals(site, site.setWidth(50));

  el = {};
  args.urlEl = el;
  site = new osapi.container.UrlSite(null, null, args);
  site.render("http://example.com", {});
  this.assertEquals(site, site.setWidth(50));

  el = {
    "firstChild" : null
  };
  args.urlEl = el;
  site = new osapi.container.UrlSite(null, null, args);
  site.render("http://example.com", {});
  this.assertEquals(site, site.setWidth(50));

  el = {
    "firstChild" : {
      "style" : {
        "width" : 0
      }
    }
  };
  args.urlEl = el;
  site = new osapi.container.UrlSite(null, null, args);
  site.render("http://example.com", {});
  site.setWidth(50);
  this.assertEquals("50px", el.firstChild.style.width);
}

UrlSiteTest.prototype.testSetHeight = function() {
  var el = {};
  var args = {
    "urlEl" : el
  };
  var site = new osapi.container.UrlSite(null, null, args);
  this.assertEquals(site, site.setHeight(50));

  el = {};
  args.urlEl = el;
  site = new osapi.container.UrlSite(null, null, args);
  site.render("http://example.com", {});
  this.assertEquals(site, site.setHeight(50));

  el = {
    "firstChild" : null
  };
  args.urlEl = el;
  site = new osapi.container.UrlSite(null, null, args);
  site.render("http://example.com", {});
  this.assertEquals(site, site.setHeight(50));

  el = {
    "firstChild" : {
      "style" : {
        "height" : 0
      }
    }
  };
  args.urlEl = el;
  site = new osapi.container.UrlSite(null, null, args);
  site.render("http://example.com", {});
  site.setHeight(50);
  this.assertEquals("50px", el.firstChild.style.height);
}
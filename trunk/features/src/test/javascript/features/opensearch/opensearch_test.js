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
 * Unittests for the opensearch feature
 */
function OpenSearchTest(name) {
  TestCase.call(this, name);
}

OpenSearchTest.inherits(TestCase);

OpenSearchTest.prototype.setUp = function() {
  this.apiUri = window.__API_URI;
  window.__API_URI = shindig.uri('http://shindig.com');
  this.containerUri = window.__CONTAINER_URI;
  window.__CONTAINER_URI = shindig.uri('http://container.com');
  this.shindigContainerGadgetSite = osapi.container.GadgetSite;
  this.shindigContainerUrlSite = osapi.container.UrlSite;
  this.gadgetsRpc = gadgets.rpc;
};

OpenSearchTest.prototype.tearDown = function() {
  window.__API_URI = this.apiUri;
  window.__CONTAINER_URI = this.containerUri;
  osapi.container.GadgetSite = this.shindigContainerGadgetSite;
  osapi.container.UrlSite = this.shindigContainerUrlSite;
  gadgets.rpc = this.gadgetsRpc;
};

setDescriptions = function(container) {
  container.opensearch
  .setDescriptions_( {
    "http://hosting.gmodules.com/ig/gadgets/file/109228598702359180066/twitterfinal.xml" : {
    "OpenSearchDescription" : {
    "@xmlns" : "http://a9.com/-/spec/opensearch/1.1/",
    "#text" : [ "\u000a ", "\u000a ", "\u000a ", "\u000a ", "\u000a ",
                "\u000a ", "\u000a\u000a" ],
                "ShortName" : "Twitter Search",
                "Description" : "Realtime Twitter Search",
                "Url" : {
    "@type" : "application/atom+xml",
    "@method" : "get",
    "@template" : "http://search.twitter.com/search.atom?q={searchTerms}"
  },
  "Image" : {
    "@width" : "16",
    "@height" : "16",
    "#text" : "http://search.twitter.com/favicon.png"
  },
  "InputEncoding" : "UTF-8",
  "SearchForm" : "http://search.twitter.com/"
  }
  },
  "http://hosting.gmodules.com/ig/gadgets/file/109228598702359180066/myspacefinal.xml" : {
    "OpenSearchDescription" : {
    "@xmlns" : "http://a9.com/-/spec/opensearch/1.1/",
    "#text" : [ "\u000a ", "\u000a ", "\u000a ", "\u000a ", "\u000a ",
                "\u000a ", "\u000a" ],
                "ShortName" : "MySpace Video",
                "Description" : "Search MySpace videos.",
                "Tags" : "myspace opensearch search video",
                "Image" : {
    "@height" : "16",
    "@width" : "16",
    "@type" : "image/x-icon",
    "#text" : "http://www.myspace.com/favicon.ico"
  },
  "Url" : {
    "@type" : "application/atom+xml",
    "@xmlns:myspace" : "http://api.myspace.com/-/opensearch/extensions/1.0/",
    "@template" : "http://api.myspace.com/opensearch/videos?format=xml&searchTerms={searchTerms}"
  },
  "Attribution" : "Search data Copyright 2003-2010 MySpace.com. All Rights Reserved."
  }
  },
  "http://hosting.gmodules.com/ig/gadgets/file/109228598702359180066/myspacemultitemplate.xml" : {
    "OpenSearchDescription" : {
    "@xmlns" : "http://a9.com/-/spec/opensearch/1.1/",
    "#text" : [ "\u000a ", "\u000a ", "\u000a ", "\u000a ", "\u000a ",
                "\u000a ", "\u000a\u000a ", "\u000a ", "\u000a" ],
                "ShortName" : "MySpace Video",
                "Description" : "Search MySpace videos.",
                "Tags" : "myspace opensearch search video",
                "Image" : {
    "@height" : "16",
    "@width" : "16",
    "@type" : "image/x-icon",
    "#text" : "http://www.myspace.com/favicon.ico"
  },
  "Url" : [
           {
             "@type" : "text/html",
             "@xmlns:myspace" : "http://api.myspace.com/-/opensearch/extensions/1.0/",
             "@template" : "http://searchservice.myspace.com/index.cfm?fuseaction=sitesearch.results&type=MySpaceTV&qry={searchTerms}&pg={startPage?}"
           },
           {
             "@type" : "application/atom+json",
             "@xmlns:myspace" : "http://api.myspace.com/-/opensearch/extensions/1.0/",
             "@template" : "http://api.myspace.com/opensearch/videos?format=json&searchTerms={searchTerms}&count={count?}&startPage={startPage?}&tag={myspace:tag?}&videoMode={myspace:videoMode?}&culture={myspace:culture?}"
           },
           {
             "@type" : "application/atom+xml",
             "@xmlns:myspace" : "http://api.myspace.com/-/opensearch/extensions/1.0/",
             "@template" : "http://api.myspace.com/opensearch/videos?format=xml&searchTerms={searchTerms}&count={count?}&startPage={startPage?}&tag={myspace:tag?}&videoMode={myspace:videoMode?}&culture={myspace:culture?}"
           } ],
           "Attribution" : "Search data Copyright 2003-2010 MySpace.com. All Rights Reserved."
  }
  }
  });
}

//The following two methods should be uncommented for unit testing after 
//uncommenting the setDescriptions method in the opensearch.js code.
/*OpenSearchTest.prototype.testUrls = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container();
  setDescriptions(container);
  urls = container.opensearch.getOpenSearchURLs();
  this.assertEquals('all urls', 5, urls.length);
  urls = container.opensearch.getOpenSearchURLs("application/atom+xml");
  this.assertEquals('atom urls', 3, urls.length);
  urls = container.opensearch.getOpenSearchURLs("text/html");
  this.assertEquals('text/html urls', 1, urls.length);
  urls = container.opensearch.getOpenSearchURLs("application/shindig");
  this.assertEquals('bad type urls', 0, urls.length);

};*/

/*OpenSearchTest.prototype.testDescriptions = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container();

  setDescriptions(container);

  descriptions = container.opensearch.getOpenSearchDescriptions();
  this.assertEquals('all descriptions', 3, descriptions.length);
  descriptions = container.opensearch
  .getOpenSearchDescriptions("application/atom+xml");
  this.assertEquals('atom descriptions', 3, descriptions.length);
  descriptions = container.opensearch.getOpenSearchDescriptions("text/html");
  this.assertEquals('text/html descriptions', 1, descriptions.length);
  descriptions = container.opensearch
  .getOpenSearchDescriptions("application/shindig");
  this.assertEquals('bad type descriptions', 0, descriptions.length);
};*/

OpenSearchTest.prototype.testCallbacks = function() {
  this.setupGadgetsRpcRegister();
  var container = new osapi.container.Container();
  callback = function() {
  };
  //add callback to the container
  container.opensearch.addOpenSearchCallback(callback);
  //remove callback
  this.assertTrue(container.opensearch.removeOpenSearchCallback(callback));
  //check that the removed callback is no longer there. 
  this.assertFalse(container.opensearch.removeOpenSearchCallback(callback));
};

OpenSearchTest.prototype.setupGadgetSite = function(id, gadgetInfo,
    gadgetHolder) {
  var self = this;
  osapi.container.GadgetSite = function() {
    return {
      'getId' : function() {
      return id;
    },
    'navigateTo' : function(gadgetUrl, viewParams, renderParams, func) {
      self.site_navigateTo_gadgetUrl = gadgetUrl;
      self.site_navigateTo_viewParams = viewParams;
      self.site_navigateTo_renderParams = renderParams;
      func(gadgetInfo);
    },
    'getActiveSiteHolder' : function() {
      return gadgetHolder;
    }
    };
  };
};

OpenSearchTest.prototype.setupGadgetsRpcRegister = function() {
  gadgets.rpc = {
      register : function() {
      }
  }
};

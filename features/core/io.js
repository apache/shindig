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

var gadgets = gadgets || {};

/**
 * @fileoverview Provides remote content retrieval facilities.
 *     Available to every gadget.
 */

/**
 * @static
 * @class Provides remote content retrieval functions.
 * @name gadgets.io
 */

gadgets.io = function() {
  /**
   * Holds configuration-related data such as proxy urls.
   */
  var config = {};

  /**
   * Internal facility to create an xhr request.
   */
  function makeXhr() {
    if (window.XMLHttpRequest) {
      return new XMLHttpRequest();
    } else if (window.ActiveXObject) {
      var x = new ActiveXObject("Msxml2.XMLHTTP");
      if (!x) {
        x = new ActiveXObject("Microsoft.XMLHTTP");
      }
      return x;
    }
  }

  var UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";

  /**
   * Handles XHR callback processing.
   *
   * @param {String} url
   * @param {Function} callback
   * @param {Object} params
   * @param {Object} xobj
   */
  function processResponse(url, callback, params, xobj) {
    if (xobj.readyState !== 4) {
      return;
    }
    if (xobj.status !== 200) {
      // TODO Need to work on standardizing errors
      callback({errors : ["Error " + xobj.status] });
      return;
    }
    var txt = xobj.responseText;
    // remove unparseable cruft.
    // TODO: really remove this by eliminating it. It's not any real security
    //    to begin with, and we can solve this problem by using post requests
    //    and / or passing the url in the http headers.
    txt = txt.substr(UNPARSEABLE_CRUFT.length);
    var data = gadgets.json.parse(txt);
    data = data[url];
    var resp = {
     text: data.body,
     errors: []
    };
    switch (params.CONTENT_TYPE) {
      case "JSON":
      case "FEED":
        resp.data = gadgets.json.parse(resp.text);
        if (!resp.data) {
          resp.errors.push("failed to parse JSON");
          resp.data = null;
        }
        break;
      case "DOM":
        var dom;
        if (window.ActiveXObject) {
          dom = new ActiveXObject("Microsoft.XMLDOM");
          dom.async = false;
          dom.validateOnParse = false;
          dom.resolveExternals = false;
          if (!dom.loadXML(resp.text)) {
            resp.errors.push("failed to parse XML");
          } else {
            resp.data = dom;
          }
        } else {
          var parser = new DOMParser();
          dom = parser.parseFromString(resp.text, "text/xml");
          if ("parsererror" == dom.documentElement.nodeName) {
            resp.errors.push("failed to parse XML");
          } else {
            resp.data = dom;
          }
        }
        break;
      default:
        resp.data = resp.text;
        break;
    }
    callback(resp);
  }

  return /** @scope gadgets.io */ {
    /**
     * Fetches content from the provided URL and feeds that content into the
     * callback function.
     *
     * Example:
     * <pre>
     * gadgets.io.makeRequest(url, fn,
     *    {contentType: gadgets.io.ContentType.FEED});
     * </pre>
     *
     * @param {String} url The URL where the content is located
     * @param {Function} callback The function to call with the data from the
     *     URL once it is fetched
     * @param {Map.&lt;gadgets.io.RequestParameters, Object&gt;} opt_params
     *     Additional
     *     <a href="gadgets.io.RequestParameters.html">parameters</a>
     *     to pass to the request
     *
     * @member gadgets.io
     */
    makeRequest : function (url, callback, opt_params) {
      // TODO: This method also needs to respect all members of
      // gadgets.io.RequestParameters, and validate them.
      var xhr = makeXhr();
      var params = opt_params || {};
      var newUrl = config.jsonProxyUrl.replace("%url%",
          encodeURIComponent(url));

      // Check if authorization is requested
      if (params.AUTHORIZATION && params.AUTHORIZATION !== "NONE") {
        newUrl += "&authz=" + params.AUTHORIZATION.toLowerCase();
        // Add the security-token if available
        if (gadgets.util.getUrlParameters()["st"]) {
          newUrl += "&st=" + gadgets.util.getUrlParameters()["st"];
        }
      }
      // TODO: Fetcher cannot distinguish between GET & POST yet.
      xhr.open(params.METHOD || "GET", newUrl, true);
      if (callback) {
        xhr.onreadystatechange = gadgets.util.makeClosure(null,
            processResponse, url, callback, params, xhr);
      }
      if (params.METHOD === "POST") {
        xhr.setRequestHeader('Content-Type',
            'application/x-www-form-urlencoded');
        if (params.POST_DATA) {
          xhr.send("postData=" + encodeURIComponent(params.POST_DATA));
        } else {
          xhr.send("postData=");
        }
      } else {
        xhr.send(null);
      }
    },

    /**
     * Converts an input object into a URL-encoded data string.
     * (key=value&amp;...)
     *
     * @param {Object} fields The post fields you wish to encode
     * @return {String} The processed post data; this will include a trailing
     *    ampersand (&)
     *
     * @member gadgets.io
     */
    encodeValues : function (fields) {
      var buf = [];
      for (var i in fields) {
        buf.push(encodeURIComponent(i));
        buf.push("=");
        buf.push(encodeURIComponent(fields[i]));
        buf.push("&");
      }
      return buf.join("");
    },

    /**
     * Gets the proxy version of the passed-in URL.
     *
     * @param {String} url The URL to get the proxy URL for
     * @return {String} The proxied version of the URL
     *
     * @member gadgets.io
     */
    getProxyUrl : function (url) {
      return config.proxyUrl.replace("%url%", encodeURIComponent(url));
    },

    /**
     * Initializes fetchers
     *
     * @param {Object} configuration Configuration settings
     *     Required:
     *       - proxyUrl: The url for content proxy requests. Include %url%
     *           as a placeholder for the actual url.
     *       - jsonProxyUrl: The url for dynamic proxy requests. Include %url%
     *           as a placeholder for the actual url.
     * @private
     */
    init : function (configuration) {
      config = configuration;
      if (!config.proxyUrl || !config.jsonProxyUrl) {
        throw new Error("proxyUrl and jsonProxyUrl are required.");
      }
    }
  };
}();

gadgets.io.RequestParameters = gadgets.util.makeEnum([
  "METHOD",
  "CONTENT_TYPE",
  "POST_DATA",
  "HEADERS",
  "AUTHORIZATION",
  "NUM_ENTRIES",
  "GET_SUMMARIES"
]);

// PUT, DELETE, and HEAD not supported currently.
gadgets.io.MethodType = gadgets.util.makeEnum([
  "GET", "POST", "PUT", "DELETE", "HEAD"
]);

gadgets.io.ContentType = gadgets.util.makeEnum([
  "TEXT", "DOM", "JSON", "FEED"
]);

gadgets.io.AuthorizationType = gadgets.util.makeEnum([
  "NONE", "SIGNED", "AUTHENTICATED"
]);